package com.wistrain.cloud.service;

import com.wistrain.cloud.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端数据看板（US-503）：汇总统计 + 近 7 日趋势
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final ProjectMapper projectMapper;
    private final MaterialMapper materialMapper;
    private final MemberMapper memberMapper;
    private final AgentMapper agentMapper;
    private final SessionMapper sessionMapper;
    private final UserMapper userMapper;

    public Map<String, Object> summary(Long deptId) {
        String since = LocalDate.now().minusDays(6).atStartOfDay().toLocalDate().toString();
        Map<String, Object> m = new HashMap<>();
        m.put("projectCount", projectMapper.countAll(deptId));
        m.put("activeProjects", projectMapper.countActive(deptId));
        m.put("employeeCount", userMapper.countByDept(deptId));
        m.put("memberCount", memberMapper.countByDept(deptId));
        m.put("materialCount", materialMapper.countByDept(deptId));
        m.put("agentCount", agentMapper.countByDept(deptId, null));
        m.put("sessionCount7d", sessionMapper.countSince(deptId, since));
        return m;
    }

    /** 近 7 日趋势：每日加入人数 + 会话数（补齐缺失日期为 0） */
    public List<Map<String, Object>> trend(Long deptId) {
        String since = LocalDate.now().minusDays(6).atStartOfDay().toLocalDate().toString();
        Map<String, Long> join = toMap(memberMapper.countJoinTrend(deptId, since));
        Map<String, Long> chat = toMap(sessionMapper.countTrend(deptId, since));
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            String day = LocalDate.now().minusDays(i).toString();
            result.add(Map.of("day", day, "joins", join.getOrDefault(day, 0L), "chats", chat.getOrDefault(day, 0L)));
        }
        return result;
    }

    private Map<String, Long> toMap(List<Map<String, Object>> rows) {
        Map<String, Long> m = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String d = String.valueOf(row.get("d"));
            long c = ((Number) row.get("c")).longValue();
            m.put(d, c);
        }
        return m;
    }
}
