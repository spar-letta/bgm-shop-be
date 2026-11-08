package com.app.itibari.services;

import com.app.itibari.dataTypes.OperationTimingEntityType;
import com.app.itibari.dataTypes.VanSaleStatus;
import com.app.itibari.dataTypes.InTransitTransferType;
import com.app.itibari.dataTypes.RouteStatus;
import com.app.itibari.models.request.TransferInTransitRequest;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.response.ProductItemDTO;
import com.app.itibari.models.response.RouteProductsDTO;
import com.app.itibari.repository.*;
import com.app.itibari.services.helper.LogisticsHelper;
import com.app.itibari.services.helper.StockMovementHelper;
import com.app.itibari.utils.helpers.TimingLogHelper;
import com.app.itibari.utils.reportHelper.StaticReport;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

@RunWith(MockitoJUnitRunner.class)
public class LogisticServiceImplTest {

    private static final UUID ROUTE_PUBLIC_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID AGENT_PUBLIC_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID WAREHOUSE_PUBLIC_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ORDER_PRODUCT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID VAN_ONLY_PRODUCT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @InjectMocks
    private LogisticServiceImpl service;

    @Mock private VehicleRepository vehicleRepository;
    @Mock private PersonRepository personRepository;
    @Mock private OrderCutOffTimeRepository orderCutOffTimeRepository;
    @Mock private WarehouseRepository warehouseRepository;
    @Mock private OrganizationRegionsRepository organizationRegionsRepository;
    @Mock private CounterService counterService;
    @Mock private SalesOrdersRepository salesOrdersRepository;
    @Mock private DeliveryAgentsRepository deliveryAgentsRepository;
    @Mock private RoutePlanningRepository routePlanningRepository;
    @Mock private LogisticsHelper logisticsHelper;
    @Mock private VanSalesRepository vanSalesRepository;
    @Mock private LiveStockEntityRepository liveStockEntityRepository;
    @Mock private StaticReport staticReport;
    @Mock private StockMovementHelper stockMovementHelper;
    @Mock private ProductReconRepository productReconRepository;
    @Mock private ProductItemRepository productItemRepository;
    @Mock private ReconciliationRepository reconciliationRepository;
    @Mock private RejectionReasonRepository rejectionReasonRepository;
    @Mock private RejectionCodeRepository rejectionCodeRepository;
    @Mock private IssuanceRepository issuanceRepository;
    @Mock private DeliveryAgentOperationRepository deliveryAgentOperationRepository;
    @Mock private EventsRepository eventsRepository;
    @Mock private ItibariSmsService itibariSmsService;
    @Mock private TimingLogHelper timingLogHelper;

    @Before
    public void setUp() {
        doAnswer(invocation -> {
            Runnable work = invocation.getArgument(5);
            work.run();
            return null;
        }).when(timingLogHelper).time(
                anyString(),
                any(OperationTimingEntityType.class),
                anyString(),
                anyString(),
                nullable(String.class),
                any(Runnable.class));
    }

    @Test
    public void orderTransfer_usesActiveRouteVehicle_whenRequestVehicleIsMissing() {
        assertOrderTransferUsesActiveRouteVehicle(null);
    }

    @Test
    public void orderTransfer_ignoresRequestVehicle_whenTargetHasActiveRoute() {
        assertOrderTransferUsesActiveRouteVehicle(UUID.randomUUID());
    }

