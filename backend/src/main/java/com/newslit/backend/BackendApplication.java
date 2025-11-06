package com.newslit.backend;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {
    @Value("${spring.application.name}")
    private static String appName;

    @Value("${app.secret-message}")
    private static String secret;

    public static void main(String[] args) {

        System.out.println(appName);
        System.out.println(secret);
        SpringApplication.run(BackendApplication.class, args);

    }


}
