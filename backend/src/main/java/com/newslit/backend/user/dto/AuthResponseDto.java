package com.newslit.backend.user.dto;

import lombok.Builder;

@Builder
public class AuthResponseDto {
    private String message;
    private String email;
}
