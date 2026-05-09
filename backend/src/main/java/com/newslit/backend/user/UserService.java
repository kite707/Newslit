package com.newslit.backend.user;

import com.newslit.backend.mail.EmailVerification;
import com.newslit.backend.mail.EmailVerificationRepository;
import com.newslit.backend.mail.MailService;
import com.newslit.backend.user.dto.AuthResponseDto;
import com.newslit.backend.user.dto.SendCodeResponseDto;
import com.newslit.backend.user.exception.DuplicatedEmailException;
import com.newslit.backend.user.exception.UserNotFoundException;
import java.security.SecureRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final EmailVerificationRepository emailVerificationRepository;

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

    public SendCodeResponseDto sendCode(String email) {
        String code = String.format("%06d", new SecureRandom().nextInt(1_000_000));

        User user = userRepository.findByEmail(email).orElseThrow(UserNotFoundException::new);

        EmailVerification verification = emailVerificationRepository.findByUser(user)
                .orElseGet(() -> EmailVerification.builder()
                        .user(user)
                        .sendCount(0)
                        .attemptCount(0)
                        .build());
        verification.setCode(code);

        mailService.sendVerifyMail(user, code);
        emailVerificationRepository.save(verification);

        //TODO: 변경필요
        return SendCodeResponseDto.builder()
                .code("200")
                .message("성공")
                .build();
    }
}