    @Test
    public void activeRouteTransferMovesProductQuantitiesAndRecalculatesBothCashTotals() {
        ProductTransfer transfer = prepareProductTransfer(10, 3, true);

        service.transferOrderInTransit(transfer.request, "admin@test.com");

        assertEquals(7, transfer.sourceItem.getQuantityPicked());
        assertEquals(7, transfer.sourceItem.getTotalUnits());
        assertEquals(5, transfer.sourceItem.getOrderQuantity());
        assertEquals(2, transfer.sourceItem.getVanSaleQuantity());
        assertFalse(transfer.sourceItem.getDeleted());
        assertEquals(7, transfer.targetItem.getQuantityPicked());
        assertEquals(7, transfer.targetItem.getTotalUnits());
        assertEquals(3, transfer.targetItem.getOrderQuantity());
        assertTrue(transfer.targetItem.isHasOrdered());
        assertTrue(transfer.targetItem.isHasVanSale());
        assertEquals(new BigDecimal("700"), transfer.sourceCash.getTotalAmountPicked());
        assertEquals(new BigDecimal("700"), transfer.targetCash.getDeliveryAmount());
        assertEquals(List.of(transfer.order), transfer.target.getSalesOrders());
        assertEquals(AGENT_PUBLIC_ID, transfer.order.getDeliveryAgent().getPublicId());
        assertEquals(transfer.target.getPublicId().toString(), transfer.order.getRoutePublicId());
        assertFalse(transfer.source.getSalesOrders().contains(transfer.order));
        verify(salesOrdersRepository).save(transfer.order);
        verify(productItemRepository).save(transfer.sourceItem);
        verify(productItemRepository).save(transfer.targetItem);
    }

    @Test
    public void activeRouteTransferCreatesTargetItemAndClearsFullyTransferredSourceItem() {
        ProductTransfer transfer = prepareProductTransfer(3, 3, false);
        transfer.sourceItem.setOrderQuantity(3);
        transfer.sourceItem.setVanSaleQuantity(0);

        service.transferOrderInTransit(transfer.request, "admin@test.com");

        assertTrue(transfer.sourceItem.getDeleted());
        assertEquals(0, transfer.sourceItem.getQuantityPicked());
        assertEquals(0, transfer.sourceItem.getOrderQuantity());
        assertEquals(1, transfer.targetRecon.getProductItems().size());
        ProductItem created = transfer.targetRecon.getProductItems().get(0);
        assertEquals(3, created.getQuantityPicked());
        assertEquals(3, created.getOrderQuantity());
        assertEquals(transfer.targetRecon, created.getProductRecon());
        assertEquals(transfer.targetRecon.getPublicId().toString(), created.getProductReconPublicId());
        assertEquals(BigDecimal.ZERO, transfer.sourceCash.getDeliveryAmount());
        assertEquals(new BigDecimal("300"), transfer.targetCash.getTotalAmountPicked());
    }

    @Test
    public void activeRouteTransferRejectsInsufficientSourceStockBeforeSavingAnything() {
        ProductTransfer transfer = prepareProductTransfer(2, 3, false);

        assertThrows(BadRequestRestApiException.class,
                () -> service.transferOrderInTransit(transfer.request, "admin@test.com"));

        assertTrue(transfer.source.getSalesOrders().contains(transfer.order));
        assertTrue(transfer.target.getSalesOrders().isEmpty());
        verify(productItemRepository, never()).save(any());
        verify(routePlanningRepository, never()).save(any());
        verifyNoInteractions(stockMovementHelper, salesOrdersRepository, itibariSmsService);
    }

    @Test
    public void activeRouteTransferRejectsMissingTargetReconciliationBeforeChangingSource() {
        ProductTransfer transfer = prepareProductTransfer(10, 3, false);
        when(productReconRepository.findProductReconByDeliveryIdIgnoreCaseAndDeletedIsFalse("TARGET-ROUTE"))
                .thenReturn(Optional.empty());

        assertThrows(BadRequestRestApiException.class,
                () -> service.transferOrderInTransit(transfer.request, "admin@test.com"));

        assertEquals(10, transfer.sourceItem.getQuantityPicked());
        verify(productItemRepository, never()).save(any());
        verify(routePlanningRepository, never()).save(any());
        verifyNoInteractions(stockMovementHelper, salesOrdersRepository, itibariSmsService);
    }

