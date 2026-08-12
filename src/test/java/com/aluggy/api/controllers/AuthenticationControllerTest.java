package com.aluggy.api.controllers;

import com.aluggy.api.entities.User;
import com.aluggy.api.entities.enums.Role;
import com.aluggy.api.exceptions.UserAlreadyExistsException;
import com.aluggy.api.repositories.UserRepository;
import com.aluggy.api.services.TokenService;
import com.aluggy.api.services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthenticationController.class)
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UserRepository userRepository;

    private User createTestUser() {
        User user = new User("johndoe", "John Doe", "john@email.com", "99123456789", "encoded-password", Role.USER);
        user.setId(UUID.randomUUID());
        return user;
    }

    @Test
    void login_validCredentials_returns200WithToken() throws Exception {
        User user = createTestUser();
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        when(tokenService.generateToken(user)).thenReturn("mock-jwt-token");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"johndoe\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-jwt-token"));
    }

    @Test
    void login_validEmailCredentials_returns200WithToken() throws Exception {
        User user = createTestUser();
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        when(tokenService.generateToken(user)).thenReturn("mock-jwt-token");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"john@email.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-jwt-token"));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"johndoe\"," +
                                "\"password\":\"wrongpassword\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_nonExistentUser_returns401() throws Exception {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"nonexistent\",\"password\":\"password123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_nullLoginField_returns400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_nullPasswordField_returns400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"johndoe\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_emptyBody_returns400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_validData_returns201WithUserResponse() throws Exception {
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");

        User savedUser = createTestUser();
        savedUser.setPassword("encoded-password");
        when(userService.insert(any(User.class))).thenReturn(savedUser);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userName\":\"johndoe\",\"fullName\":\"John Doe\",\"emailAddress\":\"john@email.com\",\"contactNumber\":\"99123456789\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userName").value("johndoe"))
                .andExpect(jsonPath("$.fullName").value("John Doe"))
                .andExpect(jsonPath("$.EmailAddress").value("john@email.com"))
                .andExpect(jsonPath("$.contactNumber").value("99123456789"));
    }

    @Test
    void register_validData_createsUserWithRoleUSER() throws Exception {
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");

        User savedUser = createTestUser();
        when(userService.insert(any(User.class))).thenReturn(savedUser);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userName\":\"johndoe\",\"fullName\":\"John Doe\",\"emailAddress\":\"john@email.com\",\"contactNumber\":\"99123456789\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated());

        verify(userService).insert(argThat(user -> user.getRole() == Role.USER));
    }

    @Test
    void register_duplicateUser_returns409() throws Exception {
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userService.insert(any(User.class)))
                .thenThrow(new UserAlreadyExistsException("User already exists"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userName\":\"johndoe\",\"fullName\":\"John Doe\",\"emailAddress\":\"john@email.com\",\"contactNumber\":\"99123456789\",\"password\":\"password123\"}"))
                .andExpect(status().isConflict());

        verify(userService).insert(any(User.class));
    }

    @Test
    void register_missingFullName_returns400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userName\":\"johndoe\",\"emailAddress\":\"john@email.com\",\"contactNumber\":\"99123456789\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_missingContactNumber_returns400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userName\":\"johndoe\",\"fullName\":\"John Doe\",\"emailAddress\":\"john@email.com\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_emptyUserName_returns400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userName\":\"\",\"fullName\":\"John Doe\",\"emailAddress\":\"john@email.com\",\"contactNumber\":\"99123456789\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_roleFieldIsIgnored_alwaysCreatesAsUSER() throws Exception {
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");

        User savedUser = new User("adminuser", "Admin User", "admin@email.com", "99123456789", "encoded-password", Role.USER);
        savedUser.setId(UUID.randomUUID());
        when(userService.insert(any(User.class))).thenReturn(savedUser);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userName\":\"adminuser\",\"fullName\":\"Admin User\",\"emailAddress\":\"admin@email.com\",\"contactNumber\":\"99123456789\",\"password\":\"password123\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isCreated());

        verify(userService).insert(argThat(user -> user.getRole() == Role.USER));
    }

    @Test
    void register_emptyBody_returns400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_emptyPassword_returns400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userName\":\"johndoe\",\"fullName\":\"John Doe\",\"emailAddress\":\"john@email.com\",\"contactNumber\":\"99123456789\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_invalidEmailFormat_returns400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userName\":\"johndoe\",\"fullName\":\"John Doe\",\"emailAddress\":\"not-an-email\",\"contactNumber\":\"99123456789\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest());
    }
}
