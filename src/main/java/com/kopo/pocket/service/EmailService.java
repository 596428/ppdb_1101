package com.kopo.pocket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    // @Value("${app.domain:http://localhost:8084}")
    @Value("${app.domain}")
    private String domain;

    public void sendVerificationEmail(String to, String userId, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            // Thymeleaf Context 설정
            Context context = new Context();
            context.setVariable("userId", userId);
            context.setVariable("verificationLink", domain + "/api/auth/verify?token=" + token);
            
            // 템플릿을 사용하여 이메일 내용 생성
            String emailContent = templateEngine.process("verification-email", context);
            
            // 이메일 기본 정보 설정
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Pokemon Pocket Deck Builder - 이메일 인증");
            helper.setText(emailContent, true); // true는 HTML 사용을 의미
            
            // 이메일 발송
            mailSender.send(message);
            
        } catch (MessagingException e) {
            throw new RuntimeException("이메일 발송 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}