package com.app.itibari.repository;

import com.app.itibari.dataTypes.RouteStatus;
import com.app.itibari.models.entities.RoutePlanning;
import com.app.itibari.models.entities.SalesOrders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDateTime;

public interface RoutePlanningRepository extends JpaRepository<RoutePlanning, Long>, JpaSpecificationExecutor<RoutePlanning> {

    Optional<RoutePlanning> getRoutePlanningByPublicIdAndDeletedIsFalse(UUID routePlanningPublicId);

    Optional<RoutePlanning> findByPublicIdAndDeletedIsFalse(UUID routeId);

    @Query("SELECT r FROM RoutePlanning r " +
            "WHERE LOWER(r.deliveryAgentId) = LOWER(:deliveryAgentId) " +
            "AND r.routeStatus IN :statuses " +
            "AND r.deleted = false")
    List<RoutePlanning> findAllByDeliveryAgentIdIgnoreCaseAndRouteStatusesAndDeletedIsFalse(
            @Param("deliveryAgentId") String deliveryAgentId,
            @Param("statuses") List<RouteStatus> statuses
    );

    Optional<RoutePlanning> findByRouteIdAndDeletedIsFalse(String deliveryId);

    RoutePlanning findByPublicId(UUID routePublicId);

    Optional<RoutePlanning> findByDeliveryAgentIdIgnoreCaseAndDeletedIsFalseAndSalesOrdersIn(String string, List<SalesOrders> salesOrders);

    List<RoutePlanning> findByCreationTimeBetweenAndDeletedIsFalse(LocalDateTime startTime, LocalDateTime endTime);

}
