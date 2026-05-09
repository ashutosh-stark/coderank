package com.ashutosh.coderank.util;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.ashutosh.coderank.constant.UtilConstant;
import com.ashutosh.coderank.model.Users;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@Component
public class TokenUtil {

  
   @Autowired
   private Environment environment;



    public String generateToken(Users user){

        return Jwts.builder()
        .subject(user.getUserName())
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + UtilConstant.EXPIRATION_TIME))
        .claim("roles","ROLE_"+user.getRole())
        .claim("userName", user.getUserName())
        .signWith(io.jsonwebtoken.SignatureAlgorithm.HS256, environment.getProperty(UtilConstant.SECRET_KEY))
        .compact();
    }

    public Claims validateToken(String token){
    Claims claims = Jwts.parser().setSigningKey(environment.getProperty(UtilConstant.SECRET_KEY))
        .build()
        .parseClaimsJws(token)
        .getBody();

        return claims;

    }
}
