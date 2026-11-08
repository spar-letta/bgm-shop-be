package com.app.itibari.services;

import com.app.itibari.dataTypes.*;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.AmendRequest;
import com.app.itibari.repository.*;
import com.app.itibari.services.helper.*;
import com.app.itibari.utils.helpers.CustomerCheckerHelper;
import com.app.itibari.utils.helpers.TumaMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DeliveryAgentsServiceImplAmendmentApprovalTest {

    private static final UUID AMENDMENT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ORDER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID REASON_CODE_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

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

    private SalesOrders salesOrder;
    private OrderAmendment orderAmendment;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        salesOrder = new SalesOrders();
        salesOrder.setPublicId(ORDER_ID);
        salesOrder.setSalesOrderNumber("SO-1001");
        salesOrder.setSalesOrderStatus(LoadingStatusEnum.IN_TRANSIT);
        salesOrder.setAwaitingApproval(true);
        salesOrder.setOrderAmendStatus(OrderAmendStatus.Pending);

        SalesOrderItems orderItem = new SalesOrderItems();
        orderItem.setQuantity(4);
        orderItem.setQuantityDelivered(0);
        orderItem.setStatus(OrderItemStatus.NotDelivered);
        salesOrder.setSalesOrderItems(Set.of(orderItem));

        orderAmendment = new OrderAmendment();
        orderAmendment.setPublicId(AMENDMENT_ID);
        orderAmendment.setAmendAction(AmendAction.CancelAndRequestVanSale);
        orderAmendment.setStatus(AmendmentStatus.Pending);
        orderAmendment.setSalesOrder(salesOrder);

        when(orderAmendmentRepository.findByPublicIdAndDeletedIsFalse(AMENDMENT_ID))
                .thenReturn(Optional.of(orderAmendment));
        when(salesOrdersRepository.save(any(SalesOrders.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderAmendmentRepository.save(any(OrderAmendment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        RejectionsCode rejectionCode = new RejectionsCode();
        rejectionCode.setPublicId(REASON_CODE_ID);
        when(rejectionCodeRepository.findRejectionsCodeByPublicIdAndDeletedIsFalse(REASON_CODE_ID))
                .thenReturn(Optional.of(rejectionCode));
    }

    @Test
    void approvePartialQuantityAmendment_doesNotMarkOrderFailed() {
        RejectionsCode rejectionCode = new RejectionsCode();
        rejectionCode.setPublicId(UUID.randomUUID());
        orderAmendment.setRejectionsCode(rejectionCode);

        AmendmentProductItem amendmentItem = new AmendmentProductItem();
        amendmentItem.setIssuedQuantity(4);
        amendmentItem.setDeliveredQuantity(2);
        amendmentItem.setAmendmentQuantity(2);
        amendmentItem.setPricePerUnit(BigDecimal.TEN);
        orderAmendment.setAmendmentProductItems(List.of(amendmentItem));

        AmendRequest amendRequest = new AmendRequest();
        amendRequest.setAmendOrderPublicId(AMENDMENT_ID);
        amendRequest.setActionType(ActionType.APPROVE);
        amendRequest.setComment("Approved partial delivery");
        amendRequest.setReasonCodePublicId(REASON_CODE_ID);

        service.amendOrderActions(amendRequest, "office@itibari.com");

        assertEquals(LoadingStatusEnum.IN_TRANSIT, salesOrder.getSalesOrderStatus());
        assertEquals(OrderAmendStatus.Approved, salesOrder.getOrderAmendStatus());
        assertEquals(false, salesOrder.isAwaitingApproval());
        verify(eventsRepository, never()).save(any(Events.class));
    }

    @Test
    void approveAllQuantitiesRemovedViaAmendOrder_marksOrderFailed() {
        RejectionsCode rejectionCode = new RejectionsCode();
        rejectionCode.setPublicId(UUID.randomUUID());
        orderAmendment.setRejectionsCode(rejectionCode);

        AmendmentProductItem amendmentItem = new AmendmentProductItem();
        amendmentItem.setIssuedQuantity(4);
        amendmentItem.setDeliveredQuantity(0);
        amendmentItem.setAmendmentQuantity(4);
        amendmentItem.setPricePerUnit(BigDecimal.TEN);
        orderAmendment.setAmendmentProductItems(List.of(amendmentItem));

        when(salesOrdersRepository.findByPublicIdAndDeletedIsFalse(ORDER_ID))
                .thenReturn(Optional.of(salesOrder));

        AmendRequest amendRequest = new AmendRequest();
        amendRequest.setAmendOrderPublicId(AMENDMENT_ID);
        amendRequest.setActionType(ActionType.APPROVE);
        amendRequest.setComment("Approved full quantity removal via amendOrder");
        amendRequest.setReasonCodePublicId(REASON_CODE_ID);

        service.amendOrderActions(amendRequest, "office@itibari.com");

        assertEquals(LoadingStatusEnum.FAILED_DELIVERY, salesOrder.getSalesOrderStatus());
        assertEquals(OrderStatusEnum.Failed, salesOrder.getStatus());
        verify(eventsRepository, atLeastOnce()).save(any(Events.class));
    }

    @Test
    void approveFullOrderCancellationAmendment_marksOrderFailed() {
        orderAmendment.setRejectionsCode(null);
        AmendmentProductItem amendmentItem = new AmendmentProductItem();
        amendmentItem.setIssuedQuantity(4);
        amendmentItem.setDeliveredQuantity(0);
        amendmentItem.setAmendmentQuantity(4);
        amendmentItem.setPricePerUnit(BigDecimal.TEN);
        orderAmendment.setAmendmentProductItems(List.of(amendmentItem));

        when(salesOrdersRepository.findByPublicIdAndDeletedIsFalse(ORDER_ID))
                .thenReturn(Optional.of(salesOrder));

        AmendRequest amendRequest = new AmendRequest();
        amendRequest.setAmendOrderPublicId(AMENDMENT_ID);
        amendRequest.setActionType(ActionType.APPROVE);
        amendRequest.setComment("Approved full cancellation");
        amendRequest.setReasonCodePublicId(REASON_CODE_ID);

        service.amendOrderActions(amendRequest, "office@itibari.com");

        assertEquals(LoadingStatusEnum.FAILED_DELIVERY, salesOrder.getSalesOrderStatus());
        assertEquals(OrderStatusEnum.Failed, salesOrder.getStatus());
        verify(eventsRepository, atLeastOnce()).save(any(Events.class));
    }

    @Test
    void approveFullOrderCancellation_convertsOrderItemsToVanSaleOnlyOnce() {
        UUID routeId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        UUID productId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

        DeliveryAgents deliveryAgent = new DeliveryAgents();
        deliveryAgent.setPublicId(UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee"));
        salesOrder.setDeliveryAgent(deliveryAgent);
        salesOrder.setRoutePublicId(routeId.toString());

        Product product = new Product();
        product.setPublicId(productId);
        product.setSku("SKU-1");
        product.setProductName("Product 1");

        SalesOrderItems orderItem = new SalesOrderItems();
        orderItem.setQuantity(4);
        orderItem.setQuantityDelivered(0);
        orderItem.setStatus(OrderItemStatus.NotDelivered);
        orderItem.setProduct(product);
        orderItem.setPricePerUnit(BigDecimal.TEN);
        salesOrder.setSalesOrderItems(Set.of(orderItem));

        RoutePlanning routePlanning = new RoutePlanning();
        routePlanning.setPublicId(routeId);
        routePlanning.setRouteId("ROUTE-1");

        ProductRecon productRecon = new ProductRecon();
        productRecon.setDeliveryId("ROUTE-1");

        ProductItem productItem = new ProductItem();
        productItem.setPublicId(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"));
        productItem.setProductPublicId(productId);
        productItem.setProductCode("SKU-1");
        productItem.setProductName("Product 1");
        productItem.setVanSaleQuantity(0);

        orderAmendment.setRejectionsCode(null);
        AmendmentProductItem amendmentItem = new AmendmentProductItem();
        amendmentItem.setIssuedQuantity(4);
        amendmentItem.setDeliveredQuantity(0);
        amendmentItem.setAmendmentQuantity(4);
        amendmentItem.setPricePerUnit(BigDecimal.TEN);
        orderAmendment.setAmendmentProductItems(List.of(amendmentItem));
        orderAmendment.setFailedOrder(true);

        when(salesOrdersRepository.findByPublicIdAndDeletedIsFalse(ORDER_ID))
                .thenReturn(Optional.of(salesOrder));
        when(routePlanningRepository.findByPublicIdAndDeletedIsFalse(routeId))
                .thenReturn(Optional.of(routePlanning));
        when(productReconRepository.findProductReconByDeliveryIdIgnoreCaseAndDeletedIsFalse("ROUTE-1"))
                .thenReturn(Optional.of(productRecon));
        when(orderAmendmentRepository.findOrderAmendmentBySalesOrderPublicIdAndDeletedIsFalse(ORDER_ID.toString()))
                .thenReturn(Optional.of(orderAmendment));
        when(productItemRepository.findByProductReconAndProductPublicIdAndDeletedIsFalse(productRecon, productId))
                .thenReturn(Optional.of(productItem));
        when(productItemRepository.save(any(ProductItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(salesOrderItemsRepository.save(any(SalesOrderItems.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(vanSalesRepository.save(any(VanSales.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryAgentOperationRepository.findByRouteIdAndOrderNumberAndSkuAndDeletedIsFalse(any(), any(), any()))
                .thenReturn(Optional.empty());

        AmendRequest amendRequest = new AmendRequest();
        amendRequest.setAmendOrderPublicId(AMENDMENT_ID);
        amendRequest.setActionType(ActionType.APPROVE);
        amendRequest.setComment("Approved full cancellation");
        amendRequest.setReasonCodePublicId(REASON_CODE_ID);

        service.amendOrderActions(amendRequest, "office@itibari.com");
        service.amendOrderActions(amendRequest, "office@itibari.com");

        assertEquals(4, productItem.getVanSaleQuantity());
        assertEquals(true, orderItem.isVanSaleConverted());
        verify(productItemRepository, times(2)).save(any(ProductItem.class));
    }
}
