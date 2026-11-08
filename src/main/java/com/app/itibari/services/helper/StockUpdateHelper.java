package com.app.itibari.services.helper;

import com.app.itibari.dataTypes.*;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.UpdateProductReconSoldProductRequest;
import com.app.itibari.repository.InventoryProductsRepository;
import com.app.itibari.repository.InventoryRepository;
import com.app.itibari.repository.ProductItemRepository;
import com.app.itibari.repository.ProductRepository;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.helpers.InventoryLogHelper;
import com.app.itibari.utils.helpers.ProductStockHelper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

@Component
@Slf4j
public class StockUpdateHelper {

    private final InventoryRepository inventoryRepository;

    private final InventoryProductsRepository inventoryProductsRepository;

    private final ProductRepository productRepository;

    private final ProductItemRepository productItemRepository;
    private final LiveStockHelper liveStockHelper;
    private final StockMovementHelper stockMovementHelper;

    private final ProductStockHelper productStockHelper;
    private final InventoryLogHelper inventoryLogHelper;

    public StockUpdateHelper(InventoryRepository inventoryRepository,
                             InventoryProductsRepository inventoryProductsRepository,
                             ProductRepository productRepository,
                             ProductItemRepository productItemRepository, LiveStockHelper liveStockHelper, StockMovementHelper stockMovementHelper, ProductStockHelper productStockHelper, InventoryLogHelper inventoryLogHelper) {

        this.inventoryRepository = inventoryRepository;
        this.inventoryProductsRepository = inventoryProductsRepository;
        this.productRepository = productRepository;
        this.productItemRepository = productItemRepository;
        this.liveStockHelper = liveStockHelper;
        this.stockMovementHelper = stockMovementHelper;
        this.productStockHelper = productStockHelper;
        this.inventoryLogHelper = inventoryLogHelper;
    }

    public void updateProductStockOnReceiveGoods(Map<Product, ProductItem> productItemMap, Warehouse warehouse) {

        Inventory inventory = this.getInventory(warehouse);

        productItemMap.forEach((product, productItem) -> {
            InventoryProducts inventoryProducts = this.getInventoryProduct(product, inventory);
            Long productAvailableStock = product.getAvailableStock() != null ? product.getAvailableStock() : 0;
            Long inventoryProductAvailableStock = inventoryProducts.getAvailableStock() != null ? inventoryProducts.getAvailableStock() : 0;
            Long receivedStock = productItem.getQuantityReceived();

            product.setAvailableStock(productAvailableStock + receivedStock);
            inventoryProducts.setAvailableStock(inventoryProductAvailableStock + receivedStock);

            // save them

            productRepository.save(product);
            inventoryProductsRepository.save(inventoryProducts);
        });
    }

    @Transactional
    public void updateProductStockOnReceiveShipment(Map<Product, ReceivedProductItem> productItemMap,
                                                    Warehouse warehouse, String actionType) {
        Inventory inventory = this.getInventory(warehouse);

        List<Product> updatedProducts = new ArrayList<>();
        List<InventoryProducts> updatedInventoryProducts = new ArrayList<>();

        productItemMap.forEach((product, productItem) -> {
            InventoryProducts inventoryProducts = this.getInventoryProduct(product, inventory);
            long receivedStock = productItem.getQuantityReceived();
            logStock(actionType, OpBeforeAfter.Before, product, inventoryProducts, Long.valueOf(receivedStock));

            product.setAvailableStock(Objects.requireNonNullElse(product.getAvailableStock(), 0L) + receivedStock);
            inventoryProducts.setAvailableStock(Objects.requireNonNullElse(inventoryProducts.getAvailableStock(), 0L) + receivedStock);

            updatedProducts.add(product);
            updatedInventoryProducts.add(inventoryProducts);
            logStock(actionType, OpBeforeAfter.After, product, inventoryProducts, Long.valueOf(receivedStock));
        });

        productRepository.saveAll(updatedProducts);
        List<InventoryProducts> inventoryProducts = inventoryProductsRepository.saveAll(updatedInventoryProducts);

    }


