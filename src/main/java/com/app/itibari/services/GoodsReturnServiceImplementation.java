package com.app.itibari.services;

import com.app.itibari.dataTypes.*;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.GoodsReturnNoteRequest;
import com.app.itibari.models.request.PromoItemRequest;
import com.app.itibari.models.request.PromoRequestNoteRequest;
import com.app.itibari.models.response.ReceivedProductItemResponse;
import com.app.itibari.repository.*;
import com.app.itibari.services.helper.*;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.helpers.ProductStockHelper;
import com.app.itibari.utils.helpers.Validators;
import com.app.itibari.utils.specification.GoodsReceivedSpecification;
import com.app.itibari.utils.specification.GoodsReturnDeliveryNoteSpecification;
import com.app.itibari.utils.specification.GoodsReturnSpecification;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsReturnServiceImplementation implements GoodsReturnNoteService {

    private final GoodsReturnRepository goodsReturnRepository;

    private final PersonRepository personRepository;

    private final PurchaseOrderRepository purchaseOrderRepository;

    private final PurchaseOrderItemRepository purchaseOrderItemRepository;

    private final CounterService counterService;

    private final EventsRepository eventsRepository;

    private final RejectionCodeRepository rejectionCodeRepository;

    private final ProductItemRepository productItemRepository;

    private final ProductRepository productRepository;

    private final NotificationHelper notificationHelper;

    private final SupplierRepository supplierRepository;

    private final WarehouseRepository warehouseRepository;

    private final LiveStockHelper liveStockHelper;

    private final AvcoHelper avcoHelper;

    private final StockUpdateHelper stockUpdateHelper;

    private final InventoryRepository inventoryRepository;

    private final InventoryProductsRepository inventoryProductsRepository;

    private final GoodsReceivedNoteRepository goodsReceivedNoteRepository;

    private final GoodsReturnDeliveryNoteRepository goodsReturnDeliveryNoteRepository;

    private final ReturnedProductItemRepository returnedProductItemRepository;

    private final ReceivedProductItemRepository receivedProductItemRepository;
    private final StockMovementHelper stockMovementHelper;
    private final ProductStockHelper productStockHelper;
    private final ProcurementService procurementService;

    @Override
    public void createGoodsReturnNote(GoodsReturnNoteRequest goodsReturnNoteRequest, String name) {

        GoodsReturnNote goodsReturnNote = new GoodsReturnNote();

        Optional<Person> person = personRepository.findByEmailAndDeletedIsFalse(name);

        Optional<GoodsReturnNote> optionalGoodsReturnNote = goodsReturnRepository.findByPublicIdAndDeletedIsFalse(goodsReturnNoteRequest.getReturnNotePublicId());

        if (optionalGoodsReturnNote.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Goods received return note not found")
                    .userMessage("Goods received return note not found");
        }
        GoodsReturnNote foundNote = optionalGoodsReturnNote.get();

        PurchaseOrder purchaseOrder = purchaseOrderRepository
                .findByPurchaseOrderNumberAndDeletedIsFalse(foundNote.getPurchaseOrderNumber())
                .orElse(null);

        if (purchaseOrder == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Purchase order not found")
                    .userMessage("Purchase order not found");
        }

        Warehouse warehouse = warehouseRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(purchaseOrder.getWarehouseId()))
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Warehouse not found")
                        .userMessage("Warehouse not found"));

        Inventory inventory = inventoryRepository
                .findByWarehouseAndDeletedIsFalse(warehouse)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Inventory not found for warehouse")
                        .userMessage("Inventory not found for warehouse"));

        if (!(purchaseOrder.getStatus().equals(PurchaseOrderStatus.PARTIAL_RECEIVED) || purchaseOrder
                .getStatus()
                .equals(PurchaseOrderStatus.RECEIVED))) {
            throw new BadRequestRestApiException()
                    .developerMessage("Purchase order is already delivered")
                    .userMessage("Purchase order is already delivered");
        }

        if (goodsReturnNoteRequest.getGoodsNoteItemRequests().isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Goods return note item request is empty")
                    .userMessage("Goods return note item request is empty");
        }

        if (goodsReturnNoteRequest
                .getGoodsNoteItemRequests()
                .stream()
                .anyMatch(goodsReturnNoteItemRequest -> goodsReturnNoteItemRequest.getQuantity() <= 0)) {
            throw new BadRequestRestApiException()
                    .developerMessage("Quantity must be greater than 0")
                    .userMessage("Quantity must be greater than 0");
        }

        if (goodsReturnNoteRequest
                .getGoodsNoteItemRequests()
                .stream()
                .anyMatch(goodsReturnNoteItemRequest -> goodsReturnNoteItemRequest.getRejectionCodePublicId() == null)) {
            throw new BadRequestRestApiException()
                    .developerMessage("Rejection code is required")
                    .userMessage("Rejection code is required");
        }

        Map<Product, ProductItem> productItemMap = new HashMap<>();
        List<ProductItem> productItems = new ArrayList<>();
        goodsReturnNoteRequest.getGoodsNoteItemRequests().forEach(goodsReturnNoteItemRequest -> {

            Optional<ProductItem> optionalProductItem = productItemRepository.findByPublicIdAndDeletedIsFalse(goodsReturnNoteItemRequest.getItemPublicId());

            if (optionalProductItem.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Product item not found")
                        .userMessage("Product item not found");
            }

            ProductItem productItem = optionalProductItem.get();

            Product foundProduct = productRepository.findByPublicId(productItem.getProductPublicId());

            InventoryProducts inventoryProducts = inventoryProductsRepository
                    .findByInventoryAndProductAndDeletedIsFalse(inventory, foundProduct)
                    .orElseThrow(() -> new BadRequestRestApiException()
                            .developerMessage("Inventory product not found")
                            .userMessage("Inventory product not found"));

            if (goodsReturnNoteItemRequest.getQuantity() > (productItem.getQuantityReceived())) {

                throw new BadRequestRestApiException()
                        .developerMessage("Quantity must be less than or equal to quantity received")
                        .userMessage("Quantity must be less than or equal to quantity received");
            }

            if (goodsReturnNoteItemRequest.getQuantity() > inventoryProducts.getAvailableStock()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Not enough stock")
                        .userMessage("Not enough stock, Quantity to be returned is more than available stock");
            }

            Optional<RejectionsCode> optionalRejectionsCode = rejectionCodeRepository.findRejectionsCodeByPublicIdAndDeletedIsFalse(
                    goodsReturnNoteItemRequest.getRejectionCodePublicId());

            if (optionalRejectionsCode.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Rejection code not found")
                        .userMessage("Rejection code not found");
            }

            RejectionsCode rejectionsCode = optionalRejectionsCode.get();

            ProductItem product = new ProductItem();
            product.setProductName(productItem.getProductName());
            product.setQuantityReturned(goodsReturnNoteItemRequest.getQuantity());
            product.setQuantityReceived(productItem.getQuantityReceived());
            product.setQuantityRequested(productItem.getQuantityRequested());
            product.setProductCode(productItem.getProductCode());
            product.setRejectionReason(rejectionsCode.getDescription());
            product.setRejectionCode(rejectionsCode.getCode());
            product.setProductPublicId(productItem.getProductPublicId());
            ProductItem saved = productItemRepository.save(product);
            productItems.add(saved);
        });

        goodsReturnNote.setGoodsReturnNoteDate(goodsReturnNoteRequest.getGoodsReturnDate());
        person.ifPresent(goodsReturnNote::setCreatedBy);
        goodsReturnNote.setPurchaseOrderNumber(purchaseOrder.getPurchaseOrderNumber());
        goodsReturnNote.setSupplier(purchaseOrder.getSupplier());
        goodsReturnNote.setPurchaseOrderStatus(purchaseOrder.getStatus());
        goodsReturnNote.setGoodsNoteType(GoodsNoteType.RETURN_GOODS);
        goodsReturnNote.setGoodsReturnNoteDate(LocalDate.now());
        goodsReturnNote.setStatus(GoodsReturnStatus.PENDING);
        goodsReturnNote.setGoodsReceiveNoteId(foundNote.getPublicId().toString());
        goodsReturnNote.setGoodsReceiveNoteNumber(foundNote.getGoodsReturnNoteNumber());
        goodsReturnNote.setDeliveryNoteNumber(foundNote.getDeliveryNoteNumber());
        goodsReturnNote.setGoodsReturnNoteNumber("GRTN" + counterService.getNextCounter(CounterType.GOODS_RETURN_NOTE));
        GoodsReturnNote saved = goodsReturnRepository.save(goodsReturnNote);

        saved.setProductItems(productItems);

        GoodsReturnNote savedGoodsReturnNote = goodsReturnRepository.save(saved);

        if (!productItems.isEmpty()) {

            productItems.forEach(productItem -> {
                productItem.setReturnNotePublicId(String.valueOf(savedGoodsReturnNote.getPublicId()));
                ProductItem savedItem = productItemRepository.save(productItem);
                Optional<Product> optionalProduct = productRepository.findByPublicIdAndDeletedIsFalse(savedItem.getProductPublicId());

                optionalProduct.ifPresent(product -> productItemMap.put(product, savedItem));
            });
        }

        stockUpdateHelper.updateProductStockReturnGoods(productItemMap, warehouse);

        String description = "Goods return note created for purchase order number " + purchaseOrder.getPurchaseOrderNumber() + " and return quantity " + savedGoodsReturnNote
                .getProductItems()
                .size();

        eventsRepository.save(new Events(savedGoodsReturnNote.getPublicId().toString(),
                "GOODS RETURN CREATED",
                LocalDateTime.now(),
                description,
                "CREATED",
                person.orElse(null)));
    }

    @Override
    public Page<GoodsReturnNote> getGoodsReturnNote(GoodsReturnStatus status,
                                                    String search,
                                                    String purchaseOrderNumber,
                                                    UUID supplierPublicId,
                                                    LocalDate to,
                                                    LocalDate from,
                                                    GoodsNoteType goodsNoteType,
                                                    Pageable pageable) {

        Specification<GoodsReturnNote> spec = Specification
                .where(GoodsReturnSpecification.isNotDeleted())
                .and(GoodsReturnSpecification.hasSupplier(supplierPublicId))
                .and(GoodsReturnSpecification.hasStatus(status))
                .and(GoodsReturnSpecification.hasGoodNoteType(goodsNoteType))
                .and(GoodsReturnSpecification.hasSearchTerm(search))
                .and(GoodsReturnSpecification.hasDateRange(from, to))
                .and(GoodsReturnSpecification.hasPurchaseOrder(purchaseOrderNumber));

        return goodsReturnRepository.findAll(spec, pageable);
    }

    @Override
    public Page<ProductItem> getGoodsReceiveNoteItems(String search,
                                                      UUID goodsReturnNotePublicId,
                                                      GoodsNoteType goodsNoteType,
                                                      Pageable pageable) {

        Optional<GoodsReturnNote> optionalGoodsReturnNote = goodsReturnRepository.findByPublicIdAndDeletedIsFalse(goodsReturnNotePublicId);

        if (optionalGoodsReturnNote.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Goods return note not found")
                    .userMessage("Goods return note not found");
        }

        GoodsReturnNote goodsReturnNote = optionalGoodsReturnNote.get();

        if (!goodsReturnNote.getGoodsNoteType().equals(goodsNoteType)) {
            throw new BadRequestRestApiException()
                    .developerMessage("Goods return note type mismatch")
                    .userMessage("Goods return note type mismatch");
        }

        return new PageImpl<>(goodsReturnNote.getProductItems(), pageable, goodsReturnNote.getProductItems().size());
    }

    @Override
    public Page<ProductItem> getGoodsReturnNoteItems(String search,
                                                     UUID goodsReturnNotePublicId,
                                                     GoodsNoteType goodsNoteType,
                                                     Pageable pageable) {

        Optional<GoodsReturnNote> optionalGoodsReturnNote = goodsReturnRepository.findByPublicIdAndDeletedIsFalse(goodsReturnNotePublicId);

        if (optionalGoodsReturnNote.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Goods return note not found")
                    .userMessage("Goods return note not found");
        }
        GoodsReturnNote goodsReturnNote = optionalGoodsReturnNote.get();

        return productItemRepository.findAllByReturnNotePublicIdAndDeletedIsFalse(goodsReturnNote.getPublicId().toString(), pageable);
    }

    @Transactional
    @Override
    public void receivePromoGoods(PromoType promoType, PromoRequestNoteRequest promoNoteRequest, String name) {
        Optional<Person> person = personRepository.findByEmailAndDeletedIsFalse(name);

        if (promoNoteRequest.getItems().isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Promo note item request is empty")
                    .userMessage("Promo note item request is empty");
        }

        if (Validators.anyEqualNull(promoNoteRequest.getPurchaseOrderNumber())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Purchase order number required")
                    .userMessage("Sorry. Purchase order number required");
        }

        if (promoNoteRequest.getItems().stream().anyMatch(promoNoteItemRequest -> promoNoteItemRequest.getQuantityReceived() <= 0)) {
            throw new BadRequestRestApiException()
                    .developerMessage("Quantity must be greater than 0")
                    .userMessage("Quantity must be greater than 0");
        }

        Optional<PurchaseOrder> optionalPurchaseOrder = purchaseOrderRepository.findByPurchaseOrderNumberAndDeletedIsFalse(promoNoteRequest.getPurchaseOrderNumber());

        if (optionalPurchaseOrder.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Purchase order not found")
                    .userMessage("Purchase order not found");
        }

        PurchaseOrder purchaseOrder = optionalPurchaseOrder.get();

        Warehouse warehouse = warehouseRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(purchaseOrder.getWarehouseId()))
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Warehouse not found")
                        .userMessage("Warehouse not found"));

        Optional<GoodsReceivedNote> optionalGoodsReceiveNote = goodsReceivedNoteRepository.findByPublicIdAndDeletedIsFalse(promoNoteRequest.getReturnNotePublicId());

        if (optionalGoodsReceiveNote.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Goods received note not found")
                    .userMessage("Sorry. Goods received note not found!");
        }

        GoodsReceivedNote goodsReceivedNote = optionalGoodsReceiveNote.get();
        person.ifPresent(goodsReceivedNote::setUpdatedBy);
        goodsReceivedNote.setUpdateDate(LocalDateTime.now());

        List<ReceivedProductItem> receivedProductItems = new ArrayList<>();

        Map<Product, ReceivedProductItem> productItemMap = new HashMap<>();

        promoNoteRequest.getItems().forEach(item -> {
            Optional<Product> optionalProduct = productRepository.findByPublicIdAndDeletedIsFalse(item.getProductPublicId());
            if (optionalProduct.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Product not found")
                        .userMessage("Product not found");
            }
            Product product = optionalProduct.get();
            ReceivedProductItem receivedProductItem = createReceiveProductItem(item, product, promoType, goodsReceivedNote);

            // add bonus

            updateLiveStockWithBonuses(warehouse, receivedProductItem);

                productStockHelper.updateProductStock(product,
                        warehouse,
                        receivedProductItem.getQuantityReceived(),
                        StockMovementTransactionType.BONUS);


            receivedProductItems.add(receivedProductItem);
            goodsReceivedNote.addProduct(receivedProductItem);
        });
        goodsReceivedNoteRepository.save(goodsReceivedNote);

        receivedProductItems.forEach(receivedProductItem -> {
            Optional<Product> optionalProduct = productRepository.findByPublicIdAndDeletedIsFalse(receivedProductItem.getProductPublicId());

            optionalProduct.ifPresent(product -> productItemMap.put(product, receivedProductItem));
        });

        stockUpdateHelper.updateProductStockOnReceiveShipment(productItemMap, warehouse, InventoryEvent.PROMO.name());
    }

    @Override
    public Page<GoodsReceivedNote> getShipmentReceiveNote(GoodsReturnStatus status,
                                                          String search,
                                                          String purchaseOrderNumber,
                                                          UUID supplierPublicId,
                                                          LocalDate to,
                                                          LocalDate from,
                                                          GoodsNoteType goodsNoteType,
                                                          PurchaseOrderStatus poStatus,
                                                          Pageable pageable) {

        Specification<GoodsReceivedNote> spec = Specification
                .where(GoodsReceivedSpecification.isNotDeleted())
                .and(GoodsReceivedSpecification.hasSupplier(supplierPublicId))
                .and(GoodsReceivedSpecification.hasStatus(status))
                .and(GoodsReceivedSpecification.hasSearchTerm(search))
                .and(GoodsReceivedSpecification.hasDateRange(from, to))
                .and(GoodsReceivedSpecification.hasPurchaseOrder(purchaseOrderNumber))
                .and(GoodsReceivedSpecification.hasHasPoStatus(poStatus));
//                .and(GoodsReceivedSpecification.orderByCreationTimeDesc());

        return goodsReceivedNoteRepository.findAll(spec, pageable);
    }

    @Override
    public ReceivedProductItemResponse getShipmentNoteItems(String search, UUID goodsReceivedNotePublicId, Pageable pageable) {

        GoodsReceivedNote goodsReceivedNote =
                goodsReceivedNoteRepository.findByPublicId(goodsReceivedNotePublicId).orElse(null);

        if (goodsReceivedNote == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Shipment note not found")
                    .userMessage("Shipment note not found");
        }

        List<ReceivedProductItem> receivedProductItems =
                receivedProductItemRepository.findByGoodsReceivedNoteAndDeletedIsFalse(goodsReceivedNote, pageable).getContent();

        BigDecimal totalReceivedValue = receivedProductItems.stream()
                .map(ReceivedProductItem::getTotalReceivedValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalValue = receivedProductItems.stream()
                .map(ReceivedProductItem::getTotalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ReceivedProductItemResponse receivedProductItemResponse = ReceivedProductItemResponse.builder()
                .grandTotalReceivedValue(totalReceivedValue)
                .grandTotalValue(totalValue)
                .productItemList(receivedProductItems)
                .build();

        return receivedProductItemResponse;
//        return receivedProductItemRepository.findByGoodsReceivedNoteAndDeletedIsFalse(goodsReceivedNote, pageable);
    }

    @Transactional
    @Override
    public void createGoodsReturnDeliveryNote(GoodsReturnNoteRequest goodsReturnNoteRequest, String name) {

        GoodsReturnDeliveryNote goodsReturnNote = new GoodsReturnDeliveryNote();

        Optional<Person> person = personRepository.findByEmailAndDeletedIsFalse(name);

        Optional<GoodsReceivedNote> optionalGoodsReceivedNote =
                goodsReceivedNoteRepository.findByPublicIdAndDeletedIsFalse(goodsReturnNoteRequest.getReturnNotePublicId());

        if (optionalGoodsReceivedNote.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Goods received return note not found")
                    .userMessage("Goods received return note not found");
        }
        GoodsReceivedNote goodsReceivedNote = optionalGoodsReceivedNote.get();

        PurchaseOrder purchaseOrder = purchaseOrderRepository
                .findByPurchaseOrderNumberAndDeletedIsFalse(goodsReceivedNote.getPurchaseOrderNumber())
                .orElse(null);

        if (purchaseOrder == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Purchase order not found")
                    .userMessage("Purchase order not found");
        }

        Warehouse warehouse = warehouseRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(purchaseOrder.getWarehouseId()))
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Warehouse not found")
                        .userMessage("Warehouse not found"));

        Inventory inventory = inventoryRepository
                .findByWarehouseAndDeletedIsFalse(warehouse)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Inventory not found for warehouse")
                        .userMessage("Inventory not found for warehouse"));

        if (!(purchaseOrder.getStatus().equals(PurchaseOrderStatus.PARTIAL_RECEIVED) || purchaseOrder
                .getStatus()
                .equals(PurchaseOrderStatus.RECEIVED))) {
            throw new BadRequestRestApiException()
                    .developerMessage("Purchase order is already delivered")
                    .userMessage("Purchase order is already delivered");
        }

        if (goodsReturnNoteRequest.getGoodsNoteItemRequests().isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Goods return note item request is empty")
                    .userMessage("Goods return note item request is empty");
        }

        if (goodsReturnNoteRequest
                .getGoodsNoteItemRequests()
                .stream()
                .anyMatch(goodsReturnNoteItemRequest -> goodsReturnNoteItemRequest.getQuantity() <= 0)) {
            throw new BadRequestRestApiException()
                    .developerMessage("Quantity must be greater than 0")
                    .userMessage("Quantity must be greater than 0");
        }

        if (goodsReturnNoteRequest
                .getGoodsNoteItemRequests()
                .stream()
                .anyMatch(goodsReturnNoteItemRequest -> goodsReturnNoteItemRequest.getRejectionCodePublicId() == null)) {
            throw new BadRequestRestApiException()
                    .developerMessage("Rejection code is required")
                    .userMessage("Rejection code is required");
        }

        Map<Product, ReturnedProductItem> productItemMap = new HashMap<>();
        List<ReturnedProductItem> productItems = new ArrayList<>();
        goodsReturnNoteRequest.getGoodsNoteItemRequests().forEach(goodsReturnNoteItemRequest -> {

            Optional<ReceivedProductItem> optionalProductItem =
                    receivedProductItemRepository.findByPublicIdAndDeletedIsFalse(goodsReturnNoteItemRequest.getItemPublicId());

            if (optionalProductItem.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Product item not found")
                        .userMessage("Product item not found");
            }

            ReceivedProductItem productItem = optionalProductItem.get();

            Product foundProduct = productRepository.findByPublicId(productItem.getProductPublicId());

            InventoryProducts inventoryProducts = inventoryProductsRepository
                    .findByInventoryAndProductAndDeletedIsFalse(inventory, foundProduct)
                    .orElseThrow(() -> new BadRequestRestApiException()
                            .developerMessage("Inventory product not found")
                            .userMessage("Inventory product not found"));

            if (goodsReturnNoteItemRequest.getQuantity() > (productItem.getQuantityReceived())) {

                throw new BadRequestRestApiException()
                        .developerMessage("Quantity must be less than or equal to quantity received")
                        .userMessage("Quantity must be less than or equal to quantity received");
            }

            if (goodsReturnNoteItemRequest.getQuantity() > inventoryProducts.getAvailableStock()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Not enough stock")
                        .userMessage("Not enough stock, Quantity to be returned is more than available stock");
            }

            Optional<RejectionsCode> optionalRejectionsCode = rejectionCodeRepository.findRejectionsCodeByPublicIdAndDeletedIsFalse(
                    goodsReturnNoteItemRequest.getRejectionCodePublicId());

            if (optionalRejectionsCode.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Rejection code not found")
                        .userMessage("Rejection code not found");
            }

            RejectionsCode rejectionsCode = optionalRejectionsCode.get();

            ReturnedProductItem product = new ReturnedProductItem();
            product.setProductName(productItem.getProductName());
            product.setQuantityReturned(goodsReturnNoteItemRequest.getQuantity());
            product.setProductCode(productItem.getProductCode());
            product.setRejectionCode(rejectionsCode.getCode());
            product.setProductPublicId(productItem.getProductPublicId());
            product.setQuantityRequested(productItem.getQuantityRequested());
            product.setCostPrice(productItem.getPurchasePrice());
            product.setTotalValue(calculateTotalValue(productItem.getPurchasePrice(), goodsReturnNoteItemRequest.getQuantity()));
            ReturnedProductItem saved = returnedProductItemRepository.save(product);
            productItems.add(saved);
        });

        goodsReturnNote.setGoodsReturnNoteDate(goodsReturnNoteRequest.getGoodsReturnDate());
        person.ifPresent(goodsReturnNote::setCreatedBy);
        goodsReturnNote.setPurchaseOrderNumber(purchaseOrder.getPurchaseOrderNumber());
        goodsReturnNote.setSupplier(purchaseOrder.getSupplier());
        goodsReturnNote.setGoodsReturnNoteDate(LocalDate.now());
        goodsReturnNote.setStatus(GoodsReturnStatus.PENDING);
        goodsReturnNote.setGoodsReturnNoteNumber("GRTN" + counterService.getNextCounter(CounterType.GOODS_RETURN_NOTE));
        goodsReturnNote.setGoodsReceivedNote(goodsReceivedNote);
        GoodsReturnDeliveryNote savedGoodsReturnDeliveryNote = goodsReturnDeliveryNoteRepository.save(goodsReturnNote);

        for (ReturnedProductItem productItem : productItems) {
            productItem.setGoodsReturnDeliveryNote(savedGoodsReturnDeliveryNote);
            returnedProductItemRepository.save(productItem);
        }

        updateLiveStock(warehouse.getPublicId(), savedGoodsReturnDeliveryNote);

        if (!productItems.isEmpty()) {
            for (ReturnedProductItem returnedProductItem : productItems) {
                Optional<Product> optionalProduct = productRepository.findByPublicIdAndDeletedIsFalse(returnedProductItem.getProductPublicId());

                optionalProduct.ifPresent(product -> productItemMap.put(product, returnedProductItem));
            }
        }

        stockUpdateHelper.updateProductStockWithReturnGoods(productItemMap, warehouse, InventoryEvent.GRTN.name());

        String description =
                "Goods return note created for purchase order number " + purchaseOrder.getPurchaseOrderNumber() + " and return quantity " + savedGoodsReturnDeliveryNote
                        .getReturnedProductItems()
                        .size();

        eventsRepository.save(new Events(savedGoodsReturnDeliveryNote.getPublicId().toString(), "GOODS RETURN " +
                "CREATED", LocalDateTime.now(), description, "CREATED", person.orElse(null)));


        // record stock movement

        productItemMap.forEach((product, returnedProductItem) -> {
            stockMovementHelper.recordStockMovement(product, (int) returnedProductItem.getQuantityReturned(),
                    StockMovementTransactionType.GRTN, TransactionBoundType.OUTGOING,
                    person.map(Person::getFullName).orElse("System"), warehouse
            );
        });

        procurementService.createReplacementPurchaseOrder(savedGoodsReturnDeliveryNote.getPublicId(), name);
    }

    private BigDecimal calculateTotalValue(BigDecimal purchasePrice, Long quantity) {
        if (purchasePrice == null || quantity == null) {
            return BigDecimal.ZERO;
        }
        return purchasePrice.multiply(BigDecimal.valueOf(quantity));
    }


    @Override
    public Page<GoodsReturnDeliveryNote> listReturnedGoodsNotes(GoodsReturnStatus status,
                                                                String search,
                                                                String purchaseOrderNumber,
                                                                UUID supplierPublicId,
                                                                LocalDate to,
                                                                LocalDate from,
                                                                GoodsNoteType goodsNoteType,
                                                                Pageable pageable) {

        Specification<GoodsReturnDeliveryNote> spec = Specification
                .where(GoodsReturnDeliveryNoteSpecification.isNotDeleted())
                .and(GoodsReturnDeliveryNoteSpecification.hasSupplier(supplierPublicId))
                .and(GoodsReturnDeliveryNoteSpecification.hasStatus(status))
                .and(GoodsReturnDeliveryNoteSpecification.hasSearchTerm(search))
                .and(GoodsReturnDeliveryNoteSpecification.hasDateRange(from, to))
                .and(GoodsReturnDeliveryNoteSpecification.hasPurchaseOrder(purchaseOrderNumber));

        return goodsReturnDeliveryNoteRepository.findAll(spec, pageable);
    }

    @Override
    public Page<ReturnedProductItem> getReturnedProductItems(String search, UUID goodsReturnedNotePublicId, Pageable pageable) {

        GoodsReturnDeliveryNote goodsReturnDeliveryNote =
                goodsReturnDeliveryNoteRepository.findByPublicIdAndDeletedIsFalse(goodsReturnedNotePublicId).orElse(null);

        if (goodsReturnDeliveryNote == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Goods Return DeliveryNote not found")
                    .userMessage("Goods Return DeliveryNote not found");
        }

        return returnedProductItemRepository.findByGoodsReturnDeliveryNoteAndDeletedIsFalse(goodsReturnDeliveryNote, pageable);
    }

