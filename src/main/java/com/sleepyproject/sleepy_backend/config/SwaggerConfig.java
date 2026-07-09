package com.sleepyproject.sleepy_backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        // JWT 인증 헤더 설정
        String jwtSchemeName = "jwtAuth";
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);
        Components components = new Components()
                .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
                        .name(jwtSchemeName)
                        .type(SecurityScheme.Type.HTTP) // HTTP 방식
                        .scheme("bearer")
                        .bearerFormat("JWT"));

        return new OpenAPI()
                .info(apiInfo())
                .addSecurityItem(securityRequirement)
                .components(components);
    }

    private Info apiInfo() {
        return new Info()
                .title("Sleepy (Slime Market) API 명세서")
                .description("슬라임 쇼핑몰 및 커뮤니티 프로젝트를 위한 REST API 명세서입니다.<br>" +
                        "<b>인증이 필요한 API 테스트 방법:</b> 우측 상단의 [Authorize] 버튼을 누르고 프론트엔드에서 발급받은 JWT 토큰을 입력하세요.")
                .version("1.0.0");
    }
}
