package com.app.itibari.repository;

import com.app.itibari.dataTypes.WalletItemStatus;
import com.app.itibari.models.entities.PaymentItem;
import com.app.itibari.models.entities.Wallet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaymentItemRepository extends JpaRepository<PaymentItem,Long> {
    Page<PaymentItem> findByWalletAndDeletedIsFalse(Wallet wallet, Pageable pageable);


    @Query(value = """
            SELECT *
            FROM itibari.payment_item pi
            WHERE pi.order_number = :orderNumber
              AND pi.status = :status
              AND pi.deleted = false
            """, nativeQuery = true)
    List<PaymentItem> findOrderPaymentsByStatusNative(
            @Param("orderNumber") String orderNumber,
            @Param("status") String status
    );

    List<PaymentItem> findByOrderNumberIgnoreCaseAndStatusAndDeletedIsFalse(String orderNumber, WalletItemStatus walletItemStatus);

    List<PaymentItem> findByOrderNumberIgnoreCaseAndPaymentMethodIgnoreCaseAndDeletedIsFalse(String orderNumber, String bnplWalletDepositFromWallet);
}
