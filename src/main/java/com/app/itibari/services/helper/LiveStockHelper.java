package com.app.itibari.services.helper;

import com.app.itibari.dataTypes.ProductUpdateType;
import com.app.itibari.models.entities.*;
import com.app.itibari.repository.*;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.helpers.WebhookClient;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class LiveStockHelper {

    private final InventoryRepository inventoryRepository;

    private final WarehouseRepository warehouseRepository;

    private final InventoryProductsRepository inventoryProductsRepository;

    private final ProductRepository productRepository;

    private final LiveStockEntityRepository liveStockEntityRepository;
    private final LogisticsHelper logisticsHelper;

    private final WebhookClient webhookClient;
    private final static String webhookUrl = "https://chat.googleapis.com/v1/spaces/AAQAWYJGAwc/messages?key" +
            "=AIzaSyDdI0hCZtE6vySjMm-WEfRq3CPzqKqqsHI&token=d4hFr7EOaHubFEVISlE1KsRkoPGyZIqaI4-oUwOLpg8";

    public LiveStockHelper(InventoryRepository inventoryRepository,
                           WarehouseRepository warehouseRepository,
                           InventoryProductsRepository inventoryProductsRepository,
                           ProductRepository productRepository,
                           LiveStockEntityRepository liveStockEntityRepository, LogisticsHelper logisticsHelper, WebhookClient webhookClient) {

        this.inventoryRepository = inventoryRepository;
        this.warehouseRepository = warehouseRepository;
        this.inventoryProductsRepository = inventoryProductsRepository;
        this.productRepository = productRepository;
        this.liveStockEntityRepository = liveStockEntityRepository;
        this.logisticsHelper = logisticsHelper;
        this.webhookClient = webhookClient;
    }

    @Transactional
    public void updateLiveStockValue(UUID warehousePublicId,
                                     UUID productPublicId,
                                     Integer quantity,
                                     ProductUpdateType productType,
                                     LocalDate date) {
        Warehouse warehouse = warehouseRepository.findByPublicIdAndDeletedIsFalse(warehousePublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Warehouse not found")
                        .userMessage("The specified warehouse does not exist or has been deleted."));

        Inventory inventory = inventoryRepository.findByWarehouseAndDeletedIsFalse(warehouse)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Inventory not found")
                        .userMessage("The inventory for the specified warehouse does not exist or has been deleted."));

        Product product = productRepository.findByPublicIdAndDeletedIsFalse(productPublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Product not found")
                        .userMessage("The specified product does not exist or has been deleted."));

        log.info("==========Updating live stock for warehouse {} and product {}, {},{} with quantity {} and type {}.===========", warehousePublicId, productPublicId,product.getProductName(),product.getSku(), quantity, productType);

        Optional<InventoryProducts> inventoryProductsOptional = inventoryProductsRepository.findByInventoryAndProductAndDeletedIsFalse(inventory, product);

        if(inventoryProductsOptional.isEmpty()) {
            log.info("==========Inventory product association not found for warehouse {} and product {}, {},{}. Skipping live stock update.===========", warehousePublicId, productPublicId,product.getProductName(),product.getSku());
            throw  new BadRequestRestApiException()
                    .developerMessage("Inventory Product not found")
                    .userMessage("The specified product is not associated with the inventory of the warehouse.");
        }

        InventoryProducts inventoryProduct = inventoryProductsOptional.get();

        LiveStockEntity liveStockEntity = findLatestLiveStock(
                inventoryProduct.getPublicId().toString(),
                inventory.getPublicId().toString(),
                warehouse.getPublicId().toString(),
                date)
                .orElseGet(() -> createNewLiveStockEntity(inventoryProduct, inventory, warehouse, product, date));

        switch (productType) {
            case ISSUED_STOCK -> liveStockEntity.setIssuedStock(increment(liveStockEntity.getIssuedStock(), quantity));
            case GOODS_RECEIVED_STOCK ->
                    liveStockEntity.setGoodsReceivedStock(increment(liveStockEntity.getGoodsReceivedStock(), quantity));
            case GOODS_RETURNED_STOCK ->
                    liveStockEntity.setGoodsReturnedStock(increment(liveStockEntity.getGoodsReturnedStock(), quantity));
            case REPLACED_STOCK ->
                    liveStockEntity.setReplacedStock(increment(liveStockEntity.getReplacedStock(), quantity));
            case RETURNED_FROM_TRADE_STOCK ->
                    liveStockEntity.setReturnedFromTradeStock(increment(liveStockEntity.getReturnedFromTradeStock(), quantity));
            case SOLD_STOCK -> liveStockEntity.setSoldStock(increment(liveStockEntity.getSoldStock(), quantity));
            case SOLD_STOCK_MINUS_AMENDED -> liveStockEntity.setSoldStock(decrement(liveStockEntity.getSoldStock(), quantity)); // we need a column
            case RETURNED_FROM_QUARANTINE_STOCK ->
                    liveStockEntity.setReturnedFromQuarantineStock(increment(liveStockEntity.getReturnedFromQuarantineStock(), quantity));
            case QUARANTINE_STOCK ->
                    liveStockEntity.setQuarantineStock(increment(liveStockEntity.getQuarantineStock(), quantity));
            case QUARANTINE_STOCK_DISPOSED ->
                    liveStockEntity.setQuarantineStock(decrement(liveStockEntity.getQuarantineStock(), quantity));

            case CUSTOMER_RETURN ->
                    liveStockEntity.setCustomerReturnedStock(increment(liveStockEntity.getCustomerReturnedStock(), quantity));

            case STOCK_ADJUSTMENT ->
                    liveStockEntity.setAdjustedStock(increment(liveStockEntity.getAdjustedStock(), quantity));
            case STOCK_ADJUSTMENT_DECREASE ->
                    liveStockEntity.setNegativeAdjustedStock(increment(liveStockEntity.getNegativeAdjustedStock(), quantity));
            case BONUS_STOCK -> liveStockEntity.setBonusStock(increment(liveStockEntity.getBonusStock(), quantity));
            case REBATE_STOCK -> liveStockEntity.setRebateStock(increment(liveStockEntity.getRebateStock(), quantity));

            case OPENING_STOCK -> liveStockEntity.setOpeningStock(resolveOpeningStock(inventoryProduct, date));
            case DISPATCH_PURCHASE_ORDER -> liveStockEntity.setIncomingStock(increment(liveStockEntity.getIncomingStock(), quantity));
            case RECEIVE_PURCHASE_ORDER -> liveStockEntity.setIncomingStock(decrement(liveStockEntity.getIncomingStock(), quantity));
        }

        liveStockEntity.setClosingStock(liveStockEntity.getTotalStock());
        liveStockEntity.setCreationTime(LocalDateTime.now());
        LiveStockEntity saveLiveStockEntity = liveStockEntityRepository.save(liveStockEntity);
        if (productType != ProductUpdateType.OPENING_STOCK) {
            sendMessageOut(inventoryProduct, saveLiveStockEntity.getOpeningStock());
        }
    }

    @Transactional
    public void updateLiveStockValueManualRun(UUID warehousePublicId,
                                              UUID productPublicId,
                                              LocalDate date) {
        Warehouse warehouse = warehouseRepository.findByPublicIdAndDeletedIsFalse(warehousePublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Warehouse not found")
                        .userMessage("The specified warehouse does not exist or has been deleted."));

        Inventory inventory = inventoryRepository.findByWarehouseAndDeletedIsFalse(warehouse)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Inventory not found")
                        .userMessage("The inventory for the specified warehouse does not exist or has been deleted."));

        Product product = productRepository.findByPublicIdAndDeletedIsFalse(productPublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Product not found")
                        .userMessage("The specified product does not exist or has been deleted."));

        InventoryProducts inventoryProduct = inventoryProductsRepository.findByInventoryAndProductAndDeletedIsFalse(inventory, product)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Inventory Product not found")
                        .userMessage("The specified product is not associated with the inventory of the warehouse."));

        LiveStockEntity liveStockEntity = findLatestLiveStock(
                inventoryProduct.getPublicId().toString(),
                inventory.getPublicId().toString(),
                warehouse.getPublicId().toString(),
                date)
                .orElseGet(() -> createNewLiveStockEntity(inventoryProduct, inventory, warehouse, product, date));

        liveStockEntity.setOpeningStock(Math.toIntExact(inventoryProduct.getClosingStock()
                == null ? 0 : inventoryProduct.getClosingStock()));

        liveStockEntity.setClosingStock(liveStockEntity.getTotalStock());
        liveStockEntity.setCreationTime(LocalDateTime.now());
        liveStockEntityRepository.save(liveStockEntity);
    }

    /**
     * Creates today's live-stock row only if it is missing. Copies yesterday's
     * closing/incoming stock when present; otherwise opens from inventory closing.
     *
     * @return true if a row was created
     */
    @Transactional
    public boolean ensureTodayLiveStockRecord(Warehouse warehouse,
                                             Inventory inventory,
                                             InventoryProducts inventoryProduct,
                                             Product product,
                                             LocalDate date) {
        if (findLatestLiveStock(
                inventoryProduct.getPublicId().toString(),
                inventory.getPublicId().toString(),
                warehouse.getPublicId().toString(),
                date).isPresent()) {
            return false;
        }

        Optional<LiveStockEntity> previous = findLatestLiveStock(
                inventoryProduct.getPublicId().toString(),
                inventory.getPublicId().toString(),
                warehouse.getPublicId().toString(),
                date.minusDays(1));

        LiveStockEntity entity = new LiveStockEntity();
        entity.setLiveDate(date);
        entity.setInventoryItemPublicId(inventoryProduct.getPublicId().toString());
        entity.setInventoryPublicId(inventory.getPublicId().toString());
        entity.setWarehousePublicId(warehouse.getPublicId().toString());
        entity.setWarehouseName(warehouse.getWarehouseName());
        entity.setProductName(product.getProductName());
        entity.setProductCode(product.getSku());
        entity.setCreationTime(LocalDateTime.now());

        if (previous.isPresent()) {
            LiveStockEntity yesterday = previous.get();
            entity.setOpeningStock(yesterday.getClosingStock() == null ? 0 : yesterday.getClosingStock());
            entity.setIncomingStock(yesterday.getIncomingStock() == null ? 0 : yesterday.getIncomingStock());
            log.info("STOCK-BACKFILL live stock for sku={} warehouse={} copied from {} closing={}",
                    product.getSku(), warehouse.getWarehouseName(), date.minusDays(1), entity.getOpeningStock());
        } else {
            long closing = inventoryProduct.getClosingStock() == null ? 0L : inventoryProduct.getClosingStock();
            entity.setOpeningStock((int) closing);
            entity.setIncomingStock(0);
            log.info("STOCK-BACKFILL live stock for sku={} warehouse={} created from inventory closing={}",
                    product.getSku(), warehouse.getWarehouseName(), entity.getOpeningStock());
        }

        entity.setClosingStock(entity.getTotalStock());
        liveStockEntityRepository.save(entity);
        return true;
    }

    private void sendMessageOut(InventoryProducts inventoryProduct, long openingStock) {
        String payload = String.format(
                "{ \"text\": \"Live static stock update for %s: opening stock = %d\" }",
                inventoryProduct.getProduct().getProductName(),
                openingStock
        );

        webhookClient.sendMessage(webhookUrl, payload);
    }

    private int resolveOpeningStock(InventoryProducts inventoryProduct, LocalDate date) {
        LocalDate previousDate = date.minusDays(1);
        List<LiveStockEntity> previous = liveStockEntityRepository
                .findLatestLiveStockRowsByInventoryItemAndDate(
                        inventoryProduct.getPublicId().toString(),
                        previousDate);
        if (previous.size() > 1) {
            log.warn("Duplicate live stock rows ({}) for inventoryItem={} date={}. Using latest id={}",
                    previous.size(), inventoryProduct.getPublicId(), previousDate, previous.get(0).getId());
        }
        if (!previous.isEmpty()) {
            Integer closingStock = previous.get(0).getClosingStock();
            return closingStock == null ? 0 : closingStock;
        }
        return inventoryProduct.getClosingStock() == null
                ? 0
                : inventoryProduct.getClosingStock().intValue();
    }


    private int calculateOpeningStock(LiveStockEntity y) {
        int opening = safeInt(y.getOpeningStock());
        int issued = safeInt(y.getIssuedStock());
        int grtn = safeInt(y.getGoodsReturnedStock());

        int adjustments = safeInt(y.getAdjustedStock())
                + safeInt(y.getGoodsReceivedStock())
                + safeInt(y.getQuarantineStock())
                + safeInt(y.getReplacedStock())
                + safeInt(y.getNegativeAdjustedStock());

        return (opening - issued - grtn) + adjustments;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }


    private int increment(Integer current, int quantity) {
        return (current == null ? 0 : current) + quantity;
    }

    private int decrement(Integer current, int quantity) {
        return (current == null ? 0 : current) - quantity;
    }

    private LiveStockEntity createNewLiveStockEntity(InventoryProducts inventoryProduct,
                                                     Inventory inventory,
                                                     Warehouse warehouse,
                                                     Product product,
                                                     LocalDate date) {

        Optional<LiveStockEntity> previousRecord = findLatestLiveStock(
                inventoryProduct.getPublicId().toString(),
                inventory.getPublicId().toString(),
                warehouse.getPublicId().toString(),
                date.minusDays(1));

        LiveStockEntity entity = new LiveStockEntity();
        entity.setLiveDate(date);
        entity.setInventoryItemPublicId(inventoryProduct.getPublicId().toString());
        entity.setInventoryPublicId(inventory.getPublicId().toString());
        entity.setWarehousePublicId(warehouse.getPublicId().toString());
        entity.setWarehouseName(warehouse.getWarehouseName());
        entity.setProductName(product.getProductName());
        entity.setProductCode(product.getSku());
        entity.setCreationTime(LocalDateTime.now());
        entity.setIncomingStock(
                previousRecord
                        .map(r -> r.getIncomingStock() == null ? 0 : r.getIncomingStock())
                        .orElse(0)
        );
        LiveStockEntity savedLiveStockEntity = liveStockEntityRepository.save(entity);
        return savedLiveStockEntity;
    }

    private Optional<LiveStockEntity> findLatestLiveStock(String inventoryItemPublicId,
                                                          String inventoryPublicId,
                                                          String warehousePublicId,
                                                          LocalDate date) {
        List<LiveStockEntity> rows = liveStockEntityRepository.findLatestLiveStockRows(
                inventoryItemPublicId, inventoryPublicId, warehousePublicId, date);
        if (rows.size() > 1) {
            log.warn("Duplicate live stock rows ({}) for inventoryItem={} warehouse={} date={}. Using latest id={}",
                    rows.size(), inventoryItemPublicId, warehousePublicId, date, rows.get(0).getId());
        }
        return rows.stream().findFirst();
    }

    private int calculateClosingStock(LiveStockEntity entity) {
        int opening = Optional.ofNullable(entity.getOpeningStock()).orElse(0);
        int received = Optional.ofNullable(entity.getGoodsReceivedStock()).orElse(0);
        int issued = Optional.ofNullable(entity.getIssuedStock()).orElse(0);
        int returned = Optional.ofNullable(entity.getGoodsReturnedStock()).orElse(0);
        int adjusted = Optional.ofNullable(entity.getAdjustedStock()).orElse(0);
        int negativeAdjusted = Optional.ofNullable(entity.getNegativeAdjustedStock()).orElse(0);

        return opening + received + returned + adjusted - issued - negativeAdjusted;
    }

// safeValue(goodsReceivedStock) +
//                        safeValue(replacedStock) +
//                        safeValue(returnedFromTradeStock) +
//                        safeValue(getExpectedAfterIssuedStock()) +
//                        safeValue(adjustedStock)) -
//                (safeValue(goodsReturnedStock) + safeValue(negativeAdjustedStock)

}
