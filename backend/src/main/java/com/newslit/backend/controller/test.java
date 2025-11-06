package com.newslit.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class test {

    @Value("${spring.application.name}")
    private String publicMessage;

    @Value("${app.secret-message}")
    private String secretMessage;


    @GetMapping("/properties")
    public Map<String, String> testProperties() {
        Map<String, String> result = new HashMap<>();

        result.put("public-message", publicMessage);
        result.put("secret-message", secretMessage);
        result.put("status", "Properties loaded successfully!");

        return result;
    }

}
