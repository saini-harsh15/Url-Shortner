package com.harsh.urlshortner.dto;

public class RegisterResponse {

    private Long id;
    private String email;

    public RegisterResponse(Long id, String email) {
        this.id = id;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }
}