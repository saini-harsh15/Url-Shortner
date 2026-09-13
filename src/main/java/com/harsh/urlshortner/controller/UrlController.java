package com.harsh.urlshortner.controller;

import com.harsh.urlshortner.dto.CreateUrlRequest;
import com.harsh.urlshortner.dto.UrlResponse;
import com.harsh.urlshortner.service.UrlService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/urls")
public class UrlController {

    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    @PostMapping
    public ResponseEntity<UrlResponse> createUrl(
            @Valid @RequestBody CreateUrlRequest request,
            Authentication authentication) {

        Long userId = Long.parseLong(authentication.getName());

        UrlResponse response =
                urlService.createUrl(request, userId);

        return ResponseEntity.ok(response);
    }
}