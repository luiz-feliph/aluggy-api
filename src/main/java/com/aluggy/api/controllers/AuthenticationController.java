package com.aluggy.api.controllers;

import com.aluggy.api.dto.LoginRequestDTO;
import com.aluggy.api.dto.RegisterRequestDTO;
import com.aluggy.api.dto.UserResponseDTO;
import com.aluggy.api.entities.User;
import com.aluggy.api.entities.enums.Role;
import com.aluggy.api.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationManager authenticationManager;
    private final UserService service;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity login(@RequestBody @Valid LoginRequestDTO data) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.login(), data.password());
        var auth = authenticationManager.authenticate(usernamePassword);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(@RequestBody @Valid RegisterRequestDTO data) {

        if (service.existsByUserName(data.userName())) return ResponseEntity.badRequest().build();

        if (service.existsByEmailAddress(data.emailAddress())) return ResponseEntity.badRequest().build();

        String encryptedPassword = passwordEncoder.encode(data.password());
        User newUser = new User(data.userName(), data.fullName(), data.emailAddress(), data.contactNumber(), encryptedPassword, Role.USER);


        newUser = service.insert(newUser);

        URI uri = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(newUser.getId())
                .toUri();

        UserResponseDTO response = new UserResponseDTO(
                newUser.getId(),
                newUser.getUsername(),
                newUser.getFullName(),
                newUser.getEmailAddress(),
                newUser.getContactNumber()
        );

        return ResponseEntity.created(uri).body(response);
    }
}
