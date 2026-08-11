package com.aluggy.api.dto;

public record RegisterRequestDTO(String userName, String fullName, String emailAddress, String contactNumber, String password) {
}
