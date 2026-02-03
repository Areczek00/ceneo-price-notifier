package com.authservice.integration;

import com.authservice.dtos.auth.AuthenticationRequest;
import com.authservice.dtos.auth.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;



@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AuthenticationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldAuthenticateUserSuccessfully() throws Exception {
        String email = "login-" + UUID.randomUUID() + "@test.com";

        RegisterRequest register = new RegisterRequest(email, "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().is2xxSuccessful());

        AuthenticationRequest authRequest =
                new AuthenticationRequest(email, "password123");

        mockMvc.perform(post("/api/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().is2xxSuccessful());
    }


    @Test
    void shouldRegisterUserSuccessfully() throws Exception {
        RegisterRequest request = new RegisterRequest("test@example.com", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void shouldFailRegisterWhenEmailIsMissing() throws Exception {
        RegisterRequest request = new RegisterRequest("","password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFailAuthenticationWithWrongPassword() throws Exception {
        AuthenticationRequest authRequest = new AuthenticationRequest("nonexistent@example.com", "wrongpassword");

        mockMvc.perform(post("/api/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isUnauthorized());
    }
}
