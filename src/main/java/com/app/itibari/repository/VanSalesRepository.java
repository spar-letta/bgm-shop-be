package com.app.itibari.repository;

import com.app.itibari.dataTypes.VanSaleStatus;
import com.app.itibari.models.entities.VanSales;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VanSalesRepository extends JpaRepository<VanSales, Long>, JpaSpecificationExecutor<VanSales> {

    Optional<VanSales> findByPublicIdAndDeletedIsFalse(UUID publicId);

    List<VanSales> findByDeliveryAgents_PublicIdAndStatusAndDeletedIsFalse(UUID deliveryAgentPublicId, VanSaleStatus vanSaleStatus);

    VanSales findByDeliveryAgents_PublicIdAndRoutePublicIdAndDeletedIsFalse(UUID deliveryAgentPublicId, String routePublicId);
}
