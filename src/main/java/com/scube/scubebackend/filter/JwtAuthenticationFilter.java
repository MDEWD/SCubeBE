package com.scube.scubebackend.filter;

import com.scube.scubebackend.model.dto.LoginUser;
import com.scube.scubebackend.util.JwtUtil;
import com.scube.scubebackend.util.UserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        String token = getTokenFromRequest(request);
        
        if (token != null && jwtUtil.validateToken(token)) {
            try {
                Long userId = jwtUtil.getUserIdFromToken(token);
                String role = jwtUtil.getRoleFromToken(token);
                
                LoginUser loginUser = new LoginUser();
                loginUser.setId(userId);
                loginUser.setUserRole(role);
                
                UserContext.setUser(loginUser);
                
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    loginUser, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                // Token invalid, continue without authentication
            }
        }
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }
    
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}

