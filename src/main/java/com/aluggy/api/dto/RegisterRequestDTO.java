package com.aluggy.api.dto;

import com.aluggy.api.entities.enums.Role;

public record RegisterRequestDTO(String userName, String fullName, String emailAddress, String contactNumber, String password, Role role) {
}
