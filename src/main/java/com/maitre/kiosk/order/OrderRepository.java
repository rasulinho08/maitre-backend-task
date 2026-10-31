package com.maitre.kiosk.order;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {

    @EntityGraph(attributePaths = "lines")
    Optional<CustomerOrder> findWithLinesByIdAndRestaurantId(Long id, Long restaurantId);

    /*
     * Listing is done in two steps: first a page of ids, then the orders with their lines.
     * Fetch-joining a collection together with LIMIT/OFFSET would make Hibernate paginate in memory.
     */
    @Query(value = "select o.id from CustomerOrder o where o.restaurantId = :restaurantId",
            countQuery = "select count(o) from CustomerOrder o where o.restaurantId = :restaurantId")
    Page<Long> findIdsByRestaurant(@Param("restaurantId") Long restaurantId, Pageable pageable);

    @Query(value = """
            select o.id from CustomerOrder o
            where o.restaurantId = :restaurantId and o.status = :status
            """,
            countQuery = """
            select count(o) from CustomerOrder o
            where o.restaurantId = :restaurantId and o.status = :status
            """)
    Page<Long> findIdsByRestaurantAndStatus(@Param("restaurantId") Long restaurantId,
                                            @Param("status") OrderStatus status,
                                            Pageable pageable);

    @Query("select distinct o from CustomerOrder o left join fetch o.lines where o.id in :ids")
    List<CustomerOrder> findAllWithLinesByIdIn(@Param("ids") Collection<Long> ids);
}
