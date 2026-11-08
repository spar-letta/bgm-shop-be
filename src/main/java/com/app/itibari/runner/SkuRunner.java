//package com.app.itibari.runner;
//
//import com.app.itibari.dataTypes.DiscountStatus;
//import com.app.itibari.dataTypes.DiscountType;
//import com.app.itibari.models.entities.Inventory;
//import com.app.itibari.models.entities.Product;
//import com.app.itibari.models.entities.Warehouse;
//import com.app.itibari.repository.*;
//import com.app.itibari.services.JobService;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.core.annotation.Order;
//import org.springframework.data.domain.Pageable;
//import org.springframework.stereotype.Component;
//
//import java.util.Optional;
//import java.util.UUID;
//
//
//@Order(6)
//@Component
//@Slf4j
//public class SkuRunner implements CommandLineRunner {
//    private final JobService jobService;
//    private final WarehouseRepository warehouseRepository;
//    private final InventoryRepository inventoryRepository;
//    private final InventoryProductsRepository inventoryProductsRepository;
//    private final ProductRepository productRepository;
//    private final DiscountRepository discountRepository;
//
//    public SkuRunner(JobService jobService, WarehouseRepository warehouseRepository, InventoryRepository inventoryRepository, InventoryProductsRepository inventoryProductsRepository, ProductRepository productRepository, DiscountRepository discountRepository) {
//        this.jobService = jobService;
//        this.warehouseRepository = warehouseRepository;
//        this.inventoryRepository = inventoryRepository;
//        this.inventoryProductsRepository = inventoryProductsRepository;
//        this.productRepository = productRepository;
//        this.discountRepository = discountRepository;
//    }
//
//    @Override
//    public void run(String... args) throws Exception {
//        log.info("Product SKU job started");
////        UUID publicId = UUID.fromString("7371cd7e-da28-4325-b331-e5787e211b14");
////        Optional<Warehouse> darNorth = warehouseRepository.findByPublicIdAndDeletedIsFalse(publicId);
////
////        if (darNorth.isPresent()){
////            Warehouse warehouse = darNorth.get();
////
////            Optional<Inventory> optionalInventory = inventoryRepository.findByWarehouseAndDeletedIsFalse(warehouse);
////
////            optionalInventory.ifPresent(inventory -> inventoryProductsRepository.findAllByInventoryAndDeletedIsFalse(inventory, Pageable.unpaged()).forEach(inventoryProduct -> {
////                if (inventoryProduct.getClosingStock() != null) {
////                    inventoryProduct.setAvailableStock(inventoryProduct.getClosingStock() - inventoryProduct.getReservedStock());
////                    inventoryProductsRepository.save(inventoryProduct);
////
////                    inventoryProduct.getProduct().setAvailableStock(inventoryProduct.getClosingStock()- inventoryProduct.getReservedStock());
////                    productRepository.save(inventoryProduct.getProduct());
////                }
////
////            }));
////        }
//
//
//
////        discountRepository.findAllByDiscountTypeAndStatusAndDeletedIsFalse(DiscountType.VolumeDiscount,DiscountStatus.Active)
////                .forEach(discount -> {
////                    UUID productId = discount.getProductPublicId();
////                    Optional<Product> productOptional = productRepository.findByPublicIdAndDeletedIsFalse(productId);
////                    productOptional.ifPresent(product -> {
////                        log.info("Product SKU found with product id: {}", product.getId());
////                        product.setDiscountMoq(discount.getLeastVolumeRangeMoq());
////                        productRepository.save(product);
////
////                        log.info("Product SKU found and updated with MOQ: {}", product.getDiscountMoq());
////                    });
////                });
////
////        log.info("Product SKU job ended");
//    }
//}
