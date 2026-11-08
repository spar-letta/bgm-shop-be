package com.app.itibari.utils.helpers;

import com.app.itibari.models.entities.Inventory;
import com.app.itibari.models.entities.InventoryProducts;
import com.app.itibari.models.response.LiveStockPageable;
import com.app.itibari.repository.InventoryProductsRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class LiveStockValueHelper {

    private final InventoryProductsRepository inventoryProductsRepository;

    public LiveStockValueHelper(InventoryProductsRepository inventoryProductsRepository) {

        this.inventoryProductsRepository = inventoryProductsRepository;
    }

    public LiveStockPageable getLiveStockPageable(LocalDate localDate, Inventory inventory) {

        List<InventoryProducts> inventoryProducts = inventoryProductsRepository
                .findAllByInventoryAndDeletedIsFalse(inventory, Pageable.unpaged())
                .getContent();

        return new LiveStockPageable();
    }
}
