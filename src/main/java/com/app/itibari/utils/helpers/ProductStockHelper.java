package com.app.itibari.utils.helpers;

import com.app.itibari.dataTypes.StockMovementTransactionType;
import com.app.itibari.models.entities.Inventory;
import com.app.itibari.models.entities.Product;
import com.app.itibari.models.entities.ProductStock;
import com.app.itibari.models.entities.Warehouse;
import com.app.itibari.repository.InventoryRepository;
import com.app.itibari.repository.ProductRepository;
import com.app.itibari.repository.ProductStockRepository;
import com.app.itibari.services.ProductService;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.common.aliasing.qual.Unique;
import org.hibernate.validator.internal.util.stereotypes.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductStockHelper {

    private final ProductStockRepository productStockRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    @Transactional
    public void updateProductStock(Product product, Warehouse warehouse,
                                   Long quantity, StockMovementTransactionType stockMovementTransactionType) {
        ZoneId nairobiZone = ZoneId.of("Africa/Nairobi");
        LocalDate nairobiDate = LocalDate.now(nairobiZone);

        Inventory inventory = inventoryRepository.findByWarehouseAndDeletedIsFalse(warehouse)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Inventory not found")
                        .userMessage("The inventory for the specified warehouse does not exist or has been deleted."));


        List<ProductStock> stocks = productStockRepository
                .findByDeletedFalseAndProductPublicIdAndInventoryPublicIdAndRecordToday(
                        product.getPublicId(),
                        inventory.getPublicId(),
                        nairobiDate
                );

        Optional<ProductStock> optionalProductStock = stocks.stream().findFirst();
        ProductStock productStock = null;
        if (optionalProductStock.isPresent()) {
            productStock = optionalProductStock.get();
        } else {
            productStock = createProductStockRecord(product, inventory.getPublicId(), warehouse, nairobiDate);
        }

        switch (stockMovementTransactionType) {
            case GRTN, STOCK_ADJUSTMENT_DOWN ->
                    productStock.setAvailableStock(decreaseOrZero(productStock.getAvailableStock(), quantity));

            case STOCK_ADJUSTMENT_UP, BONUS, FROM_TRADE, GRN, CUSTOMER_RETURN ->
                    productStock.setAvailableStock(incrementOrZero(productStock.getAvailableStock(), quantity));

            case TRANSFER_TO_QUARANTINE -> {
                productStock.setQuarantinedStock(incrementOrZero(productStock.getQuarantinedStock(),
                        quantity));
                productStock.setAvailableStock(decreaseOrZero(productStock.getAvailableStock(), quantity));
            }
            case QUARANTINE_STOCK_DISPOSED -> productStock.setQuarantinedStock(decreaseOrZero(productStock.getQuarantinedStock(), quantity));

            case TRANSFER_FROM_QUARANTINE -> {
                productStock.setQuarantinedStock(decreaseOrZero(productStock.getQuarantinedStock(),
                        quantity));
                productStock.setAvailableStock(incrementOrZero(productStock.getAvailableStock(), quantity));
            }
            case TO_RESERVED -> {
                productStock.setReservedStock(incrementOrZero(productStock.getReservedStock(), quantity));

                productStock.setAvailableStock(decreaseOrZero(productStock.getAvailableStock(), quantity));
            }
            case REVERT_BEFORE_UPDATE, ORDER_CANCEL -> {
                productStock.setReservedStock(decreaseOrZero(productStock.getReservedStock(), quantity));

                productStock.setAvailableStock(incrementOrZero(productStock.getAvailableStock(), quantity));
            }
            case ISSUANCE -> productStock.setReservedStock(decreaseOrZero(productStock.getReservedStock(),
                    quantity));

        }

        productStock.setCreationTime(LocalDateTime.now());
        productStockRepository.save(productStock);
    }

    private ProductStock createProductStock(String productName, UUID productId, BigDecimal sellingPrice,
                                            String sku,
                                            UUID inventoryId,
                                            LocalDate today,
                                            Warehouse warehouse,
                                            Long availableStock,
                                            Long reservedStock,
                                            Long quarantinedStock,
                                            Long totalStock,
                                            Long closingStock,
                                            Long openingStock

    ) {
        ProductStock ps = new ProductStock();
        ps.setProductName(productName);
        ps.setProductPublicId(productId);
        ps.setInventoryPublicId(inventoryId);
        ps.setRecordToday(today);
        ps.setAvailableStock(Math.max(availableStock,0L));
        ps.setReservedStock(Math.max(reservedStock, 0L));
        ps.setOnHoldStock(0L);
        ps.setQuarantinedStock(Math.max(quarantinedStock, 0L));
        ps.setTotalStock(Math.max(totalStock ,0L));
        ps.setClosingStock(Math.max(closingStock ,0L));
        ps.setWarehouse(warehouse);
        ps.setSellPrice(sellingPrice);
        ps.setProductSku(sku);
        ps.setOpeningStock(openingStock);
        return ps;
    }

    private long incrementOrZero(Long base, Long delta) {
        return Optional.ofNullable(base).orElse(0L) + Optional.ofNullable(delta).orElse(0L);
    }

    private long decreaseOrZero(Long base, Long delta) {
        return Optional.ofNullable(base).orElse(0L) - Optional.ofNullable(delta).orElse(0L);
    }

    public ProductStock createProductStockRecord(Product product, UUID inventoryId, Warehouse warehouse, LocalDate nairobiDate) {

        List<ProductStock> productStockList =
                productStockRepository.findAllByRecordTodayAndDeletedFalseAndProductPublicIdAndInventoryPublicId(nairobiDate.minusDays(1), product.getPublicId(), inventoryId);

        if (!productStockList.isEmpty()) {
            ProductStock item = productStockList.stream().findFirst().get();
            return createProductStock(
                    item.getProductName(),
                    item.getProductPublicId(),
                    item.getSellPrice(),
                    item.getProductSku(),
                    item.getInventoryPublicId(),
                    nairobiDate,
                    item.getWarehouse(),
                    item.getClosingStock(),
                    item.getAvailableStock(),
                    item.getReservedStock(),
                    item.getQuarantinedStock(),
                    item.getTotalStockValue()
            );
        } else {
            return createProductStock(
                    product.getProductName(),
                    product.getPublicId(),
                    product.getSellPrice(),
                    product.getSku(),
                    inventoryId,
                    nairobiDate,
                    warehouse,
                    0L,
                    0L,
                    0L,
                    0L,
                    0L
            );
        }
    }

    private ProductStock createProductStock(String productName, UUID productId, BigDecimal sellingPrice,
                                            @Unique String sku, UUID inventoryId, LocalDate nairobiDate,
                                            Warehouse warehouse, Long closingStock, Long availableStock,
                                            Long reservedStock, Long quarantinedStock, Long openingStock) {
        Optional<ProductStock> optionalProductStock = productStockRepository.findByProductPublicIdAndInventoryPublicIdAndRecordTodayAndWarehouseAndDeletedFalse(productId,
                inventoryId, nairobiDate, warehouse);

        Product product = productRepository.findByPublicIdAndDeletedIsFalse(productId).orElse(null);
        if (!optionalProductStock.isPresent()) {
            ProductStock ps = new ProductStock();
            ps.setProductName(productName);
            ps.setProductPublicId(productId);
            ps.setInventoryPublicId(inventoryId);
            ps.setRecordToday(nairobiDate);
            ps.setAvailableStock(availableStock == null ? 0L : availableStock);
            ps.setReservedStock(reservedStock == null ? 0L : reservedStock);
            ps.setOnHoldStock(0L);
            ps.setQuarantinedStock(quarantinedStock == null ? 0L : quarantinedStock);
            ps.setTotalStock(closingStock == null ? 0L : closingStock);
            ps.setClosingStock(closingStock == null ? 0L : closingStock);
            ps.setOpeningStock(openingStock);
            ps.setWarehouse(warehouse);
            ps.setSellPrice(sellingPrice);
            ps.setProductSku(sku);
            ps.setProduct(product);
            return productStockRepository.save(ps);
        } else {
            return optionalProductStock.get();
        }
    }
}
