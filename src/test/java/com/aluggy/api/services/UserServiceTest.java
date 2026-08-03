package com.aluggy.api.services;

import com.aluggy.api.entities.User;
import com.aluggy.api.entities.enums.Role;
import com.aluggy.api.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

        Optional<User> result = service.findById(id);

        assertTrue(result.isPresent());
        assertEquals(user, result.get());
        verify(repository).findById(id);
    }

    @Test
    void findById_nonExistingUser_returnsEmpty() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        Optional<User> result = service.findById(id);

        assertTrue(result.isEmpty());
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
    void delete_callsRepositoryDeleteById() {
        UUID id = UUID.randomUUID();

        service.delete(id);

        verify(repository).deleteById(id);
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
