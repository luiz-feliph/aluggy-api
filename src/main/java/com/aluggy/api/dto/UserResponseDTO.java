package com.aluggy.api.dto;

import java.util.UUID;

public record UserResponseDTO(UUID id, String userName, String emailAddress, String contactNumber) {
}
