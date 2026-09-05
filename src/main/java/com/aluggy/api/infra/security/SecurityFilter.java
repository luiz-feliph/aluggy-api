package com.aluggy.api.infra.security;

import com.aluggy.api.entities.User;
import com.aluggy.api.repositories.UserRepository;
import com.aluggy.api.services.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SecurityFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = this.recoverToken(request);

        if (token != null) {
            var subject = tokenService.validateToken(token);

            if (subject != null && !subject.isEmpty()) {
                Optional<User> userOptional = userRepository.findByUserNameOrEmailAddress(subject);

                if (userOptional.isPresent()) {
                    User user = userOptional.get();

                    var authentication = new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            user.getAuthorities()
                    );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private String recoverToken(HttpServletRequest request) {
        if (request.getCookies() != null)
            for (Cookie cookie : request.getCookies())
                if ("AUTH_TOKEN".equals(cookie.getName()))
                    return cookie.getValue();

        return null;
    }
}
