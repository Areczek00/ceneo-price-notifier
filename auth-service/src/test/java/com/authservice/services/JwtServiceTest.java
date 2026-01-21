package com.authservice.services;

import com.authservice.models.Role;
import com.authservice.models.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {
    private JwtService jwtService;

    @Mock
    private UserDetails userDetails;

    private static final String SECRET_KEY = "NDIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=";
    private static final long EXPIRATION_TIME = 1000 * 60 * 60; // 1 godzina

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET_KEY);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", EXPIRATION_TIME);
    }

    @Test
    void shouldGenerateTokenWithUsernameAndRoles() {
        // Arrange
        User user = User.builder()
                .email("test@example.com")
                .password("password")
                .role(Role.ROLE_USER)
                .build();

        // Act
        String token = jwtService.generateToken(user);
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(
                        io.jsonwebtoken.io.Decoders.BASE64.decode(SECRET_KEY)
                )
                .build()
                .parseClaimsJws(token)
                .getBody();

        // Assert
        assertThat(token).isNotNull().isNotEmpty();
        assertThat(claims.getSubject()).isEqualTo(user.getEmail());
        assertThat(claims.get("roles"))
                .asList()
                .containsExactlyInAnyOrder("ROLE_USER");
        assertThat(claims.getExpiration()).isNotNull();
    }
}
