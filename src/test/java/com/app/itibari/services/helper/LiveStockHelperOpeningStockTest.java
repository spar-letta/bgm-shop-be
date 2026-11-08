package com.app.itibari.services.helper;

import com.app.itibari.dataTypes.ProductUpdateType;
import com.app.itibari.models.entities.Inventory;
import com.app.itibari.models.entities.InventoryProducts;
import com.app.itibari.models.entities.LiveStockEntity;
import com.app.itibari.models.entities.Product;
import com.app.itibari.models.entities.Warehouse;
import com.app.itibari.repository.InventoryProductsRepository;
import com.app.itibari.repository.InventoryRepository;
import com.app.itibari.repository.LiveStockEntityRepository;
import com.app.itibari.repository.ProductRepository;
import com.app.itibari.repository.WarehouseRepository;
import com.app.itibari.utils.helpers.WebhookClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LiveStockHelperOpeningStockTest {

    private static final UUID WAREHOUSE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PRODUCT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID INVENTORY_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID INVENTORY_ITEM_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Mock private InventoryRepository inventoryRepository;
    @Mock private WarehouseRepository warehouseRepository;
    @Mock private InventoryProductsRepository inventoryProductsRepository;
    @Mock private ProductRepository productRepository;
    @Mock private LiveStockEntityRepository liveStockEntityRepository;
    @Mock private LogisticsHelper logisticsHelper;
    @Mock private WebhookClient webhookClient;

    private LiveStockHelper liveStockHelper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        liveStockHelper = new LiveStockHelper(
                inventoryRepository,
                warehouseRepository,
                inventoryProductsRepository,
                productRepository,
                liveStockEntityRepository,
                logisticsHelper,
                webhookClient);
    }

    @Test
    void openingStockUsesLatestDuplicateYesterdayRowAndDoesNotSendChat() {
        LocalDate today = LocalDate.of(2026, 10, 22);
        Warehouse warehouse = warehouse(WAREHOUSE_ID);
        Inventory inventory = inventory(INVENTORY_ID, warehouse);
        Product product = product(PRODUCT_ID);
        InventoryProducts inventoryProduct = inventoryProduct(INVENTORY_ITEM_ID, inventory, product);

        stubLookups(warehouse, inventory, product, inventoryProduct);
        when(liveStockEntityRepository.findLatestLiveStockRows(
                INVENTORY_ITEM_ID.toString(), INVENTORY_ID.toString(), WAREHOUSE_ID.toString(), today))
                .thenReturn(List.of());
        when(liveStockEntityRepository.findLatestLiveStockRows(
                INVENTORY_ITEM_ID.toString(), INVENTORY_ID.toString(), WAREHOUSE_ID.toString(), today.minusDays(1)))
                .thenReturn(List.of());
        when(liveStockEntityRepository.save(any(LiveStockEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LiveStockEntity stale = liveStock(1L, 7);
        LiveStockEntity latest = liveStock(9L, 15);
        when(liveStockEntityRepository.findLatestLiveStockRowsByInventoryItemAndDate(
                INVENTORY_ITEM_ID.toString(), today.minusDays(1)))
                .thenReturn(List.of(latest, stale));

        assertDoesNotThrow(() -> liveStockHelper.updateLiveStockValue(
                WAREHOUSE_ID, PRODUCT_ID, 0, ProductUpdateType.OPENING_STOCK, today));

        ArgumentCaptor<LiveStockEntity> captor = ArgumentCaptor.forClass(LiveStockEntity.class);
        verify(liveStockEntityRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        LiveStockEntity saved = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertEquals(15, saved.getOpeningStock());
        verify(webhookClient, never()).sendMessage(any(), any());
    }

    @Test
    void ensureTodayLiveStockRecordCopiesYesterdayClosingAndDoesNotCreateWhenTodayExists() {
        LocalDate today = LocalDate.of(2026, 10, 22);
        Warehouse warehouse = warehouse(WAREHOUSE_ID);
        Inventory inventory = inventory(INVENTORY_ID, warehouse);
        Product product = product(PRODUCT_ID);
        InventoryProducts inventoryProduct = inventoryProduct(INVENTORY_ITEM_ID, inventory, product);

        LiveStockEntity todayRow = liveStock(3L, 4);
        when(liveStockEntityRepository.findLatestLiveStockRows(
                INVENTORY_ITEM_ID.toString(), INVENTORY_ID.toString(), WAREHOUSE_ID.toString(), today))
                .thenReturn(List.of(todayRow));

        boolean created = liveStockHelper.ensureTodayLiveStockRecord(
                warehouse, inventory, inventoryProduct, product, today);

        org.junit.jupiter.api.Assertions.assertFalse(created);
        verify(liveStockEntityRepository, never()).save(any());
    }

    @Test
    void ensureTodayLiveStockRecordCreatesFromYesterdayWhenMissing() {
        LocalDate today = LocalDate.of(2026, 10, 22);
        Warehouse warehouse = warehouse(WAREHOUSE_ID);
        Inventory inventory = inventory(INVENTORY_ID, warehouse);
        Product product = product(PRODUCT_ID);
        InventoryProducts inventoryProduct = inventoryProduct(INVENTORY_ITEM_ID, inventory, product);

        LiveStockEntity yesterday = liveStock(9L, 21);
        yesterday.setIncomingStock(4);
        when(liveStockEntityRepository.findLatestLiveStockRows(
                INVENTORY_ITEM_ID.toString(), INVENTORY_ID.toString(), WAREHOUSE_ID.toString(), today))
                .thenReturn(List.of());
        when(liveStockEntityRepository.findLatestLiveStockRows(
                INVENTORY_ITEM_ID.toString(), INVENTORY_ID.toString(), WAREHOUSE_ID.toString(), today.minusDays(1)))
                .thenReturn(List.of(yesterday));
        when(liveStockEntityRepository.save(any(LiveStockEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        boolean created = liveStockHelper.ensureTodayLiveStockRecord(
                warehouse, inventory, inventoryProduct, product, today);

        org.junit.jupiter.api.Assertions.assertTrue(created);
        ArgumentCaptor<LiveStockEntity> captor = ArgumentCaptor.forClass(LiveStockEntity.class);
        verify(liveStockEntityRepository).save(captor.capture());
        assertEquals(21, captor.getValue().getOpeningStock());
        assertEquals(4, captor.getValue().getIncomingStock());
        verify(webhookClient, never()).sendMessage(any(), any());
    }

    private void stubLookups(Warehouse warehouse, Inventory inventory, Product product, InventoryProducts inventoryProduct) {
        when(warehouseRepository.findByPublicIdAndDeletedIsFalse(WAREHOUSE_ID)).thenReturn(Optional.of(warehouse));
        when(inventoryRepository.findByWarehouseAndDeletedIsFalse(warehouse)).thenReturn(Optional.of(inventory));
        when(productRepository.findByPublicIdAndDeletedIsFalse(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(inventoryProductsRepository.findByInventoryAndProductAndDeletedIsFalse(inventory, product))
                .thenReturn(Optional.of(inventoryProduct));
    }

    private Warehouse warehouse(UUID publicId) {
        Warehouse warehouse = new Warehouse();
        warehouse.setPublicId(publicId);
        warehouse.setWarehouseName("Main");
        warehouse.setDeleted(false);
        return warehouse;
    }

    private Inventory inventory(UUID publicId, Warehouse warehouse) {
        Inventory inventory = new Inventory();
        inventory.setPublicId(publicId);
        inventory.setWarehouse(warehouse);
        inventory.setDeleted(false);
        return inventory;
    }

    private Product product(UUID publicId) {
        Product product = new Product();
        product.setPublicId(publicId);
        product.setProductName("Sugar");
        product.setSku("SUG-1");
        product.setDeleted(false);
        return product;
    }

    private InventoryProducts inventoryProduct(UUID publicId, Inventory inventory, Product product) {
        InventoryProducts inventoryProduct = new InventoryProducts();
        inventoryProduct.setPublicId(publicId);
        inventoryProduct.setInventory(inventory);
        inventoryProduct.setProduct(product);
        inventoryProduct.setClosingStock(3L);
        inventoryProduct.setDeleted(false);
        return inventoryProduct;
    }

    private LiveStockEntity liveStock(Long id, int closingStock) {
        LiveStockEntity entity = new LiveStockEntity();
        entity.setId(id);
        entity.setClosingStock(closingStock);
        return entity;
    }
}
