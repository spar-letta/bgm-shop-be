package com.app.itibari.services.helper;

import com.app.itibari.dataTypes.StockMovementTransactionType;
import com.app.itibari.dataTypes.TransactionBoundType;
import com.app.itibari.models.entities.*;
import com.app.itibari.repository.StockMovementItemRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class StockMovementHelper {

    private final StockMovementItemRepository stockMovementItemRepository;

    public StockMovementHelper(StockMovementItemRepository stockMovementItemRepository) {
        this.stockMovementItemRepository = stockMovementItemRepository;
    }

    public void recordStockMovement(Product product, Integer quantity,
                                    StockMovementTransactionType transactionType,
                                    TransactionBoundType boundType,
                                    String doneBy, Warehouse warehouse) {


        StockMovementItem stockMovementItem = new StockMovementItem();
        stockMovementItem.setProductName(product.getProductName());
        stockMovementItem.setProductSku(product.getSku());
        stockMovementItem.setQuantity(quantity);
        stockMovementItem.setTransactionType(transactionType);
        stockMovementItem.setBoundType(boundType);
        stockMovementItem.setDoneBy(doneBy);
        stockMovementItem.setWarehouse(warehouse.getWarehouseName());
        stockMovementItem.setWarehousePublicId(warehouse.getPublicId().toString());
        stockMovementItem.setCreationTime(LocalDateTime.now());


        stockMovementItemRepository.save(stockMovementItem);
    }

    public void recordClosingStockUpdate(InventoryProducts inventoryProducts, Integer closingStock, Person person,Integer previousClosingStock) {
        StockMovementItem stockMovementItem = new StockMovementItem();
        stockMovementItem.setProductName(inventoryProducts.getProduct().getProductName());
        stockMovementItem.setProductSku(inventoryProducts.getProduct().getSku());
        stockMovementItem.setQuantity(closingStock);
        stockMovementItem.setPreviousClosingStock(previousClosingStock);
        stockMovementItem.setTransactionType(StockMovementTransactionType.CLOSING_STOCK_UPDATE);
        stockMovementItem.setBoundType(TransactionBoundType.RECORD);
        stockMovementItem.setDoneBy(person.getFullName());
        stockMovementItem.setWarehouse(inventoryProducts.getInventory().getWarehouse().getWarehouseName());
        stockMovementItem.setWarehousePublicId(inventoryProducts.getInventory().getWarehouse().getPublicId().toString());
        stockMovementItem.setCreationTime(LocalDateTime.now());

        stockMovementItemRepository.save(stockMovementItem);
    }
}
