package com.aluggy.api.dto;

import com.aluggy.api.entities.enums.Role;

public record RegisterRequestDTO(String userName, String emailAddress, String fullName, String password, Role role) {
}
