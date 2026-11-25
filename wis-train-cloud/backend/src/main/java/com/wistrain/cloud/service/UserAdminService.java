package com.wistrain.cloud.service;

import com.wistrain.cloud.common.BizException;
import com.wistrain.cloud.dto.PageResult;
import com.wistrain.cloud.entity.User;
import com.wistrain.cloud.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;

/**
 * 管理端：员工管理（US-501）：仅本部门，不能删除自己（NF-06）
 */
@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder encoder;

    public PageResult<User> page(Long deptId, String keyword, long pageNum, long pageSize) {
        long offset = (pageNum - 1) * pageSize;
        List<User> items = userMapper.pageEmployee(deptId, keyword, offset, pageSize);
        long total = userMapper.countEmployee(deptId, keyword);
        items.forEach(u -> u.setPwd(null));   // NF-04：列表不回传密码密文
        return PageResult.of(total, items);
    }

    public User create(Long deptId, User u) {
        if (userMapper.findByLogname(u.getLogname()) != null) {
            throw new BizException("登录名已存在");
        }
        String rawPwd = (u.getPwd() == null || u.getPwd().isBlank()) ? randomPwd() : u.getPwd();
        String encoded = encoder.encode(rawPwd);   // NF-04：BCrypt 密文落库
        u.setRole(2);            // 管理端新增一律为员工
        u.setState(1);
        u.setDeptId(deptId);
        u.setPwd(encoded);
        userMapper.insert(u);
        u.setPwd(null);          // 响应不回传密文
        return u;
    }

    /** 重置密码（US-104）：返回一次性明文临时密码 */
    public String resetPwd(Long deptId, Long id) {
        User user = requireOwn(deptId, id);
        String temp = randomPwd();
        userMapper.updatePwd(user.getUserId(), encoder.encode(temp));
        return temp;
    }

    public void updateState(Long deptId, Long id, Integer state) {
        User user = requireOwn(deptId, id);
        if (user.getUserId().equals(id) && state == 0) {
            throw new BizException("不能禁用自己");
        }
        userMapper.updateState(id, state);
    }

    private User requireOwn(Long deptId, Long id) {
        User user = userMapper.findById(id);
        if (user == null || !user.getDeptId().equals(deptId) || user.getRole() != 2) {
            throw new BizException(403, "员工不存在或无权限");
        }
        return user;
    }

    private String randomPwd() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(r.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
