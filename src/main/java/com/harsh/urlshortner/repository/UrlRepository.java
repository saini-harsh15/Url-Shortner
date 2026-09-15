package com.harsh.urlshortner.repository;

import com.harsh.urlshortner.entity.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface UrlRepository extends JpaRepository<Url, Long> {

    Optional<Url> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    Optional<Url> findByShortCodeAndUserId(
            String shortCode,
            Long userId
    );

    List<Url> findAllByUserId(Long userId);

    @Transactional
    @Modifying
    @Query("""
    UPDATE Url u
    SET u.clickCount = u.clickCount + 1
    WHERE u.id = :id
""")
    void incrementClickCount(@Param("id") Long id);
}