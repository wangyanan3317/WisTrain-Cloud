package com.wistrain.cloud.service;

import com.wistrain.cloud.common.BizException;
import com.wistrain.cloud.common.CryptoUtil;
import com.wistrain.cloud.dto.MyAgentVO;
import com.wistrain.cloud.entity.Agent;
import com.wistrain.cloud.entity.AgentSession;
import com.wistrain.cloud.entity.ProjectAgent;
import com.wistrain.cloud.mapper.AgentMapper;
import com.wistrain.cloud.mapper.MemberMapper;
import com.wistrain.cloud.mapper.ProjectAgentMapper;
import com.wistrain.cloud.mapper.SessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 智能体服务（M4）：员工视角解锁列表 + SSE 流式会话
 *
 * 适配策略（设计说明书 2.1 集成层）：
 *  - platform 1/2/3 且配置了 endpoint → OpenAI 兼容流式调用
 *  - 未配置 endpoint（演示环境）→ Mock 流式回复，保证课程演示闭环
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentChatService {

    private final AgentMapper agentMapper;
    private final ProjectAgentMapper projectAgentMapper;
    private final MemberMapper memberMapper;
    private final SessionMapper sessionMapper;
    private final PermissionService permissionService;

    @Value("${wistrain.aes-key}")
    private String aesKey;

    private static final Map<Integer, String> PLATFORMS =
            Map.of(1, "COZE", 2, "DIFY", 3, "OPENAI", 4, "OTHER");

    /** 我的智能体（US-404）：员工所属项目关联的全部智能体 + 实时解锁状态 */
    public List<MyAgentVO> myAgents(Long userId) {
        List<MyAgentVO> result = new ArrayList<>();
        for (Map<String, Object> row : memberMapper.listMy(userId)) {
            Long projectId = ((Number) row.get("projectId")).longValue();
            int progress = row.get("progress") == null ? 0 : ((Number) row.get("progress")).intValue();
            LocalDateTime unlockTime = (LocalDateTime) row.get("unlockTime");
            for (ProjectAgent rule : projectAgentMapper.listByProject(projectId)) {
                Agent agent = agentMapper.findById(rule.getAgentId());
                if (agent == null) {
                    continue;
                }
                MyAgentVO vo = new MyAgentVO();
                vo.setAgentId(agent.getAgentId());
                vo.setAgentName(agent.getAgentName());
                vo.setPlatform(agent.getAgentPlatform());
                vo.setPlatformName(PLATFORMS.getOrDefault(agent.getAgentPlatform(), "OTHER"));
                vo.setAgentDesc(agent.getAgentDesc());
                vo.setEnabled(agent.getAgentState() == 1);
                vo.setUnlocked(agent.getAgentState() == 1 && permissionService.satisfied(rule, progress));
                vo.setProjectId(projectId);
                vo.setProjectName((String) row.get("projectName"));
                vo.setUnlockRule(rule.getUnlockRule());
                vo.setUnlockThreshold(rule.getUnlockThreshold());
                vo.setProgress(progress);
                vo.setUnlockTime(unlockTime);
                result.add(vo);
            }
        }
        return result;
    }

    /** 会话历史（US-405） */
    public List<AgentSession> sessions(Long userId, Long agentId) {
        return sessionMapper.listByUserAndAgent(agentId, userId);
    }

    /**
     * 对话（SSE 流式）：先服务端实时校验解锁资格（NF-08），再转发第三方或 Mock。
     * 只动 transform/opcity 无关——这里输出 event: message（增量）/ done / error。
     */
    public SseEmitter chat(Long userId, Long agentId, Long sessionId, String message) {
        SseEmitter emitter = new SseEmitter(60_000L);

        Agent agent = agentMapper.findById(agentId);
        if (agent == null || agent.getAgentState() == 0) {
            sendError(emitter, "智能体不存在或已停用");
            return emitter;
        }
        // S-04：访问前实时判定，不信任前端
        if (!permissionService.checkAccess(userId, agentId)) {
            sendError(emitter, 403, "完成对应培训项目后解锁");
            return emitter;
        }
        if (message == null || message.isBlank()) {
            sendError(emitter, "消息不能为空");
            return emitter;
        }

        // 会话：未指定则新建，标题取消息前 20 字
        AgentSession session;
        if (sessionId != null) {
            session = sessionMapper.findById(sessionId);
        } else {
            session = new AgentSession();
            session.setAgentId(agentId);
            session.setUserId(userId);
            session.setSessionTitle(message.length() > 20 ? message.substring(0, 20) : message);
            sessionMapper.insert(session);
        }

        String apiKey = CryptoUtil.decrypt(agent.getAgentApiKey(), aesKey);
        boolean mock = agent.getAgentEndpoint() == null || agent.getAgentEndpoint().isBlank();

        Thread.startVirtualThread(() -> {
            try {
                if (mock) {
                    streamMock(emitter, agent, message);
                } else {
                    streamOpenAiCompat(emitter, agent, apiKey, message);
                }
                sessionMapper.touch(session.getSessionId());
                emitter.send(SseEmitter.event().name("done").data(String.valueOf(session.getSessionId())));
                emitter.complete();
            } catch (Exception e) {
                log.warn("智能体流式响应异常: {}", e.getMessage());
                sendError(emitter, "调用失败，请重试");
            }
        });
        return emitter;
    }

    /** Mock 流式：按 12 字/60ms 切片返回，演示解锁-会话闭环 */
    private void streamMock(SseEmitter emitter, Agent agent, String message) throws Exception {
        String reply = "【" + agent.getAgentName() + "】收到你的问题：" + message +
                "。这是演示回复——为本系统内置的 Mock 流式输出。" +
                "在管理端为该智能体配置真实的第三方平台接口地址后，将转发至实际大模型服务。";
        for (int i = 0; i < reply.length(); i += 12) {
            String chunk = reply.substring(i, Math.min(i + 12, reply.length()));
            emitter.send(SseEmitter.event().name("message").data(chunk));
            Thread.sleep(60);
        }
    }

    /** OpenAI 兼容流式：POST /chat/completions stream=true，解析 SSE data 行 */
    private void streamOpenAiCompat(SseEmitter emitter, Agent agent, String apiKey, String message) throws Exception {
        String body = "{\"model\":\"" + (agent.getAgentModel() == null ? "default" : agent.getAgentModel())
                + "\",\"stream\":true,\"messages\":[{\"role\":\"user\",\"content\":"
                + com.fasterxml.jackson.databind.node.TextNode.valueOf(message).toString() + "}]}";
        HttpRequest request = HttpRequest.newBuilder(URI.create(agent.getAgentEndpoint()))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<java.io.InputStream> resp = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (resp.statusCode() != 200) {
            throw new BizException("第三方接口返回 " + resp.statusCode());
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resp.body()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) {
                    continue;
                }
                String payload = line.substring(5).trim();
                if ("[DONE]".equals(payload)) {
                    break;
                }
                String chunk = extractDelta(payload);
                if (!chunk.isEmpty()) {
                    emitter.send(SseEmitter.event().name("message").data(chunk));
                }
            }
        }
    }

    /** 从 OpenAI SSE JSON 中取 choices[0].delta.content（免建 DTO，字符串截取即可） */
    private String extractDelta(String json) {
        int i = json.indexOf("\"content\":\"");
        if (i < 0) {
            return "";
        }
        int start = i + 11;
        StringBuilder sb = new StringBuilder();
        for (int j = start; j < json.length(); j++) {
            char c = json.charAt(j);
            if (c == '"' && json.charAt(j - 1) != '\\') {
                break;
            }
            sb.append(c);
        }
        return sb.toString().replace("\\n", "\n");
    }

    private void sendError(SseEmitter emitter, String msg) {
        sendError(emitter, 1, msg);
    }

    private void sendError(SseEmitter emitter, int code, String msg) {
        try {
            emitter.send(SseEmitter.event().name("error").data(msg + "#" + code));
            emitter.complete();
        } catch (Exception ignored) {}
    }
}
