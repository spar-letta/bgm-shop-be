package com.app.itibari.services.helper;

import com.app.itibari.models.entities.Inventory;
import com.app.itibari.models.entities.InventoryProducts;
import com.app.itibari.models.entities.Product;
import com.app.itibari.models.entities.ProductStock;
import com.app.itibari.models.entities.Warehouse;
import com.app.itibari.repository.InventoryProductsRepository;
import com.app.itibari.repository.InventoryRepository;
import com.app.itibari.repository.ProductStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 04:40 catch-up after the 03:30 rollover. For every active inventory product it
 * checks that today's {@code product_stocks} and {@code live_stock_value} rows
 * exist. Missing rows are copied from yesterday; if yesterday is also missing
 * they are created from current inventory closing stock.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyStockRecordChecker {

    private final InventoryRepository inventoryRepository;
    private final InventoryProductsRepository inventoryProductsRepository;
    private final ProductStockRepository productStockRepository;
    private final LiveStockHelper liveStockHelper;

    public void ensureTodayRecords(LocalDate date) {
        log.info("STOCK-BACKFILL started for {}", date);
        int checked = 0;
        int skipped = 0;
        int productStockCreated = 0;
        int liveStockCreated = 0;
        int failed = 0;

        for (Inventory inventory : inventoryRepository.findAllByDeletedIsFalse()) {
            Warehouse warehouse = inventory.getWarehouse();
            if (warehouse == null || Boolean.TRUE.equals(warehouse.getDeleted())) {
                skipped++;
                continue;
            }

            List<InventoryProducts> inventoryProducts =
                    inventoryProductsRepository.findByInventoryAndDeletedIsFalse(inventory);
            for (InventoryProducts inventoryProduct : inventoryProducts) {
                Product product = inventoryProduct.getProduct();
                if (product == null
                        || Boolean.TRUE.equals(product.getDeleted())
                        || product.isInActive()) {
                    skipped++;
                    continue;
                }
                checked++;
                try {
                    if (ensureProductStock(inventory, warehouse, inventoryProduct, product, date)) {
                        productStockCreated++;
                    }
                    if (liveStockHelper.ensureTodayLiveStockRecord(
                            warehouse, inventory, inventoryProduct, product, date)) {
                        liveStockCreated++;
                    }
                } catch (Exception e) {
                    failed++;
                    log.error("STOCK-BACKFILL failed for sku={} warehouse={}: {}",
                            product.getSku(), warehouse.getWarehouseName(), e.getMessage(), e);
                }
            }
        }

        log.info("STOCK-BACKFILL finished for {}. checked={}, skipped={}, productStockCreated={}, liveStockCreated={}, failed={}",
                date, checked, skipped, productStockCreated, liveStockCreated, failed);
    }

    private boolean ensureProductStock(Inventory inventory,
                                       Warehouse warehouse,
                                       InventoryProducts inventoryProduct,
                                       Product product,
                                       LocalDate date) {
        UUID productId = product.getPublicId();
        UUID inventoryId = inventory.getPublicId();
        if (!latestProductStock(productId, inventoryId, date).isEmpty()) {
            return false;
        }

        ProductStock previous = latestProductStock(productId, inventoryId, date.minusDays(1))
                .stream()
                .max(Comparator.comparing(ProductStock::getId))
                .orElse(null);

        ProductStock created = new ProductStock();
        created.setProduct(product);
        created.setProductPublicId(productId);
        created.setInventoryPublicId(inventoryId);
        created.setRecordToday(date);
        created.setProductName(product.getProductName());
        created.setProductSku(product.getSku());
        created.setOnHoldStock(0L);

        if (previous != null) {
            created.setWarehouse(previous.getWarehouse() != null ? previous.getWarehouse() : warehouse);
            created.setSellPrice(previous.getSellPrice());
            created.setAvailableStock(safeLong(previous.getAvailableStock()));
            created.setReservedStock(safeLong(previous.getReservedStock()));
            created.setQuarantinedStock(safeLong(previous.getQuarantinedStock()));
            created.setTotalStock(safeLong(previous.getClosingStock()));
            created.setClosingStock(safeLong(previous.getClosingStock()));
            created.setOpeningStock(safeLong(previous.getClosingStock()));
            log.info("STOCK-BACKFILL product stock for sku={} warehouse={} copied from {}",
                    product.getSku(), warehouse.getWarehouseName(), date.minusDays(1));
        } else {
            created.setWarehouse(warehouse);
            created.setSellPrice(product.getSellPrice());
            created.setAvailableStock(safeLong(inventoryProduct.getAvailableStock()));
            created.setReservedStock(safeLong(inventoryProduct.getReservedStock()));
            created.setQuarantinedStock(safeLong(inventoryProduct.getQuarantinedStock()));
            created.setTotalStock(safeLong(inventoryProduct.getClosingStock()));
            created.setClosingStock(safeLong(inventoryProduct.getClosingStock()));
            created.setOpeningStock(safeLong(inventoryProduct.getClosingStock()));
            log.info("STOCK-BACKFILL product stock for sku={} warehouse={} created from inventory closing={}",
                    product.getSku(), warehouse.getWarehouseName(), created.getOpeningStock());
        }

        productStockRepository.save(created);
        return true;
    }

    private List<ProductStock> latestProductStock(UUID productId, UUID inventoryId, LocalDate date) {
        return productStockRepository
                .findAllByRecordTodayAndDeletedFalseAndProductPublicIdAndInventoryPublicId(
                        date, productId, inventoryId);
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }
}
