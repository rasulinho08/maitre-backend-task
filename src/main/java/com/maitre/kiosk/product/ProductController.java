package com.maitre.kiosk.product;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/restaurants/{restaurantId}/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@PathVariable Long restaurantId,
                                  @Valid @RequestBody CreateProductRequest request) {
        return productService.create(restaurantId, request);
    }

    @PatchMapping("/{productId}")
    public ProductResponse update(@PathVariable Long restaurantId,
                                  @PathVariable Long productId,
                                  @Valid @RequestBody UpdateProductRequest request) {
        return productService.update(restaurantId, productId, request);
    }

    @GetMapping("/top-selling")
    public List<TopProductResponse> topSelling(@PathVariable Long restaurantId) {
        return productService.topSelling(restaurantId);
    }
}
