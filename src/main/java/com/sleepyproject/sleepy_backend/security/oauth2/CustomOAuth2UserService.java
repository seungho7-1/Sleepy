package com.sleepyproject.sleepy_backend.security.oauth2;

import com.sleepyproject.sleepy_backend.domain.member.Member;
import com.sleepyproject.sleepy_backend.domain.member.Role;
import com.sleepyproject.sleepy_backend.repository.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        
        String email = null;
        String name = null;

        if ("kakao".equals(registrationId)) {
            Map<String, Object> attributes = oAuth2User.getAttributes();
            String id = attributes.get("id").toString();
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            if (kakaoAccount != null) {
                email = (String) kakaoAccount.get("email");
                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                if (profile != null) {
                    name = (String) profile.get("nickname");
                }
            }
            if (email == null || email.isEmpty()) {
                email = id + "@kakao.user";
            }
        } else if ("naver".equals(registrationId)) {
            Map<String, Object> response = (Map<String, Object>) oAuth2User.getAttributes().get("response");
            if (response != null) {
                String id = (String) response.get("id");
                email = (String) response.get("email");
                name = (String) response.get("name");
                if (email == null || email.isEmpty()) {
                    email = id + "@naver.user";
                }
            }
        }

        boolean isNewUser = false;
        Optional<Member> memberOptional = memberRepository.findByUsername(email);
        Member member;
        if (memberOptional.isPresent()) {
            member = memberOptional.get();
            if (!member.isOnboarded()) {
                isNewUser = true;
            }
        } else {
            isNewUser = true;
            member = Member.builder()
                    .username(email) // For social login, username is set to the email
                    .email(email)
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .nickname(name != null ? name : "USER")
                    .role(Role.BUYER)
                    .createdAt(LocalDateTime.now())
                    .onboarded(false)
                    .build();
        }

        // Update OAuth tokens on every login
        member.updateOauthProvider(registrationId.toUpperCase());
        String accessToken = userRequest.getAccessToken().getTokenValue();
        String refreshToken = null;
        if (userRequest.getAdditionalParameters() != null && userRequest.getAdditionalParameters().containsKey("refresh_token")) {
            refreshToken = userRequest.getAdditionalParameters().get("refresh_token").toString();
        }
        member.updateOauthTokens(accessToken, refreshToken);
        memberRepository.save(member);

        return new CustomOAuth2User(
                Collections.singletonList(() -> "ROLE_" + member.getRole().name()),
                oAuth2User.getAttributes(),
                userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName(),
                isNewUser,
                member.getNickname(),
                member.getUsername()
        );
    }

    @lombok.Getter
    public static class CustomOAuth2User extends DefaultOAuth2User {
        private final boolean isNewUser;
        private final String nickname;
        private final String username;

        public CustomOAuth2User(java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> authorities,
                                Map<String, Object> attributes,
                                String nameAttributeKey,
                                boolean isNewUser,
                                String nickname,
                                String username) {
            super(authorities, attributes, nameAttributeKey);
            this.isNewUser = isNewUser;
            this.nickname = nickname;
            this.username = username;
        }
    }
}
