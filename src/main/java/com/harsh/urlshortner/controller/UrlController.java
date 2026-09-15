package com.harsh.urlshortner.controller;

import com.harsh.urlshortner.dto.CreateUrlRequest;
import com.harsh.urlshortner.dto.UrlResponse;
import com.harsh.urlshortner.service.UrlService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/{shortCode}")
    public ResponseEntity<UrlResponse> getUrl(
            @PathVariable String shortCode,
            Authentication authentication) {

        Long userId = Long.parseLong(authentication.getName());

        UrlResponse response =
                urlService.getUrl(shortCode, userId);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{shortCode}")
    public ResponseEntity<Void> deleteUrl(
            @PathVariable String shortCode,
            Authentication authentication) {

        Long userId = Long.parseLong(authentication.getName());

        urlService.deleteUrl(shortCode, userId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<UrlResponse>> getUserUrls(
            Authentication authentication) {

        Long userId = Long.parseLong(authentication.getName());

        List<UrlResponse> urls = urlService.getUserUrls(userId);

        return ResponseEntity.ok(urls);
    }

}