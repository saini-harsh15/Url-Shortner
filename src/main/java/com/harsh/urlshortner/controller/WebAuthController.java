package com.harsh.urlshortner.controller;

import com.harsh.urlshortner.dto.RegisterRequest;
import com.harsh.urlshortner.service.AuthService;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class WebAuthController {

    private final AuthService authService;

    public WebAuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/register")
    public String registerPage(Model model) {

        model.addAttribute("registerRequest", new RegisterRequest());

        return "register";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("registerRequest")
            RegisterRequest request,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            authService.register(request);
        } catch (RuntimeException ex) {
            // We'll improve this error handling shortly.
            bindingResult.reject(
                    "registrationError",
                    ex.getMessage()
            );

            return "register";
        }

        return "redirect:/login?registered=true";
    }
}