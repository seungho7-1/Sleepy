package com.sleepyproject.sleepy_backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    public void sendAuthCodeEmail(String toEmail, String authCode) {
        MimeMessage message = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(senderEmail, "슬리피 슬라임");
            helper.setTo(toEmail);
            helper.setSubject("[슬리피 슬라임] 비밀번호 재설정 인증번호입니다.");
            
            String htmlContent = "<div style='font-family: \"Pretendard\", \"Apple SD Gothic Neo\", sans-serif; background-color: #f8fafc; padding: 60px 20px; margin: 0;'>"
                    + "<div style='max-width: 500px; margin: 0 auto; background-color: #ffffff; border-radius: 16px; padding: 40px; box-shadow: 0 4px 24px rgba(0,0,0,0.04); text-align: center; border: 1px solid #f1f5f9;'>"
                    + "<h1 style='color: #0f172a; font-size: 24px; margin-bottom: 12px; font-weight: 800; letter-spacing: -0.5px;'>비밀번호 찾기 인증</h1>"
                    + "<p style='color: #64748b; font-size: 15px; line-height: 1.6; margin-bottom: 32px; word-break: keep-all;'>"
                    + "안녕하세요! <b>슬리피 슬라임</b>입니다.<br>요청하신 비밀번호 재설정 인증 코드를 안내해 드립니다.<br>아래 6자리 코드를 화면에 입력해 주세요.</p>"
                    + "<div style='background-color: #f1f5f9; border-radius: 12px; padding: 24px; margin-bottom: 32px;'>"
                    + "<span style='display: block; color: #64748b; font-size: 13px; font-weight: 600; margin-bottom: 8px;'>인증 코드 (3분 유효)</span>"
                    + "<span style='display: block; font-size: 36px; font-weight: 800; letter-spacing: 12px; color: #3b82f6; text-shadow: 1px 1px 0px rgba(59,130,246,0.1); margin-left: 12px;'>" + authCode + "</span>"
                    + "</div>"
                    + "<p style='color: #94a3b8; font-size: 13px; line-height: 1.5; margin: 0;'>"
                    + "본인이 요청하지 않으셨다면 이 메일을 안전하게 무시해 주세요.<br>감사합니다.</p>"
                    + "</div></div>";
            
            helper.setText(htmlContent, true);
            javaMailSender.send(message);
            log.info("Auth email sent to {}", toEmail);
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Failed to send auth email", e);
            throw new RuntimeException("이메일 발송에 실패했습니다.");
        }
    }
}
