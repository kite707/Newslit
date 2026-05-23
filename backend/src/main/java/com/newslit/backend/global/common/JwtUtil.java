package com.newslit.backend.global.common;

import com.newslit.backend.global.common.enums.TokenType;
import com.newslit.backend.user.Role;
import com.newslit.backend.user.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JwtUtil {
    private final long EXPIRATION_TIME;
    private final SecretKey signingKey;

    public JwtUtil(@Value("${jwt.expiration}") long expirationTime, @Value("${jwt.secret}") String secret) {
        this.EXPIRATION_TIME = expirationTime;
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAuthToken(String email, Long id, Role role) {
        return Jwts.builder()
                .claim("email", email)
                .claim("id", id)
                .claim("role", role.name())
                .claim("type", TokenType.AUTH.name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(signingKey)
                .compact();
    }

    public String generateVerifyToken(String email) {
        return Jwts.builder()
                .claim("email", email)
                .claim("type", TokenType.VERIFY.name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(signingKey)
                .compact();
    }

    public String extractEmailFromVerifyToken(String token) {
        try {
            if (token == null) {
                throw new InvalidTokenException();
            }
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey).build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (!TokenType.VERIFY.name().equals(claims.get("type", String.class))) {
                throw new InvalidTokenException();
            }
            return claims.get("email", String.class);
        } catch (InvalidTokenException e) {
            throw e;
        } catch (Exception e) {
            log.error("EXTRACT EMAIL FROM VERIFY TOKEN ERROR", e);
            throw new InvalidTokenException();
        }
    }

    public String extractRole(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get("role", String.class);
        } catch (Exception e) {
            log.error("EXTRACT ROLE ERROR", e);
            return null;
        }
    }

    public Long extractId(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get("id", Long.class);
        } catch (Exception e) {
            log.error("EXTRACT ID ERROR", e);
            return null;
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
