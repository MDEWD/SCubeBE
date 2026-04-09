package com.scube.scubebackend.filter;

import com.scube.scubebackend.modules.user.model.dto.LoginUser;
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
import com.scube.scubebackend.modules.user.mapper.UserMapper;
import com.scube.scubebackend.modules.user.model.entity.User;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Collections;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private UserMapper userMapper;

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
                
                // Try to read displayId from token first (faster, avoids DB hit). Fallback to DB if absent.
                try {
                    String displayId = jwtUtil.getDisplayIdFromToken(token);
                    if (displayId != null && !displayId.isBlank()) {
                        loginUser.setDisplayId(displayId);
                        log.debug("JwtAuthenticationFilter: displayId found in token for userId={}, displayId={}", userId, displayId);
                    } else {
                        User dbUser = userMapper.selectById(userId);
                        if (dbUser != null) {
                            log.debug("JwtAuthenticationFilter: loaded dbUser id={}, displayId={}", userId, dbUser.getDisplayId());
                            if (dbUser.getDisplayId() != null) {
                                loginUser.setDisplayId(dbUser.getDisplayId());
                            }
                        } else {
                            log.debug("JwtAuthenticationFilter: no dbUser found for id={}", userId);
                        }
                    }
                } catch (Exception e) {
                    log.warn("JwtAuthenticationFilter: failed to load user displayId for userId={}, reason={}", userId, e.getMessage());
                }

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
