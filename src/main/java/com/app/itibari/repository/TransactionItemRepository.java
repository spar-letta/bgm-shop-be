package com.app.itibari.repository;

import com.app.itibari.dataTypes.TransactionType;
import com.app.itibari.dataTypes.WalletItemStatus;
import com.app.itibari.models.entities.TransactionItem;
import com.app.itibari.models.entities.Wallet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransactionItemRepository extends JpaRepository<TransactionItem,Long> {

    Optional<TransactionItem> findByTransactionTypeAndOrderNumberAndStatusAndDeletedIsFalse(TransactionType transactionType, String orderNumber, WalletItemStatus walletItemStatus);

    Page<TransactionItem> findTransactionItemsByWalletAndDeletedIsFalse(Wallet wallet, Pageable pageable);

    Page<TransactionItem> findTransactionItemsByWalletAndTransactionTypeAndDeletedIsFalse(Wallet wallet, TransactionType transactionType, Pageable pageable);
}
