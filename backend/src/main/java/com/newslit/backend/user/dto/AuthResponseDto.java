package com.newslit.backend.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthResponseDto {
    private String message;
    private Long id;
    private String email;
    private String nickname;
    private String token;
}
