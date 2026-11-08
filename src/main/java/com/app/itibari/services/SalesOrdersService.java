package com.app.itibari.services;

import com.app.itibari.dataTypes.*;
import com.app.itibari.models.CustomerReturnItem;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.*;
import com.app.itibari.models.response.*;
import com.itextpdf.io.source.ByteArrayOutputStream;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SalesOrdersService {

    SalesOrders createSalesOrder(@Valid SalesOrdersRequest salesOrdersRequest, String name);

    void updateSalesOrder(UUID salesOrderPublicId, @Valid SalesOrdersRequest SalesOrdersRequest, String name, Boolean isCustomer, Boolean isSalesAgentInitiated);

    SalesOrders getSalesOrder(UUID salesOrderPublicId);

    void deleteSalesOrder(UUID salesOrderPublicId, String name);

    SalesOrdersPageableDTO getAllSalesOrders(String searchParam,
                                             String salesOrderNumber,
                                             UUID customerId,
                                             UUID warehouseId,
                                             UUID organizationRegionId,
                                             SourceChannelEnum sourceChannel,
                                             LoadingStatusEnum salesOrderStatus,
                                             PaymentStatusEnum paymentStatus,
                                             String invoiceNumber,
                                             LocalDate startDate,
                                             LocalDate endDate,
                                             UUID location,
                                             Boolean bookedBySalesAgent,
                                             UUID salesAgentPublicId,
                                             UUID deliveryAgentPublicId,
                                             UUID parentCustomerPublicId,
                                             Boolean isCredit,
                                             List<LoadingStatusEnum> statuses,
                                             Boolean isWalkIn,
                                             Boolean bookedByParent,
                                             Pageable pageable);

    void moveOrdersToPlanning();

    Page<SalesOrderItems> getSalesOrderItems(UUID salesOrderPublicId, Pageable pageable);

    Page<SalesOrderItemSlimDTO> getSalesOrderItemsSlim(UUID salesOrderPublicId, Pageable pageable);

    Page<SalesOrderItems> getSalesOrderItemsPerAgent(UUID deliveryAgentPublicId, Pageable pageable);

    SalesOrders placeOrder(@Valid SalesOrdersRequest salesOrdersRequest);

    Page<SalesOrders> getCustomerOrders(String searchParam,
                                        String salesOrderNumber,
                                        UUID warehouseId,
                                        UUID organizationRegionId,
                                        LoadingStatusEnum salesOrderStatus,
                                        PaymentStatusEnum paymentStatus,
                                        String invoiceNumber,
                                        Boolean isCredit,
                                        String startDate,
                                        String endDate,
                                        String name,
                                        Pageable pageable);

    SalesOrders updateNewCustomerOrders(Customers customer,
                                        List<ItemRequest> itemRequests,
                                        RoutePlanning routePlanning,
                                        UUID productReconPublicId);

    void cancelSalesOrder(UUID salesOrderPublicId, Boolean isCustomerInitiated, UUID rejectionReasonPublicId, String name);

    void requestForSpecialDiscount(SpecialRequest specialRequest, String name);

    BigDecimal checkForDiscountApproval(CartDiscountRequest cartDiscountRequest, String name);

    void recalculateDiscount(UUID salesOrderPublicId, String name);

    Page<SalesOrderAgent> getSalesOrderAgent(Pageable pageable);

    Page<SalesSummaryDto> fetchSalesAgentOderSummary(UUID organisationRegionPublicId, UUID organisationLocationPublicId, LocalDate startDate, LocalDate endDate, String searchParam, Principal principal, Pageable pageable);

    List<ZoneAverageDTO1> productPerformanceDateRangeByZone(UUID productId, UUID zoneId, LocalDate startDate,
                                                            LocalDate endDate,
                                                            Pageable pageable,
                                                            Principal principal);

    List<ZoneNameDateAverageProjection> productPerformance(UUID productId, UUID regionId, LocalDate startDate,
                                                           LocalDate endDate, UUID locationId, Pageable pageable,
                                                           Principal principal);

    Page<ZoneNameAverageProjection> orderValuePerZone(UUID productId, UUID regionId, LocalDate startDate,
                                                      LocalDate endDate, UUID locationId, Pageable pageable,
                                                      Principal principal);

    ByteArrayOutputStream generateInvoice(UUID salesOrderPublic, LocalDate startDate, LocalDate endDate, Pageable pageable, Principal principal);


    ByteArrayOutputStream generateCustomerInvoiceReceipt(UUID salesOrderPublic, OrderType orderType, String name, String orderNumber);

    OrderStatsDTO getSalesOrderStats(String searchParam, String salesOrderNumber, UUID customerId, UUID warehouseId, UUID organizationRegionId, SourceChannelEnum sourceChannel, LoadingStatusEnum salesOrderStatus, PaymentStatusEnum paymentStatus, String invoiceNumber, LocalDate startDate, LocalDate endDate, UUID locationId, Boolean bookedBySalesAgent, UUID salesAgentPublicId, UUID deliveryAgentPublicId, UUID parentCustomerPublicId, Boolean isCredit, List<LoadingStatusEnum> statuses, Boolean isWalkIn, Boolean bookedByParent);

    void generateTraReceipt(UUID salesOrderPublicId, String name);

    Page<SalesOrders> getAllCustomerSalesOrders(UUID customerPublicId, Pageable pageable);

    void transitionOrders(UUID orderPublicId, String name);

    void notifyCustomerWithSmsOrderInPlanningAndTransit() throws IOException, InterruptedException;

    SalesOrders createWalkInSalesOrder(@Valid WalkInSalesOrdersRequest walkInSalesOrdersRequest, String name);

    Page<ProductRecon> getWalkInProductReconciliations(String searchParam, Boolean reconciled, String warehousePublicId, ReconStatus status, UUID createdBy, LocalDate startDate, LocalDate endDate, Principal principal, Pageable pageable);

    SalesOrders createVanSalesOrder(SalesOrdersRequest salesOrdersRequest, RoutePlanning routePlanning);

    List<String> getSalesOrderNumbers(UUID deliveryAgentPublicId, Principal principal);

    CustomerReturnOrderItem getSalesOrderItemList(String orderNumber, Principal principal);

    Page<CustomerReturn> getCustomerReturnList(String search, String orderNumber, UUID deliveryAgentPublicId, CustomerReturnStatus status, LocalDate startDate, LocalDate endDate, Principal principal, Pageable pageable);

    Page<CustomerReturnItem> getCustomerReturnItemList(UUID returnPublicId, Principal principal, Pageable pageable);

    ByteArrayOutputStream runExportCustomerReturns(String searchParam, UUID deliveryAgentPublicId, CustomerReturnStatus status, LocalDate startDate, LocalDate endDate, StatementExportType exportType, Pageable pageable, Principal principal);

    SalesAgentOrderPageable getAllSalesOrdersForSA(String searchParam, String salesOrderNumber, LocalDate startDate, LocalDate endDate, Pageable pageable, String salesAgentPublicId);

    BNPLOrderSummary getBNPLOrderSummary(UUID salesOrderPublicId);
}
