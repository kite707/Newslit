package com.newslit.backend.user;

import com.newslit.backend.global.common.enums.Verify;
import com.newslit.backend.mail.EmailVerification;
import com.newslit.backend.mail.EmailVerificationRepository;
import com.newslit.backend.mail.MailService;
import com.newslit.backend.user.dto.AuthResponseDto;
import com.newslit.backend.user.dto.SendCodeResponseDto;
import com.newslit.backend.user.exception.AlreadyVerifiedException;
import com.newslit.backend.user.exception.CodeExpiredException;
import com.newslit.backend.user.exception.CodeNotFoundException;
import com.newslit.backend.user.exception.DuplicatedEmailException;
import com.newslit.backend.user.exception.SendLimitExceededException;
import com.newslit.backend.user.exception.UserNotFoundException;
import com.newslit.backend.user.exception.WrongCodeException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final EmailVerificationRepository emailVerificationRepository;
    private static final SecureRandom secureRandom = new SecureRandom();

    private static final Integer SEND_LIMIT = 20;

    public AuthResponseDto signup(String email, String password, String name) {
        userRepository.findByEmail(email).ifPresent(user -> {
            throw new DuplicatedEmailException();
        });

        String encodedPassword = passwordEncoder.encode(password);

        User user = User.builder()
                .email(email)
                .name(name)
                .role(Role.USER)
                .password(encodedPassword)
                .build();

        User saveUser = userRepository.save(user);

        return toAuthResposeDto("회원가입 성공", saveUser);
    }

    public AuthResponseDto login(String email, String password) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException());
        String encodedPassword = user.getPassword();

        boolean isPasswordMatch = passwordEncoder.matches(password, encodedPassword);

        if (!isPasswordMatch) {
            throw new UserNotFoundException();
        }

        return toAuthResposeDto("로그인 성공", user);

    }

    private AuthResponseDto toAuthResposeDto(String message, User user) {
        return AuthResponseDto.builder()
                .message(message)
                .email(user.getEmail())
                .role(user.getRole())
                .nickname(user.getName())
                .id(user.getId())
                .build();
    }

    @Transactional
    public SendCodeResponseDto sendCode(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new AlreadyVerifiedException();
        }

        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        EmailVerification verification = emailVerificationRepository.findByEmail(email)
                .orElseGet(() -> EmailVerification.builder()
                        .email(email)
                        .sendCount(0)
                        .sendCountResetDt(LocalDateTime.now().plusDays(1))
                        .attemptCount(0)
                        .build());

        if (verification.getSendCount() >= SEND_LIMIT) {
            if (LocalDateTime.now().isAfter(verification.getSendCountResetDt())) {
                verification.startNewSendCycle();
            } else {
                throw new SendLimitExceededException();
            }
        }
        verification.setCode(code);
        verification.resetAttemptCount();
        verification.increaseSendCount();

        mailService.sendVerifyMail(email, code);
        emailVerificationRepository.save(verification);

        return SendCodeResponseDto.builder()
                .message("인증코드 발송이 완료되었습니다.")
                .build();
    }

    public void verifyCode(String email, String code) {

        if (userRepository.findByEmail(email).isPresent()) {
            throw new WrongCodeException();
        }

        EmailVerification emailVerification = emailVerificationRepository.findByEmail(email).orElseThrow(
                CodeNotFoundException::new);

        if (emailVerification.isCodeExpired()) {
            throw new CodeExpiredException();
        }

        if (!emailVerification.getCode().equals(code)) {
            throw new WrongCodeException();
        }

    }
}
