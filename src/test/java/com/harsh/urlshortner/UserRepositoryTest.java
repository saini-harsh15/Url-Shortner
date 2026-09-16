package com.harsh.urlshortner;

import com.harsh.urlshortner.entity.User;
import com.harsh.urlshortner.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("findByEmail returns User when email exists")
    void findByEmail_UserExists_ReturnsUser() {
        // Arrange
        User user = new User("alice@example.com", "hashed_pwd", LocalDateTime.now());
        entityManager.persistAndFlush(user);

        // Act
        Optional<User> found = userRepository.findByEmail("alice@example.com");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("findByEmail returns empty when email does not exist")
    void findByEmail_UserDoesNotExist_ReturnsEmpty() {
        // Act
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("existsByEmail returns true when email exists")
    void existsByEmail_UserExists_ReturnsTrue() {
        // Arrange
        User user = new User("bob@example.com", "hashed_pwd", LocalDateTime.now());
        entityManager.persistAndFlush(user);

        // Act
        boolean exists = userRepository.existsByEmail("bob@example.com");

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByEmail returns false when email does not exist")
    void existsByEmail_UserDoesNotExist_ReturnsFalse() {
        // Act
        boolean exists = userRepository.existsByEmail("nobody@example.com");

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("saving user with duplicate email throws DataIntegrityViolationException")
    void save_DuplicateEmail_ThrowsException() {
        // Arrange
        User user1 = new User("duplicate@example.com", "pwd1", LocalDateTime.now());
        entityManager.persistAndFlush(user1);

        User user2 = new User("duplicate@example.com", "pwd2", LocalDateTime.now());

        // Act & Assert
        assertThatThrownBy(() -> {
            userRepository.saveAndFlush(user2);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
