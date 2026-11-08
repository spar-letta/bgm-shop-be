package com.app.itibari.repository;

import com.app.itibari.models.entities.DeliveryAgentOperation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryAgentOperationRepository extends JpaRepository<DeliveryAgentOperation, Long> {

    void deleteByRouteId(String routeId);

    Optional<DeliveryAgentOperation> findByRouteIdAndOrderNumberAndSkuAndDeletedIsFalse(String routeId, String orderNumber, String sku);

    List<DeliveryAgentOperation> findByRouteIdAndSkuAndDeletedIsFalse(String routeId, String sku);

    Optional<DeliveryAgentOperation> findByRouteIdAndSkuAndOrderNumberIsNullAndDeletedIsFalse(String routeId, String sku);

}
