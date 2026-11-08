package com.app.itibari.services;

import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.ClosingUpdateRequest;
import com.app.itibari.repository.*;
import com.app.itibari.services.helper.StockMovementHelper;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.helpers.Validators;
import com.app.itibari.utils.specification.InventoryProductSpecification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;

    private final WarehouseRepository warehouseRepository;

    private final InventoryProductsRepository inventoryProductsRepository;

    private final PersonRepository personRepository;

    private final EventsRepository eventsRepository;
    private final ProductStockRepository productStockRepository;

    private final LiveStockEntityRepository liveStockEntityRepository;
    private final StockMovementHelper stockMovementHelper;

    public InventoryServiceImpl(InventoryRepository inventoryRepository,
                                WarehouseRepository warehouseRepository,
                                InventoryProductsRepository inventoryProductsRepository,
                                PersonRepository personRepository,
                                EventsRepository eventsRepository,
                                LiveStockEntityRepository liveStockEntityRepository, StockMovementHelper stockMovementHelper, ProductStockRepository productStockRepository, ProductStockRepository productStockRepository1) {

        this.inventoryRepository = inventoryRepository;
        this.warehouseRepository = warehouseRepository;
        this.inventoryProductsRepository = inventoryProductsRepository;
        this.personRepository = personRepository;
        this.eventsRepository = eventsRepository;
        this.liveStockEntityRepository = liveStockEntityRepository;
        this.stockMovementHelper = stockMovementHelper;
        this.productStockRepository = productStockRepository1;
    }

    @Override
    public Inventory getInventoryPerWarehouse(UUID warehousePublicId) {

        Warehouse warehouse;

        if (warehousePublicId != null) {
            Optional<Warehouse> optionalWarehouse = warehouseRepository.findByPublicIdAndDeletedIsFalse(warehousePublicId);
            if (optionalWarehouse.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Warehouse not found")
                        .userMessage("Sorry, warehouse not found");
            } else {
                warehouse = optionalWarehouse.get();
            }
        } else {
            Optional<Warehouse> optionalWarehouse = warehouseRepository.findBySuperWarehouseIsTrueAndDeletedIsFalse();

            if (optionalWarehouse.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Warehouse not found")
                        .userMessage("Sorry, warehouse not found");
            } else {
                warehouse = optionalWarehouse.get();
            }
        }
        return inventoryRepository.findByWarehouseAndDeletedIsFalse(warehouse).orElse(null);
    }

    @Override
    public Page<InventoryProducts> getInventoryProducts(UUID inventoryPublicId,
                                                        UUID productPublicId,
                                                        String searchParam,
                                                        Pageable pageable) {

        Specification<InventoryProducts> specification = Specification
                .where(InventoryProductSpecification.isNotDeleted())
                .and(InventoryProductSpecification.hasInventory(inventoryPublicId))
                .and(InventoryProductSpecification.hasProductNameOrSkuLike(searchParam))
                .and(InventoryProductSpecification.hasProduct(productPublicId));

        return inventoryProductsRepository.findAll(specification, pageable);
    }

    @Override
    public Page<Inventory> getAllInventories(String searchParam, Pageable pageable) {

        return inventoryRepository.findAllByDeletedIsFalse(pageable);
    }

    @Override
    public Page<InventoryProducts> getInventoryProductsByWarehouse(UUID warehousePublicId, String searchParam, Pageable pageable) {

        Optional<Warehouse> optionalWarehouse = warehouseRepository.findByPublicIdAndDeletedIsFalse(warehousePublicId);

        if (optionalWarehouse.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Warehouse not found")
                    .userMessage("Sorry, warehouse not found");
        }

        Warehouse warehouse = optionalWarehouse.get();

        Inventory inventory = inventoryRepository.findByWarehouseAndDeletedIsFalse(warehouse).orElse(null);

        if (inventory == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Inventory not found for the specified warehouse")
                    .userMessage("Sorry, inventory not found for the specified warehouse");
        }

        return inventoryProductsRepository.findAllByInventoryAndDeletedIsFalse(inventory, pageable);
    }

    @Override
    public void updateClosingStock(ClosingUpdateRequest request, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);
        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("User not found")
                    .userMessage("Sorry, user not found");
        }
        Person person = optionalPerson.get();

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("No items to update")
                    .userMessage("Please provide items to update");
        }
        if (request.getInventoryPublicId() == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Inventory public ID is required")
                    .userMessage("Please provide a valid inventory public ID");
        }

        Optional<Inventory> optionalInventory = inventoryRepository.findInventoryByPublicIdAndDeletedIsFalse(request.getInventoryPublicId());
        if (optionalInventory.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Inventory not found")
                    .userMessage("Sorry, inventory not found");
        }

        Inventory inventory = optionalInventory.get();


        request.getItems().forEach(item -> {
            if (Validators.anyEqualNull(item.getClosingStock(), item.getInventoryItemPublicId())) {
                throw new BadRequestRestApiException()
                        .developerMessage("Invalid item data")
                        .userMessage("Please provide valid item data");
            }
            Optional<InventoryProducts> optionalInventoryProducts = inventoryProductsRepository.findByPublicIdAndDeletedIsFalse(item.getInventoryItemPublicId());
            if (optionalInventoryProducts.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Inventory item not found")
                        .userMessage("Sorry, inventory item not found");
            }
            InventoryProducts inventoryProducts = optionalInventoryProducts.get();

            int previousClosingStock = inventoryProducts.getClosingStock() != null ? inventoryProducts.getClosingStock().intValue() : 0;

            inventoryProducts.setClosingStock(Long.valueOf(item.getClosingStock()));
            inventoryProducts.setUpdatedBy(person);
            InventoryProducts saveInventoryProducts = inventoryProductsRepository.save(inventoryProducts);
            setProductStockClosing(saveInventoryProducts.getProduct().getPublicId(), item.getClosingStock());

            stockMovementHelper.recordClosingStockUpdate(inventoryProducts, item.getClosingStock(), person,previousClosingStock);

            try {
                updateStateStock(inventoryProducts, item.getClosingStock());
            } catch (Exception e) {
                log.info("Failed to update state stock: {}", e.getMessage());
            }
        });

        // event logging can be added here

        inventory.setUpdatedBy(person);
        inventoryRepository.save(inventory);

        String description = "Updated closing stock for " + request.getItems().size() + " items in inventory " + inventory.getPublicId();
        eventsRepository.save(new Events(request.getInventoryPublicId().toString(),
                                         "Closing Stock Update",
                                         LocalDateTime.now(),
                                         description,
                                         "UPDATE",
                                         person));
    }

    private void setProductStockClosing(UUID publicId, Integer closingStock) {
        ZoneId nairobiZone = ZoneId.of("Africa/Nairobi");
        LocalDate nairobiDate = LocalDate.now(nairobiZone);

        ProductStock productStock = productStockRepository.findByDeletedFalseAndProductPublicIdAndRecordToday(publicId, nairobiDate).orElse(null);

        if (productStock != null){
            productStock.setClosingStock(Long.valueOf(closingStock));

            productStockRepository.save(productStock);
        }
    }

    private void updateStateStock(InventoryProducts inventoryProducts, Integer closingStock) {

        Optional<LiveStockEntity> optionalLiveStockEntity = liveStockEntityRepository.findLiveStockEntityByInventoryItemPublicIdAndLiveDateAndDeletedIsFalse(
                String.valueOf(inventoryProducts.getPublicId()),
                LocalDate.now());

        if (optionalLiveStockEntity.isPresent()) {
            LiveStockEntity liveStockEntity = optionalLiveStockEntity.get();
            liveStockEntity.setClosingStock(closingStock);
            liveStockEntityRepository.save(liveStockEntity);
        }
    }

    @Override
    public Inventory getInventory(UUID inventoryPublicId) {
        return null;
    }
}
