package com.app.itibari.repository;

import com.app.itibari.models.entities.StockMovementItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StockMovementItemRepository extends JpaRepository<StockMovementItem, Long>, JpaSpecificationExecutor<StockMovementItem> {
}
