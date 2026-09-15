package com.harsh.urlshortner.controller;

import com.harsh.urlshortner.entity.Url;
import com.harsh.urlshortner.exception.ResourceNotFoundException;
import com.harsh.urlshortner.repository.UrlRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
public class RedirectController {

    private final UrlRepository urlRepository;

    public RedirectController(UrlRepository urlRepository) {
        this.urlRepository = urlRepository;
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(
            @PathVariable String shortCode) {

        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Short URL not found"));

        if (url.getExpiresAt() != null &&
                url.getExpiresAt().isBefore(LocalDateTime.now())) {

            return ResponseEntity.status(HttpStatus.GONE).build();
        }

        // Increment click count
        urlRepository.incrementClickCount(url.getId());

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(
                java.net.URI.create(url.getOriginalUrl())
        );

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .headers(headers)
                .build();
    }
}