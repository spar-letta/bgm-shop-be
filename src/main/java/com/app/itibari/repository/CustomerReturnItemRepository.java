package com.app.itibari.repository;

import com.app.itibari.models.CustomerReturnItem;
import com.app.itibari.models.entities.CustomerReturn;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerReturnItemRepository extends JpaRepository<CustomerReturnItem, Long> {
    Optional<CustomerReturnItem> findByProductPublicIdAndOrderItemPublicId(UUID ProductPublicId, @NotNull UUID uuid);
}
