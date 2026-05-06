package com.newslit.backend.user;

import com.newslit.backend.global.common.JwtUtil;
import com.newslit.backend.user.dto.AuthResponseDto;
import com.newslit.backend.user.dto.LoginRequestDto;
import com.newslit.backend.user.dto.SignupRequestDto;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Value("${app.cookie.secure}")
    private boolean cookieSecure;
    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponseDto> signup(@RequestBody SignupRequestDto request, HttpServletResponse response) {
        AuthResponseDto authResponseDto = userService.signup(request.getEmail(), request.getPassword(),
                request.getNickname());
        String token = jwtUtil.generateToken(request.getEmail(), authResponseDto.getId(), authResponseDto.getRole());

        ResponseCookie cookie = makeCookie(token, jwtExpiration / 1000);

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(authResponseDto);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@RequestBody LoginRequestDto request, HttpServletResponse response) {
        AuthResponseDto authResponseDto = userService.login(request.getEmail(), request.getPassword());
        String token = jwtUtil.generateToken(request.getEmail(), authResponseDto.getId(), authResponseDto.getRole());

        ResponseCookie cookie = makeCookie(token, jwtExpiration / 1000);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(authResponseDto);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {

        ResponseCookie cookie = makeCookie("", 0);

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok("Logout successful");
    }

    private ResponseCookie makeCookie(String token, long maxAge) {
        return ResponseCookie.from("jwt", token)
                .httpOnly(true)
                .path("/")
                .secure(cookieSecure)
                .maxAge(maxAge)
                .sameSite("Lax")
                .build();
    }
}
