package com.app.itibari.services;

import com.app.itibari.dataTypes.*;
import com.app.itibari.models.entities.DeliveryAgents;
import com.app.itibari.models.entities.LocationApproval;
import com.app.itibari.models.entities.OrderAmendment;
import com.app.itibari.models.entities.SalesOrders;
import com.app.itibari.models.request.*;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.text.DocumentException;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

public interface DeliveryAgentsService {
    void updateDeliveryAgentAvailability(UUID deliveryAgentPublicId, boolean available, UUID reasonPublicId, String name);

    DeliveryAgents createDeliveryAgent(@Valid DeliveryAgentsRequest deliveryAgentsRequest, String name);

    void updateDeliveryAgent(UUID deliveryAgentPublicId, @Valid DeliveryAgentsRequest deliveryAgentsRequest, String name);

    DeliveryAgents getDeliveryAgent(UUID deliveryAgentPublicId);

    Page<DeliveryAgents> getAllDeliveryAgents(String searchParam, Pageable pageable);

    void deleteDeliveryAgent(UUID deliveryAgentPublicId, String name);

    void generatePassword(UUID deliveryAgentPublicId, String name);

    void amendOrder(UUID deliveryAgentPublicId, @Valid AmendOrderRequest amendOrderRequest, String name);

    Page<OrderAmendment> getAllOrderAmendments(String searchParam, Pageable pageable);

    void amendOrderActions(@Valid AmendRequest amendRequest, String name);

    DeliveryAgents getDeliveryAgentMobileProfile(String name);

    void acceptRoute(UUID routeId, AcceptTypeEnum acceptType, String name);

    void routeActions(UUID routeId, RouteAction action, String name);

    void markOrderAsDelivered(UUID orderPublicId, UUID routePublicId, MarkOrderDeliveredRequest request, String name) throws IOException, InterruptedException;

    void markOrderItemAsDelivered(@Valid DeliverItemRequest deliverItemRequest, String name);

    void markVanSaleItemAsSold(@Valid DeliverVanSaleItemRequest deliverItemRequest, String name);

    @Deprecated
    void verifyCustomerLocation(UUID customerPublicId, String name);

    void verifyCustomerLocationWithOtp(UUID customerPublicId,
                                       @Valid VerifyCustomerLocationRequest verifyCustomerLocationRequest,
                                       String name);

    void cancelOrder(UUID salesOrderPublicId, CancelReasonRequest cancelReasonRequest, String name);

    String sendOtpToCustomer(UUID orderPublicId, String name);

    void generateTraReceiptSchedular();

    void generateTraCustomerReceipt(SalesOrders salesOrder);

    void requestLocationApproval(LocationApprovalRequest locationApprovalRequest, String name);

    void approveOrRejectLocationApproval(UUID locationApprovalId, boolean approve,UUID rejectionCodePublicId, String name);

    void unlockLocationApprovalRequest(UUID locationApprovalPublicId, String name);

    Page<LocationApproval> getLocationApprovals(String searchParam, List<LocationApprovalStatus> statuses, String deliveryAgent, String customer,String from,String to, Pageable pageable);

    List<DeliveryAgents> getDeliveryAgentMiniData(String searchParam);

    void sellVanSaleItem(@Valid SellVanSaleRequest deliverItemRequest, String name);

    void amendFullDeliveredOrder(UUID salesOrderPublicId, @Valid AmendOrderItemRequest amendOrderItemRequest, Principal principal);

    /**
     * Convert a delivered unpaid cash order to credit without re-marking delivery.
     * Intended for IMS/admin correction when cash was not collected at delivery.
     */
    void convertDeliveredCashOrderToCredit(UUID orderPublicId, @Valid ConvertCashToCreditRequest request, String name);

    void approveCustomerReturn(UUID customerReturnPublicId, ReturnApprovalType returnApprovalType, Principal principal);

    void sendAmendmentToDepartmentOfficers() throws IOException;

    ByteArrayOutputStream runExportOrderAmendment(String searchParam, StatementExportType exportType, Pageable pageable, Principal principal) throws DocumentException, IOException;
}
