package com.harsh.urlshortner;

import com.harsh.urlshortner.entity.Url;
import com.harsh.urlshortner.entity.User;
import com.harsh.urlshortner.repository.UrlRepository;
import com.harsh.urlshortner.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RedirectControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UrlRepository urlRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(new User("redirect_user@example.com", passwordEncoder.encode("secret123"), LocalDateTime.now()));
    }

    private Url createUrl(String originalUrl, String shortCode, LocalDateTime expiresAt) {
        Url url = new Url();
        url.setUser(testUser);
        url.setOriginalUrl(originalUrl);
        url.setShortCode(shortCode);
        url.setCreatedAt(LocalDateTime.now());
        url.setExpiresAt(expiresAt);
        url.setClickCount(0L);
        return urlRepository.save(url);
    }

    @Test
    @DisplayName("GET /{shortCode} - Valid short code without expiration returns 302 Found and Location header")
    void redirect_ValidShortCode_Returns302AndRedirects() throws Exception {
        Url url = createUrl("https://spring.io", "spring1", null);

        mockMvc.perform(get("/{shortCode}", "spring1"))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "https://spring.io"));

        // Verify click count incremented
        entityManager.clear();
        Url updatedUrl = urlRepository.findById(url.getId()).orElseThrow();
        assertThat(updatedUrl.getClickCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("GET /{shortCode} - Valid short code with future expiration redirects successfully")
    void redirect_FutureExpiration_Returns302() throws Exception {
        createUrl("https://github.com", "git001", LocalDateTime.now().plusDays(10));

        mockMvc.perform(get("/{shortCode}", "git001"))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "https://github.com"));
    }

    @Test
    @DisplayName("GET /{shortCode} - Expired short code returns 410 Gone and does not increment clickCount")
    void redirect_ExpiredUrl_Returns410Gone() throws Exception {
        Url url = createUrl("https://expired.example.com", "exp001", LocalDateTime.now().minusMinutes(5));

        mockMvc.perform(get("/{shortCode}", "exp001"))
                .andExpect(status().isGone());

        // Verify click count was NOT incremented
        Url unchangedUrl = urlRepository.findById(url.getId()).orElseThrow();
        assertThat(unchangedUrl.getClickCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("GET /{shortCode} - Nonexistent short code returns 404 Not Found")
    void redirect_NonexistentShortCode_Returns404() throws Exception {
        mockMvc.perform(get("/{shortCode}", "missing9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Short URL not found"));
    }

    @Test
    @DisplayName("GET /{shortCode} - Multiple redirects increment clickCount correctly")
    void redirect_MultipleRedirects_IncrementsClickCountEachTime() throws Exception {
        Url url = createUrl("https://multi.example.com", "multi1", null);

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/{shortCode}", "multi1"))
                    .andExpect(status().isFound())
                    .andExpect(header().string(HttpHeaders.LOCATION, "https://multi.example.com"));
        }

        entityManager.clear();
        Url updatedUrl = urlRepository.findById(url.getId()).orElseThrow();
        assertThat(updatedUrl.getClickCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("GET /{shortCode} - Public access works without any JWT token or Authorization header")
    void redirect_PublicAccess_NoAuthHeaderRequired() throws Exception {
        createUrl("https://public-access.example.com", "public1", null);

        mockMvc.perform(get("/{shortCode}", "public1"))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "https://public-access.example.com"));
    }
}
