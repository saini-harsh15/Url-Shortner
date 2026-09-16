package com.harsh.urlshortner;

import com.harsh.urlshortner.dto.LoginRequest;
import com.harsh.urlshortner.dto.RegisterRequest;
import com.harsh.urlshortner.entity.User;
import com.harsh.urlshortner.exception.ConflictException;
import com.harsh.urlshortner.exception.InvalidCredentialsException;
import com.harsh.urlshortner.repository.UserRepository;
import com.harsh.urlshortner.service.AuthService;
import com.harsh.urlshortner.service.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("register should encode password and save user when email is unique")
    void register_Success() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("user@example.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");

        User savedUser = new User("user@example.com", "encoded_password", LocalDateTime.now());
        savedUser.setId(1L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        User result = authService.register(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("user@example.com");
        assertThat(result.getPassword()).isEqualTo("encoded_password");

        verify(userRepository).existsByEmail("user@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register should throw ConflictException when email already exists")
    void register_DuplicateEmail_ThrowsConflictException() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Email already registered");

        verify(userRepository).existsByEmail("existing@example.com");
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("login should return JWT when credentials are valid")
    void login_Success() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("password123");

        User user = new User("user@example.com", "encoded_password", LocalDateTime.now());
        user.setId(1L);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true);
        when(jwtService.generateToken(1L, "user@example.com")).thenReturn("mock.jwt.token");

        // Act
        String token = authService.login(request);

        // Assert
        assertThat(token).isEqualTo("mock.jwt.token");
        verify(userRepository).findByEmail("user@example.com");
        verify(passwordEncoder).matches("password123", "encoded_password");
        verify(jwtService).generateToken(1L, "user@example.com");
    }

    @Test
    @DisplayName("login should throw InvalidCredentialsException when email is not found")
    void login_UserNotFound_ThrowsInvalidCredentialsException() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("unknown@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");

        verify(userRepository).findByEmail("unknown@example.com");
        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtService, never()).generateToken(any(), any());
    }

    @Test
    @DisplayName("login should throw InvalidCredentialsException when password does not match")
    void login_WrongPassword_ThrowsInvalidCredentialsException() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("wrongpassword");

        User user = new User("user@example.com", "encoded_password", LocalDateTime.now());
        user.setId(1L);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "encoded_password")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");

        verify(userRepository).findByEmail("user@example.com");
        verify(passwordEncoder).matches("wrongpassword", "encoded_password");
        verify(jwtService, never()).generateToken(any(), any());
    }
}
