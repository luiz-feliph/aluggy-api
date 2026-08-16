package com.aluggy.api.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequestDTO(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username must contain only letters, numbers, dots, underscores and hyphens")
        String userName,

        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name must be a maximum of 100 characters")
        @Pattern(regexp = "^[\\p{L}\\p{M}' .‐-]+$", message = "Full name must contain only letters, spaces, hyphens, apostrophes and dots")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Pattern(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Invalid email format")
        @Size(max = 255, message = "Email must be a maximum of 255 characters")
        String emailAddress,

        @NotBlank(message = "Contact number is required")
        @Pattern(regexp = "^[0-9]{11}$", message = "Contact number must contain exactly 11 digits")
        String contactNumber,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[\\x21-\\x7E]+$", message = "Password must contain at least one letter, one number and only ASCII characters")
        String password) {
}
