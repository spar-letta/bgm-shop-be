package com.app.itibari.services;

import com.app.itibari.dataTypes.*;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.*;
import com.app.itibari.repository.*;
import com.app.itibari.services.helper.*;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.helpers.ProductStockHelper;
import com.app.itibari.utils.helpers.PurchaseOrderUpdateHelper;
import com.app.itibari.utils.helpers.Validators;
import com.app.itibari.utils.specification.CreditNoteSpecification;
import com.app.itibari.utils.specification.PurchaseOrderSpecification;
import com.app.itibari.utils.specification.SupplierSpecification;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcurementServiceImpl implements ProcurementService {

    private static final Set<String> NON_TAXABLE_CODES = Set.of("Zero Rate", "Exempt");
    private static final int SCALE = 2;

    private final CounterService counterService;

    private final ProductRepository productRepository;

    private final PurchaseOrderRepository purchaseOrderRepository;

    private final PurchaseOrderItemRepository purchaseOrderItemRepository;

    private final EventsRepository eventsRepository;

    private final GoodsReturnRepository goodsReturnRepository;

    private final SupplierRepository supplierRepository;

    private final PersonRepository personRepository;

    private final ProductItemRepository productItemRepository;

    private final GoodsReceivedNoteRepository goodsReceivedNoteRepository;

    private final ReceivedProductItemRepository receivedProductItemRepository;

    private final WarehouseRepository warehouseRepository;

    private final TaxCalculatorHelper taxCalculatorHelper;

    private final NotificationHelper notificationHelper;

    private final LiveStockHelper liveStockHelper;

    private final AvcoHelper avcoHelper;
    private final RemittanceRepository remittanceRepository;
    private final RejectionReasonRepository rejectionReasonRepository;

    private final StockUpdateHelper stockUpdateHelper;
    private final ReturnedProductItemRepository returnedProductItemRepository;
    private final GoodsReturnDeliveryNoteRepository goodsReturnDeliveryNoteRepository;
    private final StockMovementHelper stockMovementHelper;
    private final ProductStockHelper productStockHelper;
    private final PurchaseOrderUpdateHelper purchaseOrderUpdateHelper;
    private final PriceManagementTrackerRepository priceManagementTrackerRepository;
    private final RejectionCodeRepository rejectionCodeRepository;
    private final CreditNoteRepository creditNoteRepository;
    private final FileService fileService;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Suppliers createSupplier(SupplierRequest supplierRequest, String name) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);

        if (supplierRequest.getSupplierName() == null || supplierRequest.getSupplierName().isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Supplier name is required")
                    .userMessage("Supplier name is required");
        }
        //Optional<Suppliers> supplierOptional = supplierRepository.findBySupplierNameAndDeletedIsFalse(supplierRequest.getSupplierName());
        Optional<Suppliers> supplierOptional = supplierRepository.findBySupplierNameIgnoreCaseAndDeletedIsFalse(supplierRequest.getSupplierName());

        if (supplierOptional.isPresent()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Supplier already exists")
                    .userMessage("Supplier already exists");
        }

        Suppliers suppliers = new Suppliers();
        suppliers.setSupplierName(supplierRequest.getSupplierName());
        suppliers.setSupplierAddress(supplierRequest.getSupplierAddress());
        suppliers.setSupplierPointOfContact(supplierRequest.getSupplierPointOfContact());
        suppliers.setPhoneNumber(supplierRequest.getPhoneNumber());
        suppliers.setEmail(supplierRequest.getEmail());
        suppliers.setLeadDays(supplierRequest.getLeadDays());
        suppliers.setCreditTerms(supplierRequest.getCreditTerms());
        personOptional.ifPresent(suppliers::setCreatedBy);
        suppliers.setTinNumber(supplierRequest.getTinNumber());
        suppliers.setCreationTime(LocalDateTime.now());
        suppliers.setCreditLimit(supplierRequest.getCreditLimit());
        suppliers.setBankName(supplierRequest.getBankName());
        suppliers.setBankAccountName(supplierRequest.getBankAccountName());
        suppliers.setBankAccountNumber(supplierRequest.getBankAccountNumber());
        suppliers.setPostalAddress(supplierRequest.getPostalAddress());

        return supplierRepository.save(suppliers);
    }

    @Override
    public void updateSupplier(UUID supplierId, SupplierRequest supplierRequest, String name) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
        Optional<Suppliers> supplierOptional = supplierRepository.findByPublicIdAndDeletedIsFalse(supplierId);

        if (supplierOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Supplier not found")
                    .userMessage("Supplier not found");
        }

        Suppliers suppliers = supplierOptional.get();
        suppliers.setSupplierName(supplierRequest.getSupplierName());
        suppliers.setSupplierAddress(supplierRequest.getSupplierAddress());
        suppliers.setSupplierPointOfContact(supplierRequest.getSupplierPointOfContact());
        suppliers.setPhoneNumber(supplierRequest.getPhoneNumber());
        suppliers.setEmail(supplierRequest.getEmail());
        suppliers.setLeadDays(supplierRequest.getLeadDays());
        suppliers.setCreditTerms(supplierRequest.getCreditTerms());
        personOptional.ifPresent(suppliers::setUpdatedBy);
        suppliers.setUpdateDate(LocalDateTime.now());
        suppliers.setTinNumber(supplierRequest.getTinNumber());
        suppliers.setCreditLimit(supplierRequest.getCreditLimit());

        supplierRepository.save(suppliers);
    }

    @Override
    public Suppliers getSupplier(UUID supplierPublicId) {

        return supplierRepository.findByPublicIdAndDeletedIsFalse(supplierPublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Supplier not found")
                        .userMessage("Supplier not found"));
    }

    @Override
    public Page<Suppliers> getSuppliers(String searchParam,
                                        SupplierStatus supplierStatus,
                                        Integer maxOrder,
                                        Integer minOrder,
                                        Pageable pageable) {

        Specification<Suppliers> specification = Specification
                .where(SupplierSpecification.searchParamMatches(searchParam))
                .and(SupplierSpecification.hasStatus(supplierStatus))
                .and(SupplierSpecification.isNotDeleted());
        return supplierRepository.findAll(specification, pageable);
    }

    @Override
    public void deleteSupplier(UUID supplierPublicId, String loggedInUser) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(loggedInUser);
        Optional<Suppliers> supplierOptional = supplierRepository.findByPublicIdAndDeletedIsFalse(supplierPublicId);

        if (supplierOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Supplier not found")
                    .userMessage("Supplier not found");
        }

        Suppliers suppliers = supplierOptional.get();
        personOptional.ifPresent(suppliers::setUpdatedBy);
        suppliers.setDeleted(true);
        suppliers.setUpdateDate(LocalDateTime.now());

        supplierRepository.save(suppliers);
    }

    @Override
    public void updateSupplierActions(UUID supplierPublicId, SupplierActionRequest supplierActionRequest, String name) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);

        Optional<Suppliers> supplierOptional = supplierRepository.findByPublicIdAndDeletedIsFalse(supplierPublicId);

        if (supplierOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Supplier not found")
                    .userMessage("Supplier not found");
        }

        Suppliers suppliers = supplierOptional.get();

        if (supplierActionRequest.getProductPublicIds().isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Products are required")
                    .userMessage("Products are required");
        }

        List<Product> products = new ArrayList<>();

        List<UUID> notFoundProducts = new ArrayList<>();

        supplierActionRequest.getProductPublicIds().forEach(productPublicId -> {
            Optional<Product> optionalProduct = productRepository.findByPublicIdAndDeletedIsFalse(productPublicId);
            if (optionalProduct.isPresent()) {
                products.add(optionalProduct.get());
            } else {
                notFoundProducts.add(productPublicId);
            }
        });

        if (!notFoundProducts.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Products not found")
                    .userMessage(String.format("Products with public ids %s not found", notFoundProducts));
        }

        if (supplierActionRequest.getActionType().equals(ActionType.ADD)) {
            products.forEach(product -> {
                product.addSupplier(suppliers);
                personOptional.ifPresent(product::setUpdatedBy);
                product.setUpdateDate(LocalDateTime.now());
                productRepository.save(product);
            });
        } else if (supplierActionRequest.getActionType().equals(ActionType.REMOVE)) {
            products.forEach(product -> {
                product.removeSupplier(suppliers);
                personOptional.ifPresent(product::setUpdatedBy);
                product.setUpdateDate(LocalDateTime.now());
                productRepository.save(product);
            });
        }

        suppliers.setUpdateDate(LocalDateTime.now());
        personOptional.ifPresent(suppliers::setUpdatedBy);
        supplierRepository.save(suppliers);
    }

    //purchase order methods
    @Override
    public PurchaseOrder createPurchaseOrder(PurchaseOrderRequest purchaseOrderRequest, String name) {

        Suppliers suppliers = getSupplier(purchaseOrderRequest.getSupplierId());

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);

        if (purchaseOrderRequest.getPurchaseOrderItems().isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Purchase order items are required")
                    .userMessage("Sorry, missing items");
        }

        if (purchaseOrderRequest.getWarehouseId() == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Warehouse is required")
                    .userMessage("Sorry, Warehouse is required");
        }

        Optional<Warehouse> warehouseOptional = warehouseRepository.findByPublicIdAndDeletedIsFalse(purchaseOrderRequest.getWarehouseId());
        if (warehouseOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Warehouse not found")
                    .userMessage("Warehouse not found");
        }

        Warehouse warehouse = warehouseOptional.get();

        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setSupplier(suppliers);
        purchaseOrder.setPurchaseOrderDate(purchaseOrderRequest.getOrderDate());
        purchaseOrder.setNotes(purchaseOrderRequest.getComment());
        purchaseOrder.setStatus(PurchaseOrderStatus.PENDING);
        purchaseOrder.setPurchaseOrderNumber("PO-" + counterService.getNextCounter(CounterType.PURCHASE_ORDER));
        personOptional.ifPresent(purchaseOrder::setCreatedBy);
        purchaseOrder.setCreationTime(LocalDateTime.now());
        purchaseOrder.setProformaInvoiceNumber(purchaseOrderRequest.getProformaInvoiceNumber());
        purchaseOrder.setWarehouseId(warehouse.getPublicId().toString());
        purchaseOrder.setWarehouseName(warehouse.getWarehouseName());
        purchaseOrder.setRebate(purchaseOrderRequest.isRebate());

        PurchaseOrder savedPurchaseOrder = purchaseOrderRepository.save(purchaseOrder);

        purchaseOrderRequest.getPurchaseOrderItems().forEach(purchaseOrderItemRequest -> {
            Optional<Product> optionalProduct = productRepository.findByPublicIdAndDeletedIsFalse(purchaseOrderItemRequest.getProductPublicId());

            if (optionalProduct.isPresent()) {
                Product product = optionalProduct.get();
                PurchaseOrderItem purchaseOrderItem = new PurchaseOrderItem();
                purchaseOrderItem.setProduct(product);
                purchaseOrderItem.setQuantity(purchaseOrderItemRequest.getQuantity());
                purchaseOrderItem.setUnitPrice(purchaseOrderItemRequest.getUnitPrice());
                purchaseOrderItem.setPurchaseOrder(savedPurchaseOrder);

                // 4decimal precision for monetary values
                BigDecimal totalAmount = purchaseOrderItem
                        .getUnitPrice()
                        .multiply(BigDecimal.valueOf(purchaseOrderItem.getQuantity()))
                        .setScale(4, RoundingMode.HALF_UP);

                calculateAmountWithTax(purchaseOrderItemRequest.getUnitPrice(), product);

                BigDecimal taxAmount = Optional.ofNullable(product.getBuyingTaxAmount()).orElse(BigDecimal.ZERO);
                BigDecimal taxCodeAmount = taxAmount.multiply(BigDecimal.valueOf(purchaseOrderItem.getQuantity()));
                BigDecimal amountAfterTax = totalAmount.subtract(taxCodeAmount);

                purchaseOrderItem.setTaxAmount(taxCodeAmount);
                purchaseOrderItem.setAmountAfterTax(amountAfterTax);

                PurchaseOrderItem savedPurchaseOrderItem = purchaseOrderItemRepository.save(purchaseOrderItem);
                savedPurchaseOrder.addPurchaseOrderItem(savedPurchaseOrderItem);
            }
        });

        eventsRepository.save(new Events(savedPurchaseOrder.getPublicId().toString(),
                "Purchase Order Created",
                LocalDateTime.now(),
                String.format(" Purchase order %s creation on %s to supplier %s with order number %s",
                        savedPurchaseOrder.getPurchaseOrderNumber(),
                        LocalDate.now(),
                        suppliers.getSupplierName(),
                        savedPurchaseOrder.getProformaInvoiceNumber()),
                "CREATE",
                personOptional.orElse(null)));

        return purchaseOrderRepository.save(savedPurchaseOrder);
    }

    public void calculateAmountWithTax(BigDecimal buyingPrice, Product savedProduct) {
        if (buyingPrice == null) {
            log.warn("Buying price cannot be null for product {}", savedProduct.getPublicId());
            return;
        }

        BigDecimal amountBeforeTax;
        BigDecimal taxAmount;

        if (savedProduct.getTaxCode() != null &&
                NON_TAXABLE_CODES.contains(savedProduct.getTaxCode().getCodeName())) {

            amountBeforeTax = buyingPrice;
            taxAmount = BigDecimal.ZERO;

        } else {
            BigDecimal taxRate = savedProduct.getTaxCode() != null
                    ? savedProduct.getTaxCode().getCodeValue()
                    : BigDecimal.ZERO;

            BigDecimal taxValue = taxRate.add(BigDecimal.ONE);
            amountBeforeTax = buyingPrice.divide(taxValue, SCALE, RoundingMode.HALF_UP);
            taxAmount = buyingPrice.subtract(amountBeforeTax);
        }

        savedProduct.setBuyingPriceBeforeTax(amountBeforeTax);
        savedProduct.setBuyingTaxAmount(taxAmount);

        log.debug("Product {}: sellPrice={}, taxRate={}, beforeTax={}, taxAmount={}",
                savedProduct.getPublicId(), buyingPrice,
                savedProduct.getTaxCode() != null ? savedProduct.getTaxCode().getCodeValue() : BigDecimal.ZERO,
                amountBeforeTax, taxAmount);

        productRepository.save(savedProduct);
    }

    @Override
    @Transactional
    public void updatePurchaseOrder(UUID purchaseOrderPublicId, PurchaseOrderRequest request, String email) {
        Person person = personRepository.findByEmailAndDeletedIsFalse(email)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Person not found")
                        .userMessage("User not found"));

        PurchaseOrder purchaseOrder = purchaseOrderRepository.findByPublicIdAndDeletedIsFalse(purchaseOrderPublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Purchase order not found")
                        .userMessage("Purchase order not found"));

        Warehouse warehouseOptional =
                warehouseRepository.findByPublicIdAndDeletedIsFalse(request.getWarehouseId()).orElse(null);
        if (warehouseOptional == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Warehouse not found")
                    .userMessage("Warehouse not found");
        }

        if (request.getReasonCodePublicId() == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Select Rejection Code")
                    .userMessage("Sorry, Select Rejection Code");
        }

        RejectionsCode rejectionsCode = rejectionCodeRepository.findRejectionsCodeByPublicIdAndDeletedIsFalse(request.getReasonCodePublicId()).orElse(null);
        if (rejectionsCode == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Rejection Code not found")
                    .userMessage("Sorry, Rejection Code not found");
        }

        Suppliers suppliers = getSupplier(request.getSupplierId());

        purchaseOrderUpdateHelper.validateUpdate(purchaseOrder);

        purchaseOrder.updateFromRequest(request, person, suppliers, warehouseOptional, rejectionsCode);

        if (purchaseOrder.getStatus() == PurchaseOrderStatus.ACCOUNTANT_APPROVED) {
            purchaseOrder.setStatus(PurchaseOrderStatus.AMIN_APPROVED);
            cancelRemittance(purchaseOrder);
        }

        PurchaseOrder savedPurchaseOrder = purchaseOrderRepository.save(purchaseOrder);
        replaceItem(request.getPurchaseOrderItems(), savedPurchaseOrder);

        eventsRepository.save(new Events(savedPurchaseOrder.getPublicId().toString(),
                "Purchase Order Updated",
                LocalDateTime.now(),
                String.format(" Purchase order %s updated on %s to supplier %s , %s",
                        savedPurchaseOrder.getPurchaseOrderNumber(),
                        LocalDate.now(),
                        suppliers.getSupplierName(),
                        request.getComment()),
                "UPDATE",
                person));
    }

    private void replaceItem(List<PurchaseOrderItemRequest> purchaseOrderItems, PurchaseOrder purchaseOrder) {
        purchaseOrder.getPurchaseOrderItems().clear();

        purchaseOrderItems.forEach(purchaseOrderItemRequest -> {
            Optional<Product> optionalProduct = productRepository.findByPublicIdAndDeletedIsFalse(purchaseOrderItemRequest.getProductPublicId());

            if (optionalProduct.isPresent()) {
                Product product = optionalProduct.get();

                PurchaseOrderItem foundItem = purchaseOrderItemRepository.findByProductAndPurchaseOrderAndDeletedIsFalse(product,
                        purchaseOrder);
                if (foundItem == null) {
                    PurchaseOrderItem purchaseOrderItem = new PurchaseOrderItem();
                    purchaseOrderItem.setProduct(product);
                    purchaseOrderItem.setQuantity(purchaseOrderItemRequest.getQuantity());
                    purchaseOrderItem.setUnitPrice(purchaseOrderItemRequest.getUnitPrice());
                    purchaseOrderItem.setPurchaseOrder(purchaseOrder);

                    BigDecimal grossTotal = purchaseOrderItem
                            .getUnitPrice()
                            .multiply(BigDecimal.valueOf(purchaseOrderItem.getQuantity()));

                    if (product.getTaxCode() == null) {
                        throw new BadRequestRestApiException()
                                .developerMessage("Missing tax code")
                                .userMessage("Sorry, missing tax code");
                    }

                    if (NON_TAXABLE_CODES.contains(product.getTaxCode().getCodeName())) {
                        purchaseOrderItem.setTaxAmount(BigDecimal.ZERO);
                        purchaseOrderItem.setAmountAfterTax(grossTotal);
                    } else {
                        BigDecimal taxRate = product.getTaxCode().getCodeValue();
                        TaxResult result = calculateTax(purchaseOrderItem.getUnitPrice(),
                                purchaseOrderItem.getQuantity(),
                                taxRate);

                        purchaseOrderItem.setTaxAmount(result.taxAmount());
                        purchaseOrderItem.setAmountAfterTax(result.amountAfterTax());
                    }

                    purchaseOrderItemRepository.save(purchaseOrderItem);
                }
            }
        });
        purchaseOrderRepository.save(purchaseOrder);
    }


    @Override
    public Page<PurchaseOrder> getAllPurchaseOrders(String searchParam,
                                                    LocalDate fromDate,
                                                    LocalDate toDate,
                                                    UUID supplierId,
                                                    String orderNumber,
                                                    PurchaseOrderStatus orderStatus,
                                                    List<PurchaseOrderStatus> statuses,
                                                    Boolean rebate,
                                                    Boolean replacement,
                                                    Pageable pageable) {

        Specification<PurchaseOrder> specification = Specification
                .where(PurchaseOrderSpecification.searchParamMatches(searchParam))
                .and(PurchaseOrderSpecification.hasOrderNumber(orderNumber))
                .and(PurchaseOrderSpecification.hasStatus(orderStatus))
                .and(PurchaseOrderSpecification.hasSupplier(supplierId))
                .and(PurchaseOrderSpecification.isNotDeleted())
                .and(PurchaseOrderSpecification.hasStatuses(statuses))
                .and(PurchaseOrderSpecification.isRebate(rebate))
                .and(PurchaseOrderSpecification.isReplacement(replacement))
                .and(PurchaseOrderSpecification.hasFromToDates(fromDate, toDate));

        return purchaseOrderRepository.findAll(specification, pageable);
    }

    @Transactional
    @Override
    public void updatePurchaseOrderActions(UUID purchaseOrderPublicId, PurchaseOrderUpdateRequest purchaseOrderUpdateRequest, String name) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
        Optional<PurchaseOrder> purchaseOrderOptional = purchaseOrderRepository.findByPublicIdAndDeletedIsFalse(purchaseOrderPublicId);

        if (purchaseOrderOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Purchase order not found")
                    .userMessage("Sorry, Purchase order not found");
        }

        PurchaseOrder purchaseOrder = purchaseOrderOptional.get();

        String action = purchaseOrderUpdateRequest.getPurchaseOrderAction().name();

        String description = "";

        String event = "Purchase Order " + purchaseOrderUpdateRequest.getPurchaseOrderAction().name();

        if (purchaseOrderUpdateRequest.getPurchaseOrderAction().equals(PurchaseOrderAction.ADD_ITEM)) {
            if (!purchaseOrder.getStatus().equals(PurchaseOrderStatus.PENDING)) {
                throw new BadRequestRestApiException()
                        .developerMessage("Purchase order is already approved")
                        .userMessage("Purchase order is already processed");
            }
            if (purchaseOrderUpdateRequest.getPurchaseOrderItems().isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Purchase order items are required")
                        .userMessage("Purchase order items are required");
            }
            if (purchaseOrder.isReplacementPurchaseOrder()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Replacement purchase order cannot be updated")
                        .userMessage("Replacement purchase order cannot be updated");
            }

            purchaseOrderUpdateRequest.getPurchaseOrderItems().forEach(purchaseOrderItemRequest -> {
                Optional<Product> optionalProduct = productRepository.findByPublicIdAndDeletedIsFalse(purchaseOrderItemRequest.getProductPublicId());

                if (optionalProduct.isPresent()) {
                    Product product = optionalProduct.get();

                    PurchaseOrderItem foundItem = purchaseOrderItemRepository.findByProductAndPurchaseOrderAndDeletedIsFalse(product,
                            purchaseOrder);
                    if (foundItem == null) {
                        PurchaseOrderItem purchaseOrderItem = new PurchaseOrderItem();
                        purchaseOrderItem.setProduct(product);
                        purchaseOrderItem.setQuantity(purchaseOrderItemRequest.getQuantity());
                        purchaseOrderItem.setUnitPrice(purchaseOrderItemRequest.getUnitPrice());
                        purchaseOrderItem.setPurchaseOrder(purchaseOrder);

                        BigDecimal grossTotal = purchaseOrderItem
                                .getUnitPrice()
                                .multiply(BigDecimal.valueOf(purchaseOrderItem.getQuantity()));

                        if (product.getTaxCode() == null) {
                            throw new BadRequestRestApiException()
                                    .developerMessage("Missing tax code")
                                    .userMessage("Sorry, missing tax code");
                        }

                        if (NON_TAXABLE_CODES.contains(product.getTaxCode().getCodeName())) {
                            purchaseOrderItem.setTaxAmount(BigDecimal.ZERO);
                            purchaseOrderItem.setAmountAfterTax(grossTotal);
                        } else {
                            BigDecimal taxRate = product.getTaxCode().getCodeValue();
                            TaxResult result = calculateTax(purchaseOrderItem.getUnitPrice(),
                                    purchaseOrderItem.getQuantity(),
                                    taxRate);

                            purchaseOrderItem.setTaxAmount(result.taxAmount());
                            purchaseOrderItem.setAmountAfterTax(result.amountAfterTax());
                        }

                        purchaseOrderItemRepository.save(purchaseOrderItem);
                    }
                }
            });

            description = String.format(" Purchase order %s items Added on %s with order number %s",
                    purchaseOrder.getPurchaseOrderNumber(),
                    LocalDate.now(),
                    purchaseOrder.getProformaInvoiceNumber());
        } else if (purchaseOrderUpdateRequest.getPurchaseOrderAction().equals(PurchaseOrderAction.REMOVE_ITEM)) {
            if (purchaseOrder.isReplacementPurchaseOrder()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Replacement purchase order cannot be updated")
                        .userMessage("Replacement purchase order cannot be updated");
            }
            if (!purchaseOrder.getStatus().equals(PurchaseOrderStatus.PENDING)) {
                throw new BadRequestRestApiException()
                        .developerMessage("Purchase order is already approved")
                        .userMessage("Purchase order is already processed");
            }
            if (purchaseOrderUpdateRequest.getPurchaseOrderItems().isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Purchase order items are required")
                        .userMessage("Purchase order items are required");
            }
            purchaseOrderUpdateRequest.getPurchaseOrderItems().forEach(purchaseOrderItemRequest -> {
                Optional<Product> optionalProduct = productRepository.findByPublicIdAndDeletedIsFalse(purchaseOrderItemRequest.getProductPublicId());

                if (optionalProduct.isPresent()) {
                    Product product = optionalProduct.get();
                    PurchaseOrderItem savedPurchaseOrderItem = purchaseOrderItemRepository.findByProductAndPurchaseOrderAndDeletedIsFalse(
                            product,
                            purchaseOrder);

                    if (savedPurchaseOrderItem != null) {
                        purchaseOrder.removePurchaseOrderItem(savedPurchaseOrderItem);
                        savedPurchaseOrderItem.setDeleted(true);
                        savedPurchaseOrderItem.setUpdateDate(LocalDateTime.now());
                        savedPurchaseOrderItem.setUpdatedBy(personOptional.orElse(null));
                        purchaseOrderItemRepository.save(savedPurchaseOrderItem);
                    }
                }
            });

            description = String.format(" Purchase order %s items Removed on %s with order number %s",
                    purchaseOrder.getPurchaseOrderNumber(),
                    LocalDate.now(),
                    purchaseOrder.getProformaInvoiceNumber());
        } else if (purchaseOrderUpdateRequest.getPurchaseOrderAction().equals(PurchaseOrderAction.PROCESS)) {

            purchaseOrder.setStatus(PurchaseOrderStatus.SUBMITTED);
            personOptional.ifPresent(purchaseOrder::setUpdatedBy);
            purchaseOrder.setUpdateDate(LocalDateTime.now());
            PurchaseOrder savedPurchaseOrder = purchaseOrderRepository.save(purchaseOrder);

            description = String.format(" Purchase order %s Submitted on %s with order number %s",
                    savedPurchaseOrder.getPurchaseOrderNumber(),
                    LocalDate.now(),
                    savedPurchaseOrder.getProformaInvoiceNumber());
            Person person = personOptional.orElse(null);

            String message = String.format(" Purchase order %s Submitted on %s with order number %s by %s for approval",
                    savedPurchaseOrder.getPurchaseOrderNumber(),
                    LocalDate.now(),
                    savedPurchaseOrder.getProformaInvoiceNumber(),
                    person != null ? person.getFullName() : "Itibari");
            String title = String.format("P.O %s Submitted for approval", savedPurchaseOrder.getPurchaseOrderNumber());
            // notificationHelper.sendNotificationSms(message, "ROLE_FINANCE_MANAGER");
            notificationHelper.createNotificationHelper(
                    title,
                    message,
                    "ROLE_FINANCE_MANAGER",
                    savedPurchaseOrder.getPublicId(),
                    "PURCHASE_ORDER",
                    "FINANCE_APPROVE_PURCHASE_ORDER");

        } else if (purchaseOrderUpdateRequest.getPurchaseOrderAction().equals(PurchaseOrderAction.DISPATCH)) {

            if (!purchaseOrder.getStatus().equals(PurchaseOrderStatus.ACCOUNTANT_APPROVED)) {
                throw new BadRequestRestApiException()
                        .developerMessage("Purchase order is not accountant approved")
                        .userMessage("Sorry. Purchase order is not accountant approved");
            }
            purchaseOrder.setStatus(PurchaseOrderStatus.DISPATCHED);
            personOptional.ifPresent(purchaseOrder::setUpdatedBy);
            purchaseOrder.preUpdate();

            description = String.format(" Purchase order %s Dispatched on %s with order number %s",
                    purchaseOrder.getPurchaseOrderNumber(),
                    LocalDate.now(),
                    purchaseOrder.getProformaInvoiceNumber());
            Person person = personOptional.orElse(null);
            String message = String.format(" Purchase order %s Dispatched on %s with order number %s by %s to %s",
                    purchaseOrder.getPurchaseOrderNumber(),
                    LocalDate.now(),
                    purchaseOrder.getProformaInvoiceNumber(),
                    person != null ? person.getFullName() : "Itibari",
                    purchaseOrder.getSupplier().getSupplierName());
            String title = String.format("P.O %s Dispatched", purchaseOrder.getPurchaseOrderNumber());
            //notificationHelper.sendNotificationSms(message, "ROLE_ADMIN");
            //notificationHelper.createNotificationHelper(title, message, "ROLE_ADMIN");
            runDispatchEventOnLiveStock(purchaseOrder, purchaseOrder.getStatus(), null);
        }
        if (purchaseOrderUpdateRequest.getPurchaseOrderAction().equals(PurchaseOrderAction.CANCEL)) {
            if (!PurchaseOrderStatus.inThePurchaseOrderStatus().contains(purchaseOrder.getStatus())) {
                throw new BadRequestRestApiException()
                        .developerMessage("Operation not allowed on purchase order with status %s",
                                purchaseOrder.getStatus().name().toLowerCase())
                        .userMessage("Sorry, operation not allowed on purchase order with status %s",
                                purchaseOrder.getStatus().name().toLowerCase());
            }

            if (purchaseOrderUpdateRequest.getReasonCodePublicId() == null) {
                throw new BadRequestRestApiException()
                        .developerMessage("Select cancel reason code")
                        .userMessage("Sorry, Select cancel reason code");
            }

            RejectionsCode rejectionsCode = rejectionCodeRepository.findRejectionsCodeByPublicIdAndDeletedIsFalse(purchaseOrderUpdateRequest.getReasonCodePublicId()).orElse(null);
            if (rejectionsCode == null) {
                throw new BadRequestRestApiException()
                        .developerMessage("Rejection Code not found")
                        .userMessage("Sorry, Rejection Code not found");
            }

            if (purchaseOrder.getStatus().equals(PurchaseOrderStatus.ACCOUNTANT_APPROVED) || purchaseOrder.getStatus().equals(PurchaseOrderStatus.DISPATCHED)) {

                purchaseOrder.setPreviousStatus(purchaseOrder.getStatus());
                purchaseOrder.setStatus(PurchaseOrderStatus.PENDING_CANCEL);
                personOptional.ifPresent(purchaseOrder::setUpdatedBy);
                purchaseOrder.setUpdateDate(LocalDateTime.now());
                purchaseOrder.getRejectionsCodes().add(rejectionsCode);
                purchaseOrderRepository.save(purchaseOrder);

                description = String.format(" Purchase order %s - %s on %s",
                        purchaseOrder.getPurchaseOrderNumber(),
                        purchaseOrder.getStatus().name(),
                        LocalDate.now());
            } else {
                purchaseOrder.setStatus(PurchaseOrderStatus.CANCELLED);
                personOptional.ifPresent(purchaseOrder::setUpdatedBy);
                purchaseOrder.setUpdateDate(LocalDateTime.now());
                purchaseOrder.getRejectionsCodes().add(rejectionsCode);
                purchaseOrderRepository.save(purchaseOrder);

                description = String.format(" Purchase order %s - %s on %s",
                        purchaseOrder.getPurchaseOrderNumber(),
                        purchaseOrder.getStatus().name(),
                        LocalDate.now());
            }
        }
        if (purchaseOrderUpdateRequest.getPurchaseOrderAction().equals(PurchaseOrderAction.CANCELLATION_APPROVAL)) {


            if (purchaseOrderUpdateRequest.getCancelReason() == null || purchaseOrderUpdateRequest.getCancelReason().isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Rejection reason not found")
                        .userMessage("Sorry, Rejection reason not found");
            }

            saveReason(purchaseOrderUpdateRequest, personOptional, purchaseOrder);

            purchaseOrder.setStatus(PurchaseOrderStatus.CANCELLED);
            personOptional.ifPresent(purchaseOrder::setUpdatedBy);
            purchaseOrder.setUpdateDate(LocalDateTime.now());
            PurchaseOrder savePurchaseOrder = purchaseOrderRepository.save(purchaseOrder);

            cancelRemittance(savePurchaseOrder);
            description = String.format(" Purchase order %s - %s on %s",
                    purchaseOrder.getPurchaseOrderNumber(),
                    purchaseOrder.getStatus().name(),
                    LocalDate.now());
        }

        if (purchaseOrderUpdateRequest.getPurchaseOrderAction().equals(PurchaseOrderAction.CANCELLATION_DECLINED)) {


            if (purchaseOrderUpdateRequest.getCancelReason() == null || purchaseOrderUpdateRequest.getCancelReason().isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Rejection reason not found")
                        .userMessage("Sorry, Rejection reason not found");
            }

            saveReason(purchaseOrderUpdateRequest, personOptional, purchaseOrder);

            purchaseOrder.setStatus(purchaseOrder.getPreviousStatus());
            personOptional.ifPresent(purchaseOrder::setUpdatedBy);
            purchaseOrder.preUpdate();
            purchaseOrderRepository.save(purchaseOrder);

            description = String.format(" Purchase order %s - %s on %s",
                    purchaseOrder.getPurchaseOrderNumber(),
                    purchaseOrder.getStatus().name(),
                    LocalDate.now());
        }

        if (purchaseOrderUpdateRequest.getPurchaseOrderAction().equals(PurchaseOrderAction.REVERT)) {
            if (!purchaseOrder.getStatus().equals(PurchaseOrderStatus.DISPATCHED)) {
                throw new BadRequestRestApiException()
                        .developerMessage("Operation not allowed on purchase order with status %s",
                                purchaseOrder.getStatus().name().toLowerCase())
                        .userMessage("Sorry, operation not allowed on purchase order with status %s",
                                purchaseOrder.getStatus().name().toLowerCase());
            }

            if (purchaseOrderUpdateRequest.getReasonCodePublicId() == null) {
                throw new BadRequestRestApiException()
                        .developerMessage("Select cancel reason code")
                        .userMessage("Sorry, Select cancel reason code");
            }

            RejectionsCode rejectionsCode = rejectionCodeRepository.findRejectionsCodeByPublicIdAndDeletedIsFalse(purchaseOrderUpdateRequest.getReasonCodePublicId()).orElse(null);
            if (rejectionsCode == null) {
                throw new BadRequestRestApiException()
                        .developerMessage("Rejection Code not found")
                        .userMessage("Sorry, Rejection Code not found");
            }

            purchaseOrder.setPreviousStatus(purchaseOrder.getStatus());
            purchaseOrder.setStatus(PurchaseOrderStatus.PENDING);
            personOptional.ifPresent(purchaseOrder::setUpdatedBy);
            purchaseOrder.setUpdateDate(LocalDateTime.now());
            purchaseOrder.getRejectionsCodes().add(rejectionsCode);

            PurchaseOrder savePurchaseOrder = purchaseOrderRepository.save(purchaseOrder);

            cancelRemittance(savePurchaseOrder);
            description = String.format(" Purchase order %s - %s on %s",
                    purchaseOrder.getPurchaseOrderNumber(),
                    purchaseOrder.getStatus().name(),
                    LocalDate.now());
        }

        personOptional.ifPresent(purchaseOrder::setUpdatedBy);
        purchaseOrder.preUpdate();
        PurchaseOrder savedPurchaseOrder = purchaseOrderRepository.save(purchaseOrder);
        eventsRepository.save(new Events(savedPurchaseOrder.getPublicId().toString(),
                event,
                LocalDateTime.now(),
                description,
                action,
                personOptional.orElse(null)));
    }

    private void runDispatchEventOnLiveStock(PurchaseOrder purchaseOrder, PurchaseOrderStatus status, Map<Product, ReceivedProductItem> productItemMap) {
        ZoneId nairobiZone = ZoneId.of("Africa/Nairobi");
        LocalDate nairobiDate = LocalDate.now(nairobiZone);

        if (status.equals(PurchaseOrderStatus.DISPATCHED)) {
            for (PurchaseOrderItem purchaseOrderItem : purchaseOrder.getPurchaseOrderItems()) {
                liveStockHelper.updateLiveStockValue(UUID.fromString(purchaseOrder.getWarehouseId()), purchaseOrderItem.getProduct().getPublicId(),
                        (int) purchaseOrderItem.getQuantity(), ProductUpdateType.DISPATCH_PURCHASE_ORDER, nairobiDate);
            }
        }
        if (status.equals(PurchaseOrderStatus.RECEIVED) || status.equals(PurchaseOrderStatus.PARTIAL_RECEIVED)) {
            productItemMap.forEach((k, v) -> {
                liveStockHelper.updateLiveStockValue(UUID.fromString(purchaseOrder.getWarehouseId()),
                        k.getPublicId(),
                        (int) v.getQuantityReceived(),
                        ProductUpdateType.RECEIVE_PURCHASE_ORDER, nairobiDate);
            });
        }
    }

    private void saveReason(PurchaseOrderUpdateRequest purchaseOrderUpdateRequest, Optional<Person> personOptional, PurchaseOrder purchaseOrder) {
        RejectionReason rejectionReason = new RejectionReason();
        rejectionReason.setValidReason(purchaseOrderUpdateRequest.getCancelReason());
        rejectionReason.setPurchaseOrder(purchaseOrder);
        personOptional.ifPresent(rejectionReason::setUpdatedBy);
        rejectionReasonRepository.save(rejectionReason);
    }

    private void cancelRemittance(PurchaseOrder savePurchaseOrder) {
        Remittances remittances =
                remittanceRepository.findByPurchaseOrderNumberAndDeletedFalse(savePurchaseOrder.getPurchaseOrderNumber()).orElse(null);
        if (remittances != null) {
            remittances.setDeleted(true);
            remittances.setUpdatedBy(savePurchaseOrder.getUpdatedBy());
            remittances.setUpdateDate(LocalDateTime.now());
            remittanceRepository.save(remittances);
        }
    }

    @Override
    public PurchaseOrder getPurchaseOrder(UUID purchaseOrderPublicId) {

        return purchaseOrderRepository.findByPublicIdAndDeletedIsFalse(purchaseOrderPublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Purchase order not found")
                        .userMessage("Purchase order not found"));
    }

    @Override
    public void adminApprovePurchaseOrder(UUID purchaseOrderPublicId,
                                          PurchaseOrderApprovalRequest purchaseOrderApprovalRequest,
                                          String name) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
        Optional<PurchaseOrder> purchaseOrderOptional = purchaseOrderRepository.findByPublicIdAndDeletedIsFalse(purchaseOrderPublicId);

        if (purchaseOrderOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Purchase order not found")
                    .userMessage("Purchase order not found");
        }

        String description = "";
        String title = "";
        String message = "";
        Person person = personOptional.orElse(null);
        if (purchaseOrderApprovalRequest.getComment() != null) {
            description = purchaseOrderApprovalRequest.getComment();
        }

        PurchaseOrder purchaseOrder = purchaseOrderOptional.get();

        if (!purchaseOrder.getStatus().equals(PurchaseOrderStatus.SUBMITTED)) {
            throw new BadRequestRestApiException()
                    .developerMessage("Purchase order is already approved")
                    .userMessage("Purchase order is already processed");
        }

        if (purchaseOrderApprovalRequest.getActionType().equals(ActionType.APPROVE)) {
            purchaseOrder.setStatus(PurchaseOrderStatus.AMIN_APPROVED);
            personOptional.ifPresent(purchaseOrder::setUpdatedBy);
            purchaseOrder.setUpdateDate(LocalDateTime.now());   //
            PurchaseOrder savedPurchaseOrder = purchaseOrderRepository.save(purchaseOrder);

            description = String.format(" Purchase order %s Approved on %s with order number %s",
                    savedPurchaseOrder.getPurchaseOrderNumber(),
                    LocalDate.now(),
                    savedPurchaseOrder.getProformaInvoiceNumber());
            title = String.format("P.O %s Finance Approved", savedPurchaseOrder.getPurchaseOrderNumber());
            message = String.format(" Purchase order %s  Approved on %s with order number %s by %s to %s",
                    savedPurchaseOrder.getPurchaseOrderNumber(),
                    LocalDate.now(),
                    savedPurchaseOrder.getProformaInvoiceNumber(),
                    person != null ? person.getFullName() : "Itibari",
                    purchaseOrder.getSupplier().getSupplierName());
        } else if (purchaseOrderApprovalRequest.getActionType().equals(ActionType.REJECT)) {
            purchaseOrder.setStatus(PurchaseOrderStatus.AMIN_REJECTED);
            personOptional.ifPresent(purchaseOrder::setUpdatedBy);
            purchaseOrder.setUpdateDate(LocalDateTime.now());
            PurchaseOrder savedPurchaseOrder = purchaseOrderRepository.save(purchaseOrder);

            description = String.format(" Purchase order %s Rejected on %s with order number %s",
                    savedPurchaseOrder.getPurchaseOrderNumber(),
                    LocalDate.now(),
                    savedPurchaseOrder.getProformaInvoiceNumber());
            title = String.format("P.O %s Finance Rejected", savedPurchaseOrder.getPurchaseOrderNumber());
            message = String.format(" Purchase order %s  Rejected on %s with order number %s by %s to %s",
                    savedPurchaseOrder.getPurchaseOrderNumber(),
                    LocalDate.now(),
                    savedPurchaseOrder.getProformaInvoiceNumber(),
                    person != null ? person.getFullName() : "Itibari",
                    purchaseOrder.getSupplier().getSupplierName());
        } else {
            throw new BadRequestRestApiException()
                    .developerMessage("Action type is required")
                    .userMessage("Sorry. Invalid action type");
        }

        PurchaseOrder savedPurchaseOrder = purchaseOrderRepository.save(purchaseOrder);
        eventsRepository.save(new Events(savedPurchaseOrder.getPublicId().toString(),
                purchaseOrderApprovalRequest.getActionType().name(),
                LocalDateTime.now(),
                description,
                savedPurchaseOrder.getStatus().name(),
                person));

        //notificationHelper.sendNotificationSms(message, "ROLE_PROCUREMENT_MANAGER");
        //notificationHelper.createNotificationHelper(title, message, "ROLE_PROCUREMENT_MANAGER");
    }

    @Transactional
    @Override
    public String receiveGoods(UUID purchaseOrderPublicId, PurchaseOrderReceiveRequest purchaseOrderReceiveRequest, String name) {

        GoodsReturnNote goodsReturnNote = new GoodsReturnNote();
        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);

        Optional<PurchaseOrder> purchaseOrderOptional = purchaseOrderRepository.findByPublicIdAndDeletedIsFalse(purchaseOrderPublicId);

        if (purchaseOrderOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Purchase order not found")
                    .userMessage("Purchase order not found");
        }

        PurchaseOrder purchaseOrder = purchaseOrderOptional.get();

        Warehouse warehouse = warehouseRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(purchaseOrder.getWarehouseId()))
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Warehouse not found")
                        .userMessage("Warehouse not found"));

        if (!purchaseOrder.getStatus().equals(PurchaseOrderStatus.DISPATCHED) && !purchaseOrder
                .getStatus()
                .equals(PurchaseOrderStatus.PARTIAL_RECEIVED)) {
            throw new BadRequestRestApiException()
                    .developerMessage("Purchase order is not dispatched")
                    .userMessage("Purchase order is not dispatched");
        }

        Map<Product, ProductItem> productItemMap = new HashMap<>();

        purchaseOrderReceiveRequest.getPurchaseOrderItems().forEach(purchaseOrderItemReceiveRequest -> {

            Optional<PurchaseOrderItem> purchaseOrderItemOptional = purchaseOrderItemRepository.findByPublicIdAndDeletedIsFalse(
                    purchaseOrderItemReceiveRequest.getOrderItemPublicId());

            if (purchaseOrderItemOptional.isPresent()) {
                PurchaseOrderItem purchaseOrderItem = purchaseOrderItemOptional.get();

                if (purchaseOrderItem.getQuantityReceived() + purchaseOrderItemReceiveRequest.getQuantityReceived() > purchaseOrderItem.getQuantity()) {
                    throw new BadRequestRestApiException()
                            .developerMessage("Quantity received is more than ordered")
                            .userMessage("Quantity received is more than ordered");
                }

                int received = purchaseOrderItem.getQuantityReceived() + purchaseOrderItemReceiveRequest.getQuantityReceived();

                if (received > purchaseOrderItem.getQuantity()) {
                    throw new BadRequestRestApiException()
                            .developerMessage("Quantity received is more than ordered")
                            .userMessage("Quantity received is more than ordered");
                }

                Integer initialReceived = purchaseOrderItem.getQuantityReceived();
                purchaseOrderItem.setQuantityReceived(initialReceived + purchaseOrderItemReceiveRequest.getQuantityReceived());
                purchaseOrderItem.setUpdateDate(LocalDateTime.now());
                purchaseOrderItem.setUpdatedBy(personOptional.orElse(null));

                PurchaseOrderItem item = purchaseOrderItemRepository.save(purchaseOrderItem);
                Product product = productRepository
                        .findByPublicIdAndDeletedIsFalse(purchaseOrderItem.getProduct().getPublicId())
                        .orElse(null);

                ProductItem productItem = new ProductItem();
                productItem.setProductName(item.getProduct().getProductName());
                productItem.setQuantityReturned(0L);
                productItem.setQuantityReceived(Long.valueOf(purchaseOrderItemReceiveRequest.getQuantityReceived()));
                productItem.setQuantityRequested(Long.valueOf(item.getQuantity()));
                productItem.setProductCode(item.getProduct().getSku());
                productItem.setRejectionReason(null);
                productItem.setRejectionCode(null);
                productItem.setProductSellingPrice(item.getUnitPrice());
                productItem.setProductPublicId(item.getProduct().getPublicId());

                ProductItem saved = productItemRepository.save(productItem);
                goodsReturnNote.addProduct(saved);

                if (product != null) {

                    product.setBuyingPrice(item.getUnitPrice());
                    product.setUpdateDate(LocalDateTime.now());
                    product.setUpdatedBy(personOptional.orElse(null));
                    Product product1 = productRepository.save(product);
                    productItemMap.put(product1, saved);
                }
            }
        });

        stockUpdateHelper.updateProductStockOnReceiveGoods(productItemMap, warehouse);

        boolean hasItemNotFullyReceived = purchaseOrderItemRepository
                .findAllByPurchaseOrderAndDeletedIsFalse(purchaseOrder)
                .stream()
                .anyMatch(purchaseOrderItem -> purchaseOrderItem.getQuantityReceived() < purchaseOrderItem.getQuantity());

        if (hasItemNotFullyReceived) {
            purchaseOrder.setStatus(PurchaseOrderStatus.PARTIAL_RECEIVED);
        } else {
            purchaseOrder.setStatus(PurchaseOrderStatus.RECEIVED);
        }

        purchaseOrder.setUpdateDate(LocalDateTime.now());
        purchaseOrder.setUpdatedBy(personOptional.orElse(null));

        PurchaseOrder savedPurchaseOrder = purchaseOrderRepository.save(purchaseOrder);

        goodsReturnNote.setGoodsNoteType(GoodsNoteType.RECEIVED_GOODS);
        goodsReturnNote.setGoodsReturnNoteDate(LocalDate.now());
        goodsReturnNote.setGoodsReturnNoteNumber("GRN-" + counterService.getNextCounter(CounterType.GOODS_RECEIVE_NOTE));
        goodsReturnNote.setSupplier(savedPurchaseOrder.getSupplier());
        goodsReturnNote.setStatus(GoodsReturnStatus.APPROVED);
        goodsReturnNote.setPurchaseOrderNumber(String.valueOf(savedPurchaseOrder.getPurchaseOrderNumber()));
        personOptional.ifPresent(goodsReturnNote::setCreatedBy);
        goodsReturnNote.setCreationTime(LocalDateTime.now());
        goodsReturnNote.setDeliveryNoteNumber(purchaseOrderReceiveRequest.getDeliveryNoteNumber());
        goodsReturnNote.setPurchaseOrderStatus(savedPurchaseOrder.getStatus());

        goodsReturnRepository.save(goodsReturnNote);

        String description = String.format("Purchase order %s goods %s on %s with order number %s",
                savedPurchaseOrder.getPurchaseOrderNumber(),
                savedPurchaseOrder.getStatus().name(),
                LocalDate.now(),
                savedPurchaseOrder.getProformaInvoiceNumber());

        eventsRepository.save(new Events(savedPurchaseOrder.getPublicId().toString(),
                "Purchase Order  Goods Received",
                LocalDateTime.now(),
                description,
                "RECEIVED",
                personOptional.orElse(null)));

        Person person = personOptional.orElse(null);

        String message = String.format(" Purchase order %s Received on %s by %s",
                savedPurchaseOrder.getPurchaseOrderNumber(),
                LocalDate.now(),
                person != null ? person.getFullName() : "Itibari");
        String title = String.format("P.O %s Received", savedPurchaseOrder.getPurchaseOrderNumber());
        //notificationHelper.sendNotificationSms(message, "ROLE_PROCUREMENT_MANAGER");
        //notificationHelper.createNotificationHelper(title, message, "ROLE_PROCUREMENT_MANAGER");

        return goodsReturnNote.getPublicId().toString();
    }

    @Override
    public void editPurchaseOrderItem(UUID purchaseOrderPublicId,
                                      UUID purchaseOrderItemPublicId,
                                      PurchaseOrderItemRequest purchaseOrderItemRequest,
                                      String name) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);

        Optional<PurchaseOrder> purchaseOrderOptional = purchaseOrderRepository.findByPublicIdAndDeletedIsFalse(purchaseOrderPublicId);

        if (purchaseOrderOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Purchase order not found")
                    .userMessage("Purchase order not found");
        }

        PurchaseOrder purchaseOrder = purchaseOrderOptional.get();

        if (purchaseOrder.isReplacementPurchaseOrder()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Replacement purchase order cannot be updated")
                    .userMessage("Replacement purchase order cannot be updated");
        }

        if (!purchaseOrder.getStatus().equals(PurchaseOrderStatus.PENDING)) {
            throw new BadRequestRestApiException()
                    .developerMessage("Purchase order is already approved")
                    .userMessage("Purchase order is already processed");
        }

        Optional<PurchaseOrderItem> purchaseOrderItemOptional = purchaseOrderItemRepository.findByPublicIdAndPurchaseOrderAndDeletedIsFalse(
                purchaseOrderItemPublicId,
                purchaseOrder);

        if (purchaseOrderItemOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Purchase order item not found")
                    .userMessage("Purchase order item not found");
        }

        PurchaseOrderItem purchaseOrderItem = purchaseOrderItemOptional.get();

        purchaseOrderItem.setQuantity(purchaseOrderItemRequest.getQuantity());
        purchaseOrderItem.setUnitPrice(purchaseOrderItemRequest.getUnitPrice());

        purchaseOrderItem.setUpdateDate(LocalDateTime.now());
        purchaseOrderItem.setUpdatedBy(personOptional.orElse(null));

        purchaseOrderItemRepository.save(purchaseOrderItem);

        eventsRepository.save(new Events(purchaseOrder.getPublicId().toString(),
                "Purchase Order Item Updated",
                LocalDateTime.now(),
                String.format(" Purchase order %s item Updated on %s with order number %s",
                        purchaseOrder.getPurchaseOrderNumber(),
                        LocalDate.now(),
                        purchaseOrder.getProformaInvoiceNumber()),
                "UPDATED",
                personOptional.orElse(null)));

        purchaseOrder.setUpdateDate(LocalDateTime.now());
        purchaseOrder.setUpdatedBy(personOptional.orElse(null));

        purchaseOrderRepository.save(purchaseOrder);
    }

    @Override
    public Page<PurchaseOrderItem> getPurchaseOrderItems(UUID purchaseOrderPublicId, String searchParam, Pageable pageable) {

        Optional<PurchaseOrder> purchaseOrderOptional = purchaseOrderRepository.findByPublicIdAndDeletedIsFalse(purchaseOrderPublicId);

        if (purchaseOrderOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Purchase order not found")
                    .userMessage("Purchase order not found");
        }

        PurchaseOrder purchaseOrder = purchaseOrderOptional.get();

        return purchaseOrderItemRepository.findAllByPurchaseOrderAndDeletedIsFalse(purchaseOrder, pageable);
    }

    @Override
    public PurchaseOrder createReplacementPurchaseOrder(UUID goodReturnNotePublicId, String name) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
        Optional<GoodsReturnDeliveryNote> goodsReturnNoteOptional =
                goodsReturnDeliveryNoteRepository.findByPublicIdAndDeletedIsFalse(goodReturnNotePublicId);

        if (goodsReturnNoteOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Goods return note not found")
                    .userMessage("Goods return note not found");
        }

        GoodsReturnDeliveryNote goodsReturnNote = goodsReturnNoteOptional.get();

        if (goodsReturnNote.isReplacementRaised()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Replacement purchase order already raised for this goods return note")
                    .userMessage("Replacement purchase order already raised for this goods return note");
        }


        Optional<PurchaseOrder> purchaseOrderOptional = purchaseOrderRepository.findByPurchaseOrderNumberAndDeletedIsFalse(goodsReturnNote.getPurchaseOrderNumber());
        if (purchaseOrderOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Purchase order not found")
                    .userMessage("Purchase order not found");
        }
        PurchaseOrder existingPurchaseOrder = purchaseOrderOptional.get();
        List<ReturnedProductItem> productItems =
                returnedProductItemRepository.findByGoodsReturnDeliveryNote(goodsReturnNote);

        if (productItems.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product items not found")
                    .userMessage("We could not find any product items that are supposed to be replaced");
        }

        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setSupplier(existingPurchaseOrder.getSupplier());
        purchaseOrder.setPurchaseOrderDate(LocalDate.now());
        purchaseOrder.setNotes("Replacement for goods return note " + goodsReturnNote.getGoodsReturnNoteNumber());
        purchaseOrder.setStatus(PurchaseOrderStatus.DISPATCHED);
        purchaseOrder.setPurchaseOrderNumber("PO-" + counterService.getNextCounter(CounterType.PURCHASE_ORDER));
        personOptional.ifPresent(purchaseOrder::setCreatedBy);
        purchaseOrder.setCreationTime(LocalDateTime.now());
        purchaseOrder.setProformaInvoiceNumber("RP-" + existingPurchaseOrder.getProformaInvoiceNumber());
        purchaseOrder.setWarehouseId(existingPurchaseOrder.getWarehouseId());
        purchaseOrder.setWarehouseName(existingPurchaseOrder.getWarehouseName());
        purchaseOrder.setRebate(false);
        purchaseOrder.setReplacementPurchaseOrder(true);
        purchaseOrder.setGoodsReturnNoteNumberReference(goodsReturnNote.getGoodsReturnNoteNumber());

        PurchaseOrder savedPurchaseOrder = purchaseOrderRepository.save(purchaseOrder);

        productItems.forEach(purchaseOrderItemRequest -> {
            Optional<Product> optionalProduct = productRepository
                    .findByPublicIdAndDeletedIsFalse(purchaseOrderItemRequest.getProductPublicId());

            if (optionalProduct.isEmpty()) {
                return; // skip if product not found
            }

            Product product = optionalProduct.get();
            PurchaseOrderItem purchaseOrderItem = new PurchaseOrderItem();
            purchaseOrderItem.setProduct(product);
            purchaseOrderItem.setQuantity((int) purchaseOrderItemRequest.getQuantityReturned());
            purchaseOrderItem.setUnitPrice(purchaseOrderItemRequest.getCostPrice());
            purchaseOrderItem.setPurchaseOrder(savedPurchaseOrder);

            BigDecimal grossTotal = purchaseOrderItem.getUnitPrice()
                    .multiply(BigDecimal.valueOf(purchaseOrderItem.getQuantity()));

            if (product.getTaxCode() == null) {
                throw new BadRequestRestApiException()
                        .developerMessage("Missing tax code")
                        .userMessage("Sorry, missing tax code");
            }

            if (NON_TAXABLE_CODES.contains(product.getTaxCode().getCodeName())) {
                purchaseOrderItem.setTaxAmount(BigDecimal.ZERO);
                purchaseOrderItem.setAmountAfterTax(grossTotal);
            } else {
                BigDecimal taxRate = product.getTaxCode().getCodeValue();
                TaxResult result = calculateTax(purchaseOrderItem.getUnitPrice(),
                        purchaseOrderItem.getQuantity(),
                        taxRate);

                purchaseOrderItem.setTaxAmount(result.taxAmount());
                purchaseOrderItem.setAmountAfterTax(result.amountAfterTax());
            }

            PurchaseOrderItem savedPurchaseOrderItem = purchaseOrderItemRepository.save(purchaseOrderItem);
            savedPurchaseOrder.addPurchaseOrderItem(savedPurchaseOrderItem);
        });

        eventsRepository.save(new Events(savedPurchaseOrder.getPublicId().toString(),
                "Purchase Order Created",
                LocalDateTime.now(),
                String.format(" Replacement Purchase order %s creation on %s to supplier %s with order number %s",
                        savedPurchaseOrder.getPurchaseOrderNumber(),
                        LocalDate.now(),
                        existingPurchaseOrder.getSupplierName(),
                        savedPurchaseOrder.getProformaInvoiceNumber()),
                "CREATE",
                personOptional.orElse(null)));

        PurchaseOrder saved = purchaseOrderRepository.save(savedPurchaseOrder);

        goodsReturnNote.setReplacementRaised(true);
        goodsReturnNote.setReplacementPurchaseOrder(saved.getPublicId().toString());
        goodsReturnNote.setUpdateDate(LocalDateTime.now());
        goodsReturnNote.setUpdatedBy(personOptional.orElse(null));
        goodsReturnDeliveryNoteRepository.save(goodsReturnNote);


        Warehouse warehouse = warehouseRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(saved.getWarehouseId()))
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Warehouse not found")
                        .userMessage("Warehouse not found"));
        for (PurchaseOrderItem purchaseOrderItem : saved.getPurchaseOrderItems()) {
            stockMovementHelper.recordStockMovement(purchaseOrderItem.getProduct(),
                    purchaseOrderItem.getQuantity(),
                    StockMovementTransactionType.GRN_REPLACEMENT,
                    TransactionBoundType.RECORD,
                    personOptional.isPresent() ? personOptional.get().getFullName() : "System",
                    warehouse);
        }
        return saved;
    }

    private TaxResult calculateTax(BigDecimal unitPrice, int quantity, BigDecimal taxRate) {
        BigDecimal divisor = BigDecimal.ONE.add(taxRate);

        BigDecimal unitTaxAmount = unitPrice.subtract(
                unitPrice.divide(divisor, SCALE, RoundingMode.HALF_UP)
        );

        BigDecimal taxAmount = unitTaxAmount.multiply(BigDecimal.valueOf(quantity))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal grossTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));

        BigDecimal amountAfterTax = grossTotal.subtract(taxAmount)
                .setScale(2, RoundingMode.HALF_UP);

        return new TaxResult(taxAmount, amountAfterTax);
    }

    @Transactional
    @Override
    public String receiveShipment(UUID purchaseOrderPublicId, PurchaseOrderReceiveRequest purchaseOrderReceiveRequest, String name) {
        GoodsReceivedNote goodsReceivedNote = new GoodsReceivedNote();
        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);

        Optional<PurchaseOrder> purchaseOrderOptional = purchaseOrderRepository.findByPublicIdAndDeletedIsFalse(purchaseOrderPublicId);

        if (purchaseOrderOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Purchase order not found")
                    .userMessage("Purchase order not found");
        }

        PurchaseOrder purchaseOrder = purchaseOrderOptional.get();

        Warehouse warehouse = warehouseRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(purchaseOrder.getWarehouseId()))
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Warehouse not found")
                        .userMessage("Warehouse not found"));

        if (!purchaseOrder.getStatus().equals(PurchaseOrderStatus.DISPATCHED) && !purchaseOrder
                .getStatus()
                .equals(PurchaseOrderStatus.PARTIAL_RECEIVED)) {
            throw new BadRequestRestApiException()
                    .developerMessage("Purchase order is not dispatched")
                    .userMessage("Purchase order is not dispatched");
        }

        Map<Product, ReceivedProductItem> productItemMap = new HashMap<>();

        purchaseOrderReceiveRequest.getPurchaseOrderItems().forEach(purchaseOrderItemReceiveRequest -> {
            Optional<PurchaseOrderItem> purchaseOrderItemOptional = purchaseOrderItemRepository.findByPublicIdAndDeletedIsFalse(
                    purchaseOrderItemReceiveRequest.getOrderItemPublicId());

            if (purchaseOrderItemOptional.isPresent()) {
                PurchaseOrderItem purchaseOrderItem = purchaseOrderItemOptional.get();

                if (purchaseOrderItem.getQuantityReceived() + purchaseOrderItemReceiveRequest.getQuantityReceived() > purchaseOrderItem.getQuantity()) {
                    throw new BadRequestRestApiException()
                            .developerMessage("Quantity received is more than ordered")
                            .userMessage("Quantity received is more than ordered");
                }

                int received = purchaseOrderItem.getQuantityReceived() + purchaseOrderItemReceiveRequest.getQuantityReceived();

                if (received > purchaseOrderItem.getQuantity()) {
                    throw new BadRequestRestApiException()
                            .developerMessage("Quantity received is more than ordered")
                            .userMessage("Quantity received is more than ordered");
                }

                Integer initialReceived = purchaseOrderItem.getQuantityReceived();
                purchaseOrderItem.setQuantityReceived(initialReceived + purchaseOrderItemReceiveRequest.getQuantityReceived());
                purchaseOrderItem.setUpdateDate(LocalDateTime.now());
                purchaseOrderItem.setUpdatedBy(personOptional.orElse(null));

                PurchaseOrderItem item = purchaseOrderItemRepository.save(purchaseOrderItem);
                Product product = productRepository
                        .findByPublicIdAndDeletedIsFalse(purchaseOrderItem.getProduct().getPublicId())
                        .orElse(null);

                ReceivedProductItem productItem = new ReceivedProductItem();
                productItem.setProductName(item.getProduct().getProductName());
                productItem.setQuantityReturned(0L);
                productItem.setQuantityReceived(Long.valueOf(purchaseOrderItemReceiveRequest.getQuantityReceived()));
                productItem.setQuantityRequested(Long.valueOf(item.getQuantity()));
                productItem.setProductCode(item.getProduct().getSku());
                productItem.setRejectionReason(null);
                productItem.setRejectionCode(null);
                productItem.setProductSellingPrice(item.getUnitPrice());
                productItem.setPurchasePrice(item.getUnitPrice());
                productItem.setProductPublicId(item.getProduct().getPublicId());

                ReceivedProductItem savedReceivedProductItem = receivedProductItemRepository.save(productItem);
                goodsReceivedNote.addProduct(savedReceivedProductItem);

                if (product != null) {

                    if (!purchaseOrder.isRebate()) {
                        product.setBuyingPrice(item.getUnitPrice());
                    }
                    product.setUpdateDate(LocalDateTime.now());
                    product.setUpdatedBy(personOptional.orElse(null));
                    Product product1 = productRepository.save(product);
                    productItemMap.put(product1, savedReceivedProductItem);
                }
            }
        });

        stockUpdateHelper.updateProductStockOnReceiveShipment(productItemMap, warehouse, InventoryEvent.GRN.name());

        boolean hasItemNotFullyReceived = purchaseOrderItemRepository
                .findAllByPurchaseOrderAndDeletedIsFalse(purchaseOrder)
                .stream()
                .anyMatch(purchaseOrderItem -> purchaseOrderItem.getQuantityReceived() < purchaseOrderItem.getQuantity());

        if (hasItemNotFullyReceived) {
            purchaseOrder.setStatus(PurchaseOrderStatus.PARTIAL_RECEIVED);
            runDispatchEventOnLiveStock(purchaseOrder, purchaseOrder.getStatus(), productItemMap);
        } else {
            purchaseOrder.setStatus(PurchaseOrderStatus.RECEIVED);
            runDispatchEventOnLiveStock(purchaseOrder, purchaseOrder.getStatus(), productItemMap);
        }
        purchaseOrder.setUpdateDate(LocalDateTime.now());
        purchaseOrder.setUpdatedBy(personOptional.orElse(null));

        PurchaseOrder savedPurchaseOrder = purchaseOrderRepository.save(purchaseOrder);

        goodsReceivedNote.setNoteDate(LocalDate.now());
        goodsReceivedNote.setNoteNumber("GRN-" + counterService.getNextCounter(CounterType.GOODS_RECEIVE_NOTE));
        goodsReceivedNote.setSupplier(savedPurchaseOrder.getSupplier());
        goodsReceivedNote.setStatus(GoodsReturnStatus.APPROVED);
        goodsReceivedNote.setPurchaseOrderNumber(String.valueOf(savedPurchaseOrder.getPurchaseOrderNumber()));
        personOptional.ifPresent(goodsReceivedNote::setCreatedBy);
        goodsReceivedNote.setCreationTime(LocalDateTime.now());
        goodsReceivedNote.setDeliveryNoteNumber(purchaseOrderReceiveRequest.getDeliveryNoteNumber());
        goodsReceivedNote.setPurchaseOrderStatus(savedPurchaseOrder.getStatus());
        if (savedPurchaseOrder.isReplacementPurchaseOrder()) {
            goodsReceivedNote.setPoType(POType.REPLACEMENT);
        } else if (savedPurchaseOrder.isRebate()) {
            goodsReceivedNote.setPoType(POType.REBATE);
        } else {
            goodsReceivedNote.setPoType(POType.NORMAL);
        }

        GoodsReceivedNote savedGoodsReceivedNote = goodsReceivedNoteRepository.save(goodsReceivedNote);
        if (savedGoodsReceivedNote.getPoType() == POType.NORMAL) {
            updateLiveStock(warehouse.getPublicId(), savedGoodsReceivedNote);
        } else if (savedGoodsReceivedNote.getPoType() == POType.REBATE) {
            updateLiveStockWithRebateItems(warehouse.getPublicId(), savedGoodsReceivedNote);
        } else {
            updateLiveStockReplacement(warehouse.getPublicId(), savedGoodsReceivedNote);
        }

        goodsReceivedNote.getReceivedProductItem().forEach(receivedProductItem -> {
            receivedProductItem.setGoodsReceivedNote(savedGoodsReceivedNote);
            receivedProductItemRepository.save(receivedProductItem);
        });

        updateProductStocks(warehouse, goodsReceivedNote.getReceivedProductItem());

        String description = String.format("Purchase order %s goods %s on %s with order number %s",
                savedPurchaseOrder.getPurchaseOrderNumber(),
                savedPurchaseOrder.getStatus().name(),
                LocalDate.now(),
                savedPurchaseOrder.getProformaInvoiceNumber());

        eventsRepository.save(new Events(savedPurchaseOrder.getPublicId().toString(),
                "Purchase Order  Goods Received",
                LocalDateTime.now(),
                description,
                "RECEIVED",
                personOptional.orElse(null)));

        Person person = personOptional.orElse(null);

        String message = String.format(" Purchase order %s Received on %s by %s",
                savedPurchaseOrder.getPurchaseOrderNumber(),
                LocalDate.now(),
                person != null ? person.getFullName() : "Itibari");
        String title = String.format("P.O %s Received", savedPurchaseOrder.getPurchaseOrderNumber());
        //notificationHelper.sendNotificationSms(message, "ROLE_PROCUREMENT_MANAGER");
        //notificationHelper.createNotificationHelper(title, message, "ROLE_PROCUREMENT_MANAGER");


        // update stock movement

        productItemMap.forEach((product, receivedProductItem) -> {
            stockMovementHelper.recordStockMovement(product,
                    (int) receivedProductItem.getQuantityReceived(),
                    StockMovementTransactionType.GRN,
                    TransactionBoundType.INCOMING,
                    person != null ? person.getFullName() : "System",
                    warehouse);
        });

        return goodsReceivedNote.getPublicId().toString();
    }

    //caters for rebate, grn-normal, replacement
    private void updateProductStocks(Warehouse warehouse, List<ReceivedProductItem> receivedProductItem) {
        for (ReceivedProductItem productItem : receivedProductItem) {
            Product product = productRepository.findByPublicId(productItem.getProductPublicId());
            if (product != null) {
                productStockHelper.updateProductStock(product, warehouse, productItem.getQuantityReceived(),
                        StockMovementTransactionType.GRN);
            }
        }

    }

    private void updateLiveStock(UUID wareHousePublicId, GoodsReceivedNote savedGoodsReceivedNote) {

        List<ReceivedProductItem> receivedProductItemList = savedGoodsReceivedNote.getReceivedProductItem();
        receivedProductItemList.forEach(receivedProductItem -> {

            liveStockHelper.updateLiveStockValue(wareHousePublicId, receivedProductItem.getProductPublicId(),
                    (int) receivedProductItem.getQuantityReceived(), ProductUpdateType.GOODS_RECEIVED_STOCK, LocalDate.now());
        });
    }

    private void updateLiveStockReplacement(UUID wareHousePublicId, GoodsReceivedNote savedGoodsReceivedNote) {

        List<ReceivedProductItem> receivedProductItemList = savedGoodsReceivedNote.getReceivedProductItem();
        receivedProductItemList.forEach(receivedProductItem -> {
            liveStockHelper.updateLiveStockValue(wareHousePublicId, receivedProductItem.getProductPublicId(),
                    (int) receivedProductItem.getQuantityReceived(), ProductUpdateType.REPLACED_STOCK, LocalDate.now());
        });
    }

    private void updateLiveStockWithRebateItems(UUID wareHousePublicId, GoodsReceivedNote savedGoodsReceivedNote) {

        List<ReceivedProductItem> receivedProductItemList = savedGoodsReceivedNote.getReceivedProductItem();
        receivedProductItemList.forEach(receivedProductItem -> {
            liveStockHelper.updateLiveStockValue(wareHousePublicId, receivedProductItem.getProductPublicId(),
                    (int) receivedProductItem.getQuantityReceived(), ProductUpdateType.REBATE_STOCK, LocalDate.now());
        });
    }
