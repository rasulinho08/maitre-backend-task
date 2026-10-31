package com.maitre.kiosk.product;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import com.maitre.kiosk.ApiTestSupport;

class TopSellingApiTest extends ApiTestSupport {

    private static final String TOP = "/api/restaurants/" + RESTAURANT + "/products/top-selling";

    @Test
    void returnsFiveBestSellersByQuantityIgnoringCancelledOrders() throws Exception {
        long[] ids = new long[7];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = createProduct(RESTAURANT, "Product " + i, "1.00");
        }

        createOrder(RESTAURANT, items(ids[0], 1, ids[1], 2, ids[2], 3));
        createOrder(RESTAURANT, items(ids[3], 4, ids[4], 5, ids[5], 6));
        createOrder(RESTAURANT, items(ids[0], 9, ids[6], 1));
        long cancelled = createOrder(RESTAURANT, items(ids[6], 50, ids[1], 1));
        changeStatus(RESTAURANT, cancelled, "Cancelled");

        long completed = createOrder(RESTAURANT, items(ids[2], 1, ids[1], 1));
        changeStatus(RESTAURANT, completed, "Preparing");
        changeStatus(RESTAURANT, completed, "Ready");
        changeStatus(RESTAURANT, completed, "Completed");

        long foreignProduct = createProduct(OTHER_RESTAURANT, "Wrap", "7.00");
        createOrder(OTHER_RESTAURANT, items(foreignProduct, 99));

        mockMvc.perform(get(TOP))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[*].productId", contains(
                        (int) ids[0], (int) ids[5], (int) ids[4], (int) ids[2], (int) ids[3])))
                .andExpect(jsonPath("$[*].quantitySold", contains(10, 6, 5, 4, 4)))
                .andExpect(jsonPath("$[0].name").value("Product 0"));
    }

    @Test
    void returnsEmptyListWhenNothingWasSold() throws Exception {
        mockMvc.perform(get(TOP))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void returnsNotFoundForUnknownRestaurant() throws Exception {
        mockMvc.perform(get("/api/restaurants/999/products/top-selling"))
                .andExpect(status().isNotFound());
    }

    private static String items(long... productAndQuantity) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < productAndQuantity.length; i += 2) {
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"productId\": ").append(productAndQuantity[i])
                    .append(", \"quantity\": ").append(productAndQuantity[i + 1]).append('}');
        }
        return json.append(']').toString();
    }
}
