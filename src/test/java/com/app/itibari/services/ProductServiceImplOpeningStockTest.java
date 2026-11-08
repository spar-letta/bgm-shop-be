package com.app.itibari.services;

import com.app.itibari.dataTypes.ProductUpdateType;
import com.app.itibari.models.entities.Inventory;
import com.app.itibari.models.entities.InventoryProducts;
import com.app.itibari.models.entities.Product;
import com.app.itibari.models.entities.Warehouse;
import com.app.itibari.repository.InventoryProductsRepository;
import com.app.itibari.repository.InventoryRepository;
import com.app.itibari.services.helper.LiveStockHelper;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductServiceImplOpeningStockTest {

    private static final UUID WAREHOUSE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ACTIVE_PRODUCT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID DELETED_PRODUCT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID FAILED_PRODUCT_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    @Mock private InventoryRepository inventoryRepository;
    @Mock private InventoryProductsRepository inventoryProductsRepository;
    @Mock private LiveStockHelper liveStockHelper;

    @InjectMocks
    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void updateProductStockSkipsDeletedProductAndContinuesAfterAFailure() {
        LocalDate today = LocalDate.of(2026, 10, 22);
        Warehouse warehouse = warehouse();
        Inventory inventory = inventory(warehouse);

        InventoryProducts deletedProduct = inventoryProduct(deletedProduct());
        InventoryProducts failingProduct = inventoryProduct(product(FAILED_PRODUCT_ID, "Failing", false));
        InventoryProducts activeProduct = inventoryProduct(product(ACTIVE_PRODUCT_ID, "Active", false));

        when(inventoryRepository.findAllByDeletedIsFalse()).thenReturn(List.of(inventory));
        when(inventoryProductsRepository.findByInventoryAndDeletedIsFalse(inventory))
                .thenReturn(List.of(deletedProduct, failingProduct, activeProduct));
        doThrow(new BadRequestRestApiException().developerMessage("Product not found").userMessage("Product not found"))
                .when(liveStockHelper)
                .updateLiveStockValue(eq(WAREHOUSE_ID), eq(FAILED_PRODUCT_ID), eq(0), eq(ProductUpdateType.OPENING_STOCK), eq(today));

        assertDoesNotThrow(() -> productService.updateProductStock(today));

        verify(liveStockHelper, never()).updateLiveStockValue(
                eq(WAREHOUSE_ID), eq(DELETED_PRODUCT_ID), anyInt(), any(), any());
        verify(liveStockHelper).updateLiveStockValue(
                WAREHOUSE_ID, FAILED_PRODUCT_ID, 0, ProductUpdateType.OPENING_STOCK, today);
        verify(liveStockHelper).updateLiveStockValue(
                WAREHOUSE_ID, ACTIVE_PRODUCT_ID, 0, ProductUpdateType.OPENING_STOCK, today);
        verify(inventoryRepository, never()).findAll();
    }

    private Warehouse warehouse() {
        Warehouse warehouse = new Warehouse();
        warehouse.setPublicId(WAREHOUSE_ID);
        warehouse.setDeleted(false);
        return warehouse;
    }

    private Inventory inventory(Warehouse warehouse) {
        Inventory inventory = new Inventory();
        inventory.setPublicId(UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee"));
        inventory.setWarehouse(warehouse);
        inventory.setDeleted(false);
        return inventory;
    }

    private Product product(UUID publicId, String name, boolean deleted) {
        Product product = new Product();
        product.setPublicId(publicId);
        product.setProductName(name);
        product.setDeleted(deleted);
        return product;
    }

    private Product deletedProduct() {
        return product(DELETED_PRODUCT_ID, "Deleted", true);
    }

    private InventoryProducts inventoryProduct(Product product) {
        InventoryProducts inventoryProduct = new InventoryProducts();
        inventoryProduct.setProduct(product);
        return inventoryProduct;
    }
}
