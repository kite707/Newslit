package com.newslit.backend.mail;

import com.newslit.backend.user.User;
import com.newslit.backend.user.UserRepository;
import com.newslit.backend.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {
    private final JavaMailSender mailSender;
    private final EmailVerificationRepository emailVerificationRepository;
    private final UserRepository userRepository;

    public void sendTestMail(String to) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@newslit.net");
        message.setTo(to);
        message.setSubject("Newslit 테스트 메일");
        message.setText("OCI Email Delivery로 발송된 메일입니다.");
        mailSender.send(message);
    }

    public void sendVerifyMail(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@newslit.net");
        message.setTo(to);
        message.setSubject("[Newslit] 인증 코드");
        message.setText(code);

        User user = userRepository.findByEmail(to).orElseThrow(UserNotFoundException::new);

        EmailVerification verification = emailVerificationRepository.findByUser(user)
                .orElseGet(() -> EmailVerification.builder()
                        .user(user)
                        .sendCount(0)
                        .attemptCount(0)
                        .build());
        verification.setCode(code);
        mailSender.send(message);
        emailVerificationRepository.save(verification);

    }
}