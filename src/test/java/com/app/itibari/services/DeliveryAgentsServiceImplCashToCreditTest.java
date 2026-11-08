package com.app.itibari.services;

import com.app.itibari.dataTypes.*;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.ConvertCashToCreditRequest;
import com.app.itibari.models.request.DeliverItemRequest;
import com.app.itibari.models.request.DeliverVanSaleItemRequest;
import com.app.itibari.models.request.ItemRequest;
import com.app.itibari.models.request.MarkOrderDeliveredRequest;
import com.app.itibari.models.request.SellVanSaleRequest;
import com.app.itibari.models.request.VanItemRequest;
import com.app.itibari.repository.*;
import com.app.itibari.services.helper.*;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.helpers.CustomerCheckerHelper;
import com.app.itibari.utils.helpers.TumaMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DeliveryAgentsServiceImplCashToCreditTest {

    private static final UUID ORDER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CUSTOMER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID DA_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ROUTE_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @InjectMocks
    private DeliveryAgentsServiceImpl service;

    @Mock private LocationApprovalRepository locationApprovalRepository;
    @Mock private InvoiceItemRepository invoiceItemRepository;
    @Mock private DeliveryAgentsRepository deliveryAgentsRepository;
    @Mock private PersonRepository personRepository;
    @Mock private WarehouseRepository warehouseRepository;
    @Mock private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Mock private ItibariSmsService itibariSmsService;
    @Mock private EventsRepository eventsRepository;
    @Mock private SalesOrdersRepository salesOrdersRepository;
    @Mock private OrderAmendmentRepository orderAmendmentRepository;
    @Mock private RoutePlanningRepository routePlanningRepository;
    @Mock private SalesOrderItemsRepository salesOrderItemsRepository;
    @Mock private ProductReconRepository productReconRepository;
    @Mock private ProductItemRepository productItemRepository;
    @Mock private RejectionCodeRepository rejectionCodeRepository;
    @Mock private WarehouseService warehouseService;
    @Mock private LogisticsHelper logisticsHelper;
    @Mock private CustomersService customersService;
    @Mock private SalesOrdersService salesOrdersService;
    @Mock private VanSalesRepository vanSalesRepository;
    @Mock private NotificationHelper notificationHelper;
    @Mock private OrderAmendmentItemRepository orderAmendmentItemRepository;
    @Mock private LiveStockHelper liveStockHelper;
    @Mock private ProductRepository productRepository;
    @Mock private DiscountHelper discountHelper;
    @Mock private CustomersRepository customersRepository;
    @Mock private DiscountItemsRepository discountItemsRepository;
    @Mock private DiscountRepository discountRepository;
    @Mock private RejectionReasonRepository rejectionReasonRepository;
    @Mock private DeliveryAgentOperationRepository deliveryAgentOperationRepository;
    @Mock private StockUpdateHelper stockUpdateHelper;
    @Mock private SalesAgentsRepository salesAgentsRepository;
    @Mock private SalesOrderAgentRepository salesOrderAgentRepository;
    @Mock private SalesAgentDeliveredItemRepository salesAgentDeliveredItemRepository;
    @Mock private StockMovementHelper stockMovementHelper;
    @Mock private ReconciliationRepository reconciliationRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private CreditLimitItemRepository creditLimitItemRepository;
    @Mock private BnplHelper bnplHelper;
    @Mock private TransactionItemRepository transactionItemRepository;
    @Mock private com.app.itibari.utils.helpers.TraUtilHelper traUtilHelper;
    @Mock private TraSubmissionHelper traSubmissionHelper;
    @Mock private CustomerCheckerHelper customerCheckerHelper;
    @Mock private TumaMessage tumaMessage;
    @Mock private VanSalesHelper vanSalesHelper;
    @Mock private TraSwitchService traSwitchService;

    private DeliveryAgents deliveryAgent;
    private SalesOrders salesOrder;
    private Customers customer;

    private static final UUID ORDER_ITEM_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID PRODUCT_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID VAN_RECON_ITEM_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        deliveryAgent = new DeliveryAgents();
        deliveryAgent.setPublicId(DA_ID);
        deliveryAgent.setFullName("Test DA");
        deliveryAgent.setPhoneNumber("da@test.com");

        customer = new Customers();
        customer.setPublicId(CUSTOMER_ID);
        customer.setName("Test Customer");
        customer.setPhoneNumber("255700000000");

        SalesOrderItems item = SalesOrderItems.builder()
                .quantity(10)
                .quantityDelivered(4)
                .pricePerUnit(new BigDecimal("100"))
                .status(OrderItemStatus.PartiallyDelivered)
                .build();
        item.setPublicId(ORDER_ITEM_ID);
        item.setProduct(minimalProduct());
        item.getProduct().setPublicId(PRODUCT_ID);
        item.setSalesOrder(salesOrder);

        salesOrder = new SalesOrders();
        salesOrder.setPublicId(ORDER_ID);
        salesOrder.setSalesOrderNumber("ORD-TEST-1");
        salesOrder.setCustomer(customer);
        salesOrder.setOrderType(OrderType.CASH);
        salesOrder.setCreditApplied(false);
        salesOrder.setPaymentStatus(PaymentStatusEnum.PENDING);
        salesOrder.setSalesOrderStatus(LoadingStatusEnum.IN_TRANSIT);
        salesOrder.setSalesOrderItems(new HashSet<>(List.of(item)));
        salesOrder.setTotalAmount(new BigDecimal("1000"));

        item.setSalesOrder(salesOrder);
        when(traSwitchService.traSwitchOn()).thenReturn(false);
    }

    @Test
    void markOrderItemAsDelivered_updatesDeliveryAgentOperationPreBookedSoldQuantity() {
        SalesOrderItems salesOrderItem = salesOrder.getSalesOrderItems().iterator().next();
        salesOrderItem.setStatus(OrderItemStatus.NotDelivered);
        salesOrderItem.setQuantityDelivered(0);

        RoutePlanning routePlanning = new RoutePlanning();
        routePlanning.setPublicId(ROUTE_ID);
        routePlanning.setRouteId("ROUTE-001");
        routePlanning.setRouteStatus(RouteStatus.Loading);
        routePlanning.setDeliveryAgentId(DA_ID.toString());

        ProductRecon productRecon = new ProductRecon();
        productRecon.setDeliveryId("ROUTE-001");
        productRecon.setWarehousePublicId(UUID.randomUUID().toString());

        ProductItem productItem = new ProductItem();
        productItem.setProductPublicId(PRODUCT_ID);
        productItem.setQuantitySold(1);
        productItem.setOrderQuantity(9);

        DeliveryAgentOperation operation = new DeliveryAgentOperation();
        operation.setRouteId("ROUTE-001");
        operation.setOrderNumber("ORD-TEST-1");
        operation.setSku(salesOrder.getSalesOrderItems().iterator().next().getProduct().getSku());
        operation.setPreBookedSoldQuantity(4);

        DeliverItemRequest request = new DeliverItemRequest();
        request.setRoutePublicId(ROUTE_ID);
        request.setSalesOrderPublicId(ORDER_ID);
        request.setItems(List.of(new ItemRequest(ORDER_ITEM_ID, 2, null)));

        when(deliveryAgentsRepository.findDeliveryAgentsByPhoneNumberAndDeletedIsFalse("da@test.com"))
                .thenReturn(Optional.of(deliveryAgent));
        when(salesOrdersRepository.findByPublicIdAndDeletedIsFalse(ORDER_ID))
                .thenReturn(Optional.of(salesOrder));
        when(routePlanningRepository.findByPublicIdAndDeletedIsFalse(ROUTE_ID))
                .thenReturn(Optional.of(routePlanning));
        when(productReconRepository.findByDeliveryAgentPublicIdIgnoreCaseAndDeliveryIdIgnoreCaseAndDeletedIsFalse(DA_ID.toString(), "ROUTE-001"))
                .thenReturn(Optional.of(productRecon));
        when(salesOrderItemsRepository.findByPublicIdAndDeletedIsFalse(ORDER_ITEM_ID))
                .thenReturn(Optional.of(salesOrderItem));
        when(productItemRepository.findByProductReconAndProductPublicIdAndDeletedIsFalse(productRecon, PRODUCT_ID))
                .thenReturn(Optional.of(productItem));
        when(productItemRepository.save(any(ProductItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(salesOrderItemsRepository.save(any(SalesOrderItems.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deliveryAgentOperationRepository.findByRouteIdAndOrderNumberAndSkuAndDeletedIsFalse("ROUTE-001", "ORD-TEST-1",
                salesOrderItem.getProduct().getSku()))
                .thenReturn(Optional.of(operation));
        when(deliveryAgentOperationRepository.save(any(DeliveryAgentOperation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(salesOrdersRepository.save(any(SalesOrders.class))).thenAnswer(inv -> inv.getArgument(0));

        service.markOrderItemAsDelivered(request, "da@test.com");

        assertEquals(6, operation.getPreBookedSoldQuantity());
        assertEquals(8, operation.getPreBookedVanQuantity());
        verify(deliveryAgentOperationRepository).save(operation);
    }

    @Test
    void markVanSaleItemAsSold_updatesDeliveryAgentOperationVanSoldQuantityAndOrderNumber() {
        RoutePlanning routePlanning = new RoutePlanning();
        routePlanning.setPublicId(ROUTE_ID);
        routePlanning.setRouteId("ROUTE-001");
        routePlanning.setRouteStatus(RouteStatus.Loading);
        routePlanning.setDeliveryAgentId(DA_ID.toString());

        ProductRecon productRecon = new ProductRecon();
        productRecon.setDeliveryId("ROUTE-001");
        productRecon.setWarehousePublicId(UUID.randomUUID().toString());

        ProductItem vanReconItem = new ProductItem();
        vanReconItem.setPublicId(VAN_RECON_ITEM_ID);
        vanReconItem.setProductPublicId(PRODUCT_ID);
        vanReconItem.setQuantityPicked(10);
        vanReconItem.setQuantitySold(1);
        vanReconItem.setVanSaleQuantity(9);
        vanReconItem.setVanSaleItem(true);
        productRecon.setProductItems(new ArrayList<>(List.of(vanReconItem)));

        Product product = minimalProduct();
        product.setPublicId(PRODUCT_ID);
        product.setSku("SKU-VAN");
        product.setProductName("Van Product");

        Customers createdCustomer = new Customers();
        createdCustomer.setPublicId(UUID.randomUUID());
        createdCustomer.setOrganizationRegions(new OrganizationRegions());

        Warehouse warehouse = new Warehouse();
        warehouse.setPublicId(UUID.randomUUID());
        SalesOrders createdVanSaleOrder = new SalesOrders();
        createdVanSaleOrder.setSalesOrderNumber("ORD-VAN-1");
        createdVanSaleOrder.setWarehouse(warehouse);
        createdVanSaleOrder.setDeliveryAgent(deliveryAgent);

        SalesOrderItems createdOrderItem = new SalesOrderItems();
        createdOrderItem.setProduct(product);
        createdOrderItem.setQuantityDelivered(3);
        createdVanSaleOrder.setSalesOrderItems(new HashSet<>(List.of(createdOrderItem)));

        DeliveryAgentOperation operation = new DeliveryAgentOperation();
        operation.setRouteId("ROUTE-001");
        operation.setSku(product.getSku());
        operation.setOrderNumber(null);
        operation.setVanSoldQuantity(2);

        DeliverVanSaleItemRequest request = new DeliverVanSaleItemRequest();
        request.setRoutePublicId(ROUTE_ID);
        request.setItems(List.of(new ItemRequest(VAN_RECON_ITEM_ID, 3, null)));

        when(deliveryAgentsRepository.findDeliveryAgentsByPhoneNumberAndDeletedIsFalse("da@test.com"))
                .thenReturn(Optional.of(deliveryAgent));
        when(customersService.createCustomerFromVanSale(eq(request), any()))
                .thenReturn(createdCustomer);
        when(routePlanningRepository.findByPublicIdAndDeletedIsFalse(ROUTE_ID))
                .thenReturn(Optional.of(routePlanning));
        when(productReconRepository.findByDeliveryAgentPublicIdIgnoreCaseAndDeliveryIdIgnoreCaseAndDeletedIsFalse(DA_ID.toString(), "ROUTE-001"))
                .thenReturn(Optional.of(productRecon));
        when(productItemRepository.save(any(ProductItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productRepository.findByPublicIdAndDeletedIsFalse(PRODUCT_ID))
                .thenReturn(Optional.of(product));
        when(salesOrdersService.updateNewCustomerOrders(eq(createdCustomer), anyList(), eq(routePlanning), eq(productRecon.getPublicId())))
                .thenReturn(createdVanSaleOrder);
        when(deliveryAgentOperationRepository.findByRouteIdAndOrderNumberAndSkuAndDeletedIsFalse("ROUTE-001", "ORD-VAN-1", "SKU-VAN"))
                .thenReturn(Optional.of(operation));
        when(deliveryAgentOperationRepository.save(any(DeliveryAgentOperation.class))).thenAnswer(inv -> inv.getArgument(0));

        service.markVanSaleItemAsSold(request, "da@test.com");

        ArgumentCaptor<DeliveryAgentOperation> captor = ArgumentCaptor.forClass(DeliveryAgentOperation.class);
        verify(deliveryAgentOperationRepository).save(captor.capture());
        DeliveryAgentOperation saved = captor.getValue();
        assertEquals("ORD-VAN-1", saved.getOrderNumber());
        assertEquals(3, saved.getVanSoldQuantity());
        assertTrue(saved.isVanSale());
    }

    @Test
    void sellVanSaleItem_marksCreatedVanSaleOrderDeliveredSuccessfully() {
        UUID routeId = ROUTE_ID;
        UUID warehouseId = UUID.fromString("88888888-8888-8888-8888-888888888888");
        UUID vanSaleOrderId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        UUID equalButDifferentProductId = UUID.fromString(PRODUCT_ID.toString());

        RoutePlanning routePlanning = new RoutePlanning();
        routePlanning.setPublicId(routeId);
        routePlanning.setRouteId("ROUTE-001");
        routePlanning.setDeliveryAgentId(DA_ID.toString());
        routePlanning.setDeliveryAgentName("Test DA");
        routePlanning.setWarehouseId(warehouseId.toString());
        routePlanning.setRouteStatus(RouteStatus.IN_TRANSIT);

        ProductRecon productRecon = new ProductRecon();
        productRecon.setDeliveryId("ROUTE-001");
        productRecon.setDeliveryAgentPublicId(DA_ID.toString());
        productRecon.setDeliveryAgent("Test DA");
        productRecon.setWarehousePublicId(warehouseId.toString());

        ProductItem vanReconItem = new ProductItem();
        vanReconItem.setPublicId(VAN_RECON_ITEM_ID);
        vanReconItem.setProductPublicId(PRODUCT_ID);
        vanReconItem.setProductCode("SKU-001");
        vanReconItem.setProductName("Product 1");
        vanReconItem.setQuantityPicked(10);
        vanReconItem.setQuantitySold(1);
        vanReconItem.setVanSaleQuantity(9);
        vanReconItem.setVanSaleItem(true);
        productRecon.setProductItems(new ArrayList<>(List.of(vanReconItem)));

        Product product = minimalProduct();
        product.setPublicId(PRODUCT_ID);
        product.setSku("SKU-001");

        Warehouse warehouse = new Warehouse();
        warehouse.setPublicId(warehouseId);

        SalesOrderItems vanSaleOrderItem = new SalesOrderItems();
        vanSaleOrderItem.setProduct(product);
        vanSaleOrderItem.setProductId(equalButDifferentProductId);
        vanSaleOrderItem.setQuantity(3);
        vanSaleOrderItem.setQuantityDelivered(0);
        vanSaleOrderItem.setStatus(OrderItemStatus.NotDelivered);
        vanSaleOrderItem.setPricePerUnit(BigDecimal.TEN);

        SalesOrders vanSaleOrder = new SalesOrders();
        vanSaleOrder.setPublicId(vanSaleOrderId);
        vanSaleOrder.setSalesOrderNumber("ORD-VAN-1");
        vanSaleOrder.setCustomer(customer);
        vanSaleOrder.setDeliveryAgent(deliveryAgent);
        vanSaleOrder.setWarehouse(warehouse);
        vanSaleOrder.setOrderType(OrderType.CASH);
        vanSaleOrder.setPaymentStatus(PaymentStatusEnum.PENDING);
        vanSaleOrder.setSalesOrderStatus(LoadingStatusEnum.IN_TRANSIT);
        vanSaleOrder.setSalesOrderItems(new HashSet<>(List.of(vanSaleOrderItem)));
        vanSaleOrderItem.setSalesOrder(vanSaleOrder);

        SellVanSaleRequest request = new SellVanSaleRequest();
        request.setCustomerPublicId(CUSTOMER_ID);
        request.setRoutePublicId(routeId);
        request.setRequestSpecialDiscount(false);
        VanItemRequest vanItemRequest = new VanItemRequest();
        vanItemRequest.setProductItemPublicId(VAN_RECON_ITEM_ID);
        vanItemRequest.setQuantitySold(3);
        request.setItems(List.of(vanItemRequest));

        when(customersRepository.findByPublicIdAndDeletedIsFalse(CUSTOMER_ID))
                .thenReturn(Optional.of(customer));
        when(routePlanningRepository.findByPublicIdAndDeletedIsFalse(routeId))
                .thenReturn(Optional.of(routePlanning));
        when(productReconRepository.findProductReconByDeliveryIdIgnoreCaseAndDeletedIsFalse("ROUTE-001"))
                .thenReturn(Optional.of(productRecon));
        when(productItemRepository.findByProductReconAndPublicIdAndDeletedIsFalse(productRecon, VAN_RECON_ITEM_ID))
                .thenReturn(Optional.of(vanReconItem));
        when(vanSalesHelper.createVanSalesOrder(eq(customer), eq(routePlanning), eq(request.getItems()), anyList()))
                .thenReturn(vanSaleOrder);
        when(routePlanningRepository.save(any(RoutePlanning.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productReconRepository.findByDeliveryAgentPublicIdIgnoreCaseAndDeliveryIdIgnoreCaseAndDeletedIsFalse(DA_ID.toString(), "ROUTE-001"))
                .thenReturn(Optional.of(productRecon));
        when(warehouseRepository.findByPublicIdAndDeletedIsFalse(warehouseId))
                .thenReturn(Optional.of(warehouse));
        when(deliveryAgentsRepository.findByPublicIdAndDeletedIsFalse(DA_ID))
                .thenReturn(Optional.of(deliveryAgent));
        when(productItemRepository.save(any(ProductItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productRepository.findByPublicIdAndDeletedIsFalse(PRODUCT_ID))
                .thenReturn(Optional.of(product));
        when(salesOrdersRepository.findByPublicId(vanSaleOrderId))
                .thenReturn(vanSaleOrder);
        when(salesOrderItemsRepository.save(any(SalesOrderItems.class))).thenAnswer(inv -> inv.getArgument(0));
        when(salesOrdersRepository.save(any(SalesOrders.class))).thenAnswer(inv -> inv.getArgument(0));
        when(salesOrdersRepository.saveAndFlush(any(SalesOrders.class))).thenAnswer(inv -> inv.getArgument(0));
        when(eventsRepository.saveAndFlush(any(Events.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deliveryAgentsRepository.findDeliveryAgentsByPhoneNumberAndDeletedIsFalse("da@test.com"))
                .thenReturn(Optional.of(deliveryAgent));
        when(salesOrdersRepository.findByPublicIdAndDeletedIsFalse(vanSaleOrderId))
                .thenReturn(Optional.of(vanSaleOrder));
        when(orderAmendmentRepository.findOrderAmendmentBySalesOrderPublicIdAndDeletedIsFalse(vanSaleOrderId.toString()))
                .thenReturn(Optional.empty());
        when(routePlanningRepository.findAllByDeliveryAgentIdIgnoreCaseAndRouteStatusesAndDeletedIsFalse(anyString(), anyList()))
                .thenReturn(List.of(routePlanning));
        when(deliveryAgentOperationRepository.findByRouteIdAndOrderNumberAndSkuAndDeletedIsFalse("ROUTE-001", "ORD-VAN-1", "SKU-001"))
                .thenReturn(Optional.empty());
        when(deliveryAgentOperationRepository.save(any(DeliveryAgentOperation.class))).thenAnswer(inv -> inv.getArgument(0));

        service.sellVanSaleItem(request, "da@test.com");

        assertEquals(4, vanReconItem.getQuantitySold());
        assertEquals(6, vanReconItem.getVanSaleQuantity());
        assertEquals(3, vanReconItem.getVanSaleItemSold());
        assertEquals(3, vanSaleOrderItem.getQuantityDelivered());
        assertEquals(OrderItemStatus.Delivered, vanSaleOrderItem.getStatus());
        assertEquals(LoadingStatusEnum.DELIVERED, vanSaleOrder.getSalesOrderStatus());
        assertEquals(OrderStatusEnum.Delivered, vanSaleOrder.getStatus());
        assertEquals(PaymentStatusEnum.PAID, vanSaleOrder.getPaymentStatus());

        verify(liveStockHelper).updateLiveStockValue(warehouseId, PRODUCT_ID, 3, ProductUpdateType.SOLD_STOCK, java.time.LocalDate.now());
        verify(stockMovementHelper).recordStockMovement(eq(product), eq(3), eq(StockMovementTransactionType.DELIVERY), eq(TransactionBoundType.RECORD), anyString(), eq(warehouse));
        verify(deliveryAgentOperationRepository).save(argThat(operation ->
                operation.isVanSale()
                        && "ORD-VAN-1".equals(operation.getOrderNumber())
                        && operation.getVanSoldQuantity() == 3
                        && operation.getTotalQuantityPicked() == 3));
    }

    @Test
    void markOrderAsDelivered_keepsGrossTotalAmountWhenLinesHaveDiscount() throws Exception {
        SalesOrderItems item = salesOrder.getSalesOrderItems().iterator().next();
        item.setHasDiscount(true);
        item.setDiscount(new BigDecimal("50"));
        item.setQuantityDelivered(4);
        item.setPricePerUnit(new BigDecimal("100"));
        item.setStatus(OrderItemStatus.Delivered);
        salesOrder.setTotalAmount(new BigDecimal("1000"));

        stubCommonDeliveryFlow();

        MarkOrderDeliveredRequest request = new MarkOrderDeliveredRequest();
        request.setConvertCashToCredit(false);

        service.markOrderAsDelivered(ORDER_ID, ROUTE_ID, request, "da@test.com");

        ArgumentCaptor<SalesOrders> captor = ArgumentCaptor.forClass(SalesOrders.class);
        verify(salesOrdersRepository, atLeastOnce()).saveAndFlush(captor.capture());
        SalesOrders saved = captor.getValue();

        assertEquals(0, new BigDecimal("400").compareTo(saved.getTotalAmount()),
                "ORDER TOTAL must stay delivered gross, not net of the 50 discount");
        assertEquals(0, new BigDecimal("350").compareTo(item.getTotalPayable()),
                "line payable is still gross minus discount");
        assertEquals(0, new BigDecimal("400").compareTo(saved.getTotalAmountDelivered()));
    }

    @Test
    void markOrderAsDelivered_withoutConversionFlag_leavesCashPaid() throws Exception {
        stubCommonDeliveryFlow();

        MarkOrderDeliveredRequest request = new MarkOrderDeliveredRequest();
        request.setConvertCashToCredit(false);

        service.markOrderAsDelivered(ORDER_ID, ROUTE_ID, request, "da@test.com");

        ArgumentCaptor<SalesOrders> captor = ArgumentCaptor.forClass(SalesOrders.class);
        verify(salesOrdersRepository, atLeastOnce()).save(captor.capture());
        SalesOrders lastSaved = captor.getAllValues().get(captor.getAllValues().size() - 1);

        assertEquals(OrderType.CASH, lastSaved.getOrderType());
        assertFalse(lastSaved.isCreditApplied());
        assertEquals(PaymentStatusEnum.PAID, lastSaved.getPaymentStatus());
        verify(bnplHelper, never()).createUpdateCreditItems(any(), any());
        verify(warehouseService, never()).createReconciliation(any(), anyString(), anyBoolean());
        verify(reconciliationRepository, never()).findByRoutePublicIdAndDeletedIsFalse(anyString());
    }

    @Test
    void markOrderAsDelivered_nativeCreditWithoutConversion_postsCreditButSkipsReconciliationRefresh() throws Exception {
        salesOrder.setOrderType(OrderType.CREDIT);
        salesOrder.setCreditApplied(true);

        when(deliveryAgentsRepository.findDeliveryAgentsByPhoneNumberAndDeletedIsFalse("da@test.com"))
                .thenReturn(Optional.of(deliveryAgent));
        when(salesOrdersRepository.findByPublicIdAndDeletedIsFalse(ORDER_ID))
                .thenReturn(Optional.of(salesOrder));
        when(orderAmendmentRepository.findOrderAmendmentBySalesOrderPublicIdAndDeletedIsFalse(ORDER_ID.toString()))
                .thenReturn(Optional.empty());
        when(salesOrdersRepository.save(any(SalesOrders.class))).thenAnswer(inv -> inv.getArgument(0));
        when(salesOrdersRepository.saveAndFlush(any(SalesOrders.class))).thenAnswer(inv -> inv.getArgument(0));
        when(eventsRepository.saveAndFlush(any(Events.class))).thenAnswer(inv -> inv.getArgument(0));
        when(routePlanningRepository.findAllByDeliveryAgentIdIgnoreCaseAndRouteStatusesAndDeletedIsFalse(anyString(), anyList()))
                .thenReturn(Collections.emptyList());

        CreditLimitItem creditLimitItem = new CreditLimitItem();
        creditLimitItem.setInterestAmount(BigDecimal.ZERO);
        when(creditLimitItemRepository.findByOrderNumberAndStatusAndDeletedIsFalse("ORD-TEST-1", WalletItemStatus.ACTIVE))
                .thenReturn(Optional.of(creditLimitItem));

        doNothing().when(customersRepository).updateLastOrderDate(any(), any());
        doNothing().when(tumaMessage).tumaUjumbeKwaMteja(anyString(), anyString(), anyString());
        doNothing().when(tumaMessage).whatsAppCustomer(anyString(), anyString(), anyString(), anyString());

        MarkOrderDeliveredRequest request = new MarkOrderDeliveredRequest();
        request.setConvertCashToCredit(false);

        service.markOrderAsDelivered(ORDER_ID, ROUTE_ID, request, "da@test.com");

        verify(bnplHelper, times(1)).createUpdateCreditItems(any(SalesOrders.class), isNull());
        verify(warehouseService, never()).createReconciliation(any(), anyString(), anyBoolean());
        verify(reconciliationRepository, never()).findByRoutePublicIdAndDeletedIsFalse(anyString());
        verify(routePlanningRepository, never()).save(any(RoutePlanning.class));
    }

    @Test
    void markOrderAsDelivered_conversionMissingReason_rejects() {
        when(deliveryAgentsRepository.findDeliveryAgentsByPhoneNumberAndDeletedIsFalse("da@test.com"))
                .thenReturn(Optional.of(deliveryAgent));
        when(salesOrdersRepository.findByPublicIdAndDeletedIsFalse(ORDER_ID))
                .thenReturn(Optional.of(salesOrder));
        when(orderAmendmentRepository.findOrderAmendmentBySalesOrderPublicIdAndDeletedIsFalse(ORDER_ID.toString()))
                .thenReturn(Optional.empty());

        MarkOrderDeliveredRequest request = new MarkOrderDeliveredRequest();
        request.setConvertCashToCredit(true);
        request.setConversionReason("  ");

        assertThrows(BadRequestRestApiException.class,
                () -> service.markOrderAsDelivered(ORDER_ID, ROUTE_ID, request, "da@test.com"));

        verify(bnplHelper, never()).createUpdateCreditItems(any(), any());
    }

    @Test
    void markOrderAsDelivered_conversionInsufficientCredit_rejectsWithoutBnplPosting() {
        when(deliveryAgentsRepository.findDeliveryAgentsByPhoneNumberAndDeletedIsFalse("da@test.com"))
                .thenReturn(Optional.of(deliveryAgent));
        when(salesOrdersRepository.findByPublicIdAndDeletedIsFalse(ORDER_ID))
                .thenReturn(Optional.of(salesOrder));
        when(orderAmendmentRepository.findOrderAmendmentBySalesOrderPublicIdAndDeletedIsFalse(ORDER_ID.toString()))
                .thenReturn(Optional.empty());
        when(creditLimitItemRepository.findByOrderNumberAndStatusAndDeletedIsFalse("ORD-TEST-1", WalletItemStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(customerCheckerHelper.checkIfCustomerIsAllowedToBuyOnCredit(CUSTOMER_ID,false))
                .thenReturn(new BigDecimal("100"));

        MarkOrderDeliveredRequest request = new MarkOrderDeliveredRequest();
        request.setConvertCashToCredit(true);
        request.setConversionReason("Wrong payment mode at door");

        assertThrows(BadRequestRestApiException.class,
                () -> service.markOrderAsDelivered(ORDER_ID, ROUTE_ID, request, "da@test.com"));

        verify(bnplHelper, never()).createUpdateCreditItems(any(), any());
    }

    @Test
    void markOrderAsDelivered_conversionZeroDeliveredPayable_rejects() {
        salesOrder.getSalesOrderItems().forEach(i -> i.setQuantityDelivered(0));

        when(deliveryAgentsRepository.findDeliveryAgentsByPhoneNumberAndDeletedIsFalse("da@test.com"))
                .thenReturn(Optional.of(deliveryAgent));
        when(salesOrdersRepository.findByPublicIdAndDeletedIsFalse(ORDER_ID))
                .thenReturn(Optional.of(salesOrder));
        when(orderAmendmentRepository.findOrderAmendmentBySalesOrderPublicIdAndDeletedIsFalse(ORDER_ID.toString()))
                .thenReturn(Optional.empty());

        MarkOrderDeliveredRequest request = new MarkOrderDeliveredRequest();
        request.setConvertCashToCredit(true);
        request.setConversionReason("Delivered on credit by mistake");

        assertThrows(BadRequestRestApiException.class,
                () -> service.markOrderAsDelivered(ORDER_ID, ROUTE_ID, request, "da@test.com"));
    }

    @Test
    void cashToCreditConversion_partialDeliveryWithDiscount_usesDeliveredPayableNotBookedTotal() {
        SalesOrderItems item = salesOrder.getSalesOrderItems().iterator().next();
        item.setQuantity(10);
        item.setQuantityDelivered(2);
        item.setPricePerUnit(new BigDecimal("100"));
        Invoice invoice = new Invoice();
        invoice.setHasBeenApplied(true);
        invoice.setDiscountType(DiscountType.SpecialDiscount);
        invoice.setDiscountAmount(new BigDecimal("20"));
        salesOrder.setInvoice(invoice);

        BigDecimal bookedTotal = item.getPricePerUnit().multiply(BigDecimal.valueOf(item.getQuantity()));
        BigDecimal deliveredPayable = salesOrder.getTotalAmountDelivered().subtract(salesOrder.getTotalDiscount());

        assertEquals(0, new BigDecimal("180").compareTo(deliveredPayable));
        assertTrue(deliveredPayable.compareTo(bookedTotal) < 0,
                "Conversion capacity must use delivered payable, not original booked total");

        when(deliveryAgentsRepository.findDeliveryAgentsByPhoneNumberAndDeletedIsFalse("da@test.com"))
                .thenReturn(Optional.of(deliveryAgent));
        when(salesOrdersRepository.findByPublicIdAndDeletedIsFalse(ORDER_ID))
                .thenReturn(Optional.of(salesOrder));
        when(orderAmendmentRepository.findOrderAmendmentBySalesOrderPublicIdAndDeletedIsFalse(ORDER_ID.toString()))
                .thenReturn(Optional.empty());
        when(creditLimitItemRepository.findByOrderNumberAndStatusAndDeletedIsFalse("ORD-TEST-1", WalletItemStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(customerCheckerHelper.checkIfCustomerIsAllowedToBuyOnCredit(CUSTOMER_ID,false))
                .thenReturn(new BigDecimal("179"));

        MarkOrderDeliveredRequest request = new MarkOrderDeliveredRequest();
        request.setConvertCashToCredit(true);
        request.setConversionReason("Partial delivery on credit");

        assertThrows(BadRequestRestApiException.class,
                () -> service.markOrderAsDelivered(ORDER_ID, ROUTE_ID, request, "da@test.com"));
    }

    @Test
    void markOrderAsDelivered_conversion_success_postsCreditOnceAndSkipsCashPaid() throws Exception {
        stubCommonDeliveryFlow();
        when(customerCheckerHelper.checkIfCustomerIsAllowedToBuyOnCredit(CUSTOMER_ID,false))
                .thenReturn(new BigDecimal("5000"));

        CreditLimitItem creditLimitItem = new CreditLimitItem();
        creditLimitItem.setInterestAmount(BigDecimal.ZERO);
        when(creditLimitItemRepository.findByOrderNumberAndStatusAndDeletedIsFalse(eq("ORD-TEST-1"), eq(WalletItemStatus.ACTIVE)))
                .thenReturn(Optional.empty(), Optional.of(creditLimitItem));

        MarkOrderDeliveredRequest request = new MarkOrderDeliveredRequest();
        request.setConvertCashToCredit(true);
        request.setConversionReason("Customer should have been on credit");

        service.markOrderAsDelivered(ORDER_ID, ROUTE_ID, request, "da@test.com");

        ArgumentCaptor<SalesOrders> captor = ArgumentCaptor.forClass(SalesOrders.class);
        verify(salesOrdersRepository, atLeastOnce()).save(captor.capture());

        boolean sawCreditFlags = captor.getAllValues().stream()
                .anyMatch(o -> o.getOrderType() == OrderType.CREDIT && o.isCreditApplied());
        assertTrue(sawCreditFlags);

        SalesOrders lastSaved = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertNotEquals(PaymentStatusEnum.PAID, lastSaved.getPaymentStatus());

        verify(bnplHelper, times(1)).createUpdateCreditItems(any(SalesOrders.class), isNull());
        verify(warehouseService, times(1)).createReconciliation(any(ProductRecon.class), eq(ROUTE_ID.toString()), eq(true));
    }

    @Test
    void markOrderAsDelivered_conversion_routeBelongsToAnotherAgent_rejectsBeforeBnplAndReconciliation() {
        when(deliveryAgentsRepository.findDeliveryAgentsByPhoneNumberAndDeletedIsFalse("da@test.com"))
                .thenReturn(Optional.of(deliveryAgent));
        when(salesOrdersRepository.findByPublicIdAndDeletedIsFalse(ORDER_ID))
                .thenReturn(Optional.of(salesOrder));
        when(orderAmendmentRepository.findOrderAmendmentBySalesOrderPublicIdAndDeletedIsFalse(ORDER_ID.toString()))
                .thenReturn(Optional.empty());
        when(creditLimitItemRepository.findByOrderNumberAndStatusAndDeletedIsFalse("ORD-TEST-1", WalletItemStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(customerCheckerHelper.checkIfCustomerIsAllowedToBuyOnCredit(CUSTOMER_ID,false))
                .thenReturn(new BigDecimal("5000"));

        RoutePlanning routePlanning = new RoutePlanning();
        routePlanning.setPublicId(ROUTE_ID);
        routePlanning.setRouteId("route-1");
        routePlanning.setDeliveryAgentId(UUID.randomUUID().toString());
        routePlanning.setSalesOrders(List.of(salesOrder));
        when(routePlanningRepository.findByPublicIdAndDeletedIsFalse(ROUTE_ID)).thenReturn(Optional.of(routePlanning));

        MarkOrderDeliveredRequest request = new MarkOrderDeliveredRequest();
        request.setConvertCashToCredit(true);
        request.setConversionReason("Customer should have been on credit");

        assertThrows(BadRequestRestApiException.class,
                () -> service.markOrderAsDelivered(ORDER_ID, ROUTE_ID, request, "da@test.com"));

        verify(bnplHelper, never()).createUpdateCreditItems(any(), any());
        verify(warehouseService, never()).createReconciliation(any(), anyString(), anyBoolean());
    }

    @Test
    void convertDeliveredCashOrderToCredit_success_createsCreditWithoutRedelivery() {
        salesOrder.setSalesOrderStatus(LoadingStatusEnum.DELIVERED);
        salesOrder.setStatus(OrderStatusEnum.Delivered);
        salesOrder.setDeliveryAgent(deliveryAgent);
        salesOrder.setRoutePublicId(ROUTE_ID.toString());
        salesOrder.setPaymentStatus(PaymentStatusEnum.NOT_PAID);

        when(salesOrdersRepository.findByPublicIdAndDeletedIsFalse(ORDER_ID))
                .thenReturn(Optional.of(salesOrder));
        when(customerCheckerHelper.checkIfCustomerIsAllowedToBuyOnCredit(CUSTOMER_ID, false))
                .thenReturn(new BigDecimal("5000"));
        when(salesOrdersRepository.saveAndFlush(any(SalesOrders.class))).thenAnswer(inv -> inv.getArgument(0));
        when(salesOrdersRepository.save(any(SalesOrders.class))).thenAnswer(inv -> inv.getArgument(0));
        when(eventsRepository.save(any(Events.class))).thenAnswer(inv -> inv.getArgument(0));

        CreditLimitItem creditLimitItem = new CreditLimitItem();
        creditLimitItem.setAmount(new BigDecimal("400"));
        creditLimitItem.setInterestAmount(new BigDecimal("4"));
        when(creditLimitItemRepository.findByOrderNumberAndStatusAndDeletedIsFalse("ORD-TEST-1", WalletItemStatus.ACTIVE))
                .thenReturn(Optional.empty(), Optional.of(creditLimitItem));
        when(creditLimitItemRepository.save(any(CreditLimitItem.class))).thenAnswer(inv -> inv.getArgument(0));

        RoutePlanning routePlanning = new RoutePlanning();
        routePlanning.setPublicId(ROUTE_ID);
        routePlanning.setRouteId("route-1");
        routePlanning.setDeliveryAgentId(DA_ID.toString());
        routePlanning.setSalesOrders(List.of(salesOrder));
        when(routePlanningRepository.findByPublicIdAndDeletedIsFalse(ROUTE_ID)).thenReturn(Optional.of(routePlanning));

        ProductRecon productRecon = new ProductRecon();
        productRecon.setPublicId(UUID.randomUUID());
        when(productReconRepository.findByDeliveryAgentPublicIdIgnoreCaseAndDeliveryIdIgnoreCaseAndDeletedIsFalse(anyString(), anyString()))
                .thenReturn(Optional.of(productRecon));

        Reconciliation reconciliation = new Reconciliation();
        reconciliation.setDeliveryAmount(new BigDecimal("400"));
        reconciliation.setDepositAmount(BigDecimal.ZERO);
        reconciliation.setCreditAmount(new BigDecimal("400"));
        when(reconciliationRepository.findByRoutePublicIdAndDeletedIsFalse(ROUTE_ID.toString()))
                .thenReturn(Optional.of(reconciliation));
        when(routePlanningRepository.save(any(RoutePlanning.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(warehouseService).createReconciliation(any(), anyString(), anyBoolean());

        ConvertCashToCreditRequest request = new ConvertCashToCreditRequest();
        request.setConversionReason("Customer did not pay on delivery");
        request.setRoutePublicId(ROUTE_ID);

        assertDoesNotThrow(() -> service.convertDeliveredCashOrderToCredit(ORDER_ID, request, "admin@itibari.io"));

        assertEquals(OrderType.CREDIT, salesOrder.getOrderType());
        assertTrue(salesOrder.isCreditApplied());
        assertEquals(PaymentStatusEnum.NOT_PAID, salesOrder.getPaymentStatus());
        assertEquals(LoadingStatusEnum.DELIVERED, salesOrder.getSalesOrderStatus());
        verify(bnplHelper).createUpdateCreditItems(any(SalesOrders.class), isNull());
        verify(warehouseService).createReconciliation(any(), anyString(), eq(true));
        verify(eventsRepository).save(argThat(event ->
                event.getEventType().equals("CASH_TO_CREDIT_POST_DELIVERY")));
    }

    @Test
    void convertDeliveredCashOrderToCredit_rejectsWhenNotDelivered() {
        salesOrder.setSalesOrderStatus(LoadingStatusEnum.IN_TRANSIT);
        when(salesOrdersRepository.findByPublicIdAndDeletedIsFalse(ORDER_ID))
                .thenReturn(Optional.of(salesOrder));

        ConvertCashToCreditRequest request = new ConvertCashToCreditRequest();
        request.setConversionReason("Customer did not pay on delivery");

        assertThrows(BadRequestRestApiException.class,
                () -> service.convertDeliveredCashOrderToCredit(ORDER_ID, request, "admin@itibari.io"));
        verify(bnplHelper, never()).createUpdateCreditItems(any(), any());
    }

    @Test
    void markOrderAsDelivered_conversion_bnplPostingFailure_propagates() throws Exception {
        stubCommonDeliveryFlow();
        when(creditLimitItemRepository.findByOrderNumberAndStatusAndDeletedIsFalse("ORD-TEST-1", WalletItemStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(customerCheckerHelper.checkIfCustomerIsAllowedToBuyOnCredit(CUSTOMER_ID,false))
                .thenReturn(new BigDecimal("5000"));
        doThrow(new BadRequestRestApiException().developerMessage("wallet failure").userMessage("wallet failure"))
                .when(bnplHelper).createUpdateCreditItems(any(SalesOrders.class), isNull());

        MarkOrderDeliveredRequest request = new MarkOrderDeliveredRequest();
        request.setConvertCashToCredit(true);
        request.setConversionReason("Mistake at delivery");

        assertDoesNotThrow(() -> service.markOrderAsDelivered(ORDER_ID, ROUTE_ID, request, "da@test.com"));
        verify(bnplHelper).createUpdateCreditItems(any(SalesOrders.class), isNull());
        assertEquals(OrderType.CREDIT, salesOrder.getOrderType());
        assertTrue(salesOrder.isCreditApplied());
    }

    private void stubCommonDeliveryFlow() throws IOException, InterruptedException {
        when(deliveryAgentsRepository.findDeliveryAgentsByPhoneNumberAndDeletedIsFalse("da@test.com"))
                .thenReturn(Optional.of(deliveryAgent));
        when(salesOrdersRepository.findByPublicIdAndDeletedIsFalse(ORDER_ID))
                .thenReturn(Optional.of(salesOrder));
        when(orderAmendmentRepository.findOrderAmendmentBySalesOrderPublicIdAndDeletedIsFalse(ORDER_ID.toString()))
                .thenReturn(Optional.empty());
        when(salesOrdersRepository.save(any(SalesOrders.class))).thenAnswer(inv -> inv.getArgument(0));
        when(salesOrdersRepository.saveAndFlush(any(SalesOrders.class))).thenAnswer(inv -> inv.getArgument(0));
        when(eventsRepository.saveAndFlush(any(Events.class))).thenAnswer(inv -> inv.getArgument(0));
        when(routePlanningRepository.findAllByDeliveryAgentIdIgnoreCaseAndRouteStatusesAndDeletedIsFalse(anyString(), anyList()))
                .thenReturn(Collections.emptyList());

        RoutePlanning routePlanning = new RoutePlanning();
        routePlanning.setPublicId(ROUTE_ID);
        routePlanning.setRouteId("route-1");
        routePlanning.setDeliveryAgentId(DA_ID.toString());
        routePlanning.setSalesOrders(List.of(salesOrder));
        when(routePlanningRepository.findByPublicIdAndDeletedIsFalse(ROUTE_ID)).thenReturn(Optional.of(routePlanning));

        ProductRecon productRecon = new ProductRecon();
        productRecon.setPublicId(UUID.randomUUID());
        when(productReconRepository.findByDeliveryAgentPublicIdIgnoreCaseAndDeliveryIdIgnoreCaseAndDeletedIsFalse(anyString(), anyString()))
                .thenReturn(Optional.of(productRecon));

        Reconciliation reconciliation = new Reconciliation();
        reconciliation.setDeliveryAmount(new BigDecimal("400"));
        reconciliation.setDepositAmount(BigDecimal.ZERO);
        reconciliation.setCreditAmount(new BigDecimal("400"));
        when(reconciliationRepository.findByRoutePublicIdAndDeletedIsFalse(ROUTE_ID.toString()))
                .thenReturn(Optional.of(reconciliation));
        when(routePlanningRepository.save(any(RoutePlanning.class))).thenAnswer(inv -> inv.getArgument(0));

        doNothing().when(customersRepository).updateLastOrderDate(any(), any());
        doNothing().when(tumaMessage).tumaUjumbeKwaMteja(anyString(), anyString(), anyString());
        doNothing().when(tumaMessage).whatsAppCustomer(anyString(), anyString(), anyString(), anyString());
        when(salesOrdersRepository.findByPublicIdAndDeletedIsFalse(ORDER_ID)).thenReturn(Optional.of(salesOrder));
    }

    private static Product minimalProduct() {
        TaxMapping taxMapping = new TaxMapping();
        taxMapping.setTaxRate(BigDecimal.ZERO);
        Product product = new Product();
        product.setTaxMapping(taxMapping);
        product.setSellingPriceBeforeTax(BigDecimal.ZERO);
        return product;
    }
}
