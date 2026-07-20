package com.sleepyproject.sleepy_backend.security.oauth2;

import com.sleepyproject.sleepy_backend.security.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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

    @org.springframework.beans.factory.annotation.Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        CustomOAuth2UserService.CustomOAuth2User customUser = (CustomOAuth2UserService.CustomOAuth2User) authentication.getPrincipal();
        
        String username = customUser.getUsername();
        String nickname = customUser.getNickname();
        boolean isNewUser = customUser.isNewUser();

        String role = authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        String token = jwtUtil.generateToken(username, role);
        
        String encodedNickname = URLEncoder.encode(nickname != null ? nickname : "USER", StandardCharsets.UTF_8);

        String targetUrl;
        if (isNewUser) {
            targetUrl = frontendUrl + "/oauth2/onboarding?token=" + token + "&nickname=" + encodedNickname + "&username=" + username;
        } else {
            targetUrl = frontendUrl + "/login?token=" + token + "&role=" + role + "&nickname=" + encodedNickname;
        }
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
