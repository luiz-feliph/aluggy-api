package com.aluggy.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDTO(
                              @NotBlank(message = "Login is required")
                              @Size(max = 255, message = "Login must not exceed 255 characters")
                              String login,

                              @NotBlank(message = "Password is required")
                              @Size(max = 255, message = "Password must not exceed 255 characters")
                              String password) {
}
