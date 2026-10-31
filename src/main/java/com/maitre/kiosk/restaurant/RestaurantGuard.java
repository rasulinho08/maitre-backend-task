package com.maitre.kiosk.restaurant;

import org.springframework.stereotype.Component;

import com.maitre.kiosk.common.NotFoundException;

@Component
public class RestaurantGuard {

    private final RestaurantRepository restaurantRepository;

    public RestaurantGuard(RestaurantRepository restaurantRepository) {
        this.restaurantRepository = restaurantRepository;
    }

    public void ensureExists(Long restaurantId) {
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new NotFoundException("Restaurant " + restaurantId + " not found.");
        }
    }
}
