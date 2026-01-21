package com.authservice.config;

import com.authservice.services.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.AdditionalMatchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb"
})
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AuthenticationProvider authenticationProvider;

    @Test
    void shouldAllowAccessToAuthEndpointsWithoutToken() throws Exception {
        String body = "{\"email\":\"test@example.com\",\"password\":\"password123\"}";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void shouldAllowAuthenticateWithoutToken() throws Exception {
        String body = "{\"email\":\"test@example.com\",\"password\":\"password123\"}";

        mockMvc.perform(post("/api/auth/register"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 403) {
                        throw new AssertionError("Expected status NOT to be 403");
                    }
                });
    }


    @Test
    void shouldReturnCorsHeadersForAuthEndpoint() throws Exception {
        mockMvc.perform(options("/api/auth/register")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"))
                .andExpect(header().exists("Access-Control-Allow-Methods"));
    }
}

