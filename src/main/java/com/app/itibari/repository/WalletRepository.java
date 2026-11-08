package com.app.itibari.repository;

import com.app.itibari.models.entities.Customers;
import com.app.itibari.models.entities.Wallet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet,Long> {
    Wallet findByCustomerAndDeletedIsFalse(Customers c);

    Page<Wallet> findAllByDeletedIsFalse(Pageable pageable);

    Optional<Wallet> findByPublicIdAndDeletedIsFalse(UUID walletId);

    Page<Wallet> findTop5ByDeletedFalseOrderByCreditLimitBalanceDesc(Pageable pageable);

    @Query(value = """
            SELECT *
            FROM itibari.wallet w
            WHERE w.customer_id = :customerId
              AND w.deleted = false
            LIMIT 1
            """, nativeQuery = true)
    Optional<Wallet> findActiveByCustomerIdNative(@Param("customerId") Long customerId);

    @Query(value = """
            SELECT *
            FROM itibari.wallet w
            WHERE w.deleted = false
            ORDER BY w.credit_limit_balance DESC
            LIMIT 5
            """, nativeQuery = true)
    List<Wallet> findTop5ActiveWalletsByCreditLimitBalanceNative();

    @Query(value = """
            SELECT *
            FROM itibari.wallet w
            WHERE w.deleted = false
            """,
            countQuery = """
                    SELECT COUNT(1)
                    FROM itibari.wallet w
                    WHERE w.deleted = false
                    """,
            nativeQuery = true)
    Page<Wallet> findAllActiveWalletsNative(Pageable pageable);

    @Query("""
            SELECT wallet
            FROM Wallet wallet
            WHERE wallet.deleted = false
              AND wallet.parentWallet IS NULL
              AND wallet.customer IS NOT NULL
              AND wallet.customer.bnplAccountCreated = true
            """)
    List<Wallet> findRootBnplWallets();
}
