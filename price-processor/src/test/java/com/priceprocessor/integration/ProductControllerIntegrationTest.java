package com.priceprocessor.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.priceprocessor.dtos.api.ProductObservationByNameRequest;
import com.priceprocessor.dtos.api.ProductObservationByUrlRequest;
import com.priceprocessor.models.ProductObservation;
import com.priceprocessor.repositories.ProductRepository;
import com.priceprocessor.services.queue.NotificationProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerIntegrationTest {

    @MockBean
    private NotificationProducer notificationProducer;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    private ProductObservation sampleProduct;

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        sampleProduct = ProductObservation.builder()
                .productName("Test Product")
                .productUrl("http://example.com/product")
                .userEmail("user@example.com")
                .currentPrice(new BigDecimal("99.99"))
                .lastCheckedAt(LocalDateTime.now())
                .build();

        sampleProduct = repository.save(sampleProduct);
    }

    // ========================
    // CRUD / zapytania
    // ========================

    @Test
    void shouldReturnProductById_WhenExists() throws Exception {
        mockMvc.perform(get("/api/products/{id}", sampleProduct.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName").value("Test Product"))
                .andExpect(jsonPath("$.productUrl").value("http://example.com/product"))
                .andExpect(jsonPath("$.currentPrice").value(99.99))
                .andExpect(jsonPath("$.userEmail").value("user@example.com"));
    }

    @Test
    void shouldReturn404_WhenProductNotFound() throws Exception {
        mockMvc.perform(get("/api/products/{id}", 9999))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldAddProductByName_WhenDataIsValid() throws Exception {
        ProductObservationByNameRequest request = new ProductObservationByNameRequest("New Product");

        mockMvc.perform(post("/api/products/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName").value("New Product"));
    }

    @Test
    void shouldAddProductByUrl_WhenDataIsValid() throws Exception {
        ProductObservationByUrlRequest request = new ProductObservationByUrlRequest("http://example.com/new-product");
        mockMvc.perform(post("/api/products/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productUrl").value("http://example.com/new-product"));
    }

    @Test
    void shouldDeleteProduct_WhenExists() throws Exception {
        mockMvc.perform(delete("/api/products/{id}", sampleProduct.getId()))
                .andExpect(status().isNoContent());
    }

    // ========================
    // Walidacja danych wejściowych
    // ========================

    @Test
    void shouldReturn400_WhenAddingProductByNameWithoutRequiredField() throws Exception {
        ProductObservationByNameRequest request = new ProductObservationByNameRequest("");
        // Brak productName

        mockMvc.perform(post("/api/products/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400_WhenAddingProductByUrlWithoutRequiredField() throws Exception {
        ProductObservationByUrlRequest request = new ProductObservationByUrlRequest("");
        // Brak productUrl

        mockMvc.perform(post("/api/products/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
