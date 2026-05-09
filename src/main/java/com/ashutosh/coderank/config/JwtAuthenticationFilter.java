package com.ashutosh.coderank.config;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ashutosh.coderank.util.TokenUtil;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    TokenUtil tokenUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
      
        String jwtString = request.getHeader("Authorization");

        // Check null first, then isEmpty
        if(jwtString == null || jwtString.isEmpty() || !jwtString.startsWith("Bearer")){
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwtToken = jwtString.substring(7);

            // parse and validate the jwt token 
            Claims claims = tokenUtil.validateToken(jwtToken);
            String userName = claims.getSubject();
            String role = claims.get("roles", String.class);
            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userName, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            
        } catch (Exception e) {
            // Invalid or expired token
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
            return;
        }
        
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        return (path.equals("/auth/v1/register") || path.equals("/auth/v1/login")) 
               && method.equals("POST");
    }
}
