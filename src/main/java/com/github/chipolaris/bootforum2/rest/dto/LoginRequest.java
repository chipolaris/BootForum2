package com.github.chipolaris.bootforum2.rest.dto;

public class LoginRequest {
    @jakarta.validation.constraints.NotBlank
    private String username;
    @jakarta.validation.constraints.NotBlank
    private String password;

    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
