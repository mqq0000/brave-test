package com.bravetest.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bravetest.common.BusinessException;
import com.bravetest.dto.LoginDTO;
import com.bravetest.dto.LoginVO;
import com.bravetest.dto.RegisterDTO;
import com.bravetest.entity.Adventurer;
import com.bravetest.entity.User;
import com.bravetest.mapper.AdventurerMapper;
import com.bravetest.mapper.UserMapper;
import com.bravetest.security.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 认证服务：注册 / 登录 / 登出
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    private final UserMapper userMapper;
    private final AdventurerMapper adventurerMapper;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public Long register(RegisterDTO dto) {
        Long exists = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (exists > 0) {
            throw new BusinessException("用户名已被占用");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setRole("ADVENTURER");
        user.setStatus(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);

        // 初始冒险者档案：D 级、污染值 0、萌芽正常、金币 0
        Adventurer adv = new Adventurer();
        adv.setUserId(user.getId());
        adv.setRankLevel("D");
        adv.setCompletedCount(0);
        adv.setPollutionValue(0);
        adv.setSproutStatus("NORMAL");
        adv.setGoldBalance(0L);
        adv.setKnowledgeBanned(0);
        adv.setCreatedAt(LocalDateTime.now());
        adv.setUpdatedAt(LocalDateTime.now());
        adventurerMapper.insert(adv);
        return user.getId();
    }

    public LoginVO login(LoginDTO dto) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 1) {
            throw new BusinessException(403, "账号已被禁用");
        }
        String token = jwtUtil.create(user.getId(), user.getUsername(), user.getRole());
        return new LoginVO(token, user.getId(), user.getRole());
    }

    public void logout(String token) {
        Claims claims = jwtUtil.parse(token);
        long remainMs = claims.getExpiration().getTime() - System.currentTimeMillis();
        if (remainMs > 0) {
            redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "1",
                    Duration.ofMillis(remainMs));
        }
    }
}
