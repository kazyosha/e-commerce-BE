package com.c05.kaz.ecommercebackend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    // Các endpoint không cần JWT
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/register/customer",
            "/api/auth/register/supplier",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/public/"
    };

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getServletPath();

        // Bỏ qua các endpoint public + preflight
        if (isPublic(path) || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        // Không có token -> cho qua, SecurityConfig sẽ chặn nếu endpoint yêu cầu auth
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);
        String username;

        try {
            username = jwtService.extractUsername(jwt);
        } catch (Exception e) {
            // Token lỗi -> không set auth, để SecurityConfig xử lý tiếp
            filterChain.doFilter(request, response);
            return;
        }

        // Nếu chưa có auth trong context thì xác thực token
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                boolean valid = false;
                try {
                    valid = jwtService.isTokenValid(jwt, userDetails);
                } catch (Exception ignored) {
                }

                if (valid) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (Exception ignored) {
                // không tìm thấy user hoặc lỗi -> không set auth
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublic(String path) {
        if (path == null) return false;

        // Cho toàn bộ /api/public/**
        if (path.startsWith("/api/public/")) return true;

        // Cho các endpoint auth public cụ thể
        if (path.equals("/api/auth/login")) return true;
        if (path.equals("/api/auth/forgot-password")) return true;
        if (path.equals("/api/auth/reset-password")) return true;
        if (path.startsWith("/api/auth/register")) return true;

        return false;
    }
}