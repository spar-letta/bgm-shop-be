package com.app.itibari.services.helper;

import com.app.itibari.models.entities.Inventory;
import com.app.itibari.models.entities.InventoryProducts;
import com.app.itibari.models.entities.Product;
import com.app.itibari.models.entities.ProductStock;
import com.app.itibari.models.entities.Warehouse;
import com.app.itibari.repository.InventoryProductsRepository;
import com.app.itibari.repository.InventoryRepository;
import com.app.itibari.repository.ProductStockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DailyStockRecordCheckerTest {

    private static final UUID WAREHOUSE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID INVENTORY_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PRODUCT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID INACTIVE_PRODUCT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 22);

    @Mock private InventoryRepository inventoryRepository;
    @Mock private InventoryProductsRepository inventoryProductsRepository;
    @Mock private ProductStockRepository productStockRepository;
    @Mock private LiveStockHelper liveStockHelper;

    private DailyStockRecordChecker checker;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        checker = new DailyStockRecordChecker(
                inventoryRepository,
                inventoryProductsRepository,
                productStockRepository,
                liveStockHelper);
    }

    @Test
    void copiesMissingProductStockFromYesterdayAndAsksLiveStockHelperToFill() {
        Warehouse warehouse = warehouse();
        Inventory inventory = inventory(warehouse);
        Product product = product(PRODUCT_ID, false);
        InventoryProducts inventoryProduct = inventoryProduct(product, 4L);

        ProductStock yesterday = new ProductStock();
        yesterday.setId(88L);
        yesterday.setWarehouse(warehouse);
        yesterday.setAvailableStock(10L);
        yesterday.setReservedStock(2L);
        yesterday.setQuarantinedStock(1L);
        yesterday.setSellPrice(new BigDecimal("12.50"));

        when(inventoryRepository.findAllByDeletedIsFalse()).thenReturn(List.of(inventory));
        when(inventoryProductsRepository.findByInventoryAndDeletedIsFalse(inventory))
                .thenReturn(List.of(inventoryProduct));
        when(productStockRepository.findAllByRecordTodayAndDeletedFalseAndProductPublicIdAndInventoryPublicId(
                TODAY, PRODUCT_ID, INVENTORY_ID)).thenReturn(List.of());
        when(productStockRepository.findAllByRecordTodayAndDeletedFalseAndProductPublicIdAndInventoryPublicId(
                TODAY.minusDays(1), PRODUCT_ID, INVENTORY_ID)).thenReturn(List.of(yesterday));
        when(liveStockHelper.ensureTodayLiveStockRecord(warehouse, inventory, inventoryProduct, product, TODAY))
                .thenReturn(true);

        checker.ensureTodayRecords(TODAY);

        ArgumentCaptor<ProductStock> captor = ArgumentCaptor.forClass(ProductStock.class);
        verify(productStockRepository).save(captor.capture());
        ProductStock saved = captor.getValue();
        assertEquals(TODAY, saved.getRecordToday());
        assertEquals(10L, saved.getAvailableStock());
        assertEquals(2L, saved.getReservedStock());
        assertEquals(1L, saved.getQuarantinedStock());
        assertEquals(13L, saved.getOpeningStock());
        verify(liveStockHelper).ensureTodayLiveStockRecord(warehouse, inventory, inventoryProduct, product, TODAY);
    }

    @Test
    void createsProductStockFromInventoryWhenYesterdayIsMissing() {
        Warehouse warehouse = warehouse();
        Inventory inventory = inventory(warehouse);
        Product product = product(PRODUCT_ID, false);
        InventoryProducts inventoryProduct = inventoryProduct(product, 9L);
        inventoryProduct.setAvailableStock(7L);
        inventoryProduct.setReservedStock(1L);
        inventoryProduct.setQuarantinedStock(1L);

        when(inventoryRepository.findAllByDeletedIsFalse()).thenReturn(List.of(inventory));
        when(inventoryProductsRepository.findByInventoryAndDeletedIsFalse(inventory))
                .thenReturn(List.of(inventoryProduct));
        when(productStockRepository.findAllByRecordTodayAndDeletedFalseAndProductPublicIdAndInventoryPublicId(
                any(), eq(PRODUCT_ID), eq(INVENTORY_ID))).thenReturn(List.of());
        when(liveStockHelper.ensureTodayLiveStockRecord(any(), any(), any(), any(), any())).thenReturn(false);

        checker.ensureTodayRecords(TODAY);

        ArgumentCaptor<ProductStock> captor = ArgumentCaptor.forClass(ProductStock.class);
        verify(productStockRepository).save(captor.capture());
        ProductStock saved = captor.getValue();
        assertEquals(7L, saved.getAvailableStock());
        assertEquals(9L, saved.getOpeningStock());
        assertEquals(warehouse, saved.getWarehouse());
    }

    @Test
    void skipsInactiveProductsAndDoesNotOverwriteExistingTodayRows() {
        Warehouse warehouse = warehouse();
        Inventory inventory = inventory(warehouse);
        Product inactive = product(INACTIVE_PRODUCT_ID, true);
        Product active = product(PRODUCT_ID, false);
        InventoryProducts inactiveItem = inventoryProduct(inactive, 1L);
        InventoryProducts activeItem = inventoryProduct(active, 5L);

        ProductStock today = new ProductStock();
        today.setId(1L);

        when(inventoryRepository.findAllByDeletedIsFalse()).thenReturn(List.of(inventory));
        when(inventoryProductsRepository.findByInventoryAndDeletedIsFalse(inventory))
                .thenReturn(List.of(inactiveItem, activeItem));
        when(productStockRepository.findAllByRecordTodayAndDeletedFalseAndProductPublicIdAndInventoryPublicId(
                TODAY, PRODUCT_ID, INVENTORY_ID)).thenReturn(List.of(today));
        when(liveStockHelper.ensureTodayLiveStockRecord(warehouse, inventory, activeItem, active, TODAY))
                .thenReturn(false);

        checker.ensureTodayRecords(TODAY);

        verify(productStockRepository, never()).save(any());
        verify(liveStockHelper, never()).ensureTodayLiveStockRecord(
                any(), any(), eq(inactiveItem), eq(inactive), any());
        verify(liveStockHelper).ensureTodayLiveStockRecord(warehouse, inventory, activeItem, active, TODAY);
    }

    private Warehouse warehouse() {
        Warehouse warehouse = new Warehouse();
        warehouse.setPublicId(WAREHOUSE_ID);
        warehouse.setWarehouseName("Main");
        warehouse.setDeleted(false);
        return warehouse;
    }

    private Inventory inventory(Warehouse warehouse) {
        Inventory inventory = new Inventory();
        inventory.setPublicId(INVENTORY_ID);
        inventory.setWarehouse(warehouse);
        inventory.setDeleted(false);
        return inventory;
    }

    private Product product(UUID publicId, boolean inactive) {
        Product product = new Product();
        product.setPublicId(publicId);
        product.setProductName("Sugar");
        product.setSku("SUG-1");
        product.setSellPrice(new BigDecimal("10"));
        product.setDeleted(false);
        product.setInActive(inactive);
        return product;
    }

    private InventoryProducts inventoryProduct(Product product, Long closingStock) {
        InventoryProducts inventoryProduct = new InventoryProducts();
        inventoryProduct.setPublicId(UUID.fromString("55555555-5555-5555-5555-555555555555"));
        inventoryProduct.setProduct(product);
        inventoryProduct.setClosingStock(closingStock);
        inventoryProduct.setAvailableStock(closingStock);
        inventoryProduct.setReservedStock(0L);
        inventoryProduct.setQuarantinedStock(0L);
        return inventoryProduct;
    }
}
