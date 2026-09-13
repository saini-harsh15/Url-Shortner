package com.harsh.urlshortner.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;

    @Value("${jwt.expiration}")
    private long expiration;

    public JwtService(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    public String generateToken(Long userId, String email) {

        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .issuedAt(now)
                .expiresAt(now.plusMillis(expiration))
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        JwtEncoderParameters parameters =
                JwtEncoderParameters.from(header, claims);

        return jwtEncoder.encode(parameters).getTokenValue();
    }
}