//    private ReceivedProductItem createRec(PromoItemRequest item, Product saveProduct, PromoType promoType) {
//
//        ProductItem product = new ProductItem();
//        product.setProductName(saveProduct.getProductName());
//        product.setQuantityReceived(item.getQuantityReceived());
//        product.setProductCode(saveProduct.getSku());
//        product.setProductPublicId(saveProduct.getPublicId());
//        product.setPromoType(promoType);
//        return productItemRepository.save(product);
//    }

    private ReceivedProductItem createReceiveProductItem(PromoItemRequest item, Product product, PromoType promoType, GoodsReceivedNote goodsReceivedNote) {

        ReceivedProductItem receivedProductItem = new ReceivedProductItem();
        receivedProductItem.setProductName(product.getProductName());
        receivedProductItem.setQuantityReceived(item.getQuantityReceived());
        receivedProductItem.setProductCode(product.getSku());
        receivedProductItem.setProductPublicId(product.getPublicId());
        receivedProductItem.setPromoType(promoType);
        receivedProductItem.setGoodsReceivedNote(goodsReceivedNote);
//        receivedProductItem.setProductSellingPrice(product.getSellPrice());
        receivedProductItem.setPurchasePrice(product.getBuyingPrice());
        return receivedProductItemRepository.save(receivedProductItem);
    }

    private void updateLiveStock(UUID wareHousePublicId, GoodsReturnDeliveryNote goodsReturnDeliveryNote) {
        List<ReturnedProductItem> returnedProductItemsList =
                returnedProductItemRepository.findByGoodsReturnDeliveryNote(goodsReturnDeliveryNote);
        returnedProductItemsList.forEach(returnedProductItem -> {
            liveStockHelper.updateLiveStockValue(wareHousePublicId, returnedProductItem.getProductPublicId(),
                    (int) returnedProductItem.getQuantityReturned(), ProductUpdateType.GOODS_RETURNED_STOCK, LocalDate.now());
        });
    }

    private void updateLiveStockWithBonuses(Warehouse warehouse, ReceivedProductItem receivedProductItem) {
        liveStockHelper.updateLiveStockValue(warehouse.getPublicId(), receivedProductItem.getProductPublicId(),
                (int) receivedProductItem.getQuantityReceived(), ProductUpdateType.BONUS_STOCK, LocalDate.now());
    }
}
