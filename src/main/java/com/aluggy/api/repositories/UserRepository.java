package com.aluggy.api.repositories;

import com.aluggy.api.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUserNameOrEmailAddress(String userName, String emailAddress);
    boolean existsByUsername(String userName);
    boolean existsByEmail(String emailAddress);
}
