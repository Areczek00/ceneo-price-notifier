package com.priceprocessor.services;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtService jwtService;

    private Key signingKey;

    private String username;

    private static final String SECRET_KEY = "NDIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=";
    private static final long EXPIRATION_TIME = 1000 * 60 * 60; // 1 godzina

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        username = "user@test.com";
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET_KEY);
        signingKey = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(SECRET_KEY)
        );    }

    private String generateValidToken(long milis) {
        return Jwts.builder()
                .setSubject(username)
                .claim("roles", List.of("ROLE_USER"))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + milis))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    private String generateValidToken(Map<String, Object> claims) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject("adminUser")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }


    @Test
    void shouldExtractUsername() {
        // Arrange
        String token = generateValidToken(60000);

        // Act
        String extractedUsername = jwtService.extractUsername(token);

        // Assert
        assertThat(token).isNotNull().isNotEmpty();
        assertThat(extractedUsername).isEqualTo(username);
    }

    @Test
    void shouldExtractRoles() {

        String token = generateValidToken(60000);

        List<String> roles = jwtService.extractRoles(token);

        assertThat(roles).containsExactly("ROLE_USER");
    }

    @Test
    void shouldValidateToken_whenNotExpired() {
        // Arrange
        String token = generateValidToken(60000);

        // Assert
        assertThat(jwtService.isTokenValid(token)).isTrue();    }

    @Test
    void shouldInvalidateExpiredToken() {
        // Arrange
        String token = generateValidToken(-1000);

        // Act
        boolean isValid = jwtService.isTokenValid(token);

        // Assert
        assertThat(isValid).isFalse();
    }


    @Test
    void shouldThrowException_whenTokenIsTampered() {
        String token = generateValidToken(60000) + "tampered";

        assertThatThrownBy(() -> jwtService.extractUsername(token))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }

    @Test
    void shouldExtractCustomClaims() {
        // Arrange
        Map<String, Object> extraClaims = Map.of(
                "role", "ROLE_ADMIN",
                "id", 123
        );

        String token = generateValidToken(extraClaims);

        // Act
        String extractedRole =
                jwtService.extractClaim(token, c -> c.get("role", String.class));
        Integer extractedId =
                jwtService.extractClaim(token, c -> c.get("id", Integer.class));

        // Assert
        assertThat(extractedRole).isEqualTo("ROLE_ADMIN");
        assertThat(extractedId).isEqualTo(123);
    }
}