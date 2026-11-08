package com.app.itibari.repository;

import com.app.itibari.models.entities.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {

    Optional<InvoiceItem> findBySalesOrderItemIdAndDeletedIsFalse(String string);
}
