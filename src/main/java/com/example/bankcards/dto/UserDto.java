package com.example.bankcards.dto;

import java.util.UUID;

public class UserDto {
    private UUID id;
    private String username;
    private String role; // строкой, если у вас enum

    public UserDto() {}
    public UserDto(UUID id, String username, String role) {
        this.id = id; this.username = username; this.role = role;
    }
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
