package com.aluggy.api.services;

import com.aluggy.api.entities.User;
import com.aluggy.api.exceptions.UserNotFoundException;
import com.aluggy.api.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
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
        return repository.save(user);
    }

    public void delete(UUID id) {
        repository.deleteById(id);
    }


}