    public void updateProductStockReturnGoods(Map<Product, ProductItem> productItemMap, Warehouse warehouse) {

        Inventory inventory = this.getInventory(warehouse);

        productItemMap.forEach((product, productItem) -> {
            InventoryProducts inventoryProducts = this.getInventoryProduct(product, inventory);
            Long productAvailableStock = product.getAvailableStock() != null ? product.getAvailableStock() : 0;
            Long inventoryProductAvailableStock = inventoryProducts.getAvailableStock() != null ? inventoryProducts.getAvailableStock() : 0;
            Long onHoldStock = product.getOnHoldStock() != null ? product.getOnHoldStock() : 0;
            Long onHoldStockInventory = inventoryProducts.getOnHoldStock() != null ? inventoryProducts.getOnHoldStock() : 0;
            Long returnedStock = productItem.getQuantityReturned();

            product.setAvailableStock(productAvailableStock - returnedStock);
            // product.setOnHoldStock(onHoldStock + returnedStock);
            inventoryProducts.setAvailableStock(inventoryProductAvailableStock - returnedStock);
            // inventoryProducts.setOnHoldStock(onHoldStockInventory + returnedStock);

            // save them
            productRepository.save(product);
            inventoryProductsRepository.save(inventoryProducts);
        });
    }

    public void updateProductStockWithReturnGoods(Map<Product, ReturnedProductItem> productItemMap,
                                                  Warehouse warehouse, String actionType) {

        Inventory inventory = this.getInventory(warehouse);

        productItemMap.forEach((product, productItem) -> {
            InventoryProducts inventoryProducts = this.getInventoryProduct(product, inventory);
            Long productAvailableStock = product.getAvailableStock() != null ? product.getAvailableStock() : 0;
            Long inventoryProductAvailableStock = inventoryProducts.getAvailableStock() != null ? inventoryProducts.getAvailableStock() : 0;
            Long returnedStock = productItem.getQuantityReturned();
            logStock(actionType, OpBeforeAfter.Before, product, inventoryProducts, returnedStock);

            product.setAvailableStock(productAvailableStock - returnedStock);
            inventoryProducts.setAvailableStock(inventoryProductAvailableStock - returnedStock);

            // save them
            Product savedProduct = productRepository.save(product);
            InventoryProducts savedInventoryProducts = inventoryProductsRepository.save(inventoryProducts);

            logStock(actionType, OpBeforeAfter.After, savedProduct, savedInventoryProducts, returnedStock);
            productStockHelper.updateProductStock(product, warehouse, returnedStock,
                    StockMovementTransactionType.GRTN);
        });
    }


    public void updateProductAdjustments(Product product, Warehouse warehouse, AdjustmentType adjustmentType, int quantity, String doneBy) {

        Inventory inventory = this.getInventory(warehouse);
        InventoryProducts inventoryProducts = this.getInventoryProduct(product, inventory);

        long productAvailableStock = product.getAvailableStock() != null ? product.getAvailableStock() : 0;
        long inventoryProductAvailableStock = inventoryProducts.getAvailableStock() != null ? inventoryProducts.getAvailableStock() : 0;

        logStock(adjustmentType == AdjustmentType.Decrease ? InventoryEvent.ADJUSTMENT_DOWN.name() :
                        InventoryEvent.ADJUSTMENT_UP.name(),
                OpBeforeAfter.Before,
                product,
                inventoryProducts,
                Long.valueOf(quantity));

        if (adjustmentType == AdjustmentType.Decrease) {
            if (productAvailableStock - quantity < 0 || inventoryProductAvailableStock - quantity < 0) {
                throw new BadRequestRestApiException()
                        .developerMessage("Product stock is not enough to decrease")
                        .userMessage("Product stock is not enough to decrease in either product or inventory level");
            }
            product.setAvailableStock(productAvailableStock - quantity);
            inventoryProducts.setAvailableStock(inventoryProductAvailableStock - quantity);

            stockMovementHelper.recordStockMovement(product, quantity, StockMovementTransactionType.STOCK_ADJUSTMENT_DOWN, TransactionBoundType.OUTGOING, doneBy, warehouse);

            productStockHelper.updateProductStock(product, warehouse, (long) quantity,
                    StockMovementTransactionType.STOCK_ADJUSTMENT_DOWN);

        } else if (adjustmentType == AdjustmentType.Increase) {
            product.setAvailableStock(productAvailableStock + quantity);
            inventoryProducts.setAvailableStock(inventoryProductAvailableStock + quantity);
            stockMovementHelper.recordStockMovement(product, quantity, StockMovementTransactionType.STOCK_ADJUSTMENT_UP, TransactionBoundType.INCOMING, doneBy, warehouse);
            productStockHelper.updateProductStock(product, warehouse, (long) quantity,
                    StockMovementTransactionType.STOCK_ADJUSTMENT_UP);
        }

        Product savedProduct = productRepository.save(product);
        InventoryProducts savedInventoryProduct = inventoryProductsRepository.save(inventoryProducts);

        logStock(adjustmentType == AdjustmentType.Decrease ? InventoryEvent.ADJUSTMENT_DOWN.name() :
                        InventoryEvent.ADJUSTMENT_UP.name(),
                OpBeforeAfter.After,
                savedProduct,
                savedInventoryProduct,
                Long.valueOf(quantity));
    }

