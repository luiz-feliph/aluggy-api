package com.aluggy.api.dto;

import com.aluggy.api.entities.User;
import com.aluggy.api.entities.enums.Role;

import java.util.UUID;

public record UserResponseDTO(
        UUID id,
        String userName,
        String fullName,
        String emailAddress,
        String contactNumber,
        String description,
        Role role
) {

    public UserResponseDTO(User user) {
        this(user.getId(), user.getUsername(), user.getFullName(), user.getEmailAddress(), user.getContactNumber(), user.getDescription(), user.getRole());
    }
}
