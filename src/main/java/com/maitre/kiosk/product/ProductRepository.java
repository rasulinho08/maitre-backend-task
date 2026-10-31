package com.maitre.kiosk.product;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.maitre.kiosk.order.OrderStatus;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByIdAndRestaurantId(Long id, Long restaurantId);

    List<Product> findAllByRestaurantIdAndIdIn(Long restaurantId, Collection<Long> ids);

    boolean existsByRestaurantIdAndNameIgnoreCase(Long restaurantId, String name);

    @Query("""
            select new com.maitre.kiosk.product.TopProductResponse(p.id, p.name, p.category, sum(l.quantity))
            from OrderLine l
            join l.order o
            join Product p on p.id = l.productId
            where o.restaurantId = :restaurantId
              and o.status <> :excludedStatus
            group by p.id, p.name, p.category
            order by sum(l.quantity) desc, p.id asc
            """)
    List<TopProductResponse> findBestSellers(@Param("restaurantId") Long restaurantId,
                                             @Param("excludedStatus") OrderStatus excludedStatus,
                                             Pageable pageable);
}
