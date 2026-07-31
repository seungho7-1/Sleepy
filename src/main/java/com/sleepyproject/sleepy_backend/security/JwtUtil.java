package com.sleepyproject.sleepy_backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.ACCESS_SECRET_KEY}")
    private String accessSecretString;
    @Value("${jwt.REFRESH_SECRET_KEY}")
    private String refreshSecretString;

    // 도장도 2개로 분리!
    private Key accessSecretKey;
    private Key refreshSecretKey;

    // Access Token 수명: 30분 (1000 * 60 * 30)
    private final static long ACCESS_TOKEN_EXPIRATION = 1800000L;

    // Refresh Token 수명: 14일 (1000 * 60 * 60 * 24 * 14)
    private final static long REFRESH_TOKEN_EXPIRATION = 1209600000L;

    // 스프링이 인식할 수 있는 "진짜 자물쇠/도장(Key 객체)"으로 변환해두는 변수입니다.
    private Key secretKey;

    // 생성자 대신 @PostConstruct를 사용하여 설정값이 주입된 후 안전하게 초기화
    @PostConstruct
    protected void init() {
        // 각각의 문자열을 Key 객체로 변환
        this.accessSecretKey = Keys.hmacShaKeyFor(accessSecretString.getBytes(StandardCharsets.UTF_8));
        this.refreshSecretKey = Keys.hmacShaKeyFor(refreshSecretString.getBytes(StandardCharsets.UTF_8));
    }

    // Access Token 생성 (30분)
    public String generateAccessToken(String email,String role) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .claim("role", role)
                .setExpiration(new Date(now.getTime() + ACCESS_TOKEN_EXPIRATION)) // 설정파일의 값 사용
                .signWith(accessSecretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // Refresh Token 생성 (14일)
    public String generateRefreshToken(String email) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + REFRESH_TOKEN_EXPIRATION))
                .signWith(refreshSecretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // Access Token 검증 및 이메일 추출
    public String validateAndGetEmailFromAccessToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(accessSecretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    // Refresh Token 검증 및 이메일 추출
    public String validateAndGetEmailFromRefreshToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(refreshSecretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    public String validateAndGetRole(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(accessSecretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("role", String.class);
    }

    // XSS 방어용 HttpOnly 쿠키 생성 메서드
    public ResponseCookie createTokenCookie(String cookieName, String token, long maxAge) {
        return ResponseCookie.from(cookieName, token)
                .httpOnly(true)    // 자바스크립트 접근 차단 (XSS 방어)
                .secure(false)     // Https를 쓴다면 true로 변경해야 함. 지금은 로컬/http 테스트를 위해 false
                .path("/")         // 모든 경로에서 쿠키 사용 가능
                .maxAge(maxAge / 1000) // 초 단위 설정
                .sameSite("Lax")   // CSRF 일부 방어
                .build();
    }

    // HttpServletRequest의 쿠키 배열에서 특정 이름의 토큰 값만 쏙 빼오는 메서드
    public String extractTokenFromCookie(HttpServletRequest request, String cookieName) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    // 토큰의 남은 유효시간(ms)을 계산 (블랙리스트 등록 시 필요 - Access Token 기준)
    public long getRemainingExpiration(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(accessSecretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            long expirationTime = claims.getExpiration().getTime();
            long currentTime = new Date().getTime();
            return expirationTime - currentTime;
        } catch (Exception e) {
            return 0; // 이미 만료되었거나 검증 실패
        }
    }


    // Refresh Token 쿠키 전용 생성기
    public ResponseCookie createRefreshTokenCookie(String token) {
        return ResponseCookie.from("refreshToken", token)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(REFRESH_TOKEN_EXPIRATION / 1000)
                .sameSite("Lax")
                .build();
    }

}