package com.app.itibari.repository;

import com.app.itibari.models.entities.CustomerReturn;
import com.app.itibari.models.entities.GoodsReceivedNote;
import com.app.itibari.models.entities.SalesOrders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface CustomerReturnRepository extends JpaRepository<CustomerReturn, Long>, JpaSpecificationExecutor<CustomerReturn> {
    Optional<CustomerReturn> findByDeletedIsFalseAndSalesOrder(SalesOrders salesOrder);

    Optional<CustomerReturn> findByPublicIdAndDeletedIsFalse(UUID customerReturnPublicId);

    Page<CustomerReturn> findAllByDeletedIsFalse(Pageable pageable);
}
