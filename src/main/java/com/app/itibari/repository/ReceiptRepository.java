package com.app.itibari.repository;

import com.app.itibari.dataTypes.ReceiptPaymentType;
import com.app.itibari.models.entities.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReceiptRepository extends JpaRepository<Receipt, Long> {

    Optional<Receipt> findBySalesOrderPublicIdAndPaymentTypeAndDeletedIsFalse(String string, ReceiptPaymentType paymentType);
}
