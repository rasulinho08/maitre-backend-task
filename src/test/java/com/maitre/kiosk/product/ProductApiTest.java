package com.maitre.kiosk.product;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import com.maitre.kiosk.ApiTestSupport;

class ProductApiTest extends ApiTestSupport {

    private static final String PRODUCTS = "/api/restaurants/" + RESTAURANT + "/products";

    @Test
    void createsProductAvailableByDefault() throws Exception {
        postJson(PRODUCTS, """
                {"name": "  Veggie Burger ", "category": "Burgers", "price": 7.5}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.restaurantId").value(RESTAURANT))
                .andExpect(jsonPath("$.name").value("Veggie Burger"))
                .andExpect(jsonPath("$.category").value("Burgers"))
                .andExpect(jsonPath("$.price").value(7.50))
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void rejectsInvalidProduct() throws Exception {
        postJson(PRODUCTS, """
                {"name": "", "category": "Burgers", "price": 0}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.price").exists());

        postJson(PRODUCTS, """
                {"name": "Soup", "category": "Starters", "price": 1.999}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.price").exists());
    }

    @Test
    void rejectsDuplicateNameInSameRestaurantOnly() throws Exception {
        createProduct(RESTAURANT, "Tea", "1.50");

        postJson(PRODUCTS, """
                {"name": "tea", "category": "Drinks", "price": 2.00}
                """)
                .andExpect(status().isConflict());

        createProduct(OTHER_RESTAURANT, "Tea", "1.50");
    }

    @Test
    void returnsNotFoundForUnknownRestaurant() throws Exception {
        postJson("/api/restaurants/999/products", """
                {"name": "Tea", "category": "Drinks", "price": 1.50}
                """)
                .andExpect(status().isNotFound());
    }

    @Test
    void updatesPriceAndAvailability() throws Exception {
        long id = createProduct(RESTAURANT, "Latte", "3.00");

        patchJson(PRODUCTS + "/" + id, """
                {"price": 3.40}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(3.40))
                .andExpect(jsonPath("$.available").value(true));

        patchJson(PRODUCTS + "/" + id, """
                {"available": false}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(3.40))
                .andExpect(jsonPath("$.available").value(false));

        patchJson(PRODUCTS + "/" + id, """
                {"available": true}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void rejectsEmptyUpdate() throws Exception {
        long id = createProduct(RESTAURANT, "Latte", "3.00");

        patchJson(PRODUCTS + "/" + id, "{}").andExpect(status().isBadRequest());
    }

    @Test
    void cannotUpdateProductOfAnotherRestaurant() throws Exception {
        long id = createProduct(OTHER_RESTAURANT, "Latte", "3.00");

        patchJson(PRODUCTS + "/" + id, """
                {"price": 1.00}
                """)
                .andExpect(status().isNotFound());
    }
}
