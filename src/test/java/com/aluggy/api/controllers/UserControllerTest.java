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
import jakarta.servlet.http.Cookie;
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
        testUser = new User("johndoe", "john@email.com", "1234567890", "encoded-password", Role.USER);
        testUser.setId(testUserId);

        lenient().when(tokenService.validateToken("valid-token")).thenReturn("johndoe");
        lenient().when(userRepository.findByUserNameOrEmailAddress("johndoe"))
                .thenReturn(Optional.of(testUser));
    }

    @Test
    void delete_existingUser_returns204() throws Exception {
        mockMvc.perform(delete("/users/{id}", testUserId)
                        .cookie(new Cookie("AUTH_TOKEN", "valid-token")))
                .andExpect(status().isNoContent());

        verify(userService).delete(eq(testUserId), any(User.class));
    }

    @Test
    void delete_nonExistentUser_returns404() throws Exception {
        doThrow(new UserNotFoundException("Usuário não encontrado"))
                .when(userService).delete(eq(testUserId), any(User.class));

        mockMvc.perform(delete("/users/{id}", testUserId)
                        .cookie(new Cookie("AUTH_TOKEN", "valid-token")))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_differentUser_returns403() throws Exception {
        UUID otherId = UUID.randomUUID();
        doThrow(new AccessDeniedException("Forbidden"))
                .when(userService).delete(eq(otherId), any(User.class));

        mockMvc.perform(delete("/users/{id}", otherId)
                        .cookie(new Cookie("AUTH_TOKEN", "valid-token")))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_ownUserAsOwner_returns204() throws Exception {
        mockMvc.perform(delete("/users/{id}", testUserId)
                        .cookie(new Cookie("AUTH_TOKEN", "valid-token")))
                .andExpect(status().isNoContent());

        verify(userService).delete(eq(testUserId), any(User.class));
    }

    @Test
    void delete_ownUserAsAdmin_returns204() throws Exception {
        User adminUser = new User("adminuser", "admin@email.com", "99123456789", "encoded-password", Role.ADMIN);
        adminUser.setId(UUID.randomUUID());

        lenient().when(tokenService.validateToken("admin-token")).thenReturn("adminuser");
        lenient().when(userRepository.findByUserNameOrEmailAddress("adminuser"))
                .thenReturn(Optional.of(adminUser));

        mockMvc.perform(delete("/users/{id}", testUserId)
                        .cookie(new Cookie("AUTH_TOKEN", "admin-token")))
                .andExpect(status().isNoContent());

        verify(userService).delete(eq(testUserId), any(User.class));
    }

    @Test
    void delete_otherUserAsRegularUser_returns403() throws Exception {
        UUID otherId = UUID.randomUUID();
        doThrow(new AccessDeniedException("You are not allowed to delete this user"))
                .when(userService).delete(eq(otherId), any(User.class));

        mockMvc.perform(delete("/users/{id}", otherId)
                        .cookie(new Cookie("AUTH_TOKEN", "valid-token")))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_nonExistentId_returns404() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        doThrow(new UserNotFoundException("User with id " + nonExistentId + " does not exist"))
                .when(userService).delete(eq(nonExistentId), any(User.class));

        mockMvc.perform(delete("/users/{id}", nonExistentId)
                        .cookie(new Cookie("AUTH_TOKEN", "valid-token")))
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticated_accessReturns401() throws Exception {
        when(tokenService.validateToken(anyString())).thenReturn("");

        mockMvc.perform(delete("/users/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticated_noCookie_returns401() throws Exception {
        mockMvc.perform(delete("/users/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticated_delete_returns401() throws Exception {
        mockMvc.perform(delete("/users/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenValidButUserDeleted_returns401() throws Exception {
        when(tokenService.validateToken(anyString())).thenReturn("deleteduser");
        when(userRepository.findByUserNameOrEmailAddress("deleteduser"))
                .thenReturn(Optional.empty());

        mockMvc.perform(delete("/users/{id}", UUID.randomUUID())
                        .cookie(new Cookie("AUTH_TOKEN", "valid-token")))
                .andExpect(status().isUnauthorized());
    }
}
