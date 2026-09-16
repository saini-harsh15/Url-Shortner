package com.harsh.urlshortner;

import com.harsh.urlshortner.entity.Url;
import com.harsh.urlshortner.entity.User;
import com.harsh.urlshortner.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UrlRepositoryTest {

    @Autowired
    private UrlRepository urlRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser1;
    private User testUser2;

    @BeforeEach
    void setUp() {
        testUser1 = new User("user1@example.com", "pwd1", LocalDateTime.now());
        entityManager.persistAndFlush(testUser1);

        testUser2 = new User("user2@example.com", "pwd2", LocalDateTime.now());
        entityManager.persistAndFlush(testUser2);
    }

    private Url createAndPersistUrl(String originalUrl, String shortCode, User user, LocalDateTime expiresAt) {
        Url url = new Url();
        url.setOriginalUrl(originalUrl);
        url.setShortCode(shortCode);
        url.setCreatedAt(LocalDateTime.now());
        url.setExpiresAt(expiresAt);
        url.setClickCount(0L);
        url.setUser(user);
        return entityManager.persistAndFlush(url);
    }

    @Test
    @DisplayName("findByShortCode returns Url when shortCode exists")
    void findByShortCode_Exists_ReturnsUrl() {
        // Arrange
        createAndPersistUrl("https://example.com/find", "find01", testUser1, null);

        // Act
        Optional<Url> result = urlRepository.findByShortCode("find01");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getOriginalUrl()).isEqualTo("https://example.com/find");
        assertThat(result.get().getShortCode()).isEqualTo("find01");
    }

    @Test
    @DisplayName("findByShortCode returns empty when shortCode does not exist")
    void findByShortCode_DoesNotExist_ReturnsEmpty() {
        // Act
        Optional<Url> result = urlRepository.findByShortCode("nonexistent");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("existsByShortCode returns true when shortCode exists")
    void existsByShortCode_Exists_ReturnsTrue() {
        // Arrange
        createAndPersistUrl("https://example.com/exist", "exist1", testUser1, null);

        // Act & Assert
        assertThat(urlRepository.existsByShortCode("exist1")).isTrue();
    }

    @Test
    @DisplayName("existsByShortCode returns false when shortCode does not exist")
    void existsByShortCode_DoesNotExist_ReturnsFalse() {
        // Act & Assert
        assertThat(urlRepository.existsByShortCode("none99")).isFalse();
    }

    @Test
    @DisplayName("findByShortCodeAndUserId returns Url when both shortCode and owner match")
    void findByShortCodeAndUserId_MatchingOwner_ReturnsUrl() {
        // Arrange
        createAndPersistUrl("https://example.com/user1", "u1code", testUser1, null);

        // Act
        Optional<Url> result = urlRepository.findByShortCodeAndUserId("u1code", testUser1.getId());

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getShortCode()).isEqualTo("u1code");
        assertThat(result.get().getUser().getId()).isEqualTo(testUser1.getId());
    }

    @Test
    @DisplayName("findByShortCodeAndUserId returns empty when shortCode belongs to another user")
    void findByShortCodeAndUserId_DifferentOwner_ReturnsEmpty() {
        // Arrange
        createAndPersistUrl("https://example.com/user1", "u1code", testUser1, null);

        // Act
        Optional<Url> result = urlRepository.findByShortCodeAndUserId("u1code", testUser2.getId());

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAllByUserId returns only URLs created by that specific user")
    void findAllByUserId_ReturnsOnlyUserUrls() {
        // Arrange
        createAndPersistUrl("https://example.com/1", "code01", testUser1, null);
        createAndPersistUrl("https://example.com/2", "code02", testUser1, null);
        createAndPersistUrl("https://example.com/3", "code03", testUser2, null);

        // Act
        List<Url> user1Urls = urlRepository.findAllByUserId(testUser1.getId());
        List<Url> user2Urls = urlRepository.findAllByUserId(testUser2.getId());

        // Assert
        assertThat(user1Urls).hasSize(2)
                .extracting(Url::getShortCode)
                .containsExactlyInAnyOrder("code01", "code02");

        assertThat(user2Urls).hasSize(1)
                .extracting(Url::getShortCode)
                .containsExactly("code03");
    }

    @Test
    @DisplayName("incrementClickCount atomically increments clickCount in the database")
    void incrementClickCount_IncrementsCount() {
        // Arrange
        Url savedUrl = createAndPersistUrl("https://example.com/clicks", "clicks", testUser1, null);
        Long urlId = savedUrl.getId();
        assertThat(savedUrl.getClickCount()).isEqualTo(0L);

        // Act
        urlRepository.incrementClickCount(urlId);
        entityManager.clear(); // Clear persistence context cache to re-fetch from DB

        // Assert
        Url updatedUrl = entityManager.find(Url.class, urlId);
        assertThat(updatedUrl.getClickCount()).isEqualTo(1L);

        // Act again
        urlRepository.incrementClickCount(urlId);
        entityManager.clear();

        // Assert
        Url updatedUrl2 = entityManager.find(Url.class, urlId);
        assertThat(updatedUrl2.getClickCount()).isEqualTo(2L);
    }
}
