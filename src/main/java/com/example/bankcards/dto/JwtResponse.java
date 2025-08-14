package com.example.bankcards.dto;

// возврат токена после авторизации
public class JwtResponse {
    private String token;
    public JwtResponse() {}
    public JwtResponse(String token) { this.token = token; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
