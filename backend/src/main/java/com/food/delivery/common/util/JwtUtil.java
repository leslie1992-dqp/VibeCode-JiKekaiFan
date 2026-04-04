package com.food.delivery.common.util;

import com.food.delivery.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    private final AppProperties appProperties;

    public JwtUtil(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    private String secret() {
        return appProperties.getJwt().getSecret();
    }

    private int expireHours() {
        return appProperties.getJwt().getExpireHours();
    }

    public String generateToken(Long userId, String mobile) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("mobile", mobile);
        Date now = new Date();
        Date expire = new Date(now.getTime() + expireHours() * 60L * 60L * 1000L);
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expire)
                .signWith(Keys.hmacShaKeyFor(secret().getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secret().getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Long parseUserId(String token) {
        return toLong(parseClaims(token).get("userId"));
    }

    private static Long toLong(Object value) {
        if (value == null) {
            throw new MalformedJwtException("missing userId claim");
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(value.toString());
    }
}
