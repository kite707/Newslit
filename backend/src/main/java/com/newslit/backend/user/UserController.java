package com.newslit.backend.user;

import com.newslit.backend.global.common.JwtUtil;
import com.newslit.backend.user.dto.AuthResponseDto;
import com.newslit.backend.user.dto.LoginRequestDto;
import com.newslit.backend.user.dto.SignupRequestDto;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
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
    public ResponseEntity<AuthResponseDto> signup(@RequestBody SignupRequestDto request, HttpServletResponse response) {
        AuthResponseDto authResponseDto = userService.signup(request.getEmail(), request.getPassword(),
                request.getNickname());
        String token = jwtUtil.generateToken(request.getEmail(), authResponseDto.getId());

        ResponseCookie cookie = ResponseCookie.from("jwt", token)
                .httpOnly(true)
                .path("/")
                .secure(false)
                .maxAge(3600)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(authResponseDto);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@RequestBody LoginRequestDto request, HttpServletResponse response) {
        AuthResponseDto authResponseDto = userService.login(request.getEmail(), request.getPassword());
        String token = jwtUtil.generateToken(request.getEmail(), authResponseDto.getId());

        ResponseCookie cookie = ResponseCookie.from("jwt", token)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(3600)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(authResponseDto);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {

        ResponseCookie cookie = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .path("/")
                .secure(false)
                .maxAge(0)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok("Logout successful");
    }


    @GetMapping("/me")
    public ResponseEntity<AuthResponseDto> getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401)
                    .body(new AuthResponseDto("로그인이 필요합니다", null, null, null));
        }

        // "Bearer " 제거
        String token = authHeader.substring(7);

        // 토큰 검증
        if (!jwtUtil.validateToken(token)) {
            return ResponseEntity.status(401)
                    .body(new AuthResponseDto("유효하지 않은 토큰입니다", null, null, null));
        }

        // 토큰에서 이메일 추출
        String email = jwtUtil.extractEmail(token);
        Long id = jwtUtil.extractId(token);

        return ResponseEntity.ok(new AuthResponseDto("인증됨", id, email, token));
    }


}
