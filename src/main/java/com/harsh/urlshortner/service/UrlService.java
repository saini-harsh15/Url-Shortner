package com.harsh.urlshortner.service;

import com.harsh.urlshortner.dto.CreateUrlRequest;
import com.harsh.urlshortner.dto.UrlResponse;
import com.harsh.urlshortner.entity.Url;
import com.harsh.urlshortner.entity.User;
import com.harsh.urlshortner.exception.ResourceNotFoundException;
import com.harsh.urlshortner.repository.UrlRepository;
import com.harsh.urlshortner.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class UrlService {

    private final UrlRepository urlRepository;
    private final UserRepository userRepository;

    private static final String CHARACTERS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private final Random random = new Random();

    public UrlService(
            UrlRepository urlRepository,
            UserRepository userRepository) {
        this.urlRepository = urlRepository;
        this.userRepository = userRepository;
    }

    public UrlResponse createUrl(
            CreateUrlRequest request,
            Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        String shortCode = generateUniqueShortCode();

        Url url = new Url();

        url.setOriginalUrl(request.getOriginalUrl());
        url.setShortCode(shortCode);
        url.setCreatedAt(LocalDateTime.now());
        url.setExpiresAt(request.getExpiresAt());
        url.setClickCount(0L);
        url.setUser(user);

        Url savedUrl = urlRepository.save(url);

        String shortUrl =
                "http://localhost:8080/" + savedUrl.getShortCode();

        return new UrlResponse(
                savedUrl.getId(),
                savedUrl.getOriginalUrl(),
                savedUrl.getShortCode(),
                shortUrl,
                savedUrl.getCreatedAt(),
                savedUrl.getExpiresAt(),
                savedUrl.getClickCount()
        );
    }

    private String generateUniqueShortCode() {

        String shortCode;

        do {
            StringBuilder builder = new StringBuilder();

            for (int i = 0; i < 6; i++) {
                int index = random.nextInt(CHARACTERS.length());
                builder.append(CHARACTERS.charAt(index));
            }

            shortCode = builder.toString();

        } while (urlRepository.existsByShortCode(shortCode));

        return shortCode;
    }

    public UrlResponse getUrl(
            String shortCode,
            Long userId) {

        Url url = urlRepository
                .findByShortCodeAndUserId(shortCode, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("URL not found"));

        String shortUrl =
                "http://localhost:8080/" + url.getShortCode();

        return new UrlResponse(
                url.getId(),
                url.getOriginalUrl(),
                url.getShortCode(),
                shortUrl,
                url.getCreatedAt(),
                url.getExpiresAt(),
                url.getClickCount()
        );
    }

    public void deleteUrl(
            String shortCode,
            Long userId) {

        Url url = urlRepository
                .findByShortCodeAndUserId(shortCode, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("URL not found"));

        urlRepository.delete(url);
    }
}