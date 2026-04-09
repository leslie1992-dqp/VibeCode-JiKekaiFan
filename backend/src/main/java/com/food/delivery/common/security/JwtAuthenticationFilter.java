package com.food.delivery.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.food.delivery.common.response.Result;
import com.food.delivery.common.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) {
            return true;
        }
        // 仅对需要登录的接口做鉴权：用户域 + 购物车域 + 商家待下单 + 订单
        return !(path.startsWith("/api/v1/users/")
                || path.startsWith("/api/v1/cart/")
                || path.startsWith("/api/v1/merchant-drafts/")
                || path.startsWith("/api/v1/merchant-seckill-coupons/")
                || path.startsWith("/api/v1/orders")
                || path.startsWith("/api/v1/rider/")
                || path.startsWith("/api/v1/merchant/orders/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        String token = null;
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            token = header.substring(BEARER_PREFIX.length()).trim();
        } else if (request.getRequestURI() != null && request.getRequestURI().endsWith("/delivery/subscribe")) {
            token = request.getParameter("token");
        }
        if (token == null || token.isBlank()) {
            writeUnauthorized(response, "missing or invalid Authorization header");
            return;
        }
        try {
            Long userId = jwtUtil.parseUserId(token);
            request.setAttribute(AuthConstants.ATTR_USER_ID, userId);
        } catch (Exception e) {
            writeUnauthorized(response, "invalid or expired token");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        byte[] body = objectMapper.writeValueAsBytes(Result.fail(40003, message));
        response.getOutputStream().write(body);
    }
}
