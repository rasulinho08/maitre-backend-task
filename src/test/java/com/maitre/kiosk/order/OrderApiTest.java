package com.maitre.kiosk.order;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import com.maitre.kiosk.ApiTestSupport;

class OrderApiTest extends ApiTestSupport {

    private static final String ORDERS = "/api/restaurants/" + RESTAURANT + "/orders";

    @Test
    void createsPendingOrderWithTotals() throws Exception {
        long burger = createProduct(RESTAURANT, "Burger", "8.90");
        long fries = createProduct(RESTAURANT, "Fries", "3.50");

        postJson(ORDERS, """
                {"items": [
                    {"productId": %d, "quantity": 2},
                    {"productId": %d, "quantity": 1},
                    {"productId": %d, "quantity": 1}
                ]}
                """.formatted(burger, fries, burger))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.lines", hasSize(2)))
                .andExpect(jsonPath("$.lines[0].productName").value("Burger"))
                .andExpect(jsonPath("$.lines[0].quantity").value(3))
                .andExpect(jsonPath("$.lines[0].unitPrice").value(8.90))
                .andExpect(jsonPath("$.lines[0].lineTotal").value(26.70))
                .andExpect(jsonPath("$.lines[1].productName").value("Fries"))
                .andExpect(jsonPath("$.lines[1].lineTotal").value(3.50))
                .andExpect(jsonPath("$.total").value(30.20));
    }

