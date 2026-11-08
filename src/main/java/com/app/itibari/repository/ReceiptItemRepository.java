package com.app.itibari.repository;

import com.app.itibari.models.entities.Receipt;
import com.app.itibari.models.entities.ReceiptItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReceiptItemRepository extends JpaRepository<ReceiptItem, Integer> {
    List<ReceiptItem> findByReceiptAndDeletedIsFalse(Receipt receipt);
}
