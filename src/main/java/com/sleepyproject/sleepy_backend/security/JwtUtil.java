package com.sleepyproject.sleepy_backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.SECRET_KEY}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private Key secretKey;

    // 생성자 대신 @PostConstruct를 사용하여 설정값이 주입된 후 안전하게 초기화
    @PostConstruct
    protected void init() {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // 토큰 생성
    public String generateToken(String email,String role) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .claim("role", role)
                .setExpiration(new Date(now.getTime() + expiration)) // 설정파일의 값 사용
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // 토큰 검증 및 이메일 추출
    public String validateAndGetEmail(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    public String validateAndGetRole(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.get("role", String.class);
    }
}