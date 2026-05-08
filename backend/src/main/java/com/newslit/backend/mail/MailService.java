package com.newslit.backend.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {
    private final JavaMailSender mailSender;

    public void sendTestMail(String to) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@newslit.net");  // Approved Sender에 등록한 주소
        message.setTo(to);
        message.setSubject("Newslit 테스트 메일");
        message.setText("OCI Email Delivery로 발송된 메일입니다.");
        mailSender.send(message);
    }
}