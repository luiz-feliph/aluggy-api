package com.aluggy.api.controllers;

import com.aluggy.api.dto.LoginRequestDTO;
import com.aluggy.api.dto.RegisterRequestDTO;
import com.aluggy.api.dto.UserResponseDTO;
import com.aluggy.api.entities.User;
import com.aluggy.api.entities.enums.Role;
import com.aluggy.api.services.TokenService;
import com.aluggy.api.services.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Duration;

@RestController
@RequestMapping("auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationManager authenticationManager;
    private final UserService service;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Valid RegisterRequestDTO data, HttpServletResponse response) {

        String encryptedPassword = passwordEncoder.encode(data.password());

        User newUser = new User(data.userName(), data.emailAddress(), data.contactNumber(), encryptedPassword, Role.USER);

        newUser = service.insert(newUser);

        setAuthCookie(response, newUser);

        URI uri = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(newUser.getId())
                .toUri();

        return ResponseEntity.created(uri).build();
    }

    /* TODO (login and logout): set secure(true) in production */

    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestBody @Valid LoginRequestDTO data, HttpServletResponse response) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.login(), data.password());
        var auth = authenticationManager.authenticate(usernamePassword);

        if (!(auth.getPrincipal() instanceof User user)) {
            throw new AuthenticationServiceException("Unexpected principal type");
        }

        setAuthCookie(response, user);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        ResponseCookie expiredCookie = ResponseCookie.from("AUTH_TOKEN", "")
                .path("/")
                .httpOnly(true)
                .secure(false)
                .maxAge(0)
                .sameSite("Strict")
                .build();

        response.setHeader(HttpHeaders.SET_COOKIE, expiredCookie.toString());

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getMyData(@AuthenticationPrincipal User authenticatedUser) {
        UserResponseDTO response = new UserResponseDTO(authenticatedUser);

        return ResponseEntity.ok().body(response);
    }

    private void setAuthCookie(HttpServletResponse response, User user) {
        String token = tokenService.generateToken(user);

        ResponseCookie cookie = ResponseCookie.from("AUTH_TOKEN", token)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Strict")
                .maxAge(Duration.ofHours(2))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
