//package com.app.itibari.runner;
//
//import com.app.itibari.dataTypes.ReconStatus;
//import com.app.itibari.models.entities.ProductItem;
//import com.app.itibari.models.entities.ProductRecon;
//import com.app.itibari.models.entities.Reconciliation;
//import com.app.itibari.models.entities.RoutePlanning;
//import com.app.itibari.models.entities.SalesOrderItems;
//import com.app.itibari.repository.ProductItemRepository;
//import com.app.itibari.repository.ProductReconRepository;
//import com.app.itibari.repository.ReconciliationRepository;
//import com.app.itibari.repository.RoutePlanningRepository;
//import jakarta.transaction.Transactional;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.util.LinkedHashMap;
//import java.util.UUID;
//import java.util.Map;
//import java.util.Optional;
//
//@Slf4j
//@Component
//public class ProductReconRunner implements CommandLineRunner {
//    private static final String SOURCE_ROUTE_ID = "RT-104409";
//    private static final String TARGET_ROUTE_ID = "RT-104416";
//
//    private final ProductReconRepository productReconRepository;
//    private final ProductItemRepository productItemRepository;
//    private final RoutePlanningRepository routePlanningRepository;
//    private final ReconciliationRepository reconciliationRepository;
//
//    public ProductReconRunner(ProductReconRepository productReconRepository,
//                              ProductItemRepository productItemRepository,
//                              RoutePlanningRepository routePlanningRepository,
//                              ReconciliationRepository reconciliationRepository) {
//        this.productReconRepository = productReconRepository;
//        this.productItemRepository = productItemRepository;
//        this.routePlanningRepository = routePlanningRepository;
//        this.reconciliationRepository = reconciliationRepository;
//    }
//
//    @Override
//    @Transactional
//    public void run(String... args) throws Exception {
//        // Repair missing recon data for transferred route:
//        // source RT-104409
//        // New RT-104416
//        log.info("============================start==================");
//        Optional<RoutePlanning> targetRouteOptional = routePlanningRepository.findByRouteIdAndDeletedIsFalse(TARGET_ROUTE_ID);
//        if (targetRouteOptional.isEmpty()) {
//            log.info("Skipping ProductRecon transfer repair. Target route {} was not found.", TARGET_ROUTE_ID);
//            return;
//        }
//
//        RoutePlanning targetRoute = targetRouteOptional.get();
//        ProductRecon targetRecon = ensureProductRecon(targetRoute);
//        ensureProductItems(targetRoute, targetRecon);
//        ensureReconciliation(targetRoute, targetRecon);
//
//        log.info("ProductRecon transfer repair completed for target route {} using recon {}", TARGET_ROUTE_ID, targetRecon.getPublicId());
//    }
//
//
//
//    private ProductRecon ensureProductRecon(RoutePlanning targetRoute) {
//        Optional<ProductRecon> targetReconOptional =
//                productReconRepository.findProductReconByDeliveryIdIgnoreCaseAndDeletedIsFalse(TARGET_ROUTE_ID);
//
//        if (targetReconOptional.isPresent()) {
//            ProductRecon targetRecon = targetReconOptional.get();
//            updateReconRouteDetails(targetRecon, targetRoute);
//            return productReconRepository.save(targetRecon);
//        }
//
//        Optional<ProductRecon> sourceReconOptional =
//                productReconRepository.findProductReconByDeliveryIdIgnoreCaseAndDeletedIsFalse(SOURCE_ROUTE_ID);
//
//        ProductRecon recon = sourceReconOptional.orElseGet(ProductRecon::new);
//        updateReconRouteDetails(recon, targetRoute);
//        recon.setStatus(recon.getStatus() != null ? recon.getStatus() : ReconStatus.Pending);
//        recon.setReconciled(false);
//        recon.setCreatedBy(recon.getCreatedBy() != null ? recon.getCreatedBy() : targetRoute.getCreatedBy());
//        recon.setUpdatedBy(targetRoute.getUpdatedBy() != null ? targetRoute.getUpdatedBy() : targetRoute.getCreatedBy());
//
//        ProductRecon saved = productReconRepository.save(recon);
//        log.info("ProductRecon repaired for transferred route {} using recon {}", TARGET_ROUTE_ID, saved.getPublicId());
//        return saved;
//    }
//
//    private void updateReconRouteDetails(ProductRecon recon, RoutePlanning route) {
//        recon.setDeliveryAgent(route.getDeliveryAgentName());
//        recon.setDeliveryAgentPublicId(route.getDeliveryAgentId());
//        recon.setDeliveryId(route.getRouteId());
//        recon.setVehicleNumber(route.getVehicleName());
//        recon.setWarehouseName(route.getWarehouseName());
//        recon.setWarehousePublicId(route.getWarehouseId());
//        recon.setUpdatedBy(route.getUpdatedBy() != null ? route.getUpdatedBy() : route.getCreatedBy());
//        recon.setUpdateDate(LocalDateTime.now());
//    }
//
//    private void ensureProductItems(RoutePlanning targetRoute, ProductRecon targetRecon) {
//        if (!productItemRepository.findByProductReconAndDeletedIsFalse(targetRecon).isEmpty()) {
//            return;
//        }
//
//        Map<UUID, ProductItem> itemsByProduct = new LinkedHashMap<>();
//        targetRoute.getSalesOrders().forEach(order -> order.getSalesOrderItems().forEach(orderItem -> {
//            UUID productPublicId = resolveProductPublicId(orderItem);
//            if (productPublicId == null) {
//                return;
//            }
//
//            ProductItem item = itemsByProduct.computeIfAbsent(productPublicId, id -> buildProductItem(targetRecon, orderItem, id));
//            long quantity = orderItem.getQuantity();
//            item.setQuantityPicked(item.getQuantityPicked() + quantity);
//            item.setTotalUnits(item.getTotalUnits() + quantity);
//            item.setOrderQuantity(item.getOrderQuantity() + quantity);
//        }));
//
//        productItemRepository.saveAll(itemsByProduct.values());
//        log.info("Created {} ProductItems for repaired ProductRecon {}", itemsByProduct.size(), targetRecon.getPublicId());
//    }
//
//    private UUID resolveProductPublicId(SalesOrderItems orderItem) {
//        if (orderItem.getProduct() != null) {
//            return orderItem.getProduct().getPublicId();
//        }
//        return orderItem.getProductId();
//    }
//
//    private ProductItem buildProductItem(ProductRecon targetRecon, SalesOrderItems orderItem, UUID productPublicId) {
//        ProductItem item = new ProductItem();
//        item.setProductRecon(targetRecon);
//        item.setProductReconPublicId(targetRecon.getPublicId().toString());
//        item.setProductPublicId(productPublicId);
//        item.setProductName(orderItem.getProduct() != null ? orderItem.getProduct().getProductName() : null);
//        item.setProductCode(orderItem.getProduct() != null ? orderItem.getProduct().getSku() : null);
//        item.setProductSellingPrice(orderItem.getPricePerUnit() != null ? orderItem.getPricePerUnit() : BigDecimal.ZERO);
//        item.setDiscount(orderItem.getDiscount() != null ? orderItem.getDiscount() : BigDecimal.ZERO);
//        item.setLoaded(true);
//        item.setHasOrdered(true);
//        item.setHasVanSale(false);
//        item.setCreationTime(LocalDateTime.now());
//        return item;
//    }
//
//    private void ensureReconciliation(RoutePlanning targetRoute, ProductRecon targetRecon) {
//        BigDecimal totalPicked = productItemRepository.findByProductReconAndDeletedIsFalse(targetRecon).stream()
//                .map(item -> {
//                    BigDecimal price = item.getProductSellingPrice() != null ? item.getProductSellingPrice() : BigDecimal.ZERO;
//                    return price.multiply(BigDecimal.valueOf(item.getQuantityPicked()));
//                })
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        Optional<Reconciliation> targetReconciliationOptional =
//                reconciliationRepository.findByRoutePublicIdAndDeletedIsFalse(targetRoute.getPublicId().toString());
//
//        Reconciliation reconciliation = targetReconciliationOptional
//                .or(() -> reconciliationRepository.findByProductReconPublicIdAndDeletedIsFalse(targetRecon.getPublicId().toString()))
//                .orElseGet(() -> {
//                    Reconciliation sourceReconciliation = reconciliationRepository.findByDeliveryIdIgnoreCase(SOURCE_ROUTE_ID);
//                    return sourceReconciliation != null ? sourceReconciliation : new Reconciliation();
//                });
//
//        reconciliation.setDeliveryId(targetRoute.getRouteId());
//        reconciliation.setDeliveryAgent(targetRoute.getDeliveryAgentName());
//        reconciliation.setDeliveryAgentPublicId(targetRoute.getDeliveryAgentId());
//        reconciliation.setVehicleNumber(targetRoute.getVehicleName());
//        reconciliation.setDeliveryStatus(reconciliation.getDeliveryStatus() != null ? reconciliation.getDeliveryStatus() : "Created");
//        reconciliation.setWarehouseHouseStatus(reconciliation.getWarehouseHouseStatus() != null
//                ? reconciliation.getWarehouseHouseStatus()
//                : targetRecon.getStatus() != null ? targetRecon.getStatus().name() : ReconStatus.Pending.name());
//        reconciliation.setDeliveryAmount(totalPicked);
//        reconciliation.setTotalAmountPicked(totalPicked);
//        reconciliation.setRoutePublicId(targetRoute.getPublicId().toString());
//        reconciliation.setProductReconPublicId(targetRecon.getPublicId().toString());
//        reconciliation.setMissingAmount(reconciliation.getMissingAmount() != null ? reconciliation.getMissingAmount() : BigDecimal.ZERO);
//        reconciliation.setCreditAmount(reconciliation.getCreditAmount() != null ? reconciliation.getCreditAmount() : BigDecimal.ZERO);
//        reconciliation.setTotalDiscount(reconciliation.getTotalDiscount() != null ? reconciliation.getTotalDiscount() : BigDecimal.ZERO);
//        reconciliation.setCreatedBy(reconciliation.getCreatedBy() != null ? reconciliation.getCreatedBy() : targetRoute.getCreatedBy());
//        reconciliation.setUpdatedBy(targetRoute.getUpdatedBy() != null ? targetRoute.getUpdatedBy() : targetRoute.getCreatedBy());
//        reconciliation.setUpdateDate(LocalDateTime.now());
//
//        Reconciliation saved = reconciliationRepository.save(reconciliation);
//        log.info("Reconciliation repaired for transferred route {} using reconciliation {}", TARGET_ROUTE_ID, saved.getPublicId());
//    }
//}
