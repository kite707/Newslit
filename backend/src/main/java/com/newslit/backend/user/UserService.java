package com.newslit.backend.user;

import com.newslit.backend.global.common.JwtUtil;
import com.newslit.backend.user.dto.AuthResponseDto;
import com.newslit.backend.user.exception.DuplicatedEmailException;
import com.newslit.backend.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

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

        User saveUser = userRepository.save(user);
        String token = jwtUtil.generateToken(email, saveUser.getId());

        return toAuthResposeDto("회원가입 성공", email, token, user.getId());
    }

    public AuthResponseDto login(String email, String password) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException());
        String encodedPassword = user.getPassword();

        boolean isPasswordMatch = passwordEncoder.matches(password, encodedPassword);

        if (!isPasswordMatch) {
            throw new UserNotFoundException();
        }
        String token = jwtUtil.generateToken(email, user.getId());

        return toAuthResposeDto("로그인 성공", user.getEmail(), token, user.getId());

    }

    private AuthResponseDto toAuthResposeDto(String message, String email, String token, Long id) {
        return AuthResponseDto.builder()
                .message(message)
                .email(email)
                .id(id)
                .token(token)
                .build();
    }
}
