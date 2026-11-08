package com.app.itibari.repository;

import com.app.itibari.models.entities.PriceManagement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.Optional;

public interface PriceManagementRepository extends JpaRepository<PriceManagement, Long> {

    Page<PriceManagement> findByProductContainingIgnoreCase(String searchParam, Pageable pageable);

    Optional<PriceManagement> findByProductAndSkuAndPriceAndDeletedIsFalse(String product, String sku, BigDecimal price);
}
