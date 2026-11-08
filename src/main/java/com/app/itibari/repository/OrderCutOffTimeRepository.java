package com.app.itibari.repository;

import com.app.itibari.models.entities.OrderCutOffTime;
import com.app.itibari.models.entities.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface OrderCutOffTimeRepository extends JpaRepository<OrderCutOffTime, Long>, JpaSpecificationExecutor<OrderCutOffTime> {

    Optional<OrderCutOffTime> findOrderCutOffTimeByWarehouseAndDeletedIsFalse(Warehouse warehouse);

    Optional<OrderCutOffTime> findByPublicIdAndDeletedIsFalse(UUID orderCutOffTimePublicId);

    Optional<OrderCutOffTime> findOrderCutOffTimeByDeletedIsFalse();
}
