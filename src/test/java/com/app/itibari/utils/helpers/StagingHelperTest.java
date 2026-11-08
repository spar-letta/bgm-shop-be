package com.app.itibari.utils.helpers;

import com.app.itibari.dataTypes.ReconStatus;
import com.app.itibari.dataTypes.ReconciliationStageStatus;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.StagingActionRequest;
import com.app.itibari.repository.*;
import com.app.itibari.services.helper.LiveStockHelper;
import com.app.itibari.services.helper.StockMovementHelper;
import com.app.itibari.services.helper.StockUpdateHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class StagingHelperTest {

    private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SALES_ORDER_ITEM_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID WAREHOUSE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock private ProductRepository productRepository;
    @Mock private ReconciliationStagingRepository reconciliationStagingRepository;
    @Mock private StockUpdateHelper stockUpdateHelper;
    @Mock private ProductReconRepository productReconRepository;
    @Mock private ReconciliationStagingItemRepository reconciliationStagingItemRepository;
    @Mock private ProductItemRepository productItemRepository;
    @Mock private LiveStockHelper liveStockHelper;
    @Mock private StockMovementHelper stockMovementHelper;
    @Mock private PersonRepository personRepository;
    @Mock private ProductStockHelper productStockHelper;
    @Mock private SalesOrderItemsRepository salesOrderItemsRepository;
    @Mock private DeliveryAgentOperationRepository deliveryAgentOperationRepository;

    @InjectMocks
    private StagingHelper stagingHelper;

    @Test
    public void handleApproval_updatesPreBookedVanQuantityOnDeliveryAgentOperation() {
        Product product = new Product();
        product.setPublicId(PRODUCT_ID);
        product.setSku("SKU-1");

        ProductItem productReconItem = new ProductItem();
        productReconItem.setProductPublicId(PRODUCT_ID);
        productReconItem.setHasOrdered(true);
        productReconItem.setSalesOrderItemPublicId(SALES_ORDER_ITEM_ID.toString());

        ProductRecon productRecon = new ProductRecon();
        productRecon.setDeliveryId("ROUTE-001");
        productRecon.setStatus(ReconStatus.Pending);
        productRecon.setProductItems(List.of(productReconItem));

        ReconciliationStagingItem stagingItem = new ReconciliationStagingItem();
        stagingItem.setProductPublicId(PRODUCT_ID);
        stagingItem.setQuantityReturned(3);

        ReconciliationStaging staging = new ReconciliationStaging();
        staging.setProductRecon(productRecon);
        staging.setStatus(ReconciliationStageStatus.STAGING);
        staging.setReconciliationStagingItemList(List.of(stagingItem));

        Warehouse warehouse = new Warehouse();
        warehouse.setPublicId(WAREHOUSE_ID);

        when(productRepository.findByPublicIdAndDeletedIsFalse(PRODUCT_ID)).thenReturn(Optional.of(product));

        stagingHelper.handleApproval(
                staging,
                StagingActionRequest.builder().comment("approved").build(),
                warehouse,
                null,
                null
        );

        verify(reconciliationStagingRepository).save(staging);
        verify(deliveryAgentOperationRepository, never()).save(any(DeliveryAgentOperation.class));
    }
}
