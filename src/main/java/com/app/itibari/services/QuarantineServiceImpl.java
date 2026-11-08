package com.app.itibari.services;

import com.app.itibari.dataTypes.*;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.DisposalRequest;
import com.app.itibari.models.request.QuarantineRequest;
import com.app.itibari.models.response.DisposedQuarantineProducts;
import com.app.itibari.models.response.SalesOrdersPageableDTO;
import com.app.itibari.repository.*;
import com.app.itibari.services.helper.LiveStockHelper;
import com.app.itibari.services.helper.StockMovementHelper;
import com.app.itibari.services.helper.StockUpdateHelper;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.formatter.DateFormatter;
import com.app.itibari.utils.helpers.ProductStockHelper;
import com.app.itibari.utils.helpers.Validators;
import com.app.itibari.utils.reportHelper.QuarantineDisposedProductExport;
import com.app.itibari.utils.specification.DisposedQuarantineProductSpecification;
import com.app.itibari.utils.specification.GoodsReceivedSpecification;
import com.app.itibari.utils.specification.QuarantineSpecification;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.text.DocumentException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuarantineServiceImpl implements QuarantineService {

    private final QuarantineRepository quarantineRepository;

    private final WarehouseRepository warehouseRepository;

    private final PersonRepository personRepository;

    private final InventoryProductsRepository inventoryProductsRepository;

    private final InventoryRepository inventoryRepository;

    private final RejectionCodeRepository rejectionCodeRepository;

    private final QuarantineProductRepository quarantineProductRepository;

    private final EventsRepository eventsRepository;

    private final StockUpdateHelper stockUpdateHelper;

    private final ProductRepository productRepository;
    private final LiveStockHelper liveStockHelper;
    private final StockMovementHelper stockMovementHelper;
    private final ProductStockHelper productStockHelper;
    private final QuarantineDisposedProductExport quarantineDisposedProductExport;

    @Override
    public Quarantine createQuarantine(QuarantineRequest quarantineRequest, String name) {

        Optional<Person> personOpt = personRepository.findByEmailAndDeletedIsFalse(name);

        validateQuarantineRequest(quarantineRequest);

        Optional<Warehouse> mainWarehouseOpt = warehouseRepository.findByPublicIdAndDeletedIsFalse(quarantineRequest.getMainWarehousePublicId());

        if (mainWarehouseOpt.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Main warehouse not found")
                    .userMessage("The specified main warehouse does not exist or has been deleted.");
        }

        Optional<Warehouse> quarantineWarehouseOpt = warehouseRepository.findByPublicIdAndDeletedIsFalse(quarantineRequest.getQuarantineWarehousePublicId());

        if (quarantineWarehouseOpt.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Quarantine warehouse not found")
                    .userMessage("The specified quarantine warehouse does not exist or has been deleted.");
        }

        Warehouse mainWarehouse = mainWarehouseOpt.get();
        Warehouse quarantineWarehouse = quarantineWarehouseOpt.get();

        // check if the quarantine warehouse is part of the main warehouse

        if (quarantineWarehouse.getParentWarehouse() == null || !quarantineWarehouse.getParentWarehouse().equals(mainWarehouse)) {
            throw new BadRequestRestApiException()
                    .developerMessage("Invalid quarantine warehouse")
                    .userMessage("The specified quarantine warehouse is not a child of the main warehouse.");
        }

        // check if the main warehouse has inventory

        Optional<Inventory> inventoryOpt = inventoryRepository.findByWarehouseAndDeletedIsFalse(mainWarehouse);

        if (inventoryOpt.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Main warehouse has no inventory")
                    .userMessage("The specified main warehouse does not have an inventory.");
        }

        Inventory inventory = inventoryOpt.get();

        Quarantine quarantine = new Quarantine();
        quarantine.setMainWarehousePublicId(quarantineRequest.getMainWarehousePublicId());
        quarantine.setMainWarehouseName(mainWarehouse.getWarehouseName());
        quarantine.setQuarantineWarehousePublicId(quarantineRequest.getQuarantineWarehousePublicId());
        quarantine.setQuarantineWarehouseName(quarantineWarehouse.getWarehouseName());
        quarantine.setMainWarehouseAddress(mainWarehouse.getWarehouseCity());
        quarantine.setQuarantineStatus(QuarantineStatus.Pending);
        personOpt.ifPresent(quarantine::setCreatedBy);
        quarantine.setCreationTime(LocalDateTime.now());
        quarantine.setTransferType(TransferType.TO_QUARANTINE_WAREHOUSE);

        Quarantine savedQuarantine = quarantineRepository.save(quarantine);

        quarantineRequest.getQuarantineProducts().forEach(quarantineProductRequest -> {
            QuarantineProducts quarantineProduct = new QuarantineProducts();
            Optional<InventoryProducts> inventoryProductsOpt = inventoryProductsRepository.findByInventoryAndPublicIdAndDeletedIsFalse(
                    inventory,
                    quarantineProductRequest.getInventoryProductPublicId());
            if (inventoryProductsOpt.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Inventory product not found")
                        .userMessage("The specified inventory product does not exist or has been deleted.");
            }
            InventoryProducts inventoryProduct = inventoryProductsOpt.get();

            Optional<RejectionsCode> optionalRejectionsCode = rejectionCodeRepository.findRejectionsCodeByPublicIdAndDeletedIsFalse(
                    quarantineProductRequest.getReasonPublicId());
            if (optionalRejectionsCode.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Rejection code not found")
                        .userMessage("The specified rejection code does not exist or has been deleted.");
            }
            if (inventoryProduct.getAvailableStock() < quarantineProductRequest.getQuantity()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Insufficient stock")
                        .userMessage("The requested quantity exceeds the available stock for the product.");
            }

            RejectionsCode rejectionsCode = optionalRejectionsCode.get();
            quarantineProduct.setInventoryProductPublicId(inventoryProduct.getPublicId());
            quarantineProduct.setProductCode(inventoryProduct.getProduct().getSku());
            quarantineProduct.setProductName(inventoryProduct.getProduct().getProductName());
            quarantineProduct.setQuantity(quarantineProductRequest.getQuantity());
            quarantineProduct.setReasonPublicId(String.valueOf(rejectionsCode.getPublicId()));
            quarantineProduct.setReasonDescription(rejectionsCode.getCode());
            quarantineProduct.setQuarantine(savedQuarantine);
            personOpt.ifPresent(quarantineProduct::setCreatedBy);
            quarantineProduct.setCreationTime(LocalDateTime.now());
            QuarantineProducts savedQuarantineProduct = quarantineProductRepository.save(quarantineProduct);
            savedQuarantine.addQuarantineProduct(savedQuarantineProduct);
        });

        Quarantine savedQuarantineWithProducts = quarantineRepository.save(savedQuarantine);

        if (savedQuarantineWithProducts.getQuarantineProducts().isEmpty()) {
            // delete the quarantine if no products were added
            quarantineRepository.delete(savedQuarantineWithProducts);
            return null;
        }

        String event = "Quarantine created";
        String description = String.format("Quarantine %s created by %s",
                savedQuarantineWithProducts.getPublicId().toString(),
                personOpt.map(Person::getFullName).orElse("Itibari System"));
        String action = "CREATE";
        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
        eventsRepository.save(new Events(savedQuarantineWithProducts.getPublicId().toString(),
                event,
                LocalDateTime.now(),
                description,
                action,
                personOptional.orElse(null)));
        return savedQuarantineWithProducts;
    }

    @Override
    public void addQuarantineProducts(UUID publicId, QuarantineRequest quarantineRequest, String name) {

        Optional<Person> personOpt = personRepository.findByEmailAndDeletedIsFalse(name);

        validateUpdateQuarantineRequest(quarantineRequest);

        Optional<Quarantine> quarantineOpt = quarantineRepository.findByPublicIdAndDeletedIsFalse(publicId);

        if (quarantineOpt.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Quarantine not found")
                    .userMessage("The specified quarantine does not exist or has been deleted.");
        }
        Quarantine quarantine = quarantineOpt.get();

        if (quarantine.getQuarantineStatus() != QuarantineStatus.Pending) {
            throw new BadRequestRestApiException()
                    .developerMessage("Quarantine is not in pending status")
                    .userMessage("The quarantine can only be updated when it is in pending status.");
        }

        Warehouse mainWarehouse = warehouseRepository.findByPublicIdAndDeletedIsFalse(quarantine.getMainWarehousePublicId())
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Main warehouse not found")
                        .userMessage("The specified main warehouse does not exist or has been deleted."));

        Inventory inventory = inventoryRepository.findByWarehouseAndDeletedIsFalse(mainWarehouse)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Main warehouse has no inventory")
                        .userMessage("The specified main warehouse does not have an inventory."));

        quarantineRequest.getQuarantineProducts().forEach(quarantineProductRequest -> {
            QuarantineProducts quarantineProduct = new QuarantineProducts();
            Optional<InventoryProducts> inventoryProductsOpt = inventoryProductsRepository.findByInventoryAndPublicIdAndDeletedIsFalse(
                    inventory,
                    quarantineProductRequest.getInventoryProductPublicId());
            if (inventoryProductsOpt.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Inventory product not found")
                        .userMessage("The specified inventory product does not exist or has been deleted.");
            }
            InventoryProducts inventoryProduct = inventoryProductsOpt.get();

            Optional<RejectionsCode> optionalRejectionsCode = rejectionCodeRepository.findRejectionsCodeByPublicIdAndDeletedIsFalse(
                    quarantineProductRequest.getReasonPublicId());
            if (optionalRejectionsCode.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Rejection code not found")
                        .userMessage("The specified rejection code does not exist or has been deleted.");
            }
            if (inventoryProduct.getAvailableStock() < quarantineProductRequest.getQuantity()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Insufficient stock")
                        .userMessage("The requested quantity exceeds the available stock for the product.");
            }

            RejectionsCode rejectionsCode = optionalRejectionsCode.get();
            quarantineProduct.setInventoryProductPublicId(inventoryProduct.getPublicId());
            quarantineProduct.setProductCode(inventoryProduct.getProduct().getSku());
            quarantineProduct.setProductName(inventoryProduct.getProduct().getProductName());
            quarantineProduct.setQuantity(quarantineProductRequest.getQuantity());
            quarantineProduct.setReasonPublicId(String.valueOf(rejectionsCode.getPublicId()));
            quarantineProduct.setReasonDescription(rejectionsCode.getDescription());
            quarantineProduct.setQuarantine(quarantine);
            personOpt.ifPresent(quarantineProduct::setCreatedBy);
            quarantineProduct.setCreationTime(LocalDateTime.now());

            QuarantineProducts savedQuarantineProduct = quarantineProductRepository.save(quarantineProduct);
            quarantine.addQuarantineProduct(savedQuarantineProduct);
        });

        quarantine.setUpdateDate(LocalDateTime.now());
        quarantine.setUpdatedBy(personOpt.orElse(null));

        Quarantine savedQuarantineWithProducts = quarantineRepository.save(quarantine);

        String event = "Quarantine products added";
        String description = String.format("Quarantine %s updated by %s",
                savedQuarantineWithProducts.getPublicId().toString(),
                personOpt.map(Person::getFullName).orElse("Itibari System"));
        String action = "ADD_PRODUCTS";
        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
        eventsRepository.save(new Events(savedQuarantineWithProducts.getPublicId().toString(),
                event,
                LocalDateTime.now(),
                description,
                action,
                personOptional.orElse(null)));
    }

    @Override
    public void removeQuarantineProducts(UUID publicId, UUID quarantineProductId, String name) {

        Optional<Person> personOpt = personRepository.findByEmailAndDeletedIsFalse(name);

        Optional<Quarantine> quarantineOpt = quarantineRepository.findByPublicIdAndDeletedIsFalse(publicId);

        if (quarantineOpt.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Quarantine not found")
                    .userMessage("The specified quarantine does not exist or has been deleted.");
        }
        Quarantine quarantine = quarantineOpt.get();

        if (quarantine.getQuarantineStatus() != QuarantineStatus.Pending) {
            throw new BadRequestRestApiException()
                    .developerMessage("Quarantine is not in pending status")
                    .userMessage("The quarantine can only be updated when it is in pending status.");
        }

        Optional<QuarantineProducts> quarantineProductOpt = quarantineProductRepository.findByPublicIdAndDeletedIsFalse(quarantineProductId);

        if (quarantineProductOpt.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Quarantine product not found")
                    .userMessage("The specified quarantine product does not exist or has been deleted.");
        }

        QuarantineProducts quarantineProduct = quarantineProductOpt.get();


        if (!quarantineProduct.getQuarantine().equals(quarantine)) {
            throw new BadRequestRestApiException()
                    .developerMessage("Quarantine product does not belong to the specified quarantine")
                    .userMessage("The specified quarantine product does not belong to the specified quarantine.");
        }

        if (quarantineProduct.isDisposed()) {
            throw new BadRequestRestApiException()
                    .developerMessage(String.format("Product %s is disposed can not be removed for future record", quarantineProduct.getProductName()))
                    .userMessage(String.format("Sorry, product %s is disposed can not be removed for future record", quarantineProduct.getProductName()));
        }

        quarantine.removeQuarantineProduct(quarantineProduct);
        quarantineProduct.setDeleted(true);
        quarantineProduct.setUpdateDate(LocalDateTime.now());
        quarantineProduct.setUpdatedBy(personOpt.orElse(null));
        quarantineProductRepository.save(quarantineProduct);

        quarantine.setUpdateDate(LocalDateTime.now());
        quarantine.setUpdatedBy(personOpt.orElse(null));

        Quarantine savedQuarantineWithProducts = quarantineRepository.save(quarantine);
        String event = "Quarantine product removed";

        String description = String.format("Quarantine %s products removed by %s",
                savedQuarantineWithProducts.getPublicId().toString(),
                personOpt.map(Person::getFullName).orElse("Itibari System"));

        String action = "REMOVE_PRODUCT";
        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
        eventsRepository.save(new Events(savedQuarantineWithProducts.getPublicId().toString(),
                event,
                LocalDateTime.now(),
                description,
                action,
                personOptional.orElse(null)));
    }

    @Override
    public Quarantine getQuarantineByPublicId(UUID publicId) {

        Optional<Quarantine> quarantineOpt = quarantineRepository.findByPublicIdAndDeletedIsFalse(publicId);

        if (quarantineOpt.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Quarantine not found")
                    .userMessage("The specified quarantine does not exist or has been deleted.");
        }

        return quarantineOpt.get();
    }

    @Override
    public Page<Quarantine> getAllQuarantines(String searchParam,
                                              UUID mainWarehousePublicId,
                                              UUID quarantineWarehousePublicId,
                                              QuarantineStatus status,
                                              TransferType transferType,
                                              LocalDate startDate,
                                              LocalDate endDate,
                                              Pageable pageable) {

        Specification<Quarantine> spec = Specification
                .where(QuarantineSpecification.isNotDeleted())
                .and(QuarantineSpecification.hasMainWarehousePublicId(mainWarehousePublicId))
                .and(QuarantineSpecification.hasQuarantineWarehousePublicId(quarantineWarehousePublicId))
                .and(QuarantineSpecification.hasStatus(status))
                .and(QuarantineSpecification.hasTransferType(transferType))
                .and(QuarantineSpecification.hasDateRange(startDate, endDate))
                .and(QuarantineSpecification.searchParamMatches(searchParam));
        return quarantineRepository.findAll(spec, pageable);
    }

    @Override
    public Page<QuarantineProducts> getQuarantineProducts(UUID publicId, String searchParam, Pageable pageable) {

        Optional<Quarantine> quarantineOpt = quarantineRepository.findByPublicIdAndDeletedIsFalse(publicId);

        if (quarantineOpt.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Quarantine not found")
                    .userMessage("The specified quarantine does not exist or has been deleted.");
        }

        Quarantine quarantine = quarantineOpt.get();

        return quarantineProductRepository.findByQuarantineAndDeletedIsFalse(quarantine, pageable);
    }

    @Override
    public void deleteQuarantine(UUID publicId, String name) {

        Quarantine quarantine = getQuarantineByPublicId(publicId);
        if (quarantine.getQuarantineStatus() != QuarantineStatus.Pending) {
            throw new BadRequestRestApiException()
                    .developerMessage("Quarantine is not in pending status")
                    .userMessage("The quarantine can only be deleted when it is in pending status.");
        }

        Optional<Person> personOpt = personRepository.findByEmailAndDeletedIsFalse(name);

        if (quarantine.getQuarantineProducts().stream().anyMatch(QuarantineProducts::isDisposed)) {
            throw new BadRequestRestApiException()
                    .developerMessage("Quarantine contains disposed products!")
                    .userMessage("Sorry, quarantine contains disposed products!");
        }

        // update the quarantine products to be deleted and inventory products to be decremented
        quarantine.getQuarantineProducts().forEach(quarantineProduct -> {
            quarantineProduct.setDeleted(true);
            quarantineProduct.setUpdateDate(LocalDateTime.now());
            quarantineProduct.setUpdatedBy(personOpt.orElse(null));
            quarantineProductRepository.save(quarantineProduct);
        });

        // delete the quarantine
        quarantine.setDeleted(true);
        quarantine.setUpdateDate(LocalDateTime.now());
        quarantine.setUpdatedBy(personOpt.orElse(null));
        quarantineRepository.save(quarantine);
    }

    @Transactional
    @Override
    public void approveOrRejectQuarantine(UUID publicId, QuarantineAction action, String name) {

        Optional<Person> personOpt = personRepository.findByEmailAndDeletedIsFalse(name);

        if (personOpt.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Person not found")
                    .userMessage("The specified person does not exist or has been deleted.");
        }
        Person person = personOpt.get();

        if (Validators.anyEqualNull(publicId, action)) {
            throw new BadRequestRestApiException()
                    .developerMessage("Invalid request")
                    .userMessage("Please provide all required fields for the quarantine approval or rejection request.");
        }

        Optional<Quarantine> quarantineOpt = quarantineRepository.findByPublicIdAndDeletedIsFalse(publicId);

        if (quarantineOpt.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Quarantine not found")
                    .userMessage("The specified quarantine does not exist or has been deleted.");
        }

        Quarantine quarantine = quarantineOpt.get();

        if (quarantine.getQuarantineStatus() != QuarantineStatus.Pending) {
            throw new BadRequestRestApiException()
                    .developerMessage("Quarantine is not in pending status")
                    .userMessage("The quarantine can only be approved or rejected when it is in pending status.");
        }
        Warehouse warehouse = warehouseRepository.findByPublicIdAndDeletedIsFalse(quarantine.getMainWarehousePublicId())
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Main warehouse not found")
                        .userMessage("The specified main warehouse does not exist or has been deleted."));

        Map<Product, Long> productLongMap = new HashMap<>();
        if (action == QuarantineAction.Approve) {

            Inventory inventory = inventoryRepository.findByWarehouseAndDeletedIsFalse(warehouse)
                    .orElseThrow(() -> new BadRequestRestApiException()
                            .developerMessage("Main warehouse has no inventory")
                            .userMessage("The specified main warehouse does not have an inventory."));

            // update the inventory products


            quarantine.getQuarantineProducts().forEach(quarantineProduct -> {
                Optional<InventoryProducts> inventoryProductOpt = inventoryProductsRepository.findByInventoryAndPublicIdAndDeletedIsFalse(
                        inventory,
                        quarantineProduct.getInventoryProductPublicId());
                if (inventoryProductOpt.isPresent()) {
                    InventoryProducts inventoryProduct = inventoryProductOpt.get();

                    // check if the inventory product has enough stock
                    if (inventoryProduct.getAvailableStock() < quarantineProduct.getQuantity()) {
                        throw new BadRequestRestApiException()
                                .developerMessage("Insufficient stock")
                                .userMessage("The requested quantity exceeds the available stock for the product.");
                    }

                    productLongMap.put(inventoryProduct.getProduct(), Long.valueOf(quarantineProduct.getQuantity()));
                }
            });

            stockUpdateHelper.increamentQuarantine(productLongMap, warehouse, InventoryEvent.INCREASE_QUARANTINE.name());

            quarantine.setQuarantineStatus(QuarantineStatus.Approved);
            quarantine.setApprovedBy(person.getFullName());
            quarantine.setApprovedAt(LocalDateTime.now());
        } else if (action == QuarantineAction.Decline) {
            quarantine.setQuarantineStatus(QuarantineStatus.Declined);
            quarantine.setApprovedBy(person.getFullName());
            quarantine.setApprovedAt(LocalDateTime.now());
        } else {
            throw new BadRequestRestApiException()
                    .developerMessage("Invalid action")
                    .userMessage("The specified action is invalid. It must be either APPROVE or REJECT.");
        }

        quarantine.setUpdateDate(LocalDateTime.now());
        quarantine.setUpdatedBy(personOpt.orElse(null));
        Quarantine savedQuarantineWithProducts = quarantineRepository.save(quarantine);

        if (savedQuarantineWithProducts.getQuarantineStatus() == QuarantineStatus.Approved
                && !productLongMap.isEmpty()) {

            for (Map.Entry<Product, Long> entry : productLongMap.entrySet()) {
                Product product = entry.getKey();
                Long quantity = entry.getValue();

                updateLiveStock(
                        savedQuarantineWithProducts.getMainWarehousePublicId(),
                        product.getPublicId(),
                        Math.toIntExact(quantity)
                );

                productStockHelper.updateProductStock(product, warehouse, quantity, StockMovementTransactionType.TRANSFER_TO_QUARANTINE);


                // record stock move
                stockMovementHelper.recordStockMovement(product,
                        Math.toIntExact(quantity),
                        StockMovementTransactionType.TRANSFER_TO_QUARANTINE,
                        TransactionBoundType.OUTGOING,
                        person.getFullName(),
                        warehouse);

            }
        }


        String event = "Quarantine " + (action == QuarantineAction.Approve ? "approved" : "rejected");
        String description = String.format("Quarantine %s created by %s",
                savedQuarantineWithProducts.getPublicId().toString(),
                personOpt.map(Person::getFullName).orElse("Itibari System"));
        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
        eventsRepository.save(new Events(savedQuarantineWithProducts.getPublicId().toString(),
                event,
                LocalDateTime.now(),
                description,
                action.name(),
                personOptional.orElse(null)));
    }

    private void updateLiveStock(UUID warehouseId, UUID productId, int quantity) {
        liveStockHelper.updateLiveStockValue(warehouseId,
                productId,
                quantity,
                ProductUpdateType.QUARANTINE_STOCK,
                LocalDate.now());
    }

    @Override
    public void returnProductToMainWarehouse(UUID publicId, UUID quarantineProductId, int quantity, String name) {

        Quarantine quarantine = getQuarantineByPublicId(publicId);
        if (quarantine.getQuarantineStatus() != QuarantineStatus.Approved) {
            throw new BadRequestRestApiException()
                    .developerMessage("Quarantine is not approved")
                    .userMessage("The quarantine can only be updated when it is in approved status.");
        }

        Optional<QuarantineProducts> quarantineProductOpt = quarantineProductRepository.findByPublicIdAndDeletedIsFalse(quarantineProductId);
        if (quarantineProductOpt.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Quarantine product not found")
                    .userMessage("The specified quarantine product does not exist or has been deleted.");
        }

        QuarantineProducts quarantineProduct = quarantineProductOpt.get();
        if (quarantineProduct.getQuantity() > 0 && quantity > quarantineProduct.getQuantity()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Invalid quantity")
                    .userMessage("The quantity to return cannot be greater than the quantity in quarantine.");
        }


        UUID quarantinePublicId = quarantineProduct.getQuarantine().getPublicId();
        if (!quarantinePublicId.equals(publicId)) {
            throw new BadRequestRestApiException()
                    .developerMessage("Quarantine product does not belong to the specified quarantine")
                    .userMessage("The specified quarantine product does not belong to the specified quarantine.");
        }


        Optional<Person> personOpt = personRepository.findByEmailAndDeletedIsFalse(name);
        if (personOpt.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Person not found")
                    .userMessage("The specified person does not exist or has been deleted.");
        }
        Person person = personOpt.get();

        quarantine.setTransferType(TransferType.FROM_QUARANTINE_WAREHOUSE);
        quarantine.setUpdateDate(LocalDateTime.now());
        quarantine.setUpdatedBy(person);

        quarantineProduct.setUpdateDate(LocalDateTime.now());
        quarantineProduct.setUpdatedBy(person);
        quarantineProduct.setQuantity(quarantineProduct.getQuantity() - quantity);
        quarantineProductRepository.save(quarantineProduct);

        Optional<InventoryProducts> inventoryProductOpt = inventoryProductsRepository.findByPublicIdAndDeletedIsFalse(quarantineProduct.getInventoryProductPublicId());

        if (inventoryProductOpt.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Inventory product not found")
                    .userMessage("The specified inventory product does not exist or has been deleted.");
        }

        InventoryProducts inventoryProduct = inventoryProductOpt.get();
        stockUpdateHelper.logStock(InventoryEvent.FROM_QUARANTINE.name(), OpBeforeAfter.Before,
                inventoryProduct.getProduct(),
                inventoryProduct, (long) quantity);

        inventoryProduct.setAvailableStock(inventoryProduct.getAvailableStock() + quantity);
        inventoryProduct.setQuarantinedStock(inventoryProduct.getQuarantinedStock() - quantity);

        inventoryProduct.getProduct().setAvailableStock(inventoryProduct.getProduct().getAvailableStock() + quantity);
        inventoryProduct.getProduct().setQuarantineStock(inventoryProduct.getProduct().getQuarantineStock() - quantity);

        Product saveProduct = productRepository.save(inventoryProduct.getProduct());

        InventoryProducts saveInventoryProducts = inventoryProductsRepository.save(inventoryProduct);

        stockUpdateHelper.logStock(InventoryEvent.FROM_QUARANTINE.name(), OpBeforeAfter.After, saveProduct,
                saveInventoryProducts, (long) quantity);

        Warehouse warehouse = saveInventoryProducts.getInventory().getWarehouse();

        updateLiveStockReturnFromQuarantine(warehouse.getPublicId(), saveProduct.getPublicId(), quantity);
        productStockHelper.updateProductStock(saveProduct, warehouse, (long) quantity, StockMovementTransactionType.TRANSFER_FROM_QUARANTINE);

        Quarantine savedQuarantineWithProducts = quarantineRepository.save(quarantine);

        String event = "Quarantine product returned to main warehouse";
        String description = String.format("Quarantine %s product returned by %s",
                savedQuarantineWithProducts.getPublicId().toString(),
                personOpt.map(Person::getFullName).orElse("Itibari System"));
        String action = "RETURN_PRODUCT";
        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
        eventsRepository.save(new Events(savedQuarantineWithProducts.getPublicId().toString(),
                event,
                LocalDateTime.now(),
                description,
                action,
                personOptional.orElse(null)));

        stockMovementHelper.recordStockMovement(saveProduct, quantity, StockMovementTransactionType.TRANSFER_FROM_QUARANTINE,
                TransactionBoundType.INCOMING, person.getFullName(), warehouse);
    }

    @Override
    public void disposeQuarantineProduct(UUID publicId, UUID quarantineProductId, UUID reasonCodePublicId, ProductDisposalType disposalType, DisposalRequest disposalRequest, String name) {
        Optional<Person> personOpt = personRepository.findByEmailAndDeletedIsFalse(name);
        if (!personOpt.isPresent()) {
            throw new BadRequestRestApiException()
                    .developerMessage("User not found")
                    .userMessage("Sorry, user not found");
        }

        Quarantine quarantine = getQuarantineByPublicId(publicId);

        if (quarantine.getQuarantineStatus() != QuarantineStatus.Approved) {
            throw new BadRequestRestApiException()
                    .developerMessage("Quarantine is not approved")
                    .userMessage("Sorry, quarantine is not approved");
        }

        Optional<QuarantineProducts> quarantineProductOpt = quarantineProductRepository.findByPublicIdAndDeletedIsFalse(quarantineProductId);
        if (quarantineProductOpt.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Quarantine product not found")
                    .userMessage("Quarantine product not found");
        }

        RejectionsCode rejectionsCode = rejectionCodeRepository.findRejectionsCodeByPublicIdAndDeletedIsFalse(reasonCodePublicId).orElse(null);

        if (rejectionsCode == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Reason code not found")
                    .userMessage("Reason code not found");
        }

        QuarantineProducts quarantineProduct = quarantineProductOpt.get();

        if (quarantineProduct.isDisposed()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product already disposed")
                    .userMessage("Product already disposed");
        }

        if (disposalType == ProductDisposalType.Dispose) {
            quarantineProduct.setDisposalReason(rejectionsCode);
            quarantineProduct.setDisposedStatus(QuarantineProductStatus.Pending_Disposal);
            quarantineProduct.setUpdatedBy(personOpt.get());
            quarantineProduct.setDisposalComment(disposalRequest.comment());
            quarantineProduct.setDisposalInitiatedBy(personOpt.get());
        }

        if (disposalType == ProductDisposalType.Approve) {
            if (quarantineProduct.getDisposedStatus() != QuarantineProductStatus.Pending_Disposal) {
                throw new BadRequestRestApiException()
                        .developerMessage("Product has be pending disposal")
                        .userMessage("Product has be pending disposal");
            }
            quarantineProduct.setDisposalReason(rejectionsCode);
            quarantineProduct.setDisposedStatus(QuarantineProductStatus.Disposal_Approved);
            quarantineProduct.setDisposed(true);
            quarantineProduct.setDisposedQuantity(quarantineProduct.getQuantity());
            quarantineProduct.setQuantity(0);
            quarantineProduct.setUpdatedBy(personOpt.get());
            quarantineProduct.setDisposalApprovedBy(personOpt.get());

            updateInventoryProducts(quarantineProduct);
        }

        if (disposalType == ProductDisposalType.Decline) {
            quarantineProduct.setDisposalReason(rejectionsCode);
            quarantineProduct.setDisposedStatus(QuarantineProductStatus.Disposal_Declined);
            quarantineProduct.setUpdatedBy(personOpt.get());
            quarantineProduct.setDisposalApprovedBy(personOpt.get());
        }

        quarantineProductRepository.save(quarantineProduct);
    }

    @Override
    public Page<QuarantineProducts> getPendingDisposedQuarantineProducts(String searchParam, Pageable pageable) {

        if (searchParam != null && searchParam.length() > 3) {
            return quarantineProductRepository.findByProductNameAndDeletedIsFalse(searchParam, pageable);
        }
        return quarantineProductRepository.findByDeletedIsFalseAndDisposedStatus(QuarantineProductStatus.Pending_Disposal, pageable);
    }

    @Override
    public Page<DisposedQuarantineProducts> getApprovedDisposedQuarantineProducts(String searchParam, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Specification<QuarantineProducts> spec = Specification
                .where(DisposedQuarantineProductSpecification.isNotDeleted())
                .and(DisposedQuarantineProductSpecification.hasSearchTerm(searchParam))
                .and(DisposedQuarantineProductSpecification.hasDateRange(startDate, endDate));

        List<QuarantineProducts> all = quarantineProductRepository.findAll(spec, pageable).getContent();
        List<DisposedQuarantineProducts> convertToDisposedQuarantineProducts = convertToDisposedQuarantineProducts(all);

        return new PageImpl<>(convertToDisposedQuarantineProducts, pageable, convertToDisposedQuarantineProducts.size());
    }

    @Override
    public ByteArrayOutputStream runExportQuarantineDisposedProducts(String searchParam, LocalDate startDate, LocalDate endDate, StatementExportType exportType, Pageable pageable, Principal principal) throws DocumentException, IOException {
        Page<DisposedQuarantineProducts> disposedQuarantineProducts = getApprovedDisposedQuarantineProducts(searchParam, startDate, endDate, pageable);

        if (exportType == StatementExportType.EXCEL){
            return quarantineDisposedProductExport.createExcel(disposedQuarantineProducts.getContent(), principal);
        }else{
            return quarantineDisposedProductExport.generateLeadPdf(disposedQuarantineProducts.getContent(), principal);
        }
    }

    private List<DisposedQuarantineProducts> convertToDisposedQuarantineProducts(List<QuarantineProducts> all) {
        return all.stream()
                .map(item -> {
                    InventoryProducts inventoryProduct = inventoryProductsRepository
                            .findByPublicIdAndDeletedIsFalse(item.getInventoryProductPublicId())
                            .orElse(null);

                    BigDecimal disposalValue = BigDecimal.ZERO;
                    if (inventoryProduct != null && inventoryProduct.getProduct() != null) {
                        BigDecimal buyingPrice = inventoryProduct.getProduct().getBuyingPrice();
                        if (buyingPrice != null && item.getDisposedQuantity() != null) {
                            disposalValue = buyingPrice.multiply(BigDecimal.valueOf(item.getDisposedQuantity()));
                        }
                    }

                    return DisposedQuarantineProducts.builder()
                            .publicId(item.getPublicId())
                            .disposalDate(DateFormatter.formatDate(item.getUpdateDate()))
                            .sku(item.getProductCode())
                            .productName(item.getProductName())
                            .disposed(item.isDisposed())
                            .disposalValue(String.valueOf(disposalValue))
                            .quantity(item.getDisposedQuantity() != null ? item.getDisposedQuantity().toString() :"0")
                            .reasonCode(Optional.ofNullable(item.getDisposalReason())
                                    .map(RejectionsCode::getCode)
                                    .orElse("--"))
                            .comment(item.getDisposalComment())
                            .initiatedBy(Optional.ofNullable(item.getDisposalInitiatedBy())
                                    .map(Person::getFullName)
                                    .orElse("--"))
                            .approvedBy(Optional.ofNullable(item.getDisposalApprovedBy())
                                    .map(Person::getFullName)
                                    .orElse("--"))
                            .build();
                })
                .toList();
    }



    private void updateInventoryProducts(QuarantineProducts quarantineProduct) {
        Optional<InventoryProducts> inventoryProductOpt = inventoryProductsRepository.findByPublicIdAndDeletedIsFalse(quarantineProduct.getInventoryProductPublicId());

        if (inventoryProductOpt.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Inventory product not found")
                    .userMessage("The specified inventory product does not exist or has been deleted.");
        }

        Integer quantity = quarantineProduct.getDisposedQuantity();

        InventoryProducts inventoryProduct = inventoryProductOpt.get();

        inventoryProduct.setQuarantinedStock(inventoryProduct.getQuarantinedStock() - quantity);

        inventoryProduct.getProduct().setQuarantineStock(inventoryProduct.getProduct().getQuarantineStock() - quantity);

        Product saveProduct = productRepository.save(inventoryProduct.getProduct());

        InventoryProducts saveInventoryProducts = inventoryProductsRepository.save(inventoryProduct);

        Warehouse warehouse = saveInventoryProducts.getInventory().getWarehouse();

        updateLiveStockQuarantineDispose(warehouse.getPublicId(), saveProduct.getPublicId(), quantity);
        productStockHelper.updateProductStock(saveProduct, warehouse, (long) quantity, StockMovementTransactionType.TRANSFER_FROM_QUARANTINE);

    }

    private void updateLiveStockQuarantineDispose(UUID warehousePublicId, UUID productPublicId, int quantity) {
        liveStockHelper.updateLiveStockValue(warehousePublicId,
                productPublicId,
                quantity,
                ProductUpdateType.QUARANTINE_STOCK_DISPOSED,
                LocalDate.now());
    }

    private void updateLiveStockReturnFromQuarantine(UUID warehousePublicId, UUID productPublicId, int quantity) {
        liveStockHelper.updateLiveStockValue(warehousePublicId,
                productPublicId,
                quantity,
                ProductUpdateType.RETURNED_FROM_QUARANTINE_STOCK,
                LocalDate.now());
    }

    private void validateUpdateQuarantineRequest(QuarantineRequest quarantineRequest) {

        if (Validators.anyEqualNull(quarantineRequest.getQuarantineProducts())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Quarantine update request is invalid")
                    .userMessage("Please provide all required fields for the quarantine update request.");
        }

        if (quarantineRequest.getQuarantineProducts().isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Quarantine update request is invalid")
                    .userMessage("Please provide at least one product for the quarantine update request.");
        }

        if (quarantineRequest.getQuarantineProducts().stream()
                .anyMatch(product -> product.getQuantity() <= 0)) {
            throw new BadRequestRestApiException()
                    .developerMessage("Quarantine update request is invalid")
                    .userMessage("Product quantity must be greater than zero.");
        }

        if (quarantineRequest.getQuarantineProducts().stream()
                .anyMatch(product -> product.getReasonPublicId() == null)) {
            throw new BadRequestRestApiException()
                    .developerMessage("Quarantine update request is invalid")
                    .userMessage("Product reason must be provided for each product in the quarantine update request.");
        }
    }

    private void validateQuarantineRequest(QuarantineRequest quarantineRequest) {

        if (Validators.anyEqualNull(quarantineRequest.getQuarantineProducts(),
                quarantineRequest.getMainWarehousePublicId(),
                quarantineRequest.getQuarantineWarehousePublicId())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Quarantine request is invalid")
                    .userMessage("Please provide all required fields for the quarantine request.");
        }

        if (quarantineRequest.getQuarantineProducts().isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Quarantine request is invalid")
                    .userMessage("Please provide at least one product for the quarantine request.");
        }

        if (quarantineRequest.getQuarantineProducts().stream()
                .anyMatch(product -> product.getQuantity() <= 0)) {
            throw new BadRequestRestApiException()
                    .developerMessage("Quarantine request is invalid")
                    .userMessage("Product quantity must be greater than zero.");
        }

        if (quarantineRequest.getQuarantineProducts().stream()
                .anyMatch(product -> product.getReasonPublicId() == null)) {
            throw new BadRequestRestApiException()
                    .developerMessage("Quarantine request is invalid")
                    .userMessage("Product reason must be provided for each product in the quarantine request.");
        }
    }
}
