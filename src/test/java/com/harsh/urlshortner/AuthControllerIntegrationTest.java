package com.harsh.urlshortner;

import tools.jackson.databind.ObjectMapper;
import com.harsh.urlshortner.dto.LoginRequest;
import com.harsh.urlshortner.dto.RegisterRequest;
import com.harsh.urlshortner.entity.User;
import com.harsh.urlshortner.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("POST /auth/register - Successfully register valid user")
    void register_ValidUser_Returns201Created() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("john.doe@example.com");
        request.setPassword("securePassword123");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));
    }

    @Test
    @DisplayName("POST /auth/register - Duplicate email returns 409 Conflict")
    void register_DuplicateEmail_Returns409Conflict() throws Exception {
        // Save initial user
        User existingUser = new User("duplicate@example.com", passwordEncoder.encode("password123"), LocalDateTime.now());
        userRepository.save(existingUser);

        RegisterRequest request = new RegisterRequest();
        request.setEmail("duplicate@example.com");
        request.setPassword("password123");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Email already registered"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("POST /auth/register - Invalid email format returns 400 Bad Request")
    void register_InvalidEmail_Returns400BadRequest() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("invalid-email-address");
        request.setPassword("password123");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    @DisplayName("POST /auth/register - Blank email returns 400 Bad Request")
    void register_BlankEmail_Returns400BadRequest() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("");
        request.setPassword("password123");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    @DisplayName("POST /auth/register - Blank password returns 400 Bad Request")
    void register_BlankPassword_Returns400BadRequest() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("valid@example.com");
        request.setPassword("");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    @DisplayName("POST /auth/register - Password below 8 characters returns 400 Bad Request")
    void register_ShortPassword_Returns400BadRequest() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("valid@example.com");
        request.setPassword("1234567"); // 7 chars

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.password").value("Password must be at least 8 characters"));
    }

    @Test
    @DisplayName("POST /auth/login - Valid credentials return JWT token")
    void login_ValidCredentials_Returns200AndJwtToken() throws Exception {
        User user = new User("loginuser@example.com", passwordEncoder.encode("secret12345"), LocalDateTime.now());
        userRepository.save(user);

        LoginRequest request = new LoginRequest();
        request.setEmail("loginuser@example.com");
        request.setPassword("secret12345");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.token", not(emptyOrNullString())));
    }

    @Test
    @DisplayName("POST /auth/login - Wrong password returns 401 Unauthorized")
    void login_WrongPassword_Returns401Unauthorized() throws Exception {
        User user = new User("loginuser@example.com", passwordEncoder.encode("secret12345"), LocalDateTime.now());
        userRepository.save(user);

        LoginRequest request = new LoginRequest();
        request.setEmail("loginuser@example.com");
        request.setPassword("wrongPassword");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("POST /auth/login - Unknown email returns 401 Unauthorized")
    void login_UnknownEmail_Returns401Unauthorized() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("nosuchuser@example.com");
        request.setPassword("secret12345");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("POST /auth/login - Blank fields return 400 Bad Request")
    void login_BlankFields_Returns400BadRequest() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("");
        request.setPassword("");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    @DisplayName("POST /auth/register - Malformed JSON returns 400 or handled exception")
    void register_MalformedJson_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json}"))
                .andExpect(status().is4xxClientError());
    }
}
