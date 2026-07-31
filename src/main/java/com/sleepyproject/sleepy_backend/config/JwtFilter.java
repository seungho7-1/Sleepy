package com.sleepyproject.sleepy_backend.config;

import com.sleepyproject.sleepy_backend.repository.redis.BlackListedTokenRepository;
import com.sleepyproject.sleepy_backend.security.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final BlackListedTokenRepository blackListedTokenRepository;
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            try {
                String token = header.substring(7);
                //Redis 블랙리스트에 등록된 토큰(로그아웃 됨)인지 검사
                if (blackListedTokenRepository.existsById(token)) {
                    System.out.println("로그아웃된(블랙리스트) Access Token 접근 차단!");
                    filterChain.doFilter(request, response);
                    return;
                }
                String email = jwtUtil.validateAndGetEmailFromAccessToken(token);
                String role = jwtUtil.validateAndGetRole(token);
                System.out.println("email = " + email);

                //SecurityContext에 저장
                List<GrantedAuthority> authorities =
                        List.of(new SimpleGrantedAuthority("ROLE_" + role));

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(email, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception e) {
                // 토큰 검증 실패 시 로그만 남기고 다음 필터로 진행 (인증되지 않은 상태로 처리)
                System.out.println("JWT Token validation failed: " + e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}