    private ProductTransfer prepareProductTransfer(long sourceQuantity, int moveQuantity, boolean targetItemExists) {
        ProductTransfer transfer = new ProductTransfer();
        Person actor = new Person();
        DeliveryAgents sourceAgent = new DeliveryAgents();
        DeliveryAgents targetAgent = new DeliveryAgents();
        sourceAgent.setPublicId(UUID.randomUUID());
        transfer.order.setDeliveryAgent(sourceAgent);
        targetAgent.setPublicId(AGENT_PUBLIC_ID);
        Vehicle vehicle = new Vehicle();
        vehicle.setPublicId(UUID.randomUUID());
        transfer.source.setRouteId("SOURCE-ROUTE");
        transfer.source.setRouteStatus(RouteStatus.IN_TRANSIT);
        transfer.source.setDeliveryAgentId(sourceAgent.getPublicId().toString());
        transfer.target.setRouteId("TARGET-ROUTE");
        transfer.target.setVehicleId(vehicle.getPublicId().toString());
        transfer.target.setRouteStatus(RouteStatus.IN_TRANSIT);
        Product product = new Product();
        product.setPublicId(ORDER_PRODUCT_ID);
        product.setProductName("Product A");
        product.setSku("SKU-A");
        transfer.order.setSalesOrderItems(Set.of(SalesOrderItems.builder().product(product)
                .quantity(moveQuantity).pricePerUnit(new BigDecimal("100")).build()));
        SalesOrders remainingOrder = new SalesOrders();
        remainingOrder.setPublicId(UUID.randomUUID());
        transfer.source.setSalesOrders(new java.util.ArrayList<>(List.of(transfer.order, remainingOrder)));
        transfer.sourceItem.setProductRecon(transfer.sourceRecon);
        transfer.sourceItem.setQuantityPicked(sourceQuantity);
        transfer.sourceItem.setTotalUnits(sourceQuantity);
        transfer.sourceItem.setOrderQuantity(Math.max(0, sourceQuantity - 2));
        transfer.sourceItem.setVanSaleQuantity(2);
        transfer.sourceItem.setHasVanSale(true);
        transfer.sourceItem.setProductSellingPrice(new BigDecimal("100"));
        transfer.sourceRecon.addProductItem(transfer.sourceItem);
        transfer.targetItem.setQuantityPicked(4);
        transfer.targetItem.setTotalUnits(4);
        transfer.targetItem.setHasVanSale(true);
        transfer.targetItem.setProductSellingPrice(new BigDecimal("100"));
        if (targetItemExists) {
            transfer.targetRecon.addProductItem(transfer.targetItem);
        }
        transfer.request.setSourceRoutePublicId(transfer.source.getPublicId());
        transfer.request.setTargetDeliveryAgentPublicId(AGENT_PUBLIC_ID);
        transfer.request.setTransferType(InTransitTransferType.ORDER);
        transfer.request.setOrderPublicIds(List.of(transfer.order.getPublicId()));
        when(personRepository.findByEmailAndDeletedIsFalse("admin@test.com")).thenReturn(Optional.of(actor));
        when(routePlanningRepository.findByPublicIdAndDeletedIsFalse(transfer.source.getPublicId())).thenReturn(Optional.of(transfer.source));
        when(deliveryAgentsRepository.findByPublicIdAndDeletedIsFalse(sourceAgent.getPublicId())).thenReturn(Optional.of(sourceAgent));
        when(deliveryAgentsRepository.findByPublicIdAndDeletedIsFalse(AGENT_PUBLIC_ID)).thenReturn(Optional.of(targetAgent));
        when(routePlanningRepository.findAllByDeliveryAgentIdIgnoreCaseAndRouteStatusesAndDeletedIsFalse(
                AGENT_PUBLIC_ID.toString(), List.of(RouteStatus.Loading, RouteStatus.Loaded, RouteStatus.ReadyToGo, RouteStatus.IN_TRANSIT)))
                .thenReturn(List.of(transfer.target));
        when(vehicleRepository.findByPublicIdAndDeletedIsFalse(vehicle.getPublicId())).thenReturn(Optional.of(vehicle));
        when(productReconRepository.findProductReconByDeliveryIdIgnoreCaseAndDeletedIsFalse("SOURCE-ROUTE"))
                .thenReturn(Optional.of(transfer.sourceRecon));
        when(productReconRepository.findProductReconByDeliveryIdIgnoreCaseAndDeletedIsFalse("TARGET-ROUTE"))
                .thenReturn(Optional.of(transfer.targetRecon));
        org.mockito.Mockito.lenient().when(productItemRepository.findByProductReconAndProductPublicIdAndDeletedIsFalse(transfer.sourceRecon, ORDER_PRODUCT_ID))
                .thenReturn(Optional.of(transfer.sourceItem));
        org.mockito.Mockito.lenient().when(productItemRepository.findByProductReconAndProductPublicIdAndDeletedIsFalse(transfer.targetRecon, ORDER_PRODUCT_ID))
                .thenReturn(targetItemExists ? Optional.of(transfer.targetItem) : Optional.empty());
        org.mockito.Mockito.lenient().when(productItemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        for (ProductRecon recon : List.of(transfer.sourceRecon, transfer.targetRecon)) {
            org.mockito.Mockito.lenient().when(productItemRepository.findByProductReconAndDeletedIsFalse(recon))
                    .thenAnswer(invocation -> recon.getProductItems().stream().filter(item -> !item.getDeleted()).toList());
        }
        org.mockito.Mockito.lenient().when(reconciliationRepository.findByRoutePublicIdAndDeletedIsFalse(transfer.source.getPublicId().toString()))
                .thenReturn(Optional.of(transfer.sourceCash));
        org.mockito.Mockito.lenient().when(reconciliationRepository.findByRoutePublicIdAndDeletedIsFalse(transfer.target.getPublicId().toString()))
                .thenReturn(Optional.of(transfer.targetCash));
        return transfer;
    }

    private static class ProductTransfer {
        final RoutePlanning source = new RoutePlanning();
        final RoutePlanning target = new RoutePlanning();
        final SalesOrders order = new SalesOrders();
        final ProductRecon sourceRecon = new ProductRecon();
        final ProductRecon targetRecon = new ProductRecon();
        final ProductItem sourceItem = new ProductItem();
        final ProductItem targetItem = new ProductItem();
        final Reconciliation sourceCash = new Reconciliation();
        final Reconciliation targetCash = new Reconciliation();
        final TransferInTransitRequest request = new TransferInTransitRequest();

        ProductTransfer() {
            source.setPublicId(UUID.randomUUID());
            target.setPublicId(UUID.randomUUID());
            order.setPublicId(UUID.randomUUID());
            sourceRecon.setPublicId(UUID.randomUUID());
            targetRecon.setPublicId(UUID.randomUUID());
            sourceItem.setPublicId(UUID.randomUUID());
            targetItem.setPublicId(UUID.randomUUID());
        }
    }

    private void assertOrderTransferUsesActiveRouteVehicle(UUID requestVehicleId) {
        UUID sourceAgentId = UUID.randomUUID();
        UUID routeVehicleId = UUID.randomUUID();
        Person actor = new Person();
        DeliveryAgents sourceAgent = new DeliveryAgents();
        sourceAgent.setPublicId(sourceAgentId);
        DeliveryAgents targetAgent = new DeliveryAgents();
        targetAgent.setPublicId(AGENT_PUBLIC_ID);

        SalesOrders order = new SalesOrders();
        order.setPublicId(UUID.randomUUID());
        order.setSalesOrderItems(Set.of());
        SalesOrders remainingOrder = new SalesOrders();
        remainingOrder.setPublicId(UUID.randomUUID());

        RoutePlanning source = new RoutePlanning();
        source.setPublicId(ROUTE_PUBLIC_ID);
        source.setRouteId("SOURCE-ROUTE");
        source.setRouteStatus(RouteStatus.IN_TRANSIT);
        source.setDeliveryAgentId(sourceAgentId.toString());
        source.setSalesOrders(new java.util.ArrayList<>(List.of(order, remainingOrder)));
        RoutePlanning target = new RoutePlanning();
        target.setPublicId(UUID.randomUUID());
        target.setRouteId("TARGET-ROUTE");
        target.setVehicleId(routeVehicleId.toString());
        target.setRouteStatus(RouteStatus.IN_TRANSIT);
        Vehicle vehicle = new Vehicle();
        vehicle.setPublicId(routeVehicleId);

        when(personRepository.findByEmailAndDeletedIsFalse("admin@test.com")).thenReturn(Optional.of(actor));
        when(routePlanningRepository.findByPublicIdAndDeletedIsFalse(ROUTE_PUBLIC_ID)).thenReturn(Optional.of(source));
        when(deliveryAgentsRepository.findByPublicIdAndDeletedIsFalse(sourceAgentId)).thenReturn(Optional.of(sourceAgent));
        when(deliveryAgentsRepository.findByPublicIdAndDeletedIsFalse(AGENT_PUBLIC_ID)).thenReturn(Optional.of(targetAgent));
        when(routePlanningRepository.findAllByDeliveryAgentIdIgnoreCaseAndRouteStatusesAndDeletedIsFalse(
                AGENT_PUBLIC_ID.toString(), List.of(RouteStatus.Loading, RouteStatus.Loaded, RouteStatus.ReadyToGo, RouteStatus.IN_TRANSIT)))
                .thenReturn(List.of(target));
        when(vehicleRepository.findByPublicIdAndDeletedIsFalse(routeVehicleId)).thenReturn(Optional.of(vehicle));

        TransferInTransitRequest request = new TransferInTransitRequest();
        request.setSourceRoutePublicId(ROUTE_PUBLIC_ID);
        request.setTargetDeliveryAgentPublicId(AGENT_PUBLIC_ID);
        request.setTargetVehiclePublicId(requestVehicleId);
        request.setTransferType(InTransitTransferType.ORDER);
        request.setOrderPublicIds(List.of(order.getPublicId()));

        service.transferOrderInTransit(request, "admin@test.com");

        assertEquals(List.of(order), target.getSalesOrders());
        assertEquals(List.of(remainingOrder), source.getSalesOrders());
        assertEquals(target.getPublicId().toString(), order.getRoutePublicId());
        assertEquals(routeVehicleId.toString(), target.getVehicleId());
        verify(vehicleRepository).findByPublicIdAndDeletedIsFalse(routeVehicleId);
        org.mockito.Mockito.verifyNoMoreInteractions(vehicleRepository);
    }

    @Test
    public void getRoutePlanningOrdersAndVanSalesProducts_populatesIssuanceTable() {
        RoutePlanning routePlanning = new RoutePlanning();
        routePlanning.setPublicId(ROUTE_PUBLIC_ID);
        routePlanning.setRouteId("ROUTE-001");
        routePlanning.setDeliveryAgentId(AGENT_PUBLIC_ID.toString());
        routePlanning.setWarehouseId(WAREHOUSE_PUBLIC_ID.toString());
        routePlanning.setSalesOrders(List.of(buildSalesOrder()));

        DeliveryAgents deliveryAgent = new DeliveryAgents();
        deliveryAgent.setPublicId(AGENT_PUBLIC_ID);

        VanSales vanSale = new VanSales();
        vanSale.setProductItems(List.of(
                buildVanSaleItem(ORDER_PRODUCT_ID, "Product A", "SKU-A", 2L),
                buildVanSaleItem(VAN_ONLY_PRODUCT_ID, "Product B", "SKU-B", 3L)
        ));

        when(routePlanningRepository.getRoutePlanningByPublicIdAndDeletedIsFalse(ROUTE_PUBLIC_ID))
                .thenReturn(Optional.of(routePlanning));
        when(deliveryAgentsRepository.findByPublicIdAndDeletedIsFalse(AGENT_PUBLIC_ID))
                .thenReturn(Optional.of(deliveryAgent));
        when(vanSalesRepository.findByDeliveryAgents_PublicIdAndStatusAndDeletedIsFalse(AGENT_PUBLIC_ID, VanSaleStatus.Pending))
                .thenReturn(List.of(vanSale));
        when(issuanceRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryAgentOperationRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        RouteProductsDTO response = service.getRoutePlanningOrdersAndVanSalesProducts(ROUTE_PUBLIC_ID, PageRequest.of(0, 10), null);

        assertNotNull(response.getContent());
        assertEquals(2, response.getContent().size());

        Map<String, ProductItemDTO> productsBySku = new HashMap<>();
        response.getContent().forEach(product -> productsBySku.put(product.getProductCode(), product));

        assertEquals(7L, productsBySku.get("SKU-A").getQuantity());
        assertEquals(3L, productsBySku.get("SKU-B").getQuantity());

        ArgumentCaptor<List<Issuance>> issuanceCaptor = ArgumentCaptor.forClass(List.class);
        verify(issuanceRepository).saveAll(issuanceCaptor.capture());

        Map<String, Issuance> issuancesBySku = new HashMap<>();
        issuanceCaptor.getValue().forEach(issuance -> issuancesBySku.put(issuance.getSku(), issuance));

        Issuance preBookedAndVanBooked = issuancesBySku.get("SKU-A");
        assertEquals(5, preBookedAndVanBooked.getPreBookedQuantity());
        assertEquals(2, preBookedAndVanBooked.getVanBookedQuantity());
        assertEquals("ROUTE-001", preBookedAndVanBooked.getRouteId());
        assertEquals(AGENT_PUBLIC_ID, preBookedAndVanBooked.getDeliveryAgentPublicId());
        assertEquals(WAREHOUSE_PUBLIC_ID, preBookedAndVanBooked.getWarehousePublicId());

        Issuance vanOnly = issuancesBySku.get("SKU-B");
        assertEquals(0, vanOnly.getPreBookedQuantity());
        assertEquals(3, vanOnly.getVanBookedQuantity());

        InOrder issuanceOrder = inOrder(issuanceRepository);
        issuanceOrder.verify(issuanceRepository).deleteByRouteId("ROUTE-001");
        issuanceOrder.verify(issuanceRepository).saveAll(anyList());

        ArgumentCaptor<List<DeliveryAgentOperation>> operationsCaptor = ArgumentCaptor.forClass(List.class);
        verify(deliveryAgentOperationRepository).saveAll(operationsCaptor.capture());
        assertEquals(1, operationsCaptor.getValue().size());

        DeliveryAgentOperation preBookedOperation = operationsCaptor.getValue().get(0);
        assertEquals("SKU-A", preBookedOperation.getSku());
        assertEquals("ORD-TEST-1", preBookedOperation.getOrderNumber());
        assertEquals(5, preBookedOperation.getPreBookedQuantity());
        assertEquals(0, preBookedOperation.getVanBookedQuantity());
        assertEquals(5, preBookedOperation.getTotalQuantityPicked());
        assertEquals("ROUTE-001", preBookedOperation.getRouteId());

        InOrder operationOrder = inOrder(deliveryAgentOperationRepository);
        operationOrder.verify(deliveryAgentOperationRepository).deleteByRouteId("ROUTE-001");
        operationOrder.verify(deliveryAgentOperationRepository).saveAll(anyList());
    }

    private SalesOrders buildSalesOrder() {
        Product product = new Product();
        product.setProductName("Product A");
        product.setSku("SKU-A");

        SalesOrders salesOrder = new SalesOrders();
        salesOrder.setSalesOrderNumber("ORD-TEST-1");

        SalesOrderItems salesOrderItem = SalesOrderItems.builder()
                .productId(ORDER_PRODUCT_ID)
                .quantity(5)
                .pricePerUnit(new BigDecimal("100"))
                .product(product)
                .salesOrder(salesOrder)
                .build();

        salesOrder.setSalesOrderItems(Set.of(salesOrderItem));
        return salesOrder;
    }

    private ProductItem buildVanSaleItem(UUID productId, String productName, String sku, long quantity) {
        ProductItem productItem = new ProductItem();
        productItem.setProductPublicId(productId);
        productItem.setProductName(productName);
        productItem.setProductCode(sku);
        productItem.setVanSaleQuantity(quantity);
        productItem.setProductSellingPrice(new BigDecimal("100"));
        return productItem;
    }
}
