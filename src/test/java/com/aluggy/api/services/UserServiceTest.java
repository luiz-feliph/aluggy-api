package com.aluggy.api.services;

import com.aluggy.api.entities.User;
import com.aluggy.api.entities.enums.Role;
import com.aluggy.api.exceptions.UserAlreadyExistsException;
import com.aluggy.api.exceptions.UserNotFoundException;
import com.aluggy.api.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository repository;

    @InjectMocks
    private UserService service;

    private User createTestUser() {
        User user = new User("johndoe", "John Doe", "john@email.com", "1234567890", "password", Role.USER);
        user.setId(UUID.randomUUID());
        return user;
    }

    @Test
    void findAll_delegatesToRepository() {
        List<User> expectedUsers = List.of(createTestUser(), createTestUser());
        when(repository.findAll()).thenReturn(expectedUsers);

        List<User> result = service.findAll();

        assertEquals(expectedUsers, result);
        verify(repository).findAll();
    }

    @Test
    void findAll_emptyRepository_returnsEmptyList() {
        when(repository.findAll()).thenReturn(List.of());

        List<User> result = service.findAll();

        assertTrue(result.isEmpty());
        verify(repository).findAll();
    }

    @Test
    void findById_existingUser_returnsUser() {
        UUID id = UUID.randomUUID();
        User user = createTestUser();
        when(repository.findById(id)).thenReturn(Optional.of(user));

        User result = service.findById(id);

        assertEquals(user, result);
        verify(repository).findById(id);
    }

    @Test
    void findById_nonExistingUser_throwsUserNotFoundException() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> service.findById(id));
        verify(repository).findById(id);
    }

    @Test
    void insert_delegatesToRepository() {
        User user = createTestUser();
        User savedUser = createTestUser();
        savedUser.setId(UUID.randomUUID());
        when(repository.save(user)).thenReturn(savedUser);

        User result = service.insert(user);

        assertEquals(savedUser, result);
        verify(repository).save(user);
    }

    @Test
    void insert_duplicateUsername_throwsUserAlreadyExistsException() {
        User user = createTestUser();
        when(repository.existsByUserName("johndoe")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> service.insert(user));
        verify(repository, never()).save(any());
    }

    @Test
    void insert_duplicateEmail_throwsUserAlreadyExistsException() {
        User user = createTestUser();
        when(repository.existsByUserName("johndoe")).thenReturn(false);
        when(repository.existsByEmailAddress("john@email.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> service.insert(user));
        verify(repository, never()).save(any());
    }

    @Test
    void delete_owner_deletesSuccessfully() {
        User authenticatedUser = createTestUser();
        UUID id = authenticatedUser.getId();
        when(repository.existsById(id)).thenReturn(true);

        service.delete(id, authenticatedUser);

        verify(repository).deleteById(id);
    }

    @Test
    void delete_admin_deletesAnyUser() {
        User admin = createTestUser();
        admin.setRole(Role.ADMIN);

        User target = createTestUser();
        UUID targetId = target.getId();

        when(repository.existsById(targetId)).thenReturn(true);

        service.delete(targetId, admin);

        verify(repository).deleteById(targetId);
    }

    @Test
    void delete_nonOwnerUser_throwsAccessDeniedException() {
        User authenticatedUser = createTestUser();
        UUID otherId = UUID.randomUUID();
        when(repository.existsById(otherId)).thenReturn(true);

        assertThrows(AccessDeniedException.class, () -> service.delete(otherId, authenticatedUser));
        verify(repository, never()).deleteById(any());
    }

    @Test
    void delete_nonExistentUser_throwsUserNotFoundException() {
        User authenticatedUser = createTestUser();
        UUID id = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(false);

        assertThrows(UserNotFoundException.class, () -> service.delete(id, authenticatedUser));
        verify(repository, never()).deleteById(any());
    }

    @Test
    void existsByUserName_delegatesToRepository() {
        when(repository.existsByUserName("johndoe")).thenReturn(true);
        when(repository.existsByUserName("other")).thenReturn(false);

        assertTrue(service.existsByUserName("johndoe"));
        assertFalse(service.existsByUserName("other"));
        verify(repository).existsByUserName("johndoe");
        verify(repository).existsByUserName("other");
    }

    @Test
    void existsByEmailAddress_delegatesToRepository() {
        when(repository.existsByEmailAddress("john@email.com")).thenReturn(true);
        when(repository.existsByEmailAddress("other@email.com")).thenReturn(false);

        assertTrue(service.existsByEmailAddress("john@email.com"));
        assertFalse(service.existsByEmailAddress("other@email.com"));
        verify(repository).existsByEmailAddress("john@email.com");
        verify(repository).existsByEmailAddress("other@email.com");
    }
}