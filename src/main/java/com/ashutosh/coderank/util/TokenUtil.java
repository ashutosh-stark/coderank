package com.ashutosh.coderank.util;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.ashutosh.coderank.constant.UtilConstant;
import com.ashutosh.coderank.model.Users;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class TokenUtil {

    @Autowired
    private Environment environment;

    private SecretKey getSigningKey() {
        String secret = environment.getProperty(UtilConstant.SECRET_KEY);
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret key is not configured (property: "
                    + UtilConstant.SECRET_KEY + ")");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Users user) {

        return Jwts.builder()
                .subject(user.getUserName())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + UtilConstant.EXPIRATION_TIME))
                .claim("roles", user.getRole())
                .claim("userName", user.getUserName())
                .signWith(getSigningKey())
                .compact();
    }

    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
