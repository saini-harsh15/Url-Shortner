package com.harsh.urlshortner.controller;


import com.harsh.urlshortner.dto.LoginRequest;
import com.harsh.urlshortner.dto.LoginResponse;
import com.harsh.urlshortner.dto.RegisterRequest;
import com.harsh.urlshortner.dto.RegisterResponse;
import com.harsh.urlshortner.entity.User;
import com.harsh.urlshortner.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {


    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        User user = authService.register(request);

        RegisterResponse response =
                new RegisterResponse(
                        user.getId(),
                        user.getEmail()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        String token = authService.login(request);

        return ResponseEntity.ok(
                new LoginResponse(token)
        );
    }
}