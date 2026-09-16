package com.harsh.urlshortner;

import com.harsh.urlshortner.dto.CreateUrlRequest;
import com.harsh.urlshortner.dto.UrlResponse;
import com.harsh.urlshortner.entity.Url;
import com.harsh.urlshortner.entity.User;
import com.harsh.urlshortner.exception.ResourceNotFoundException;
import com.harsh.urlshortner.repository.UrlRepository;
import com.harsh.urlshortner.repository.UserRepository;
import com.harsh.urlshortner.service.UrlService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UrlService urlService;

    @Test
    @DisplayName("createUrl should create and return URL without expiration")
    void createUrl_WithoutExpiration_Success() {
        // Arrange
        Long userId = 1L;
        User user = new User("user@example.com", "hash", LocalDateTime.now());
        user.setId(userId);

        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://spring.io/projects/spring-boot");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(urlRepository.existsByShortCode(anyString())).thenReturn(false);

        when(urlRepository.save(any(Url.class))).thenAnswer(invocation -> {
            Url url = invocation.getArgument(0);
            url.setId(10L);
            return url;
        });

        // Act
        UrlResponse response = urlService.createUrl(request, userId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getOriginalUrl()).isEqualTo("https://spring.io/projects/spring-boot");
        assertThat(response.getShortCode()).hasSize(6);
        assertThat(response.getShortUrl()).isEqualTo("http://localhost:8080/" + response.getShortCode());
        assertThat(response.getExpiresAt()).isNull();
        assertThat(response.getClickCount()).isEqualTo(0L);

        ArgumentCaptor<Url> urlCaptor = ArgumentCaptor.forClass(Url.class);
        verify(urlRepository).save(urlCaptor.capture());
        Url saved = urlCaptor.getValue();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getClickCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("createUrl should create and return URL with expiration")
    void createUrl_WithExpiration_Success() {
        // Arrange
        Long userId = 1L;
        User user = new User("user@example.com", "hash", LocalDateTime.now());
        user.setId(userId);

        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://example.com/docs");
        request.setExpiresAt(expiresAt);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(urlRepository.existsByShortCode(anyString())).thenReturn(false);

        when(urlRepository.save(any(Url.class))).thenAnswer(invocation -> {
            Url url = invocation.getArgument(0);
            url.setId(20L);
            return url;
        });

        // Act
        UrlResponse response = urlService.createUrl(request, userId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(20L);
        assertThat(response.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(response.getShortCode()).hasSize(6);
    }

    @Test
    @DisplayName("createUrl should throw ResourceNotFoundException when user is not found")
    void createUrl_UserNotFound_ThrowsException() {
        // Arrange
        Long userId = 999L;
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> urlService.createUrl(request, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");

        verify(urlRepository, never()).save(any());
    }

    @Test
    @DisplayName("createUrl should handle shortCode collisions by regenerating until unique")
    void createUrl_ShortCodeCollision_Regenerates() {
        // Arrange
        Long userId = 1L;
        User user = new User("user@example.com", "hash", LocalDateTime.now());
        user.setId(userId);

        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        // Simulate one collision, then success
        when(urlRepository.existsByShortCode(anyString()))
                .thenReturn(true)
                .thenReturn(false);

        when(urlRepository.save(any(Url.class))).thenAnswer(invocation -> {
            Url url = invocation.getArgument(0);
            url.setId(30L);
            return url;
        });

        // Act
        UrlResponse response = urlService.createUrl(request, userId);

        // Assert
        assertThat(response).isNotNull();
        verify(urlRepository, times(2)).existsByShortCode(anyString());
    }

    @Test
    @DisplayName("getUrl should return UrlResponse when URL belongs to the user")
    void getUrl_Success() {
        // Arrange
        Long userId = 1L;
        String shortCode = "abc123";

        User user = new User("user@example.com", "hash", LocalDateTime.now());
        user.setId(userId);

        Url url = new Url();
        url.setId(5L);
        url.setOriginalUrl("https://example.com/test");
        url.setShortCode(shortCode);
        url.setCreatedAt(LocalDateTime.now());
        url.setClickCount(12L);
        url.setUser(user);

        when(urlRepository.findByShortCodeAndUserId(shortCode, userId)).thenReturn(Optional.of(url));

        // Act
        UrlResponse response = urlService.getUrl(shortCode, userId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getShortCode()).isEqualTo(shortCode);
        assertThat(response.getOriginalUrl()).isEqualTo("https://example.com/test");
        assertThat(response.getShortUrl()).isEqualTo("http://localhost:8080/" + shortCode);
        assertThat(response.getClickCount()).isEqualTo(12L);
    }

    @Test
    @DisplayName("getUrl should throw ResourceNotFoundException when URL not found or belongs to another user")
    void getUrl_NotFound_ThrowsException() {
        // Arrange
        Long userId = 1L;
        String shortCode = "notmycode";

        when(urlRepository.findByShortCodeAndUserId(shortCode, userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> urlService.getUrl(shortCode, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("URL not found");
    }

    @Test
    @DisplayName("deleteUrl should delete URL when it belongs to user")
    void deleteUrl_Success() {
        // Arrange
        Long userId = 1L;
        String shortCode = "del123";

        User user = new User("user@example.com", "hash", LocalDateTime.now());
        user.setId(userId);

        Url url = new Url();
        url.setId(7L);
        url.setShortCode(shortCode);
        url.setUser(user);

        when(urlRepository.findByShortCodeAndUserId(shortCode, userId)).thenReturn(Optional.of(url));

        // Act
        urlService.deleteUrl(shortCode, userId);

        // Assert
        verify(urlRepository).delete(url);
    }

    @Test
    @DisplayName("deleteUrl should throw ResourceNotFoundException when URL not found or belongs to another user")
    void deleteUrl_NotFound_ThrowsException() {
        // Arrange
        Long userId = 1L;
        String shortCode = "othercode";

        when(urlRepository.findByShortCodeAndUserId(shortCode, userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> urlService.deleteUrl(shortCode, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("URL not found");

        verify(urlRepository, never()).delete(any());
    }

    @Test
    @DisplayName("getUserUrls should return list of URLs belonging to user")
    void getUserUrls_ReturnsMappedList() {
        // Arrange
        Long userId = 1L;
        User user = new User("user@example.com", "hash", LocalDateTime.now());
        user.setId(userId);

        Url url1 = new Url();
        url1.setId(1L);
        url1.setOriginalUrl("https://example1.com");
        url1.setShortCode("code01");
        url1.setCreatedAt(LocalDateTime.now());
        url1.setClickCount(5L);
        url1.setUser(user);

        Url url2 = new Url();
        url2.setId(2L);
        url2.setOriginalUrl("https://example2.com");
        url2.setShortCode("code02");
        url2.setCreatedAt(LocalDateTime.now());
        url2.setClickCount(10L);
        url2.setUser(user);

        when(urlRepository.findAllByUserId(userId)).thenReturn(List.of(url1, url2));

        // Act
        List<UrlResponse> responses = urlService.getUserUrls(userId);

        // Assert
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getShortCode()).isEqualTo("code01");
        assertThat(responses.get(0).getShortUrl()).isEqualTo("http://localhost:8080/code01");
        assertThat(responses.get(1).getShortCode()).isEqualTo("code02");
        assertThat(responses.get(1).getShortUrl()).isEqualTo("http://localhost:8080/code02");
    }
}
