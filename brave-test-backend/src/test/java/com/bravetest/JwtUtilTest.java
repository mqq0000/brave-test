package com.bravetest;

import com.bravetest.security.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JWT 工具单元测试：签发/解析/过期/篡改
 */
class JwtUtilTest {

    private static final String SECRET = "unit-test-secret-key-0123456789abcdef0123456789abcdef";
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expireHours", 2L);
    }

    @Test
    void createAndParse_roundtrip() {
        String token = jwtUtil.create(42L, "勇者", "ADVENTURER");
        Claims claims = jwtUtil.parse(token);
        assertEquals("42", claims.getSubject());
        assertEquals("勇者", claims.get("username", String.class));
        assertEquals("ADVENTURER", claims.get("role", String.class));
        // 2小时有效期
        assertTrue(claims.getExpiration().getTime() - claims.getIssuedAt().getTime() == 2 * 3600_000L);
    }

    @Test
    void parse_expiredToken_throws() {
        JwtUtil expired = new JwtUtil();
        ReflectionTestUtils.setField(expired, "secret", SECRET);
        ReflectionTestUtils.setField(expired, "expireHours", -1L);
        String token = expired.create(1L, "u", "ADVENTURER");
        assertThrows(ExpiredJwtException.class, () -> jwtUtil.parse(token));
    }

    @Test
    void parse_tamperedSignature_throws() {
        String token = jwtUtil.create(1L, "u", "ADVENTURER");
        JwtUtil other = new JwtUtil();
        ReflectionTestUtils.setField(other, "secret", SECRET + "-different");
        assertThrows(JwtException.class, () -> other.parse(token));
    }
}
