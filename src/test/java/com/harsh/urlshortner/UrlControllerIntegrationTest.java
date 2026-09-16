package com.harsh.urlshortner;

import tools.jackson.databind.ObjectMapper;
import com.harsh.urlshortner.dto.CreateUrlRequest;
import com.harsh.urlshortner.entity.Url;
import com.harsh.urlshortner.entity.User;
import com.harsh.urlshortner.repository.UrlRepository;
import com.harsh.urlshortner.repository.UserRepository;
import com.harsh.urlshortner.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UrlControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UrlRepository urlRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User user1;
    private String token1;
    private User user2;
    private String token2;

    @BeforeEach
    void setUp() {
        user1 = userRepository.save(new User("user1@example.com", passwordEncoder.encode("password123"), LocalDateTime.now()));
        token1 = jwtService.generateToken(user1.getId(), user1.getEmail());

        user2 = userRepository.save(new User("user2@example.com", passwordEncoder.encode("password123"), LocalDateTime.now()));
        token2 = jwtService.generateToken(user2.getId(), user2.getEmail());
    }

    private Url createUrlEntity(User owner, String originalUrl, String shortCode, LocalDateTime expiresAt) {
        Url url = new Url();
        url.setUser(owner);
        url.setOriginalUrl(originalUrl);
        url.setShortCode(shortCode);
        url.setCreatedAt(LocalDateTime.now());
        url.setExpiresAt(expiresAt);
        url.setClickCount(0L);
        return urlRepository.save(url);
    }

    @Test
    @DisplayName("POST /api/urls - Create URL when authenticated with valid HTTP/HTTPS URL")
    void createUrl_Authenticated_Success() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://example.com/some/long/path");

        mockMvc.perform(post("/api/urls")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.originalUrl").value("https://example.com/some/long/path"))
                .andExpect(jsonPath("$.shortCode").isString())
                .andExpect(jsonPath("$.shortCode", hasLength(6)))
                .andExpect(jsonPath("$.shortUrl", containsString("http://localhost:8080/")))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.expiresAt").doesNotExist())
                .andExpect(jsonPath("$.clickCount").value(0));
    }

    @Test
    @DisplayName("POST /api/urls - Create URL with valid expiresAt")
    void createUrl_WithExpiresAt_Success() throws Exception {
        LocalDateTime futureTime = LocalDateTime.now().plusDays(30);
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("http://example.org");
        request.setExpiresAt(futureTime);

        mockMvc.perform(post("/api/urls")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalUrl").value("http://example.org"))
                .andExpect(jsonPath("$.expiresAt").exists())
                .andExpect(jsonPath("$.shortCode").isString());
    }

    @Test
    @DisplayName("POST /api/urls - Unauthenticated request returns 401 Unauthorized")
    void createUrl_Unauthenticated_Returns401() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://example.com");

        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/urls - Request with invalid JWT returns 401 Unauthorized")
    void createUrl_InvalidJwt_Returns401() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://example.com");

        mockMvc.perform(post("/api/urls")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.jwt.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/urls - Blank URL returns 400 Bad Request")
    void createUrl_BlankUrl_Returns400() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("");

        mockMvc.perform(post("/api/urls")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.originalUrl").exists());
    }

    @Test
    @DisplayName("POST /api/urls - Non-HTTP/HTTPS protocol returns 400 Bad Request")
    void createUrl_InvalidProtocol_Returns400() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("ftp://files.example.com");

        mockMvc.perform(post("/api/urls")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.originalUrl").value("URL must start with http:// or https://"));
    }

    @Test
    @DisplayName("POST /api/urls - Very long URL within allowed limit (2048 chars) succeeds")
    void createUrl_LongUrl_Success() throws Exception {
        String longUrl = "https://example.com/test?" + "a=".repeat(200); // ~400 chars
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl(longUrl);

        mockMvc.perform(post("/api/urls")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalUrl").value(longUrl));
    }

    @Test
    @DisplayName("GET /api/urls/{shortCode} - Owner can retrieve own URL")
    void getUrl_Owner_Returns200AndUrlDetails() throws Exception {
        Url url = createUrlEntity(user1, "https://user1url.com", "codeU1", null);

        mockMvc.perform(get("/api/urls/{shortCode}", "codeU1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(url.getId()))
                .andExpect(jsonPath("$.originalUrl").value("https://user1url.com"))
                .andExpect(jsonPath("$.shortCode").value("codeU1"))
                .andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/codeU1"));
    }

    @Test
    @DisplayName("GET /api/urls/{shortCode} - User 2 cannot access User 1's URL (Returns 404)")
    void getUrl_AnotherUser_Returns404NotFound() throws Exception {
        createUrlEntity(user1, "https://secret-user1.com", "codeU1", null);

        // User 2 attempts to retrieve User 1's URL
        mockMvc.perform(get("/api/urls/{shortCode}", "codeU1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token2))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("URL not found"));
    }

    @Test
    @DisplayName("GET /api/urls/{shortCode} - Nonexistent URL returns 404 Not Found")
    void getUrl_Nonexistent_Returns404() throws Exception {
        mockMvc.perform(get("/api/urls/{shortCode}", "nonexist")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("URL not found"));
    }

    @Test
    @DisplayName("GET /api/urls/{shortCode} - Unauthenticated request returns 401")
    void getUrl_Unauthenticated_Returns401() throws Exception {
        createUrlEntity(user1, "https://example.com", "mycode", null);

        mockMvc.perform(get("/api/urls/{shortCode}", "mycode"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/urls - Returns list of only authenticated user's URLs")
    void getUserUrls_ReturnsOnlyAuthenticatedUserUrls() throws Exception {
        createUrlEntity(user1, "https://user1-one.com", "u1first", null);
        createUrlEntity(user1, "https://user1-two.com", "u1second", null);
        createUrlEntity(user2, "https://user2-one.com", "u2first", null);

        mockMvc.perform(get("/api/urls")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].shortCode", containsInAnyOrder("u1first", "u1second")))
                .andExpect(jsonPath("$[*].shortCode", not(hasItem("u2first"))));
    }

    @Test
    @DisplayName("GET /api/urls - Unauthenticated returns 401")
    void getUserUrls_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/urls"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /api/urls/{shortCode} - Owner can delete URL")
    void deleteUrl_Owner_Returns204NoContent() throws Exception {
        createUrlEntity(user1, "https://example.com/delete", "del001", null);

        mockMvc.perform(delete("/api/urls/{shortCode}", "del001")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token1))
                .andExpect(status().isNoContent());

        // Verify it is gone
        mockMvc.perform(get("/api/urls/{shortCode}", "del001")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token1))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/urls/{shortCode} - Another user cannot delete URL (Returns 404)")
    void deleteUrl_AnotherUser_Returns404NotFound() throws Exception {
        createUrlEntity(user1, "https://example.com/keep", "keep01", null);

        // User 2 attempts to delete User 1's URL
        mockMvc.perform(delete("/api/urls/{shortCode}", "keep01")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token2))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        // Verify URL still exists for User 1
        mockMvc.perform(get("/api/urls/{shortCode}", "keep01")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token1))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/urls/{shortCode} - Unauthenticated user returns 401")
    void deleteUrl_Unauthenticated_Returns401() throws Exception {
        createUrlEntity(user1, "https://example.com", "code12", null);

        mockMvc.perform(delete("/api/urls/{shortCode}", "code12"))
                .andExpect(status().isUnauthorized());
    }
}
