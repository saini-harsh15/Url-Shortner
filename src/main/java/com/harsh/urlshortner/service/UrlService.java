package com.harsh.urlshortner.service;

import com.harsh.urlshortner.dto.CreateUrlRequest;
import com.harsh.urlshortner.dto.UrlResponse;
import com.harsh.urlshortner.entity.Url;
import com.harsh.urlshortner.entity.User;
import com.harsh.urlshortner.exception.BadRequestException;
import com.harsh.urlshortner.exception.ResourceNotFoundException;
import com.harsh.urlshortner.repository.UrlRepository;
import com.harsh.urlshortner.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
public class UrlService {

    private final UrlRepository urlRepository;
    private final UserRepository userRepository;

    @Value("${app.base-url}")
    private String baseUrl;

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


        if (request.getExpiresAt() != null &&
                request.getExpiresAt().isBefore(LocalDateTime.now())) {

            throw new BadRequestException(
                    "Expiration time must be in the future"
            );
        }


        String shortCode = generateUniqueShortCode();

        Url url = new Url();

        url.setOriginalUrl(request.getOriginalUrl());
        url.setShortCode(shortCode);
        url.setCreatedAt(LocalDateTime.now());
        url.setExpiresAt(request.getExpiresAt());
        url.setClickCount(0L);
        url.setUser(user);

        Url savedUrl = urlRepository.save(url);

        String shortUrl = baseUrl + "/" + savedUrl.getShortCode();

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
                baseUrl + "/" + url.getShortCode();

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

    public List<UrlResponse> getUserUrls(Long userId) {

        List<Url> urls = urlRepository.findAllByUserId(userId);

        return urls.stream()
                .map(url -> new UrlResponse(
                        url.getId(),
                        url.getOriginalUrl(),
                        url.getShortCode(),
                        baseUrl + "/" + url.getShortCode(),
                        url.getCreatedAt(),
                        url.getExpiresAt(),
                        url.getClickCount()
                ))
                .toList();
    }
}