    public void updateSalesOrderBookedStock(Map<Product, Integer> reservedStockMap, Warehouse warehouse,
                                            String actionType) {   // 200 50   avail = 20, 150

        Inventory inventory = this.getInventory(warehouse);
        reservedStockMap.forEach((product, reservedStock) -> {
            InventoryProducts inventoryProducts = this.getInventoryProduct(product, inventory);

            Long productAvailableStock = product.getAvailableStock() != null ? product.getAvailableStock() : 0;
            Long inventoryProductAvailableStock = inventoryProducts.getAvailableStock() != null ? inventoryProducts.getAvailableStock() : 0;
            Long reservedStockInventory = inventoryProducts.getReservedStock() != null ? inventoryProducts.getReservedStock() : 0;
            Long productReservedStock = product.getReservedStock() != null ? product.getReservedStock() : 0;

            logStock(actionType, OpBeforeAfter.Before, product, inventoryProducts, Long.valueOf(reservedStock));

            if (reservedStock > productAvailableStock || reservedStock > inventoryProductAvailableStock) {
                throw new BadRequestRestApiException()
                        .developerMessage("Reserved stock is more than available stock")
                        .userMessage("Reserved stock is more than available stock in either product or inventory level");
            }

            product.setReservedStock(productReservedStock + reservedStock);
            inventoryProducts.setReservedStock(reservedStockInventory + reservedStock);

            product.setAvailableStock(productAvailableStock - reservedStock);
            inventoryProducts.setAvailableStock(inventoryProductAvailableStock - reservedStock);

            Product savedProduct = productRepository.save(product);
            InventoryProducts savedInventoryProduct = inventoryProductsRepository.save(inventoryProducts);

            productStockHelper.updateProductStock(product, warehouse, Long.valueOf(reservedStock), StockMovementTransactionType.TO_RESERVED);

            logStock(actionType, OpBeforeAfter.After, savedProduct, savedInventoryProduct, Long.valueOf(reservedStock));
        });
    }

    public void revertToPreviousState(Map<Product, Integer> reservedStockMap, Warehouse warehouse, String doneBy) {

        Inventory inventory = this.getInventory(warehouse);
        reservedStockMap.forEach((product, reservedStock) -> {
            InventoryProducts inventoryProducts = this.getInventoryProduct(product, inventory);

            Long productAvailableStock = product.getAvailableStock() != null ? product.getAvailableStock() : 0;
            Long inventoryProductAvailableStock = inventoryProducts.getAvailableStock() != null ? inventoryProducts.getAvailableStock() : 0;
            Long reservedStockInventory = inventoryProducts.getReservedStock() != null ? inventoryProducts.getReservedStock() : 0;
            Long productReservedStock = product.getReservedStock() != null ? product.getReservedStock() : 0;

            product.setReservedStock(productReservedStock - reservedStock);
            inventoryProducts.setReservedStock(reservedStockInventory - reservedStock);

            product.setAvailableStock(productAvailableStock + reservedStock);
            inventoryProducts.setAvailableStock(inventoryProductAvailableStock + reservedStock);

            Product saveProduct = productRepository.save(product);
            inventoryProductsRepository.save(inventoryProducts);

            productStockHelper.updateProductStock(saveProduct, warehouse, Long.valueOf(reservedStock), StockMovementTransactionType.REVERT_BEFORE_UPDATE);

            stockMovementHelper.recordStockMovement(product, reservedStock, StockMovementTransactionType.ORDER_EDIT, TransactionBoundType.INCOMING, doneBy, warehouse);
        });
    }

