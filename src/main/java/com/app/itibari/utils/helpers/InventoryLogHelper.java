package com.app.itibari.utils.helpers;

import com.app.itibari.dataTypes.OpBeforeAfter;
import com.app.itibari.models.entities.InventoryLog;
import com.app.itibari.repository.InventoryLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class InventoryLogHelper {

    @Autowired
    private InventoryLogRepository inventoryLogRepository;


    public void createLog(String actionType, OpBeforeAfter opBeforeAfter, UUID productPublicId, Long quantity,
                          Long productAvailableStock, Long productReservedStock, Long inventoryProductAvailableStock, Long reservedStockInventory) {

        InventoryLog inventoryLog = new InventoryLog();
        inventoryLog.setCreationTime(LocalDateTime.now());
        inventoryLog.setProductPublicId(productPublicId);
        inventoryLog.setEventType(actionType);
        inventoryLog.setOpBeforeAfter(opBeforeAfter);
        inventoryLog.setQuantity(quantity);
        inventoryLog.setProductReservedStock(productReservedStock);
        inventoryLog.setProductAvailableStock(productAvailableStock);
        inventoryLog.setInventoryProductAvailableStock(inventoryProductAvailableStock);
        inventoryLog.setInventoryProductReservedStock(reservedStockInventory);
        inventoryLogRepository.save(inventoryLog);
    }
}
