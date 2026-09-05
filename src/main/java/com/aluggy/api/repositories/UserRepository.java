package com.aluggy.api.repositories;

import com.aluggy.api.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("""
    SELECT user FROM User user
    WHERE LOWER(user.userName) = LOWER(:login)
       OR LOWER(user.emailAddress) = LOWER(:login)
    """)
    Optional<User> findByUserNameOrEmailAddress(@Param("login") String login);

    boolean existsByUserName(String userName);
    boolean existsByEmailAddress(String emailAddress);
}
