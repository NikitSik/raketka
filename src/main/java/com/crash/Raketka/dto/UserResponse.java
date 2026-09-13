package com.crash.Raketka.dto;

public record UserResponse(Long userId, String username,
                           int balance, int points) {
}