package com.aluggy.api.controllers;

import com.aluggy.api.entities.User;
import com.aluggy.api.entities.enums.Role;
import com.aluggy.api.exceptions.UserAlreadyExistsException;
import com.aluggy.api.exceptions.UserNotFoundException;
import com.aluggy.api.infra.security.SecurityConfigurations;
import com.aluggy.api.infra.security.SecurityFilter;
import com.aluggy.api.repositories.UserRepository;
import com.aluggy.api.services.TokenService;
import com.aluggy.api.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = UserController.class, excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import({SecurityConfigurations.class, SecurityFilter.class})
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

        lenient().when(tokenService.validateToken("valid-token")).thenReturn("johndoe");
        lenient().when(userRepository.findByUserNameOrEmailAddress("johndoe", "johndoe"))
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
        when(userService.findById(testUserId)).thenReturn(testUser);

        mockMvc.perform(get("/users/{id}", testUserId)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("johndoe"));
    }

    @Test
    void findById_nonExistentUser_returns404() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        when(userService.findById(nonExistentId))
                .thenThrow(new UserNotFoundException("Usuário não encontrado"));

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
    void insert_duplicateUsername_returns409() throws Exception {
        when(userService.insert(any(User.class)))
                .thenThrow(new UserAlreadyExistsException("Username já cadastrado"));

        mockMvc.perform(post("/users")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userName\":\"janedoe\",\"fullName\":\"Jane Doe\",\"emailAddress\":\"jane@email.com\",\"contactNumber\":\"0987654321\",\"password\":\"password\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void delete_existingUser_returns204() throws Exception {
        mockMvc.perform(delete("/users/{id}", testUserId)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNoContent());

        verify(userService).delete(eq(testUserId), any(User.class));
    }

    @Test
    void delete_nonExistentUser_returns404() throws Exception {
        doThrow(new UserNotFoundException("Usuário não encontrado"))
                .when(userService).delete(eq(testUserId), any(User.class));

        mockMvc.perform(delete("/users/{id}", testUserId)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_differentUser_returns403() throws Exception {
        UUID otherId = UUID.randomUUID();
        doThrow(new AccessDeniedException("Forbidden"))
                .when(userService).delete(eq(otherId), any(User.class));

        mockMvc.perform(delete("/users/{id}", otherId)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticated_accessReturns401() throws Exception {
        when(tokenService.validateToken(anyString())).thenReturn("");

        mockMvc.perform(get("/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticated_noTokenHeader_returns401() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticated_findById_returns401() throws Exception {
        mockMvc.perform(get("/users/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticated_delete_returns401() throws Exception {
        mockMvc.perform(delete("/users/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticated_insert_returns401() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userName\":\"test\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenValidButUserDeleted_returns401() throws Exception {
        when(tokenService.validateToken(anyString())).thenReturn("deleteduser");
        when(userRepository.findByUserNameOrEmailAddress("deleteduser", "deleteduser"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/users")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenValidButUserDeleted_findById_returns401() throws Exception {
        when(tokenService.validateToken(anyString())).thenReturn("deleteduser");
        when(userRepository.findByUserNameOrEmailAddress("deleteduser", "deleteduser"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/users/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isUnauthorized());
    }
}