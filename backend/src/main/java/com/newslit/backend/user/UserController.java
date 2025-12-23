package com.newslit.backend.user;

import com.newslit.backend.user.dto.AuthResponseDto;
import com.newslit.backend.user.dto.LoginRequestDto;
import com.newslit.backend.user.dto.SignupRequestDto;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
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

    @PostMapping("/signup")
    public ResponseEntity<AuthResponseDto> signup(@RequestBody SignupRequestDto request) {
        AuthResponseDto response = userService.signup(request.getEmail(), request.getPassword(), request.getName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("login")
    public ResponseEntity<AuthResponseDto> login(@RequestBody LoginRequestDto request, HttpSession session) {
        AuthResponseDto response = userService.login(request.getEmail(), request.getPassword());
        session.setAttribute("email", request.getEmail());
        return ResponseEntity.ok(response);
    }


}