    public void checkIfProductIsAvailable(Product product, Integer quantity, Warehouse warehouse) {
        log.info("----product {}, {}, {}", product.getProductName(), quantity, warehouse.getId());

        Inventory inventory = this.getInventory(warehouse);
        InventoryProducts inventoryProducts = this.getInventoryProduct(product, inventory);

        log.info("----inventoryP {}, {}, {}, {}", product.getProductName(), quantity, warehouse.getId(),
                inventoryProducts.getAvailableStock());
        if (product.getAvailableStock() == null || inventoryProducts.getAvailableStock() == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product stock is not available")
                    .userMessage(String.format("Product %s is not available in stock", product.getProductName()));
        }

        if (product.getAvailableStock() == 0) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product stock is not available")
                    .userMessage(String.format("Product %s is not available in stock", product.getProductName()));
        }
        if (inventoryProducts.getAvailableStock() == 0) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product stock is not available")
                    .userMessage(String.format("Product %s is not available in stock", product.getProductName()));
        }

        if (quantity > product.getAvailableStock() || quantity > inventoryProducts.getAvailableStock()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product stock is not enough to decrease")
                    .userMessage(String.format("Product %s does not have enough stock. Available stock in product level is %d and inventory level is %d", product.getProductName(), product.getAvailableStock(), inventoryProducts.getAvailableStock()));
        }
    }

    public void updateCancelledSalesOrderBookedStock(Map<Product, Integer> reservedStockMap, Warehouse warehouse, String actionType) {

        Inventory inventory = this.getInventory(warehouse);
        reservedStockMap.forEach((product, reservedStock) -> {
            InventoryProducts inventoryProducts = this.getInventoryProduct(product, inventory);
            logStock(actionType, OpBeforeAfter.Before, product, inventoryProducts, Long.valueOf(reservedStock));

            Long productAvailableStock = product.getAvailableStock() != null ? product.getAvailableStock() : 0;
            Long inventoryProductAvailableStock = inventoryProducts.getAvailableStock() != null ? inventoryProducts.getAvailableStock() : 0;
            Long reservedStockInventory = inventoryProducts.getReservedStock() != null ? inventoryProducts.getReservedStock() : 0;
            Long productReservedStock = product.getReservedStock() != null ? product.getReservedStock() : 0;

            product.setReservedStock(productReservedStock - reservedStock);
            inventoryProducts.setReservedStock(reservedStockInventory - reservedStock);
            product.setAvailableStock(productAvailableStock + reservedStock);
            inventoryProducts.setAvailableStock(inventoryProductAvailableStock + reservedStock);

            Product saveProduct = productRepository.save(product);
            InventoryProducts saveInventoryProducts = inventoryProductsRepository.save(inventoryProducts);

            productStockHelper.updateProductStock(saveProduct, warehouse, Long.valueOf(reservedStock), StockMovementTransactionType.ORDER_CANCEL);
            logStock(actionType, OpBeforeAfter.After, saveProduct, saveInventoryProducts, Long.valueOf(reservedStock));
        });
    }

    public void updateStockReturned(Map<Product, Integer> returnedStock, Warehouse warehouse, String actionType) {

        Inventory inventory = this.getInventory(warehouse);
        returnedStock.forEach((product, quantity) -> {
            InventoryProducts inventoryProducts = this.getInventoryProduct(product, inventory);

            logStock(actionType, OpBeforeAfter.Before, product, inventoryProducts, Long.valueOf(quantity));

            Long productAvailableStock = product.getAvailableStock() != null ? product.getAvailableStock() : 0;
            Long inventoryProductAvailableStock = inventoryProducts.getAvailableStock() != null ? inventoryProducts.getAvailableStock() : 0;

            product.setAvailableStock(productAvailableStock + quantity);
            inventoryProducts.setAvailableStock(inventoryProductAvailableStock + quantity);

            // save them
            Product savedProduct = productRepository.save(product);
            InventoryProducts savedInventoryProducts = inventoryProductsRepository.save(inventoryProducts);
            logStock(actionType, OpBeforeAfter.After, savedProduct, savedInventoryProducts, Long.valueOf(quantity));
        });
    }

    public void updateQuarantineTransfer(Map<Product, Long> productItemMap,
                                         Warehouse warehouse,
                                         Warehouse quarantineWarehouse,
                                         TransferType transferType) {

        Inventory inventory = this.getInventory(warehouse);
        Inventory quarantineInventory = this.getInventory(quarantineWarehouse);

        productItemMap.forEach((product, quantity) -> {
            InventoryProducts inventoryProducts = this.getInventoryProduct(product, inventory);
            InventoryProducts quarantineInventoryProducts = this.getInventoryProduct(product, quarantineInventory);

            Long inventoryQuarantineStock = inventoryProducts.getQuarantinedStock() != null ? inventoryProducts.getQuarantinedStock() : 0;
            Long quarantineInventoryQuarantineStock = quarantineInventoryProducts.getQuarantinedStock() != null ? quarantineInventoryProducts.getQuarantinedStock() : 0;
            Long productAvailableStock = product.getAvailableStock() != null ? product.getAvailableStock() : 0;
            Long inventoryProductAvailableStock = inventoryProducts.getAvailableStock() != null ? inventoryProducts.getAvailableStock() : 0;

            if (transferType == TransferType.TO_QUARANTINE_WAREHOUSE) {
                if (inventoryQuarantineStock < quantity || quarantineInventoryQuarantineStock < quantity) {
                    throw new BadRequestRestApiException()
                            .developerMessage("Not enough stock in main warehouse to transfer")
                            .userMessage("Not enough stock in main warehouse to transfer");
                }
                product.setAvailableStock(productAvailableStock - quantity);
                inventoryProducts.setAvailableStock(inventoryProductAvailableStock - quantity);

                inventoryProducts.setQuarantinedStock(inventoryQuarantineStock + quantity);
                quarantineInventoryProducts.setQuarantinedStock(quarantineInventoryQuarantineStock + quantity);
            } else if (transferType == TransferType.FROM_QUARANTINE_WAREHOUSE) {

                if (quarantineInventoryQuarantineStock < quantity) {
                    throw new BadRequestRestApiException()
                            .developerMessage("Not enough stock in quarantine warehouse to transfer")
                            .userMessage("Not enough stock in quarantine warehouse to transfer");
                }
                product.setAvailableStock(productAvailableStock + quantity);
                inventoryProducts.setAvailableStock(inventoryProducts.getAvailableStock() + quantity);

                inventoryProducts.setQuarantinedStock(inventoryQuarantineStock - quantity);
                quarantineInventoryProducts.setQuarantinedStock(quarantineInventoryQuarantineStock - quantity);
            }

            // save them
            productRepository.save(product);
            inventoryProductsRepository.save(inventoryProducts);
            inventoryProductsRepository.save(quarantineInventoryProducts);
        });
    }

    public void updateBookedVanSales(Map<Product, Long> productIntegerMap, Warehouse warehouse) {

        Inventory inventory = this.getInventory(warehouse);
        productIntegerMap.forEach((product, quantity) -> {
            InventoryProducts inventoryProducts = this.getInventoryProduct(product, inventory);
            Long productAvailableStock = product.getAvailableStock() != null ? product.getAvailableStock() : 0;
            Long inventoryProductAvailableStock = inventoryProducts.getAvailableStock() != null ? inventoryProducts.getAvailableStock() : 0;
            Long productReservedStock = product.getReservedStock() != null ? product.getReservedStock() : 0;
            Long inventoryProductReservedStock = inventoryProducts.getReservedStock() != null ? inventoryProducts.getReservedStock() : 0;

            if (productAvailableStock < quantity || inventoryProductAvailableStock < quantity) {
                throw new BadRequestRestApiException()
                        .developerMessage("Not enough stock in main warehouse to book")
                        .userMessage("Not enough stock in main warehouse to book");
            }

            product.setAvailableStock(productAvailableStock - quantity);
            inventoryProducts.setAvailableStock(inventoryProductAvailableStock - quantity);

            product.setReservedStock(productReservedStock + quantity);
            inventoryProducts.setReservedStock(inventoryProductReservedStock + quantity);

            // save them
            productRepository.save(product);
            inventoryProductsRepository.save(inventoryProducts);
        });
    }

    public void UnBookVanSales(Map<Product, Long> returnedStock, Warehouse warehouse) {

        Inventory inventory = this.getInventory(warehouse);
        returnedStock.forEach((product, quantity) -> {
            InventoryProducts inventoryProducts = this.getInventoryProduct(product, inventory);
            Long productReservedStock = product.getReservedStock() != null ? product.getReservedStock() : 0;
            Long inventoryProductReservedStock = inventoryProducts.getReservedStock() != null ? inventoryProducts.getReservedStock() : 0;

            if (productReservedStock < quantity || inventoryProductReservedStock < quantity) {
                throw new BadRequestRestApiException()
                        .developerMessage("Not enough stock in main warehouse to unbook")
                        .userMessage("Not enough stock in main warehouse to unbook");
            }

            product.setReservedStock(productReservedStock - quantity);
            inventoryProducts.setReservedStock(inventoryProductReservedStock - quantity);

            // save them
            productRepository.save(product);
            inventoryProductsRepository.save(inventoryProducts);
        });
    }

    public void increamentQuarantine(Map<Product, Long> productLongMap, Warehouse warehouse, String actionType) {

        Inventory inventory = this.getInventory(warehouse);
        productLongMap.forEach((product, quantity) -> {
            InventoryProducts inventoryProducts = this.getInventoryProduct(product, inventory);
            logStock(actionType, OpBeforeAfter.Before, product, inventoryProducts, Long.valueOf(quantity));

            Long inventoryQuarantineStock = inventoryProducts.getQuarantinedStock() != null ? inventoryProducts.getQuarantinedStock() : 0;
            Long productQuarantinedStock = product.getQuarantineStock() != null ? product.getQuarantineStock() : 0;
            Long productAvailableStock = product.getAvailableStock() != null ? product.getAvailableStock() : 0;
            Long inventoryProductAvailableStock = inventoryProducts.getAvailableStock() != null ? inventoryProducts.getAvailableStock() : 0;

            inventoryProducts.setQuarantinedStock(inventoryQuarantineStock + quantity);
            product.setQuarantineStock(productQuarantinedStock + quantity);

            product.setAvailableStock(productAvailableStock - quantity);
            inventoryProducts.setAvailableStock(inventoryProductAvailableStock - quantity);


            // save them
            Product savedProduct = productRepository.save(product);
            InventoryProducts savedInventoryProducts = inventoryProductsRepository.save(inventoryProducts);

            logStock(actionType, OpBeforeAfter.After, savedProduct, savedInventoryProducts, Long.valueOf(quantity));
        });
    }

    public void updatePickedIssuedStock(Map<Product, Long> productLongMap, Warehouse warehouse, String actionType) {

        Inventory inventory = this.getInventory(warehouse);
        productLongMap.forEach((product, quantity) -> {
            InventoryProducts inventoryProducts = this.getInventoryProduct(product, inventory);
            Long productReservedStock = product.getReservedStock() != null ? product.getReservedStock() : 0;
            Long inventoryProductReservedStock = inventoryProducts.getReservedStock() != null ? inventoryProducts.getReservedStock() : 0;
            log.info("-------check one--product {}, {}, {}, {}", product.getProductName(), product.getPublicId(), product.getSku(), quantity);
            logStock(actionType, OpBeforeAfter.Before, product, inventoryProducts, Long.valueOf(quantity));
            log.info("-------product {}, {}, {}, {}", product.getProductName(), product.getPublicId(), product.getSku(), quantity);
            if (productReservedStock < quantity || inventoryProductReservedStock < quantity) {
                throw new BadRequestRestApiException()
                        .developerMessage("Not enough stock in main warehouse to issue")
                        .userMessage("Not enough stock in main warehouse to issue");
            }

            product.setReservedStock(productReservedStock - quantity);
            inventoryProducts.setReservedStock(inventoryProductReservedStock - quantity);

            // save them
            Product savedProduct = productRepository.save(product);
            InventoryProducts savedInventoryProducts = inventoryProductsRepository.save(inventoryProducts);

            logStock(actionType, OpBeforeAfter.After, savedProduct, savedInventoryProducts, Long.valueOf(quantity));
        });
    }

    public void updateProductStockOnReceiveGoodsChunk(Map<Product, Long> productLongMap, Warehouse warehouse) {

        Inventory inventory = this.getInventory(warehouse);
        productLongMap.forEach((product, quantity) -> {
            InventoryProducts inventoryProducts = this.getInventoryProduct(product, inventory);
            Long productAvailableStock = product.getAvailableStock() != null ? product.getAvailableStock() : 0;
            Long inventoryProductAvailableStock = inventoryProducts.getAvailableStock() != null ? inventoryProducts.getAvailableStock() : 0;

            product.setAvailableStock(productAvailableStock + quantity);
            inventoryProducts.setAvailableStock(inventoryProductAvailableStock + quantity);

            // save them
            productRepository.save(product);
            inventoryProductsRepository.save(inventoryProducts);
        });
    }

    public void updateStockTransfer(Product product,
                                    ProductItem productItem,
                                    UpdateProductReconSoldProductRequest updateProductReconSoldProductRequest,
                                    Person person,
                                    Warehouse warehouse) {

        Inventory inventory = this.getInventory(warehouse);
        InventoryProducts inventoryProducts = this.getInventoryProduct(product, inventory);

        long productAvailableStock = product.getAvailableStock() != null ? product.getAvailableStock() : 0;
        long inventoryProductAvailableStock = inventoryProducts.getAvailableStock() != null ? inventoryProducts.getAvailableStock() : 0;

        if (updateProductReconSoldProductRequest.getIncrementType() == IncrementType.Increment) {

            if (updateProductReconSoldProductRequest.getProductColumn() == ProductColumn.Returned) {
                product.setAvailableStock(productAvailableStock + updateProductReconSoldProductRequest.getQuantity());
                inventoryProducts.setAvailableStock(inventoryProductAvailableStock + updateProductReconSoldProductRequest.getQuantity());
                productItem.setQuantityReturned(productItem.getQuantityReturned() + updateProductReconSoldProductRequest.getQuantity());
            } else if (updateProductReconSoldProductRequest.getProductColumn() == ProductColumn.Missing) {
                productItem.setQuantityMissing(productItem.getQuantityMissing() + updateProductReconSoldProductRequest.getQuantity());
            } else if (updateProductReconSoldProductRequest.getProductColumn() == ProductColumn.Sold) {
                productItem.setQuantitySold(productItem.getQuantitySold() + updateProductReconSoldProductRequest.getQuantity());
            }
        } else if (updateProductReconSoldProductRequest.getIncrementType() == IncrementType.Decrement) {
            if (updateProductReconSoldProductRequest.getProductColumn() == ProductColumn.Returned) {
                if (productItem.getQuantityReturned() - updateProductReconSoldProductRequest.getQuantity() < 0) {
                    throw new BadRequestRestApiException()
                            .developerMessage("Returned quantity cannot be less than zero")
                            .userMessage("Returned quantity cannot be less than zero");
                }

                if (productAvailableStock < updateProductReconSoldProductRequest.getQuantity() || inventoryProductAvailableStock < updateProductReconSoldProductRequest.getQuantity()) {
                    throw new BadRequestRestApiException()
                            .developerMessage("Not enough stock in main warehouse to return")
                            .userMessage("Not enough stock in main warehouse to return");
                }
                product.setAvailableStock(productAvailableStock - updateProductReconSoldProductRequest.getQuantity());
                inventoryProducts.setAvailableStock(inventoryProductAvailableStock - updateProductReconSoldProductRequest.getQuantity());
                productItem.setQuantityReturned(productItem.getQuantityReturned() - updateProductReconSoldProductRequest.getQuantity());
            } else if (updateProductReconSoldProductRequest.getProductColumn() == ProductColumn.Missing) {
                if (productItem.getQuantityMissing() - updateProductReconSoldProductRequest.getQuantity() < 0) {
                    throw new BadRequestRestApiException()
                            .developerMessage("Missing quantity cannot be less than zero")
                            .userMessage("Missing quantity cannot be less than zero");
                }
                productItem.setQuantityMissing(productItem.getQuantityMissing() - updateProductReconSoldProductRequest.getQuantity());
            } else if (updateProductReconSoldProductRequest.getProductColumn() == ProductColumn.Sold) {
                if (productItem.getQuantitySold() - updateProductReconSoldProductRequest.getQuantity() < 0) {
                    throw new BadRequestRestApiException()
                            .developerMessage("Sold quantity cannot be less than zero")
                            .userMessage("Sold quantity cannot be less than zero");
                }
                productItem.setQuantitySold(productItem.getQuantitySold() - updateProductReconSoldProductRequest.getQuantity());
            }
        }

        productItem.setUpdatedBy(person);
        productItem.setUpdateDate(LocalDateTime.now());

        // save them

        productRepository.save(product);
        inventoryProductsRepository.save(inventoryProducts);
        productItemRepository.save(productItem);
    }

    /**
     * private methods
     */
    private Inventory getInventory(Warehouse warehouse) {
        Optional<Inventory> optionalInventory = inventoryRepository.findByWarehouseAndDeletedIsFalse(warehouse);
        if (optionalInventory.isPresent()) {
            return optionalInventory.get();
        }
        Inventory inventory1 = new Inventory();
        inventory1.setWarehouse(warehouse);
        inventory1.setMainInventory(warehouse.isSuperWarehouse());
        return inventoryRepository.save(inventory1);
    }

    private InventoryProducts getInventoryProduct(Product product, Inventory inventory) {

        Optional<InventoryProducts> optionalInventoryProduct = inventoryProductsRepository.findByInventoryAndProductAndDeletedIsFalse(
                inventory,
                product);

        if (optionalInventoryProduct.isPresent()) {
            return optionalInventoryProduct.get();
        }
        InventoryProducts inventoryProducts = new InventoryProducts();
        inventoryProducts.setProduct(product);
        inventoryProducts.setInventory(inventory);
        return inventoryProductsRepository.save(inventoryProducts);
    }

    public void checkIfProductIsAvailableForUpdate(Product product, Integer quantity, Integer prev, Warehouse warehouse) {

        Inventory inventory = this.getInventory(warehouse);

        InventoryProducts inventoryProducts = this.getInventoryProduct(product, inventory);


        if (product.getAvailableStock() == null || inventoryProducts.getAvailableStock() == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product stock is not available")
                    .userMessage(String.format("Product %s is not available in stock", product.getProductName()));
        }

        if (product.getAvailableStock() == 0) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product stock is not available")
                    .userMessage(String.format("Product %s is not available in stock", product.getProductName()));
        }

        if (inventoryProducts.getAvailableStock() == 0) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product stock is not available")
                    .userMessage(String.format("Product stock is not available in inventory level for product %s", product.getProductName()));
        }

        if (quantity > (product.getAvailableStock() + prev) || quantity > (inventoryProducts.getAvailableStock() + prev)) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product stock is not enough to decrease")
                    .userMessage(String.format("Product %s is not available in stock", product.getProductName()));
        }
    }

    public void logStock(String actionType, OpBeforeAfter state,
                         Product product, InventoryProducts inventoryProducts,
                         Long quantityStock) {
        inventoryLogHelper.createLog(actionType, state,
                product.getPublicId(),
                quantityStock,
                safeStock(product.getAvailableStock()),
                product.getReservedStock(),
                safeStock(inventoryProducts.getAvailableStock()),
                inventoryProducts.getReservedStock());
    }

    private Long safeStock(Long stock) {
        return stock != null ? stock : 0L;
    }
}