    @Test
    void rejectsInvalidOrderRequests() throws Exception {
        long burger = createProduct(RESTAURANT, "Burger", "8.90");

        postJson(ORDERS, "{\"items\": []}").andExpect(status().isBadRequest());
        postJson(ORDERS, "{\"items\": [{\"productId\": %d, \"quantity\": 0}]}".formatted(burger))
                .andExpect(status().isBadRequest());
        postJson(ORDERS, "{\"items\": [{\"productId\": %d, \"quantity\": 60}, {\"productId\": %d, \"quantity\": 60}]}"
                .formatted(burger, burger))
                .andExpect(status().isUnprocessableEntity());
        postJson("/api/restaurants/999/orders", "{\"items\": [{\"productId\": %d, \"quantity\": 1}]}"
                .formatted(burger))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsProductsThatAreUnavailableOrFromAnotherRestaurant() throws Exception {
        long burger = createProduct(RESTAURANT, "Burger", "8.90");
        long foreign = createProduct(OTHER_RESTAURANT, "Wrap", "7.00");
        patchJson("/api/restaurants/" + RESTAURANT + "/products/" + burger, "{\"available\": false}")
                .andExpect(status().isOk());

        postJson(ORDERS, "{\"items\": [{\"productId\": %d, \"quantity\": 1}]}".formatted(burger))
                .andExpect(status().isUnprocessableEntity());
        postJson(ORDERS, "{\"items\": [{\"productId\": %d, \"quantity\": 1}]}".formatted(foreign))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void keepsPriceFromTheMomentOfOrdering() throws Exception {
        long burger = createProduct(RESTAURANT, "Burger", "8.90");
        createOrder(RESTAURANT, "[{\"productId\": %d, \"quantity\": 2}]".formatted(burger));

        patchJson("/api/restaurants/" + RESTAURANT + "/products/" + burger, "{\"price\": 12.00}")
                .andExpect(status().isOk());

        mockMvc.perform(get(ORDERS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].lines[0].unitPrice").value(8.90))
                .andExpect(jsonPath("$.items[0].total").value(17.80));
    }

    @Test
    void movesThroughTheWholeLifecycle() throws Exception {
        long burger = createProduct(RESTAURANT, "Burger", "8.90");
        long order = createOrder(RESTAURANT, "[{\"productId\": %d, \"quantity\": 1}]".formatted(burger));

        changeStatus(RESTAURANT, order, "Preparing");
        changeStatus(RESTAURANT, order, "Ready");
        patchJson(ORDERS + "/" + order + "/status", "{\"status\": \"Completed\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        patchJson(ORDERS + "/" + order + "/status", "{\"status\": \"Cancelled\"}")
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsSkippingAStep() throws Exception {
        long burger = createProduct(RESTAURANT, "Burger", "8.90");
        long order = createOrder(RESTAURANT, "[{\"productId\": %d, \"quantity\": 1}]".formatted(burger));

        patchJson(ORDERS + "/" + order + "/status", "{\"status\": \"Ready\"}")
                .andExpect(status().isConflict());
    }

    @Test
    void allowsCancellingOnlyWhilePending() throws Exception {
        long burger = createProduct(RESTAURANT, "Burger", "8.90");
        long pending = createOrder(RESTAURANT, "[{\"productId\": %d, \"quantity\": 1}]".formatted(burger));
        long preparing = createOrder(RESTAURANT, "[{\"productId\": %d, \"quantity\": 1}]".formatted(burger));
        changeStatus(RESTAURANT, preparing, "Preparing");

        changeStatus(RESTAURANT, pending, "Cancelled");
        patchJson(ORDERS + "/" + preparing + "/status", "{\"status\": \"Cancelled\"}")
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsUnknownStatusAndForeignOrder() throws Exception {
        long burger = createProduct(RESTAURANT, "Burger", "8.90");
        long order = createOrder(RESTAURANT, "[{\"productId\": %d, \"quantity\": 1}]".formatted(burger));

        patchJson(ORDERS + "/" + order + "/status", "{\"status\": \"Done\"}")
                .andExpect(status().isBadRequest());
        patchJson("/api/restaurants/" + OTHER_RESTAURANT + "/orders/" + order + "/status",
                "{\"status\": \"Preparing\"}")
                .andExpect(status().isNotFound());
    }

    @Test
    void listsOrdersNewestFirstWithFilterAndPagination() throws Exception {
        long burger = createProduct(RESTAURANT, "Burger", "8.90");
        long first = createOrder(RESTAURANT, "[{\"productId\": %d, \"quantity\": 1}]".formatted(burger));
        long second = createOrder(RESTAURANT, "[{\"productId\": %d, \"quantity\": 2}]".formatted(burger));
        long third = createOrder(RESTAURANT, "[{\"productId\": %d, \"quantity\": 3}]".formatted(burger));
        changeStatus(RESTAURANT, second, "Preparing");

        long foreignProduct = createProduct(OTHER_RESTAURANT, "Wrap", "7.00");
        createOrder(OTHER_RESTAURANT, "[{\"productId\": %d, \"quantity\": 1}]".formatted(foreignProduct));

        mockMvc.perform(get(ORDERS).param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id", contains((int) third, (int) second)))
                .andExpect(jsonPath("$.items[0].lines[0].productName").value("Burger"))
                .andExpect(jsonPath("$.items[0].lines[0].quantity").value(3))
                .andExpect(jsonPath("$.items[0].lines[0].lineTotal").value(26.70))
                .andExpect(jsonPath("$.items[0].total").value(26.70))
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(get(ORDERS).param("page", "1").param("size", "2"))
                .andExpect(jsonPath("$.items[*].id", contains((int) first)));

        mockMvc.perform(get(ORDERS).param("status", "pending"))
                .andExpect(jsonPath("$.items[*].id", contains((int) third, (int) first)))
                .andExpect(jsonPath("$.totalItems").value(2));
    }

    @Test
    void rejectsInvalidListParameters() throws Exception {
        mockMvc.perform(get(ORDERS).param("size", "500")).andExpect(status().isBadRequest());
        mockMvc.perform(get(ORDERS).param("page", "-1")).andExpect(status().isBadRequest());
        mockMvc.perform(get(ORDERS).param("status", "unknown")).andExpect(status().isBadRequest());
    }
}
