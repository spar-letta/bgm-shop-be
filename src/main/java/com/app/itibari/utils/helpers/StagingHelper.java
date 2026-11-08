package com.app.itibari.utils.helpers;

import com.app.itibari.dataTypes.*;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.StagingActionRequest;
import com.app.itibari.repository.*;
import com.app.itibari.services.helper.LiveStockHelper;
import com.app.itibari.services.helper.StockMovementHelper;
import com.app.itibari.services.helper.StockUpdateHelper;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class StagingHelper {

    private final ProductRepository productRepository;
    private final ReconciliationStagingRepository reconciliationStagingRepository;
    private final StockUpdateHelper stockUpdateHelper;
    private final ProductReconRepository productReconRepository;
    private final ReconciliationStagingItemRepository reconciliationStagingItemRepository;
    private final ProductItemRepository productItemRepository;
    private final LiveStockHelper liveStockHelper;
    private final StockMovementHelper stockMovementHelper;
    private final PersonRepository personRepository;
    private final ProductStockHelper productStockHelper;
    private final SalesOrderItemsRepository salesOrderItemsRepository;
    private final DeliveryAgentOperationRepository deliveryAgentOperationRepository;

    public void handleApproval(ReconciliationStaging staging,
                               StagingActionRequest request,
                               Warehouse warehouse,
                               String email,
                               UUID publicId) {

        Person person;

        if (email != null && !email.isEmpty()) {
            Optional<Person> p = personRepository.findByEmailAndDeletedIsFalse(email);
            person = p.orElse(null);
        } else {
            person = null;
        }

        Map<Product, Integer> returnedStock = new HashMap<>();
        staging.getReconciliationStagingItemList().forEach(item -> {

            productRepository.findByPublicIdAndDeletedIsFalse(item.getProductPublicId())
                    .ifPresent(product -> {
                        returnedStock.put(product,
                                returnedStock.getOrDefault(product, 0) + (int) item.getQuantityReturned());
                    });
        });

        staging.setStatus(ReconciliationStageStatus.APPROVED);
        staging.setComment(
                (request.comment() == null || request.comment().isEmpty())
                        ? "--"
                        : request.comment()
        );
        reconciliationStagingRepository.save(staging);

        stockUpdateHelper.updateStockReturned(returnedStock, warehouse, InventoryEvent.APPROVE_RECONCILIATION_STAGE.name());

        ProductRecon productRecon = staging.getProductRecon();
        if (productRecon.getStatus() != ReconStatus.Reconciled) {
            productRecon.setStatus(ReconStatus.AwaitingApproval);
        }
        productReconRepository.save(productRecon);
        updateDeliveryAgentOperationReturnedQuantities(staging);

        updateLiveStock(warehouse.getPublicId(), staging);

        returnedStock.forEach((product, quantity) -> {
            productStockHelper.updateProductStock(product, warehouse, Long.valueOf(quantity),
                    StockMovementTransactionType.FROM_TRADE);
        });


        returnedStock.forEach((product, quantity) -> {
            stockMovementHelper.recordStockMovement(
                    product,
                    quantity,
                    StockMovementTransactionType.DA_RETURN,
                    TransactionBoundType.INCOMING,
                    person != null ? person.getFullName() : "System",
                    warehouse);

        });

        // TODO: publish ApprovalEvent Hapa
    }

    private void updateDeliveryAgentOperationReturnedQuantities(ReconciliationStaging staging) {
        ProductRecon productRecon = staging.getProductRecon();
        List<ProductItem> productItems = productRecon.getProductItems();

        for (ReconciliationStagingItem stagingItem : staging.getReconciliationStagingItemList()) {
            ProductItem productItem = productRecon.getProductItems().stream()
                    .filter(ProductItem::isHasOrdered)
                    .filter(item -> stagingItem.getProductPublicId().equals(item.getProductPublicId()))
                    .findFirst()
                    .orElse(null);

            if (productItem == null || productItem.getSalesOrderItemPublicId() == null || productItem.getSalesOrderItemPublicId().isBlank()) {
                continue;
            }

//            for (ProductItem item : productItems) {
//                if (productItem.getProductPublicId().equals(item.getProductPublicId())){
//                    long  remaining = productItem.getQuantityPicked() - productItem.getQuantitySold();
//                    if (remaining != stagingItem.getQuantityReturned()){
//                        throw new BadRequestRestApiException()
//                                .developerMessage("Returned quantity does not match "+ remaining)
//                                .userMessage("Sorry. Returned quantity does not match "+ remaining);
//                    }
//                }
//            }
//            SalesOrderItems salesOrderItem = salesOrderItemsRepository
//                    .findByPublicIdAndDeletedIsFalse(UUID.fromString(productItem.getSalesOrderItemPublicId()))
//                    .orElseThrow(() -> new BadRequestRestApiException()
//                            .developerMessage("Sales order item not found for product recon item " + productItem.getPublicId())
//                            .userMessage("Sorry. Sales order item not found"));
//
//            List<DeliveryAgentOperation> deliveryAgentOperation = deliveryAgentOperationRepository
//                    .findByRouteIdAndSkuAndDeletedIsFalse(
//                            productRecon.getDeliveryId(),
//                            salesOrderItem.getProduct().getSku());
//
//            if (!deliveryAgentOperation.isEmpty()){
//                for (DeliveryAgentOperation agentOperation : deliveryAgentOperation) {
//                    agentOperation.setVanReturnQuantity(stagingItem.getQuantityReturned());
//                    deliveryAgentOperationRepository.save(agentOperation);
//                }
//            }


        }
    }

    private void updateLiveStock(UUID wareHousePublicId, ReconciliationStaging reconciliationStaging) {
        for (ReconciliationStagingItem reconciliationStagingItem : reconciliationStaging.getReconciliationStagingItemList()) {

            liveStockHelper.updateLiveStockValue(wareHousePublicId, reconciliationStagingItem.getProductPublicId(),
                    Math.toIntExact(reconciliationStagingItem.getQuantityReturned()), ProductUpdateType.RETURNED_FROM_TRADE_STOCK,
                    LocalDate.now());
        }
    }

    public void validateEditItemRequest(StagingActionRequest request, UUID itemPublicId) {
        if (itemPublicId == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Missing Item parameters")
                    .userMessage("Sorry. Missing Item details");
        }

        if (request.quantity() < 0) {
            throw new BadRequestRestApiException()
                    .developerMessage("Quantity must not be a negative value")
                    .userMessage("Sorry. Quantity must not be a negative value");
        }

        if (request.comment() == null || request.comment().isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Missing comment")
                    .userMessage("Sorry. Missing comment");
        }
    }

    public void handleItemEdit(UUID itemPublicId,
                               StagingActionRequest request,
                               Person person, ProductRecon productRecon) {

        ReconciliationStagingItem item = reconciliationStagingItemRepository.findByPublicIdAndDeletedIsFalse(itemPublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Item not found")
                        .userMessage("Sorry. Item not found"));

        if (item.getAnticipatedProduct() < request.quantity()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Quantity can not be more than initial requested value")
                    .userMessage("Sorry. Quantity can not be more than initial requested value");
        }

        item.setQuantityReturned(request.quantity());
        item.setReason(request.comment());
        item.setUpdatedBy(person);
        ReconciliationStagingItem savedproductReconciliationStagingItem =
                reconciliationStagingItemRepository.save(item);

        handleProductReconUpdate(productRecon, savedproductReconciliationStagingItem);

        // TODO: publish ItemEditedEvent Hapa
    }

    private void handleProductReconUpdate(ProductRecon productRecon,
                                          ReconciliationStagingItem savedItem) {

        productRecon.getProductItems().stream()
                .filter(item -> item.getProductPublicId().equals(savedItem.getProductPublicId()))
                .findFirst()
                .ifPresentOrElse(productItem -> {
                    long picked = productItem.getQuantityPicked();
                    long sold = productItem.getQuantitySold();
                    int returned = savedItem.getQuantityReturned();

                    int missing = Math.toIntExact(picked - (returned + sold));

                    productItem.setQuantityMissing(missing);
                    productItem.setQuantityReturned(returned);
                    productItemRepository.save(productItem);


                    log.info("Updated ProductItem {} in ProductRecon {}: picked={}, sold={}, returned={}, missing={}",
                            productItem.getPublicId(),
                            productRecon.getPublicId(),
                            picked, sold, returned, missing);
                }, () -> {
                    log.warn("No ProductItem found in ProductRecon {} for staging item {}",
                            productRecon.getPublicId(),
                            savedItem.getPublicId());
                });
    }
}
