package com.sleepyproject.sleepy_backend.config;

import com.sleepyproject.sleepy_backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.sleepyproject.sleepy_backend.security.oauth2.CustomOAuth2UserService;

/**
 * Spring Security 보안 설정 클래스입니다.
 * JWT 필터 적용 및 URL별 권한 제어를 담당합니다.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final CustomOAuth2UserService customOAuth2UserService;

    /**
     * 비밀번호 암호화를 위한 BCryptPasswordEncoder 빈 등록
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * JWT 인증 필터 빈 등록
     */
    @Bean
    public JwtFilter jwtFilter() {
        return new JwtFilter(jwtUtil);
    }

    /**
     * HTTP 보안 필터 체인 설정
     * - CORS 활성화, CSRF/세션 비활성화
     * - URL별 세부 권한 설정
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .cors(org.springframework.security.config.Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                )

                // JWT 필터를 UsernamePasswordAuthenticationFilter 이전에 등록
                .addFilterBefore(
                        jwtFilter(),
                        org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class
                )

                // URL 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // 1. 회원가입, 로그인 등 인증 관련 공개 API
                        .requestMatchers("/api/auth/signup", "/api/auth/login", "/api/auth/logout").permitAll()
                        
                        // 2. 상품 조회 관련 공개 API (GET만 허가)
                        .requestMatchers(HttpMethod.GET, "/api/products/list", "/api/products/detail/**").permitAll()
                        
                        // 3. 리뷰 조회 관련 공개 API
                        .requestMatchers(HttpMethod.GET, "/api/reviews/product/**").permitAll()
                        
                        // 4. 커뮤니티 조회 관련 공개 API (목록 및 상세조회, 댓글 조회)
                        .requestMatchers(HttpMethod.GET, "/api/board/posts", "/api/board/posts/**", "/api/board/comments").permitAll()
                        
                        // 5. 정적 리소스 및 Swagger UI 공개 설정
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        
                        // 6. 관리자 권한 필요한 API
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        
                        // 7. 판매자 권한 필요한 API (상품 등록, 수정, 삭제, 크롤링)
                        .requestMatchers("/api/products/create", "/api/products/update/**", "/api/products/delete/**", "/api/products/crawl").hasRole("SELLER")
                        
                        // 8. 그 외 모든 API는 인증된 회원만 접근 가능
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}