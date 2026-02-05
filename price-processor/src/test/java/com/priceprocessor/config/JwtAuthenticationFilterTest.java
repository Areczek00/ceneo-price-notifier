package com.priceprocessor.config;

import com.priceprocessor.services.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private JwtAuthenticationFilter jwtFilter;

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldContinueFilterChain_WhenAuthHeaderIsMissing() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(null);

        // Act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtService);
    }

    @Test
    void shouldContinueFilterChain_WhenAuthHeaderDoesNotStartWithBearer() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Basic Aladin:open sesame");

        // Act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtService);
    }

    @Test
    void shouldAuthenticateUser_WhenTokenIsValid() throws ServletException, IOException {
        // Arrange
        String token = "valid.jwt.token";
        String userEmail = "test@user.com";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractUsername(token)).thenReturn(userEmail);
        when(jwtService.extractRoles(token)).thenReturn(List.of("ROLE_USER"));
        when(jwtService.isTokenValid(token)).thenReturn(true);
        // Act
        jwtFilter.doFilterInternal(request, response, filterChain);
        // Assert
        verify(filterChain).doFilter(request, response);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertInstanceOf(UsernamePasswordAuthenticationToken.class, auth);
        assertEquals(userEmail, auth.getPrincipal());
        assertTrue(
                auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_USER"))
        );
    }

    @Test
    void shouldNotAuthenticate_WhenTokenIsInvalid() throws ServletException, IOException {
        // Arrange
        String token = "invalid.jwt.token";
        String userEmail = "test@user.com";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractUsername(token)).thenReturn(userEmail);
        when(jwtService.isTokenValid(token)).thenReturn(false);

        // Act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldNotOverrideExistingAuthentication() throws Exception {
        var existingAuth = new UsernamePasswordAuthenticationToken(
                "existing@user.com",
                null,
                List.of()
        );
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        when(request.getHeader("Authorization"))
                .thenReturn("Bearer valid.jwt.token");

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertSame(
                existingAuth,
                SecurityContextHolder.getContext().getAuthentication()
        );
    }

    @Test
    void shouldNotAuthenticate_WhenUsernameIsNull() throws Exception {
        when(request.getHeader("Authorization"))
                .thenReturn("Bearer token");
        when(jwtService.extractUsername("token"))
                .thenReturn(null);
        when(jwtService.extractRoles("token"))
                .thenReturn(List.of("ROLE_USER"));

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

}