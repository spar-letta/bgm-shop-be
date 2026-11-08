package com.app.itibari.repository;

import com.app.itibari.models.entities.Product;
import com.app.itibari.models.entities.SalesOrderItems;
import com.app.itibari.models.entities.SalesOrders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SalesOrderItemsRepository extends JpaRepository<SalesOrderItems, Long> {

    Optional<SalesOrderItems> findByPublicIdAndDeletedIsFalse(UUID productItemId);

    List<SalesOrderItems> findAllByProductInAndDeletedIsFalseAndCreationTimeBetween(List<Product> supplierProducts, LocalDateTime localDateTime, LocalDateTime localDateTime1);

    List<SalesOrderItems> findAllBySalesOrderAndDeletedIsFalse(SalesOrders salesOrder);

    Optional<SalesOrderItems> findBySalesOrderPublicIdAndProductIdAndDeletedIsFalse(UUID uuid, UUID publicId);
}
