package com.maitre.kiosk.order;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maitre.kiosk.common.BusinessRuleException;
import com.maitre.kiosk.common.NotFoundException;
import com.maitre.kiosk.common.PageResponse;
import com.maitre.kiosk.product.Product;
import com.maitre.kiosk.product.ProductRepository;
import com.maitre.kiosk.restaurant.RestaurantGuard;

@Service
public class OrderService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final RestaurantGuard restaurantGuard;
    private final Clock clock;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository,
                        RestaurantGuard restaurantGuard, Clock clock) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.restaurantGuard = restaurantGuard;
        this.clock = clock;
    }

    @Transactional
    public OrderResponse create(Long restaurantId, CreateOrderRequest request) {
        restaurantGuard.ensureExists(restaurantId);

        Map<Long, Integer> quantities = mergeQuantities(request.items());
        Map<Long, Product> products = productRepository
                .findAllByRestaurantIdAndIdIn(restaurantId, quantities.keySet()).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<Long> missing = quantities.keySet().stream().filter(id -> !products.containsKey(id)).toList();
        if (!missing.isEmpty()) {
            throw new BusinessRuleException("Products not found in this restaurant: " + missing);
        }
        List<Long> unavailable = products.values().stream()
                .filter(product -> !product.isAvailable())
                .map(Product::getId)
                .sorted()
                .toList();
        if (!unavailable.isEmpty()) {
            throw new BusinessRuleException("Products are not available for sale: " + unavailable);
        }

        CustomerOrder order = new CustomerOrder(restaurantId, clock.instant());
        quantities.forEach((productId, quantity) -> order.addLine(products.get(productId), quantity));
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse changeStatus(Long restaurantId, Long orderId, OrderStatus status) {
        restaurantGuard.ensureExists(restaurantId);
        CustomerOrder order = orderRepository.findWithLinesByIdAndRestaurantId(orderId, restaurantId)
                .orElseThrow(() -> new NotFoundException("Order " + orderId + " not found."));

        order.changeStatus(status, clock.instant());
        orderRepository.flush();
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> list(Long restaurantId, OrderStatus status, int page, int size) {
        restaurantGuard.ensureExists(restaurantId);
        PageRequest pageRequest = PageRequest.of(page, size, NEWEST_FIRST);

        Page<Long> ids = status == null
                ? orderRepository.findIdsByRestaurant(restaurantId, pageRequest)
                : orderRepository.findIdsByRestaurantAndStatus(restaurantId, status, pageRequest);

        List<OrderResponse> items = List.of();
        if (ids.hasContent()) {
            Map<Long, CustomerOrder> ordersById = orderRepository.findAllWithLinesByIdIn(ids.getContent()).stream()
                    .collect(Collectors.toMap(CustomerOrder::getId, Function.identity()));
            items = ids.getContent().stream()
                    .map(ordersById::get)
                    .map(OrderResponse::from)
                    .toList();
        }
        return new PageResponse<>(items, page, size, ids.getTotalElements(), ids.getTotalPages());
    }

    /**
     * The same product may appear more than once in a kiosk request; it becomes a single line.
     */
    private static Map<Long, Integer> mergeQuantities(List<CreateOrderRequest.Item> items) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (CreateOrderRequest.Item item : items) {
            quantities.merge(item.productId(), item.quantity(), Integer::sum);
        }
        quantities.forEach((productId, quantity) -> {
            if (quantity > 99) {
                throw new BusinessRuleException("Quantity for product " + productId + " cannot exceed 99.");
            }
        });
        return quantities;
    }
}
