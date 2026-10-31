package com.maitre.kiosk;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class ApiTestSupport {

    protected static final long RESTAURANT = 1L;
    protected static final long OTHER_RESTAURANT = 2L;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        try {
            Path file = Files.createTempFile("kiosk-test", ".db");
            file.toFile().deleteOnExit();
            registry.add("KIOSK_DB_PATH", file::toString);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM order_lines");
        jdbcTemplate.update("DELETE FROM orders");
        jdbcTemplate.update("DELETE FROM products");
    }

    protected ResultActions postJson(String url, Object body) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json(body)));
    }

    protected ResultActions patchJson(String url, Object body) throws Exception {
        return mockMvc.perform(patch(url).contentType(MediaType.APPLICATION_JSON).content(json(body)));
    }

    protected long createProduct(long restaurantId, String name, String price) throws Exception {
        String body = """
                {"name": "%s", "category": "Food", "price": %s}
                """.formatted(name, price);
        return readId(postJson("/api/restaurants/" + restaurantId + "/products", body)
                .andExpect(status().isCreated()));
    }

    protected long createOrder(long restaurantId, String itemsJson) throws Exception {
        return readId(postJson("/api/restaurants/" + restaurantId + "/orders", "{\"items\": " + itemsJson + "}")
                .andExpect(status().isCreated()));
    }

    protected void changeStatus(long restaurantId, long orderId, String newStatus) throws Exception {
        patchJson("/api/restaurants/" + restaurantId + "/orders/" + orderId + "/status",
                "{\"status\": \"" + newStatus + "\"}")
                .andExpect(status().isOk());
    }

    private long readId(ResultActions result) throws Exception {
        JsonNode node = objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
        return node.get("id").asLong();
    }

    private String json(Object body) throws Exception {
        return body instanceof String text ? text : objectMapper.writeValueAsString(body);
    }
}
