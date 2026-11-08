package com.app.itibari.repository;

import com.app.itibari.models.entities.Department;
import com.app.itibari.models.entities.OrderAmendment;
import com.app.itibari.models.entities.SalesOrders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderAmendmentRepository extends JpaRepository<OrderAmendment, Long> {

    Page<OrderAmendment> findByDeletedIsFalse(Pageable pageable);

    Optional<OrderAmendment> findByPublicIdAndDeletedIsFalse(UUID amendOrderPublicId);

    Optional<OrderAmendment> findOrderAmendmentBySalesOrderPublicIdAndDeletedIsFalse(String string);

    Optional<OrderAmendment> findFirstByRouteIdAndSalesOrderAndDeletedIsFalseOrderByIdDesc(String routeId, SalesOrders salesOrder);

    List<OrderAmendment> findByRejectionsCode_DepartmentAndCreationTimeBetween(Department department, LocalDateTime startOfDay, LocalDateTime endOfDay);
}
