package com.aluggy.api.services;

import com.aluggy.api.entities.User;
import com.aluggy.api.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository repository;

    public List<User> findAll() {
        return repository.findAll();
    }

    public Optional<User> findById(UUID id) {
        return repository.findById(id);
    }

    public User insert(User user) {
        return repository.save(user);
    }

    public void delete(UUID id) {
        repository.deleteById(id);
    }
}
