package com.newslit.backend.mail;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class MailService {
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${mail.verify-sender}")
    private String sender;

    public void sendTestMail(String to) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(to);
        message.setSubject("Newslit 테스트 메일");
        message.setText("OCI Email Delivery로 발송된 메일입니다.");
        mailSender.send(message);
    }

    public void sendVerifyMail(String email, String code) {
        try {
            Context context = new Context();
            context.setVariable("code", code);
            String html = templateEngine.process("email/verify-code", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(sender);
            helper.setTo(email);
            helper.setSubject("[Newslit] 이메일 인증 코드");
            helper.setText(html, true);

            mailSender.send(message);
        } catch (Exception e) {
            throw new MailSendException("이메일 발송 실패", e);
        }
    }
}