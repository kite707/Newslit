package com.newslit.backend.user;

import com.newslit.backend.global.common.JwtUtil;
import com.newslit.backend.user.dto.AuthResponseDto;
import com.newslit.backend.user.dto.LoginRequestDto;
import com.newslit.backend.user.dto.SignupRequestDto;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponseDto> signup(@RequestBody SignupRequestDto request) {
        AuthResponseDto response = userService.signup(request.getEmail(), request.getPassword(), request.getName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@RequestBody LoginRequestDto request, HttpSession session) {
        AuthResponseDto response = userService.login(request.getEmail(), request.getPassword());
        session.setAttribute("email", request.getEmail());
        return ResponseEntity.ok(response);
    }


    @GetMapping("/me")
    public ResponseEntity<AuthResponseDto> getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("authHeader: " + authHeader);
            return ResponseEntity.status(401)
                    .body(new AuthResponseDto("로그인이 필요합니다", null, null));
        }

        // "Bearer " 제거
        String token = authHeader.substring(7);

        // 토큰 검증
        if (!jwtUtil.validateToken(token)) {
            return ResponseEntity.status(401)
                    .body(new AuthResponseDto("유효하지 않은 토큰입니다", null, null));
        }

        // 토큰에서 이메일 추출
        String email = jwtUtil.extractEmail(token);

        return ResponseEntity.ok(new AuthResponseDto("인증됨", email, token));
    }


}
