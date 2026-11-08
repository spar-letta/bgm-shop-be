package com.app.itibari.services.helper;

import com.app.itibari.models.entities.Inventory;
import com.app.itibari.models.entities.InventoryProducts;
import com.app.itibari.models.entities.LiveStockEntity;
import com.app.itibari.models.entities.Product;
import com.app.itibari.models.entities.ProductStock;
import com.app.itibari.models.entities.Suppliers;
import com.app.itibari.models.entities.Warehouse;
import com.app.itibari.repository.InventoryProductsRepository;
import com.app.itibari.repository.InventoryRepository;
import com.app.itibari.repository.LiveStockEntityRepository;
import com.app.itibari.repository.ProductStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Read-only nightly stock health pass. In a single sweep over every inventory
 * product it reports:
 *
 * 1. Layer drift — the independently-maintained stock layers disagreeing:
 *    operational stock (InventoryProducts.availableStock), the daily snapshot
 *    (ProductStock.availableStock) and the live/static report
 *    (LiveStockEntity.closingStock).
 * 2. Low stock — products at or below the reorder point (mirrors the
 *    Product.getStatus() LowOnStock rule), with supplier contacts so a
 *    purchase order can be raised immediately.
 *
 * This detector NEVER writes. Findings are logged (prefixed STOCK-DRIFT /
 * STOCK-LOW for alerting) and returned as a {@link StockHealthReport} for the
 * Google Chat notifier. Only active products ({@code inActive = false}, not
 * deleted) are included.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockDriftDetector {

    /** Reorder point: matches the LowOnStock rule in Product.getStatus(). */
    private static final long LOW_STOCK_THRESHOLD = 10L;

    /** Cap detailed per-product warnings so a widely-drifted day cannot flood the logs. */
    private static final int MAX_DETAIL_LOGS = 200;

    /** Worst offenders included in the report/card; the summary counts are always complete. */
    private static final int MAX_TOP_DRIFTS = 10;
    private static final int MAX_LOW_STOCK_ENTRIES = 25;
    private static final int MAX_SUPPLIERS_PER_PRODUCT = 3;

    private final InventoryRepository inventoryRepository;
    private final InventoryProductsRepository inventoryProductsRepository;
    private final ProductStockRepository productStockRepository;
    private final LiveStockEntityRepository liveStockEntityRepository;

    @Transactional(readOnly = true)
    public StockHealthReport runStockHealthCheck(LocalDate date) {
        log.info("STOCK-DRIFT report started for {}", date);

        int checked = 0;
        int missingSnapshot = 0;
        int missingLive = 0;
        int lowStockCount = 0;
        int detailLogs = 0;
        List<StockHealthReport.DriftEntry> drifts = new ArrayList<>();
        List<StockHealthReport.LowStockEntry> lowStock = new ArrayList<>();

        for (Inventory inventory : inventoryRepository.findAll()) {
            Warehouse warehouse = inventory.getWarehouse();
            if (warehouse == null || Boolean.TRUE.equals(inventory.getDeleted())) {
                continue;
            }

            List<InventoryProducts> inventoryProducts = inventoryProductsRepository
                    .findAllByInventoryAndDeletedIsFalse(inventory, Pageable.unpaged())
                    .getContent();

            for (InventoryProducts inventoryProduct : inventoryProducts) {
                Product product = inventoryProduct.getProduct();
                // Active products only — inactive/closed SKUs are out of business scope for this report.
                if (product == null
                        || Boolean.TRUE.equals(product.getDeleted())
                        || product.isInActive()) {
                    continue;
                }
                checked++;

                long operational = safeLong(inventoryProduct.getAvailableStock());

                Long snapshotAvailable = productStockRepository
                        .findAllByRecordTodayAndDeletedFalseAndProductPublicIdAndInventoryPublicId(
                                date, product.getPublicId(), inventory.getPublicId())
                        .stream()
                        .findFirst()
                        .map(ProductStock::getAvailableStock)
                        .orElse(null);

                Integer liveClosing = liveStockEntityRepository
                        .findByInventoryItemPublicIdAndInventoryPublicIdAndWarehousePublicIdAndLiveDateAndDeletedFalse(
                                inventoryProduct.getPublicId().toString(),
                                inventory.getPublicId().toString(),
                                warehouse.getPublicId().toString(),
                                date)
                        .map(LiveStockEntity::getClosingStock)
                        .orElse(null);

                if (snapshotAvailable == null) {
                    missingSnapshot++;
                }
                if (liveClosing == null) {
                    missingLive++;
                }

                boolean snapshotDrift = snapshotAvailable != null && snapshotAvailable != operational;
                boolean liveDrift = liveClosing != null && liveClosing != operational;

                if (snapshotDrift || liveDrift) {
                    drifts.add(new StockHealthReport.DriftEntry(
                            product.getProductName(), product.getSku(), warehouse.getWarehouseName(),
                            operational, snapshotAvailable, liveClosing));
                    if (detailLogs < MAX_DETAIL_LOGS) {
                        detailLogs++;
                        log.warn("STOCK-DRIFT product='{}' sku={} warehouse='{}' operationalAvailable={} snapshotAvailable={} liveClosing={}",
                                product.getProductName(),
                                product.getSku(),
                                warehouse.getWarehouseName(),
                                operational,
                                snapshotAvailable,
                                liveClosing);
                    }
                }

                if (operational <= LOW_STOCK_THRESHOLD) {
                    lowStockCount++;
                    if (lowStock.size() < MAX_LOW_STOCK_ENTRIES) {
                        String suppliers = describeSuppliers(product);
                        lowStock.add(new StockHealthReport.LowStockEntry(
                                product.getProductName(), product.getSku(), warehouse.getWarehouseName(),
                                operational, suppliers));
                        log.warn("STOCK-LOW product='{}' sku={} warehouse='{}' available={} suppliers='{}'",
                                product.getProductName(), product.getSku(), warehouse.getWarehouseName(),
                                operational, suppliers);
                    }
                }
            }
        }

        List<StockHealthReport.DriftEntry> topDrifts = drifts.stream()
                .sorted(Comparator.comparingLong(StockHealthReport.DriftEntry::magnitude).reversed())
                .limit(MAX_TOP_DRIFTS)
                .collect(Collectors.toList());

        // Most-depleted products first so the card leads with what to reorder now.
        lowStock.sort(Comparator.comparingLong(StockHealthReport.LowStockEntry::available));

        log.info("STOCK-DRIFT report finished for {}: checked={}, drifted={}, missingSnapshot={}, missingLive={}, lowStock={}",
                date, checked, drifts.size(), missingSnapshot, missingLive, lowStockCount);

        return new StockHealthReport(date, checked, drifts.size(), missingSnapshot, missingLive,
                lowStockCount, topDrifts, lowStock);
    }

    private String describeSuppliers(Product product) {
        List<Suppliers> suppliers = product.getSuppliers();
        if (suppliers == null || suppliers.isEmpty()) {
            return "No supplier linked";
        }
        return suppliers.stream()
                .limit(MAX_SUPPLIERS_PER_PRODUCT)
                .map(supplier -> {
                    String contact = supplier.getPhoneNumber() != null && !supplier.getPhoneNumber().isBlank()
                            ? supplier.getPhoneNumber()
                            : supplier.getEmail();
                    return contact == null || contact.isBlank()
                            ? supplier.getSupplierName()
                            : supplier.getSupplierName() + " (" + contact + ")";
                })
                .collect(Collectors.joining(", "));
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }
}
