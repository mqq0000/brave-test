package com.bravetest.security;

import com.bravetest.common.BusinessException;
import com.bravetest.common.UserContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 登录拦截器：校验 token + Redis 黑名单，写入 UserContext
 */
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new BusinessException(401, "未登录或缺少令牌");
        }
        String token = auth.substring(7);
        Claims claims;
        try {
            claims = jwtUtil.parse(token);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(401, "令牌无效或已过期");
        }
        // 登出黑名单校验
        Boolean black = redisTemplate.hasKey(BLACKLIST_PREFIX + token);
        if (Boolean.TRUE.equals(black)) {
            throw new BusinessException(401, "令牌已注销，请重新登录");
        }
        UserContext.set(new UserContext.UserInfo(
                Long.valueOf(claims.getSubject()),
                claims.get("username", String.class),
                claims.get("role", String.class)));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
