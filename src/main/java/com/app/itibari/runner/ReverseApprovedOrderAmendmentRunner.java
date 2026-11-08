//package com.app.itibari.runner;
//
//import com.app.itibari.dataTypes.*;
//import com.app.itibari.models.entities.*;
//import com.app.itibari.repository.*;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.core.annotation.Order;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.util.Optional;
//import java.util.UUID;
//
//@Order(20)
//@Slf4j
//@Component
//@RequiredArgsConstructor
////@ConditionalOnProperty(name = "runner.reverse-approved-amendment.enabled", havingValue = "true")
//public class ReverseApprovedOrderAmendmentRunner implements CommandLineRunner {
//
//    private final SalesOrdersRepository salesOrdersRepository;
//    private final OrderAmendmentRepository orderAmendmentRepository;
//    private final RoutePlanningRepository routePlanningRepository;
//    private final ProductReconRepository productReconRepository;
//    private final ProductItemRepository productItemRepository;
//    private final SalesOrderItemsRepository salesOrderItemsRepository;
//    private final DeliveryAgentOperationRepository deliveryAgentOperationRepository;
//
//
//    private final String routeId = "RT-105129";
//
//
//    private final String orderId = "ORD180186";
//
//    @Override
//    @Transactional
//    public void run(String... args) {
//        String effectiveRouteId = readArg(args, "route-id").orElse(routeId);
//        String effectiveOrderId = readArg(args, "order-id").orElse(orderId);
//
//        if (isBlank(effectiveRouteId) || isBlank(effectiveOrderId)) {
//            throw new IllegalArgumentException("runner.reverse-approved-amendment.route-id and order-id are required");
//        }
//
//        SalesOrders salesOrder = resolveSalesOrder(effectiveOrderId.trim());
//        OrderAmendment amendment = orderAmendmentRepository
//                .findFirstByRouteIdAndSalesOrderAndDeletedIsFalseOrderByIdDesc(effectiveRouteId.trim(), salesOrder)
//                .orElseThrow(() -> new IllegalArgumentException("No order amendment found for route "
//                        + effectiveRouteId + " and order " + effectiveOrderId));
//
//        if (amendment.getStatus() != AmendmentStatus.Approved) {
//            throw new IllegalStateException("Order amendment " + amendment.getPublicId()
//                    + " is not approved. Current status: " + amendment.getStatus());
//        }
//
//        log.info("Reversing approved amendment {} for route {} order {}",
//                amendment.getPublicId(), effectiveRouteId, salesOrder.getSalesOrderNumber());
//
//        reverseAutoFailedDelivery(amendment, salesOrder);
//        reverseApprovalStatus(amendment, salesOrder);
//
//        log.info("Reversed approved amendment {} for route {} order {}",
//                amendment.getPublicId(), effectiveRouteId, salesOrder.getSalesOrderNumber());
//    }
//
//    private SalesOrders resolveSalesOrder(String value) {
//
//
//        return salesOrdersRepository.findBySalesOrderNumberAndDeletedIsFalse(value)
//                .orElseThrow(() -> new IllegalArgumentException("Sales order not found: " + value));
//
//    }
//
//    private void reverseApprovalStatus(OrderAmendment amendment, SalesOrders salesOrder) {
//        amendment.setStatus(AmendmentStatus.Pending);
//        amendment.setCxReasonCode(null);
//        amendment.setUpdateDate(LocalDateTime.now());
//
//        salesOrder.setAwaitingApproval(true);
//        salesOrder.setOrderAmendStatus(OrderAmendStatus.Pending);
//        salesOrder.setUpdateDate(LocalDateTime.now());
//
//        salesOrdersRepository.save(salesOrder);
//        orderAmendmentRepository.save(amendment);
//    }
//
//    private void reverseAutoFailedDelivery(OrderAmendment amendment, SalesOrders salesOrder) {
//        if (amendment.getAmendAction() != AmendAction.CancelAndRequestVanSale
//                || salesOrder.getSalesOrderStatus() != LoadingStatusEnum.FAILED_DELIVERY
//                || hasAnyDeliveredItems(salesOrder)) {
//            return;
//        }
//
//        salesOrder.setSalesOrderStatus(LoadingStatusEnum.IN_TRANSIT);
//        salesOrder.setStatus(OrderStatusEnum.Pending);
//        salesOrder.setDeliveredAmount(BigDecimal.ZERO);
//        salesOrder.setDeliveryDate(null);
//
//        if (amendment.isFailedOrder()) {
//            reverseVanSaleConversion(amendment, salesOrder);
//        }
//    }
//
//    private void reverseVanSaleConversion(OrderAmendment amendment, SalesOrders salesOrder) {
//        RoutePlanning routePlanning = parseUuid(salesOrder.getRoutePublicId())
//                .flatMap(routePlanningRepository::findByPublicIdAndDeletedIsFalse)
//                .orElseGet(() -> routePlanningRepository.findByRouteIdAndDeletedIsFalse(amendment.getRouteId())
//                        .orElse(null));
//
//        if (routePlanning == null) {
//            log.warn("Skipping van-sale conversion reversal. Route not found for order {}", salesOrder.getSalesOrderNumber());
//            return;
//        }
//
//        ProductRecon productRecon = productReconRepository
//                .findProductReconByDeliveryIdIgnoreCaseAndDeletedIsFalse(routePlanning.getRouteId())
//                .orElse(null);
//
//        if (productRecon == null) {
//            log.warn("Skipping van-sale conversion reversal. Product recon not found for route {}", routePlanning.getRouteId());
//            return;
//        }
//
//        for (AmendmentProductItem amendmentItem : amendment.getAmendmentProductItems()) {
//            SalesOrderItems salesOrderItem = salesOrder.getSalesOrderItems()
//                    .stream()
//                    .filter(item -> item.getProduct() != null)
//                    .filter(item -> item.getProduct().getSku().equals(amendmentItem.getProductCode()))
//                    .findFirst()
//                    .orElse(null);
//
//            if (salesOrderItem == null || !salesOrderItem.isVanSaleConverted()) {
//                continue;
//            }
//
//            long convertedQuantity = salesOrderItem.getQuantity() - salesOrderItem.getQuantityDelivered();
//            if (convertedQuantity <= 0) {
//                continue;
//            }
//
//            ProductItem productItem = productItemRepository
//                    .findByProductReconAndProductPublicIdAndDeletedIsFalse(productRecon, salesOrderItem.getProduct().getPublicId())
//                    .orElse(null);
//
//            if (productItem != null) {
//                productItem.setVanSaleQuantity(Math.max(0, productItem.getVanSaleQuantity() - convertedQuantity));
//                int vanSaleItemSold = productItem.getVanSaleItemSold() != null ? productItem.getVanSaleItemSold() : 0;
//                if (productItem.getVanSaleQuantity() == 0 && vanSaleItemSold == 0) {
//                    productItem.setHasVanSale(false);
//                    productItem.setVanSaleItem(false);
//                    productItem.setVanSales(null);
//                }
//                productItem.setUpdateDate(LocalDateTime.now());
//                productItemRepository.save(productItem);
//            }
//
//            deliveryAgentOperationRepository
//                    .findByRouteIdAndOrderNumberAndSkuAndDeletedIsFalse(
//                            routePlanning.getRouteId(),
//                            salesOrder.getSalesOrderNumber(),
//                            amendmentItem.getProductCode())
//                    .ifPresent(operation -> reverseOperationVanSaleQuantity(operation, convertedQuantity));
//
//            salesOrderItem.setVanSaleConverted(false);
//            salesOrderItem.setUpdateDate(LocalDateTime.now());
//            salesOrderItemsRepository.save(salesOrderItem);
//        }
//    }
//
//    private void reverseOperationVanSaleQuantity(DeliveryAgentOperation operation, long convertedQuantity) {
//        operation.setPreBookedVanQuantity((int) Math.max(0, operation.getPreBookedVanQuantity() - convertedQuantity));
//        operation.setUpdateDate(LocalDateTime.now());
//        deliveryAgentOperationRepository.save(operation);
//    }
//
//    private boolean hasAnyDeliveredItems(SalesOrders salesOrder) {
//        return salesOrder.getSalesOrderItems()
//                .stream()
//                .anyMatch(item -> item.getQuantityDelivered() > 0
//                        || item.getStatus() == OrderItemStatus.Delivered
//                        || item.getStatus() == OrderItemStatus.PartiallyDelivered);
//    }
//
//    private Optional<String> readArg(String[] args, String name) {
//        String prefix = "--" + name + "=";
//        for (String arg : args) {
//            if (arg != null && arg.startsWith(prefix)) {
//                return Optional.of(arg.substring(prefix.length()));
//            }
//        }
//        return Optional.empty();
//    }
//
//    private Optional<UUID> parseUuid(String value) {
//        if (isBlank(value)) {
//            return Optional.empty();
//        }
//        try {
//            return Optional.of(UUID.fromString(value));
//        } catch (IllegalArgumentException ignored) {
//            return Optional.empty();
//        }
//    }
//
//    private boolean isBlank(String value) {
//        return value == null || value.trim().isEmpty();
//    }
//}
