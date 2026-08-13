package com.aluggy.api.services;

import com.aluggy.api.entities.User;
import com.aluggy.api.entities.enums.Role;
import com.aluggy.api.exceptions.UserAlreadyExistsException;
import com.aluggy.api.exceptions.UserNotFoundException;
import com.aluggy.api.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;

    public List<User> findAll() {
        return repository.findAll();
    }

    public User findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(
                        "User with id " + id + " not found"
                ));
    }

    public boolean existsByUserName(String username) {
        return repository.existsByUserName(username);
    }

    public boolean existsByEmailAddress(String emailAddress) {
        return repository.existsByEmailAddress(emailAddress);
    }

    public User insert(User user) {
        if (existsByUserName(user.getUsername())) {
            throw new UserAlreadyExistsException("User with username " + user.getUsername() + " already exists");
        }

        if (existsByEmailAddress(user.getEmailAddress())) {
            throw new UserAlreadyExistsException("User with email " + user.getEmailAddress() + " already exists");
        }

        return repository.save(user);
    }

    public void delete(UUID id, User authenticatedUser) {
        if (!repository.existsById(id)) {
            throw new UserNotFoundException("User with id " + id + " does not exist");
        }

        boolean isAdmin = authenticatedUser.getRole().equals(Role.ADMIN);
        boolean isOwner = authenticatedUser.getId().equals(id);

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("You are not allowed to delete this user");
        }

        repository.deleteById(id);
    }


}
