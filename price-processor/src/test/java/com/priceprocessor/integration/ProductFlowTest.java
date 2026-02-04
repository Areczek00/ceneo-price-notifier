package com.priceprocessor.integration;

import com.priceprocessor.dtos.crawler.PriceResponse;
import com.priceprocessor.services.clients.PriceClient;
import com.priceprocessor.services.queue.NotificationProducer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProductFlowTest {

    @MockBean
    NotificationProducer notificationProducer;

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    int port;

    private String jwt;

    private final String AUTH_BASE = "http://localhost:8081/api/auth";
    private String PRODUCTS_BASE;

    @BeforeAll
    void setup() {
        PRODUCTS_BASE = "http://localhost:" + port + "/api/products";
    }

    @Test
    void fullFlow_register_login_add_delete_product() {
        // 1️⃣ register
        restTemplate.postForEntity(
                AUTH_BASE + "/register",
                json("""
                    {
                      "email":"test@example.com",
                      "password":"password123",
                      "firstName":"Test",
                      "lastName":"User"
                    }
                """),
                Void.class
        );

        // 2️⃣ login
        ResponseEntity<Map> login = restTemplate.postForEntity(
                AUTH_BASE + "/authenticate",
                json("""
                    {
                      "email":"test@example.com",
                      "password":"password123"
                    }
                """),
                Map.class
        );

        jwt = (String) login.getBody().get("access_token");

        HttpHeaders auth = new HttpHeaders();
        auth.setBearerAuth(jwt);
        auth.setContentType(MediaType.APPLICATION_JSON);

        // 3️⃣ add product
        ResponseEntity<Map> add = restTemplate.exchange(
                PRODUCTS_BASE + "/search",
                HttpMethod.POST,
                new HttpEntity<>("""
                    { "productName":"Laptop XYZ" }
                """, auth),
                Map.class
        );

        assertThat(add.getStatusCode()).isEqualTo(HttpStatus.OK);
        Long productId = ((Number) add.getBody().get("id")).longValue();

        // 4️⃣ get product
        ResponseEntity<Map> get = restTemplate.exchange(
                PRODUCTS_BASE + "/" + productId,
                HttpMethod.GET,
                new HttpEntity<>(auth),
                Map.class
        );

        assertThat(get.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 5️⃣ delete
        ResponseEntity<Void> del = restTemplate.exchange(
                PRODUCTS_BASE + "/" + productId,
                HttpMethod.DELETE,
                new HttpEntity<>(auth),
                Void.class
        );

        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void shouldReturn401_whenNoJwt() {
        ResponseEntity<String> res = restTemplate.postForEntity(
                PRODUCTS_BASE + "/search",
                json("""
                        { "productName":"X" }
                        """),
                String.class
        );

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private HttpEntity<String> json(String body) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, h);
    }
    @TestConfiguration
    static class FakePriceClientConfig {

        @Bean
        PriceClient priceClient() {
            return new PriceClient() {
                @Override
                public Optional<PriceResponse> checkPriceByName(String name) {
                    return Optional.of(
                            new PriceResponse(
                                    name,
                                    BigDecimal.valueOf(1000),
                                    "PLN",
                                    "test-url"
                            )
                    );
                }

                @Override
                public Optional<PriceResponse> checkPriceByUrl(String url) {
                    return Optional.of(
                            new PriceResponse(
                                    "From URL",
                                    BigDecimal.valueOf(1200),
                                    "PLN",
                                    url
                            )
                    );
                }
            };
        }
    }
}

