package com.newslit.backend.user;

import com.newslit.backend.user.dto.AuthResponseDto;
import com.newslit.backend.user.exception.DuplicatedEmailException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthResponseDto signup(String email, String password, String name) {
        userRepository.findByEmail(email).ifPresent(user -> {
            throw new DuplicatedEmailException();
        });

        String encodedPassword = passwordEncoder.encode(password);

        User user = User.builder()
                .email(email)
                .name(name)
                .password(encodedPassword)
                .build();

        userRepository.save(user);

        return toAuthResposeDto("회원가입 성공", email);

    }

    private AuthResponseDto toAuthResposeDto(String message, String email) {
        return AuthResponseDto.builder()
                .message(message)
                .email(email)
                .build();
    }
}
