package com.app.itibari.repository;

import com.app.itibari.models.entities.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findBySalesOrderPublicIdAndDeletedIsFalse(String string);

    Optional<Invoice> findByDiscountPublicIdAndDeletedIsFalse(String string);
}
