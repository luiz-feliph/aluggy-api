package com.aluggy.api.dto;

import com.aluggy.api.entities.enums.Role;

public record RegisterRequestDTO(String login, String password, Role role) {
}
