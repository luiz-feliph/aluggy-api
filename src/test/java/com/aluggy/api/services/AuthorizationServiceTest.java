package com.aluggy.api.services;

import com.aluggy.api.entities.User;
import com.aluggy.api.entities.enums.Role;
import com.aluggy.api.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock
    private UserRepository repository;

    @InjectMocks
    private AuthorizationService service;

    private User createTestUser(String username, String email) {
        User user = new User(username, email, "1234567890", "encoded-password", Role.USER);
        user.setId(UUID.randomUUID());
        return user;
    }

    @Test
    void loadUserByUsername_foundByUsername() {
        User user = createTestUser("johndoe", "john@email.com");
        when(repository.findByUserNameOrEmailAddress("johndoe", "johndoe"))
                .thenReturn(Optional.of(user));

        var result = service.loadUserByUsername("johndoe");

        assertNotNull(result);
        assertEquals("johndoe", result.getUsername());
        verify(repository).findByUserNameOrEmailAddress("johndoe", "johndoe");
    }

    @Test
    void loadUserByUsername_foundByEmail() {
        User user = createTestUser("johndoe", "john@email.com");
        when(repository.findByUserNameOrEmailAddress("john@email.com", "john@email.com"))
                .thenReturn(Optional.of(user));

        var result = service.loadUserByUsername("john@email.com");

        assertNotNull(result);
        assertEquals("johndoe", result.getUsername());
        verify(repository).findByUserNameOrEmailAddress("john@email.com", "john@email.com");
    }

    @Test
    void loadUserByUsername_notFound_throwsUsernameNotFoundException() {
        when(repository.findByUserNameOrEmailAddress("nonexistent", "nonexistent"))
                .thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("nonexistent"));
    }

    @Test
    void loadUserByUsername_notFound_throwsWithCorrectMessage() {
        when(repository.findByUserNameOrEmailAddress("nonexistent", "nonexistent"))
                .thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("nonexistent"));

        assertEquals("Usuário não encontrado", exception.getMessage());
    }

    @Test
    void loadUserByUsername_delegatesToRepositoryWithSameValueForBothParams() {
        when(repository.findByUserNameOrEmailAddress("admin", "admin"))
                .thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("admin"));

        verify(repository).findByUserNameOrEmailAddress("admin", "admin");
    }

    @Test
    void loadUserByUsername_returnsUserDetails() {
        User user = createTestUser("admin", "admin@email.com");
        user.setRole(Role.ADMIN);
        when(repository.findByUserNameOrEmailAddress("admin", "admin"))
                .thenReturn(Optional.of(user));

        var result = service.loadUserByUsername("admin");

        assertNotNull(result);
        assertEquals("admin", result.getUsername());
        assertTrue(result.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }
}
