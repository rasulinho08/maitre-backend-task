package com.maitre.kiosk.product;

import java.time.Clock;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maitre.kiosk.common.ConflictException;
import com.maitre.kiosk.common.NotFoundException;
import com.maitre.kiosk.order.OrderStatus;
import com.maitre.kiosk.restaurant.RestaurantGuard;

@Service
public class ProductService {

    static final int TOP_PRODUCTS_LIMIT = 5;

    private final ProductRepository productRepository;
    private final RestaurantGuard restaurantGuard;
    private final Clock clock;

    public ProductService(ProductRepository productRepository, RestaurantGuard restaurantGuard, Clock clock) {
        this.productRepository = productRepository;
        this.restaurantGuard = restaurantGuard;
        this.clock = clock;
    }

    @Transactional
    public ProductResponse create(Long restaurantId, CreateProductRequest request) {
        restaurantGuard.ensureExists(restaurantId);

        String name = request.name().trim();
        if (productRepository.existsByRestaurantIdAndNameIgnoreCase(restaurantId, name)) {
            throw new ConflictException("A product named '" + name + "' already exists in this restaurant.");
        }

        Product product = new Product(restaurantId, name, request.category().trim(), request.price(), clock.instant());
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long restaurantId, Long productId, UpdateProductRequest request) {
        restaurantGuard.ensureExists(restaurantId);
        Product product = productRepository.findByIdAndRestaurantId(productId, restaurantId)
                .orElseThrow(() -> new NotFoundException("Product " + productId + " not found."));

        if (request.price() != null) {
            product.changePrice(request.price(), clock.instant());
        }
        if (request.available() != null) {
            product.changeAvailability(request.available(), clock.instant());
        }
        return ProductResponse.from(product);
    }

    @Transactional(readOnly = true)
    public List<TopProductResponse> topSelling(Long restaurantId) {
        restaurantGuard.ensureExists(restaurantId);
        return productRepository.findBestSellers(restaurantId, OrderStatus.CANCELLED,
                PageRequest.of(0, TOP_PRODUCTS_LIMIT));
    }
}
