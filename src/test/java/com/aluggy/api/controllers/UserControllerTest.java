package com.aluggy.api.controllers;

import com.aluggy.api.entities.User;
import com.aluggy.api.entities.enums.Role;
import com.aluggy.api.infra.security.SecurityConfigurations;
import com.aluggy.api.repositories.UserRepository;
import com.aluggy.api.services.TokenService;
import com.aluggy.api.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UserRepository userRepository;

    private User testUser;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = new User("johndoe", "John Doe", "john@email.com", "1234567890", "encoded-password", Role.USER);
        testUser.setId(testUserId);

        when(tokenService.validateToken(anyString())).thenReturn("johndoe");
        when(userRepository.findByUserNameOrEmailAddress("johndoe", "johndoe"))
                .thenReturn(Optional.of(testUser));
    }

    @Test
    void findAll_authenticated_returns200WithUsers() throws Exception {
        when(userService.findAll()).thenReturn(List.of(testUser));

        mockMvc.perform(get("/users")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("johndoe"));
    }

    @Test
    void findAll_authenticated_emptyList_returns200() throws Exception {
        when(userService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/users")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void findById_existingUser_returns200() throws Exception {
        when(userService.findById(testUserId)).thenReturn(Optional.of(testUser));

        mockMvc.perform(get("/users/{id}", testUserId)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("johndoe"));
    }

    @Test
    void findById_nonExistentUser_returns404() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        when(userService.findById(nonExistentId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/users/{id}", nonExistentId)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void insert_validUser_returns201WithLocation() throws Exception {
        User newUser = new User("janedoe", "Jane Doe", "jane@email.com", "0987654321", "password", Role.USER);
        UUID newId = UUID.randomUUID();
        newUser.setId(newId);
        when(userService.insert(any(User.class))).thenReturn(newUser);

        mockMvc.perform(post("/users")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userName\":\"janedoe\",\"fullName\":\"Jane Doe\",\"emailAddress\":\"jane@email.com\",\"contactNumber\":\"0987654321\",\"password\":\"password\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"));
    }

    @Test
    void delete_existingUser_returns204() throws Exception {
        mockMvc.perform(delete("/users/{id}", testUserId)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNoContent());

        verify(userService).delete(testUserId);
    }

    @Test
    void unauthenticated_accessReturns401Or403() throws Exception {
        when(tokenService.validateToken(anyString())).thenReturn("");

        mockMvc.perform(get("/users"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 401 && status != 403) {
                        throw new AssertionError("Expected 401 or 403 but got " + status);
                    }
                });
    }

    @Test
    void unauthenticated_noTokenHeader_returns401Or403() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 401 && status != 403) {
                        throw new AssertionError("Expected 401 or 403 but got " + status);
                    }
                });
    }

    @Test
    void unauthenticated_findById_returns401Or403() throws Exception {
        mockMvc.perform(get("/users/{id}", UUID.randomUUID()))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 401 && status != 403) {
                        throw new AssertionError("Expected 401 or 403 but got " + status);
                    }
                });
    }

    @Test
    void unauthenticated_delete_returns401Or403() throws Exception {
        mockMvc.perform(delete("/users/{id}", UUID.randomUUID()))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 401 && status != 403) {
                        throw new AssertionError("Expected 401 or 403 but got " + status);
                    }
                });
    }

    @Test
    void unauthenticated_insert_returns401Or403() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userName\":\"test\"}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 401 && status != 403) {
                        throw new AssertionError("Expected 401 or 403 but got " + status);
                    }
                });
    }

    @Test
    void tokenValidButUserDeleted_noAuthentication_noAccess() throws Exception {
        when(tokenService.validateToken(anyString())).thenReturn("deleteduser");
        when(userRepository.findByUserNameOrEmailAddress("deleteduser", "deleteduser"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/users")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 401 && status != 403) {
                        throw new AssertionError("Expected 401 or 403 when deleted user token is used, but got " + status);
                    }
                });
    }

    @Test
    void tokenValidButUserDeleted_findById_noAccess() throws Exception {
        when(tokenService.validateToken(anyString())).thenReturn("deleteduser");
        when(userRepository.findByUserNameOrEmailAddress("deleteduser", "deleteduser"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/users/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 401 && status != 403) {
                        throw new AssertionError("Expected 401 or 403 when deleted user token is used, but got " + status);
                    }
                });
    }
}
