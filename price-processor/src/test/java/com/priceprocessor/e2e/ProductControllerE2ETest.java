package com.priceprocessor.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.priceprocessor.dtos.crawler.PriceResponse;
import com.priceprocessor.services.ProductService;
import com.priceprocessor.services.clients.CeneoPriceClient;
import com.priceprocessor.services.queue.NotificationProducer;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProductControllerE2ETest {

    @MockBean
    private NotificationProducer notificationProducer;

    @MockBean
    private CeneoPriceClient ceneoPriceClient;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MockMvc mockMvc;

    private static String jwtToken;

    private static Long createdProductId;

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private ProductService productService;

    @BeforeAll
    void setUp() throws Exception {

        // Rejestracja w auth-service
        String registerJson = """
            {"email":"test@example.com","password":"password123","firstName":"","lastName":""}
            """;

        ResponseEntity<String> regResponse = restTemplate.postForEntity(
                "http://localhost:8081/api/auth/register",
                new HttpEntity<>(registerJson, createJsonHeaders()),
                String.class
        );
        if (!regResponse.getStatusCode().is2xxSuccessful() &&
                !regResponse.getStatusCode().is4xxClientError()) {
            throw new RuntimeException("Nie udało się zarejestrować użytkownika w auth-service");
        }

        // Logowanie w auth-service
        String loginJson = """
            {"email":"test@example.com","password":"password123"}
            """;

        ResponseEntity<String> loginResponse = restTemplate.postForEntity(
                "http://localhost:8081/api/auth/authenticate",
                new HttpEntity<>(loginJson, createJsonHeaders()),
                String.class
        );
        if (!loginResponse.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Nie udało się zalogować użytkownika w auth-service");
        }
        jwtToken = parseTokenFromResponse(loginResponse.getBody());
    }

    private static HttpHeaders createJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    //TODO: Do zmiany na TestRestTemplate, wywalić mockmvc
    @Test
    @Order(1)
    void shouldAddProductByName() throws Exception {
        when(ceneoPriceClient.checkPriceByName("Laptop XYZ"))
                .thenReturn(Optional.of(new PriceResponse("Laptop XYZ", BigDecimal.valueOf(1999), "PLN", "testurl")));

        String requestJson = """
            {"productName":"Laptop XYZ"}
            """;

        String response = mockMvc.perform(post("/api/products/search")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName").value("Laptop XYZ"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JSONObject json = new JSONObject(response);
        createdProductId = json.getLong("id");
    }
    //TODO: Do zmiany na TestRestTemplate, wywalić mockmvc
    @Test
    @Order(2)
    void shouldAddProductByUrl() throws Exception {
        when(productService.checkPriceByUrl("https://example.com/laptop-abc"))
                .thenReturn(Optional.of(new PriceResponse("Laptop XYZ", BigDecimal.valueOf(1299.0), "PLN", "https://example.com/laptop-abc")));

        String requestJson = """
            {"productUrl":"https://example.com/laptop-abc"}
            """;

        mockMvc.perform(post("/api/products/url")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productUrl").value("https://example.com/laptop-abc"));
    }
    //TODO: Do zmiany na TestRestTemplate, wywalić mockmvc

    @Test
    @Order(3)
    void shouldReturnProductDetails_WhenExists() throws Exception {
        mockMvc.perform(get("/api/products/{id}", createdProductId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName").value("Laptop XYZ"));
    }
    //TODO: Do zmiany na TestRestTemplate, wywalić mockmvc

    @Test
    @Order(4)
    void shouldReturn404_WhenProductDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/products/{id}", 999L)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNotFound());
    }
    //TODO: Do zmiany na TestRestTemplate, wywalić mockmvc

    @Test
    @Order(5)
    void shouldReturn400_WhenMissingRequiredFields() throws Exception {
        String requestJson = "{}"; // brak productName/url

        mockMvc.perform(post("/api/products/search")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(6)
    void shouldDeleteProduct() throws Exception {
        mockMvc.perform(delete("/api/products/{id}", createdProductId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/products/{id}", createdProductId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNotFound());
    }

    private String parseTokenFromResponse(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            return obj.getString("access_token");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}

