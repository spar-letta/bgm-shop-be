package com.app.itibari.services.helper;

import com.app.itibari.dataTypes.PromoType;
import com.app.itibari.dataTypes.TransactionType;
import com.app.itibari.models.entities.*;
import com.app.itibari.repository.AvcoRepository;
import com.app.itibari.repository.InventoryProductsRepository;
import com.app.itibari.repository.InventoryRepository;
import com.app.itibari.repository.ProductRepository;
import com.app.itibari.repository.WarehouseRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Component
public class AvcoHelper {

    private final AvcoRepository avcoRepository;

    private final InventoryRepository inventoryRepository;

    private final WarehouseRepository warehouseRepository;

    private final InventoryProductsRepository inventoryProductsRepository;

    private final ProductRepository productRepository;

    public AvcoHelper(AvcoRepository avcoRepository,
                      InventoryRepository inventoryRepository,
                      WarehouseRepository warehouseRepository,
                      InventoryProductsRepository inventoryProductsRepository,
                      ProductRepository productRepository) {

        this.avcoRepository = avcoRepository;
        this.inventoryRepository = inventoryRepository;
        this.warehouseRepository = warehouseRepository;
        this.inventoryProductsRepository = inventoryProductsRepository;
        this.productRepository = productRepository;
    }

    public void createAvcoItems(List<ReceivedProductItem> productItems, PurchaseOrder purchaseOrder, PromoType promoType) {

        if (promoType == PromoType.Promo || promoType == PromoType.Bonus) {
            productItems.forEach(productItem -> {

                Warehouse warehouse = warehouseRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(purchaseOrder.getWarehouseId()))
                        .orElseThrow(() -> new NoSuchElementException("Warehouse not found"));
                // get current avco
                Optional<Inventory> optionalInventory = inventoryRepository.findByWarehouseAndDeletedIsFalse(warehouse);
                if (optionalInventory.isEmpty()) {
                    throw new NoSuchElementException("Inventory not found");
                }
                Inventory inventory = optionalInventory.get();
                Product product = productRepository.findByPublicId(productItem.getProductPublicId());

                Optional<InventoryProducts> optionalInventoryProduct = inventoryProductsRepository.findByInventoryAndProductAndDeletedIsFalse(
                        inventory,
                        product);
                if (optionalInventoryProduct.isPresent()) {
                    InventoryProducts inventoryProducts = optionalInventoryProduct.get();

                    AvcoItems avcoItems = new AvcoItems();
                    avcoItems.setProductName(product.getProductName());
                    avcoItems.setProductSku(product.getSku());
                    avcoItems.setProductPublicId(product.getPublicId().toString());
                    avcoItems.setTransactionType(promoType == PromoType.Promo ? TransactionType.PROMOTION : TransactionType.REPLACEMENT);
                    avcoItems.setPurchaseOrderNumber(purchaseOrder.getPurchaseOrderNumber());
                    avcoItems.setQuantity(productItem.getQuantityReceived());
                    avcoItems.setUnitCost(BigDecimal.ZERO);
                    avcoItems.setValue(BigDecimal.ZERO);
                    avcoItems.setAvco(calculateAvco(productItem.getQuantityReceived(), inventoryProducts));
                    avcoItems.setTransactionDate(LocalDate.now());

                    avcoRepository.save(avcoItems);
                }
            });
        }
    }

    private BigDecimal calculateAvco(long quantityReceived, InventoryProducts inventoryProducts) {

        long quantityOnHand = inventoryProducts.getAvailableStock() + quantityReceived;
        BigDecimal value = inventoryProducts.getProduct().getBuyingPrice().multiply(inventoryProducts.getInventory().getTotalValue());
        BigDecimal avco = BigDecimal.ZERO;
        if (quantityOnHand > 0) {
            avco = BigDecimal.valueOf(quantityOnHand).divide(value, 2, RoundingMode.HALF_UP);
        }
        return avco;
    }

    public void createAvcoItemspurchase(PurchaseOrder purchaseOrder, ProductItem productItem) {

        Warehouse warehouse = warehouseRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(purchaseOrder.getWarehouseId()))
                .orElseThrow(() -> new NoSuchElementException("Warehouse not found"));
        // get current avco
        Optional<Inventory> optionalInventory = inventoryRepository.findByWarehouseAndDeletedIsFalse(warehouse);
        if (optionalInventory.isEmpty()) {
            throw new NoSuchElementException("Inventory not found");
        }
        Inventory inventory = optionalInventory.get();
        Product product = productRepository.findByPublicId(productItem.getProductPublicId());

        Optional<InventoryProducts> optionalInventoryProduct = inventoryProductsRepository.findByInventoryAndProductAndDeletedIsFalse(
                inventory,
                product);
        if (optionalInventoryProduct.isPresent()) {
            InventoryProducts inventoryProducts = optionalInventoryProduct.get();

            AvcoItems avcoItems = new AvcoItems();
            avcoItems.setProductName(product.getProductName());
            avcoItems.setProductSku(product.getSku());
            avcoItems.setProductPublicId(product.getPublicId().toString());
            avcoItems.setTransactionType(TransactionType.PURCHASE);
            avcoItems.setPurchaseOrderNumber(purchaseOrder.getPurchaseOrderNumber());
            avcoItems.setQuantity(productItem.getQuantityReceived());
            avcoItems.setUnitCost(product.getBuyingPrice());
            avcoItems.setValue(product.getBuyingPrice().multiply(BigDecimal.valueOf(productItem.getQuantityReceived())));
            avcoItems.setAvco(calculateAvco1(inventoryProducts));
            avcoItems.setTransactionDate(LocalDate.now());
            avcoItems.setCreationTime(LocalDateTime.now());

            avcoRepository.save(avcoItems);
        }
    }

    private BigDecimal calculateAvco1(InventoryProducts inventoryProducts) {

        long quantityOnHand = inventoryProducts.getAvailableStock();
        BigDecimal value = inventoryProducts.getProduct().getBuyingPrice().multiply(BigDecimal.valueOf(quantityOnHand));
        BigDecimal avco = BigDecimal.ZERO;
        if (quantityOnHand > 0) {
            avco = BigDecimal.valueOf(quantityOnHand).divide(value, 2, RoundingMode.HALF_UP);
        }
        return avco;
    }
}
