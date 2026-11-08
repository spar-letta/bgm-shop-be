package com.app.itibari.repository;

import com.app.itibari.models.entities.DiscountItems;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DiscountItemsRepository extends JpaRepository<DiscountItems, Long> {

    Optional<DiscountItems> findDiscountItemsBySalesOrderItemPublicIdAndDeletedIsFalse(String string);

    List<DiscountItems> findDiscountItemsByProductReconItemPublicIdAndDeletedIsFalse(String string);
}