// credit notes

    @Override
    public CreditNote raiseCreditNoteFromPurchaseOrder(UUID purchaseOrderPublicId, CreditNoteRequest creditNoteRequest, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);
        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("User not found")
                    .userMessage("User not found");
        }
        Person person = optionalPerson.get();

        if (Validators.anyEqualNull(
                creditNoteRequest.getAction(),
                creditNoteRequest.getCreditNoteAmount(),
                creditNoteRequest.getDescription(),
                creditNoteRequest.getAction(),
                creditNoteRequest.getCreditNoteDate())
        ) {
            throw new BadRequestRestApiException()
                    .developerMessage("All fields are required")
                    .userMessage("All fields are required");
        }

        PurchaseOrder purchaseOrder = getPurchaseOrder(purchaseOrderPublicId);

        if (!(purchaseOrder.getStatus() == PurchaseOrderStatus.PARTIAL_RECEIVED || purchaseOrder.getStatus() == PurchaseOrderStatus.DISPATCHED)) {
            throw new BadRequestRestApiException()
                    .developerMessage("Credit note can only be raised for purchase orders that are partially received or dispatched")
                    .userMessage("Credit note can only be raised for purchase orders that are partially received or dispatched");
        }

        // check if a credit note is raise for action

        checkIfHasCreditNoteWithCloseAction(purchaseOrder);

        CreditNote creditNote = new CreditNote();
        creditNote.setAmount(creditNoteRequest.getCreditNoteAmount());
        creditNote.setNote(creditNoteRequest.getDescription());
        creditNote.setActionedBy(name);
        creditNote.setSupplier(purchaseOrder.getSupplier());
        creditNote.setCreditNoteNumber("CN-" + counterService.getNextCounter(CounterType.CREDIT_NOTE));
        creditNote.setStatus(CreditNoteStatus.PENDING);
        creditNote.setPurchaseOrderPublicId(purchaseOrderPublicId.toString());
        creditNote.setActionApplied(creditNoteRequest.getAction());
        creditNote.setCreatedBy(person);
        creditNote.setCreditNoteDate(creditNoteRequest.getCreditNoteDate());


        CreditNote savedCreditNote = creditNoteRepository.save(creditNote);


        eventsRepository.save(new Events(savedCreditNote.getPublicId().toString(),
                "Credit Note Raised",
                LocalDateTime.now(),
                String.format("Credit note %s created for purchase order %s with amount %s on %s by %s",
                        savedCreditNote.getCreditNoteNumber(),
                        purchaseOrder.getPurchaseOrderNumber(),
                        savedCreditNote.getAmount(),
                        LocalDate.now(),
                        name),
                "CREDIT_NOTE_CREATED",
                person));


        String description = String.format("Credit note %s raised for purchase order %s with amount %s on %s by %s",
                savedCreditNote.getCreditNoteNumber(),
                purchaseOrder.getPurchaseOrderNumber(),
                savedCreditNote.getAmount(),
                LocalDate.now(),
                name);


        eventsRepository.save(new Events(purchaseOrderPublicId.toString(),
                "Credit Note Raised",
                LocalDateTime.now(),
                description,
                "CREDIT_NOTE_RAISED",
                person));

        if (creditNote.getActionApplied() == CreditNoteOrderAction.CLOSE_PURCHASE_ORDER) {
            purchaseOrder.setStatus(PurchaseOrderStatus.PENDING_CREDIT_NOTE_APPROVE);
            purchaseOrderRepository.save(purchaseOrder);
        }


        return savedCreditNote;
    }

    @Override
    public void approveCreditNote(UUID creditNotePublicId, CreditNoteApproveAction creditNoteApproveAction, String comment, String name) {
        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);
        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("User not found")
                    .userMessage("User not found");
        }
        Person person = optionalPerson.get();

        Optional<CreditNote> creditNoteOptional = creditNoteRepository.findByPublicIdAndDeletedIsFalse(creditNotePublicId);

        if (creditNoteOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Credit note not found")
                    .userMessage("Credit note not found");
        }

        CreditNote creditNote = creditNoteOptional.get();

        if (creditNote.getStatus() != CreditNoteStatus.PENDING) {
            throw new BadRequestRestApiException()
                    .developerMessage("Only pending credit notes can be approved")
                    .userMessage("Only pending credit notes can be approved");
        }

        if (creditNote.getActionApplied() != null && creditNote.getActionApplied() == CreditNoteOrderAction.CLOSE_PURCHASE_ORDER && creditNoteApproveAction == CreditNoteApproveAction.APPROVE) {
            // check if there are pending credit note for the same order. if there are, reject the pending credit note and approve the close action credit note. if there are no pending credit note, approve the close action credit note.
            checkIfCreditNoteIsCloseAction(creditNote);

        }


        creditNote.setStatus(creditNoteApproveAction == CreditNoteApproveAction.APPROVE ? CreditNoteStatus.APPROVED : CreditNoteStatus.REJECTED);
        creditNote.setUpdateDate(LocalDateTime.now());
        creditNote.setUpdatedBy(person);


        eventsRepository.save(new Events(creditNote.getPublicId().toString(),
                "Credit Note Updated",
                LocalDateTime.now(),
                String.format("Credit note %s %s for purchase order %s with amount %s on %s by %s. Comment: %s",
                        creditNote.getCreditNoteNumber(),
                        creditNote.getStatus() == CreditNoteStatus.APPROVED ? "approved" : "rejected",
                        creditNote.getPurchaseOrderPublicId(),
                        creditNote.getAmount(),
                        LocalDate.now(),
                        name,
                        comment),
                creditNote.getStatus() == CreditNoteStatus.APPROVED ? "CREDIT_NOTE_APPROVED" : "CREDIT_NOTE_REJECTED",
                person));


        String description = String.format("Credit note %s %s for purchase order %s with amount %s on %s by %s. Comment: %s",
                creditNote.getCreditNoteNumber(),
                creditNote.getStatus() == CreditNoteStatus.APPROVED ? "approved" : "rejected",
                creditNote.getPurchaseOrderPublicId(),
                creditNote.getAmount(),
                LocalDate.now(),
                name,
                comment);

        eventsRepository.save(new Events(creditNote.getPurchaseOrderPublicId(),
                "Credit Note Updated",
                LocalDateTime.now(),
                description,
                creditNote.getStatus() == CreditNoteStatus.APPROVED ? "CREDIT_NOTE_APPROVED" : "CREDIT_NOTE_REJECTED",
                person));

        CreditNote updated = creditNoteRepository.save(creditNote);

        // update P.O

        if (updated.getStatus() == CreditNoteStatus.APPROVED && updated.getActionApplied() == CreditNoteOrderAction.CLOSE_PURCHASE_ORDER) {
            PurchaseOrder purchaseOrder = purchaseOrderRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(updated.getPurchaseOrderPublicId()))
                    .orElseThrow(() -> new BadRequestRestApiException()
                            .developerMessage("Purchase order not found")
                            .userMessage("Purchase order not found"));

            purchaseOrder.setStatus(PurchaseOrderStatus.CLOSED);
            purchaseOrder.setUpdateDate(LocalDateTime.now());
            purchaseOrder.setUpdatedBy(person);
            purchaseOrderRepository.save(purchaseOrder);

            eventsRepository.save(new Events(purchaseOrder.getPublicId().toString(),
                    "Purchase Order Closed",
                    LocalDateTime.now(),
                    String.format("Purchase order %s closed on %s by %s due to credit note approval",
                            purchaseOrder.getPurchaseOrderNumber(),
                            LocalDate.now(),
                            name),
                    "PURCHASE_ORDER_CLOSED",
                    person));
        }


    }

    @Override
    public Page<CreditNote> getCreditNotes(String searchParam, LocalDate fromDate, LocalDate toDate, UUID supplierId, List<CreditNoteStatus> orderStatuses, Boolean tagged, UUID purchaseOrderPublicId, Pageable pageable) {
        Specification<CreditNote> specification = Specification
                .where(CreditNoteSpecification.hasSearchParam(searchParam))
                .and(CreditNoteSpecification.hasFromToDates(fromDate, toDate))
                .and(CreditNoteSpecification.hasSupplier(supplierId))
                .and(CreditNoteSpecification.hasStatuses(orderStatuses))
                .and(CreditNoteSpecification.isTagged(tagged))
                .and(CreditNoteSpecification.hasOriginalPurchaseOrder(purchaseOrderPublicId))
                .and(CreditNoteSpecification.hasPurchaseOrder(purchaseOrderPublicId));

        return creditNoteRepository.findAll(specification, pageable);
    }

    private void checkIfCreditNoteIsCloseAction(CreditNote creditNote) {
        List<CreditNote> creditNotes = creditNoteRepository.findByPurchaseOrderPublicIdAndDeletedIsFalse(creditNote.getPurchaseOrderPublicId());
        if (!creditNotes.isEmpty()) {
            boolean hasPendingCreditNote = creditNotes.stream()
                    .anyMatch(note -> note.getStatus() == CreditNoteStatus.PENDING && !note.getPublicId().equals(creditNote.getPublicId()));
            if (hasPendingCreditNote) {
                throw new BadRequestRestApiException()
                        .developerMessage("There are pending credit notes for this purchase order. Please approve or reject the pending credit notes before approving this close action credit note")
                        .userMessage("There are pending credit notes for this purchase order. Please approve or reject the pending credit notes before approving this close action credit note");
            }
        }

    }

    @Override
    public CreditNote raiseCreditForSupplier(UUID supplierPublicId, CreditNoteRequest creditNoteRequest, String name) {
        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);
        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("User not found")
                    .userMessage("User not found");
        }
        Person person = optionalPerson.get();

        if (Validators.anyEqualNull(
                creditNoteRequest.getCreditNoteAmount(),
                creditNoteRequest.getDescription(),
                creditNoteRequest.getCreditNoteDate())
        ) {
            throw new BadRequestRestApiException()
                    .developerMessage("All fields are required")
                    .userMessage("All fields are required");
        }

        Suppliers supplier = supplierRepository.findByPublicIdAndDeletedIsFalse(supplierPublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Supplier not found")
                        .userMessage("Supplier not found"));

        CreditNote creditNote = new CreditNote();
        creditNote.setAmount(creditNoteRequest.getCreditNoteAmount());
        creditNote.setNote(creditNoteRequest.getDescription());
        creditNote.setActionedBy(name);
        creditNote.setSupplier(supplier);
        creditNote.setCreditNoteNumber("CN-" + counterService.getNextCounter(CounterType.CREDIT_NOTE));
        creditNote.setStatus(CreditNoteStatus.PENDING);
        creditNote.setPurchaseOrderPublicId(null);
        creditNote.setActionApplied(creditNoteRequest.getAction());
        creditNote.setCreatedBy(person);
        creditNote.setSupplierRaised(true);
        creditNote.setCreditNoteDate(creditNoteRequest.getCreditNoteDate());

        return creditNoteRepository.save(creditNote);
    }

    private void checkIfHasCreditNoteWithCloseAction(PurchaseOrder purchaseOrder) {
        List<CreditNote> creditNotes = creditNoteRepository.findByPurchaseOrderPublicIdAndDeletedIsFalse(purchaseOrder.getPublicId().toString());
        if (!creditNotes.isEmpty()) {
            boolean hasCloseAction = creditNotes.stream()
                    .anyMatch(creditNote -> creditNote.getActionApplied() == CreditNoteOrderAction.CLOSE_PURCHASE_ORDER
                            && creditNote.getStatus() != CreditNoteStatus.REJECTED);
            if (hasCloseAction) {
                throw new BadRequestRestApiException()
                        .developerMessage("A credit note with close purchase order action has already been raised for this purchase order")
                        .userMessage("Sorry!. A close credit note has already been raised for this purchase order");
            }
        }

    }

    @Override
    public void raiseRefundRequest(UUID purchaseOrderPublicId, String name) {
        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);
        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("User not found")
                    .userMessage("User not found");
        }
        Person person = optionalPerson.get();

        PurchaseOrder purchaseOrder = purchaseOrderRepository.findByPublicIdAndDeletedIsFalse(purchaseOrderPublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Purchase order not found")
                        .userMessage("Purchase order not found"));

        if (purchaseOrder.getStatus() != PurchaseOrderStatus.PARTIAL_RECEIVED && purchaseOrder.getStatus() != PurchaseOrderStatus.DISPATCHED) {
            throw new BadRequestRestApiException()
                    .developerMessage("Refund can only be raised for purchase orders that are partially received or dispatched")
                    .userMessage("Refund can only be raised for purchase orders that are partially received or dispatched");
        }

        // check if refund receipt is uploaded.

        // check if proof is submitted

        fileService.checkIfProofFileIsSubmited(purchaseOrder.getPublicId(), ReferenceType.PURCHASE_ORDER_REFUND);

        purchaseOrder.setUpdatedBy(person);
        purchaseOrder.setUpdateDate(LocalDateTime.now());
        purchaseOrder.setPreviousStatus(purchaseOrder.getStatus());
        purchaseOrder.setStatus(PurchaseOrderStatus.REFUND_REQUESTED);

        // event

        purchaseOrderRepository.save(purchaseOrder);

        eventsRepository.save(new Events(purchaseOrder.getPublicId().toString(),
                "Purchase Order Refund Requested",
                LocalDateTime.now(),
                "Purchase order refund requested for purchase order ",
                "PURCHASE ORDER REFUND REQUESTED",
                optionalPerson.orElse(null)));

    }
}
