package com.aluggy.api.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequestDTO(@NotBlank(message = "Username is required")
                                 @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
                                 String userName,

                                 @NotBlank(message = "Full name is required")
                                 @Size(max = 100, message = "Full name must be a maximum of 100 characters")
                                 String fullName,

                                 @NotBlank(message = "Email is required")
                                 @Email(message = "Invalid email format")
                                 @Size(max = 255, message = "Email must be a maximum of 255 characters")
                                 String emailAddress,

                                 @NotBlank(message = "Contact number is required")
                                 @Size(min = 11, max=11, message = "Contact number must be exactly 11 characters")
                                 String contactNumber,

                                 @NotBlank(message = "Password is required")
                                 @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
                                 String password) {
}
