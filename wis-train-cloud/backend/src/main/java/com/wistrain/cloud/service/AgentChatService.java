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

        new Thread(() -> {
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
        }, "agent-stream").start();
        return emitter;
    }

    /** Mock 流式：按 4 字/80ms 切片返回，模拟真实打字机效果 */
    private void streamMock(SseEmitter emitter, Agent agent, String message) throws Exception {
        String reply = mockReply(agent.getAgentName(), message);
        for (int i = 0; i < reply.length(); i += 4) {
            String chunk = reply.substring(i, Math.min(i + 4, reply.length()));
            emitter.send(SseEmitter.event().name("message").data(chunk));
            Thread.sleep(80);
        }
    }

    /**
     * Mock 问答知识库：关键词匹配内置回复（课程演示用）。
     * 按条目顺序匹配，命中即返回；未命中走兜底话术。
     */
    private String mockReply(String agentName, String message) {
        String q = message == null ? "" : message.toLowerCase();
        for (MockQa qa : MOCK_QA_LIST) {
            for (String k : qa.keywords) {
                if (q.contains(k)) {
                    return qa.reply;
                }
            }
        }
        return "【" + agentName + "】这个问题暂时超出我的知识范围啦。\n"
                + "你可以这样问我：\n"
                + "· 「你是谁 / 你能做什么」——了解我的能力\n"
                + "· 「怎么解锁智能体」——权限规则说明\n"
                + "· 「怎么搭建智能客服」——课程核心内容\n"
                + "· 「知识库怎么建」——实战步骤讲解";
    }

    /** 单条 Mock 问答：关键词组 + 固定回复 */
    private record MockQa(String[] keywords, String reply) { }

    private static final List<MockQa> MOCK_QA_LIST = List.of(
        new MockQa(new String[]{"你是谁", "介绍下你", "介绍一下", "self"},
            "你好！我是智培云（WisTrain-Cloud）的 AI 实操助手。\n"
            + "我由部门管理员统一接入、按培训项目权限解锁——你只有完成对应课程，才能和我对话。\n"
            + "我可以陪你练习客服话术、解答课程疑问、演示流式对话效果。"),
        new MockQa(new String[]{"你能做什么", "会什么", "能干", "功能", "用途"},
            "我的主要能力有：\n"
            + "1. 课程答疑：围绕「智能客服实战训练营」的 3 节课内容进行问答；\n"
            + "2. 话术陪练：模拟客户咨询，练习应答流程；\n"
            + "3. 知识查询：新品规格、价格政策、售后规范速查。\n"
            + "试试问我「怎么搭建智能客服」！"),
        new MockQa(new String[]{"解锁", "权限", "为什么不能用", "打不开", "403"},
            "智能体的解锁由培训进度驱动：\n"
            + "· 规则一「完成解锁」：学完项目全部资料（进度 100%）自动解锁；\n"
            + "· 规则二「阈值解锁」：进度达到管理员设定的百分比（如 60%）解锁。\n"
            + "解锁判定在后端实时计算，管理端修改规则立即生效。学习更多资料，我就会点亮！"),
        new MockQa(new String[]{"搭建", "怎么建客服", "流程", "上线"},
            "搭建智能客服的四步流程（对应课程第 1-3 课）：\n"
            + "① 需求分析：梳理高频客户问题，确定客服边界；\n"
            + "② 知识库构建：整理产品文档，切分为问答对并标注；\n"
            + "③ 智能体编排：配置提示词、开场白与兜底话术，绑定知识库；\n"
            + "④ 测试上线：灰度试运行，观察命中率后全量发布。"),
        new MockQa(new String[]{"知识库", "文档", "语料"},
            "知识库构建三要点：\n"
            + "1. 语料质量优先——优先使用正式产品文档，删除过期内容；\n"
            + "2. 问答对切分——一个知识点一条，控制在 200 字以内，检索更准；\n"
            + "3. 持续运营——每周根据「未命中问题」补充语料。\n"
            + "课程第 2 课视频里有完整演示。"),
        new MockQa(new String[]{"你好", "您好", "hi", "hello", "在吗"},
            "你好，很高兴见到你！我是你的培训实操助手。\n"
            + "可以先问我「你是谁」或「你能做什么」，也可以直接提出课程相关问题。"),
        new MockQa(new String[]{"谢谢", "感谢", "thanks"},
            "不客气！学习路上有我陪跑。\n"
            + "记得完成剩余课程资料——进度达标后还有新的智能体等你解锁哦！"),
        new MockQa(new String[]{"培训", "项目", "课程"},
            "当前培训安排：\n"
            + "· 智能客服实战训练营（3 份资料，进行中）；\n"
            + "· 新产品知识库培训（2 份资料，进行中）；\n"
            + "· 信息安全合规培训（已结课归档）。\n"
            + "在「学习中心」逐份标记已学，进度会实时更新并联动智能体解锁。")
    );

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
