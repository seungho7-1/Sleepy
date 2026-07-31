package com.sleepyproject.sleepy_backend.security.oauth2;

import com.sleepyproject.sleepy_backend.domain.redis.RefreshToken;
import com.sleepyproject.sleepy_backend.repository.HttpCookieOAuth2AuthorizationRequestRepository;
import com.sleepyproject.sleepy_backend.repository.redis.RefreshTokenRepository;
import com.sleepyproject.sleepy_backend.security.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    @Value("${app.frontend-url}")
    private String frontendUrl;
    private final CustomOAuth2UserService customOAuth2UserService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        CustomOAuth2UserService.CustomOAuth2User customUser = (CustomOAuth2UserService.CustomOAuth2User) authentication.getPrincipal();
        
        String username = customUser.getUsername();
        String nickname = customUser.getNickname();
        boolean isNewUser = customUser.isNewUser();
        String role = authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");

        String accessToken = jwtUtil.generateAccessToken(username, role);
        String refreshToken = jwtUtil.generateRefreshToken(username);
        refreshTokenRepository.save(new com.sleepyproject.sleepy_backend.domain.redis.RefreshToken(username, refreshToken));

        // 3. 발급한 리프레시 토큰을 HttpOnly 쿠키로 변환하여 응답 헤더에 세팅
        // Refresh Token 수명은 14일(1209600000ms)
        response.addHeader("Set-Cookie", jwtUtil.createTokenCookie("refreshToken", refreshToken, 1209600000).toString());

        // Access Token은 URL 파라미터로 붙여서 프론트엔드가 메모리에 담을 수 있게 해줌!
        String encodedNickname = URLEncoder.encode(nickname != null ? nickname : "USER", StandardCharsets.UTF_8);
        String targetUrl;

        if (isNewUser) {
            targetUrl = frontendUrl + "/oauth2/onboarding?token=" + accessToken + "&nickname=" + encodedNickname + "&username=" + username;
        } else {
            targetUrl = frontendUrl + "/login?token=" + accessToken + "&role=" + role + "&nickname=" + encodedNickname;
        }

        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
