package com.app.itibari.services;

import com.app.itibari.dataTypes.CreditNoteApproveAction;
import com.app.itibari.dataTypes.CreditNoteStatus;
import com.app.itibari.dataTypes.PurchaseOrderStatus;
import com.app.itibari.dataTypes.SupplierStatus;
import com.app.itibari.models.entities.CreditNote;
import com.app.itibari.models.entities.PurchaseOrder;
import com.app.itibari.models.entities.PurchaseOrderItem;
import com.app.itibari.models.entities.Suppliers;
import com.app.itibari.models.request.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ProcurementService {

    Suppliers createSupplier(@Valid SupplierRequest supplierRequest, String name);

    void updateSupplier(UUID supplierId, @Valid SupplierRequest supplierRequest, String name);

    Suppliers getSupplier(UUID supplierPublicId);

    Page<Suppliers> getSuppliers(String searchParam, SupplierStatus supplierStatus, Integer maxOrder, Integer minOrder, Pageable pageable);

    void deleteSupplier(UUID supplierPublicId, String loggedInUser);

    PurchaseOrder createPurchaseOrder(@Valid PurchaseOrderRequest purchaseOrderRequest, String name);

    Page<PurchaseOrder> getAllPurchaseOrders(String searchParam,
                                             LocalDate fromDate,
                                             LocalDate toDate,
                                             UUID supplierId,
                                             String orderNumber,
                                             PurchaseOrderStatus orderStatus,
                                             List<PurchaseOrderStatus> orderStatuses,
                                             Boolean rebate,
                                             Boolean replacement,
                                             Pageable pageable);

    void updatePurchaseOrderActions(UUID purchaseOrderPublicId, @Valid PurchaseOrderUpdateRequest purchaseOrderRequest, String name);

    void updateSupplierActions(UUID supplierPublicId, @Valid SupplierActionRequest supplierActionRequest, String name);

    PurchaseOrder getPurchaseOrder(UUID purchaseOrderPublicId);

    void updatePurchaseOrder(UUID purchaseOrderPublicId, @Valid PurchaseOrderRequest purchaseOrderRequest, String name);

    void adminApprovePurchaseOrder(UUID purchaseOrderPublicId,
                                   @Valid PurchaseOrderApprovalRequest purchaseOrderApprovalRequest,
                                   String name);

    String receiveGoods(UUID purchaseOrderPublicId, @Valid PurchaseOrderReceiveRequest purchaseOrderReceiveRequest, String name);

    void editPurchaseOrderItem(UUID purchaseOrderPublicId,
                               UUID purchaseOrderItemPublicId,
                               @Valid PurchaseOrderItemRequest purchaseOrderItemRequest,
                               String name);

    Page<PurchaseOrderItem> getPurchaseOrderItems(UUID purchaseOrderPublicId, String searchParam, Pageable pageable);

    PurchaseOrder createReplacementPurchaseOrder(UUID goodReturnNotePublicId, String name);

    String receiveShipment(UUID purchaseOrderPublicId, @Valid PurchaseOrderReceiveRequest purchaseOrderReceiveRequest, String name);

    CreditNote raiseCreditNoteFromPurchaseOrder(UUID purchaseOrderPublicId, @Valid CreditNoteRequest creditNoteRequest, String name);

    void approveCreditNote(UUID creditNotePublicId, CreditNoteApproveAction creditNoteApproveAction,String comment, String name);

    Page<CreditNote> getCreditNotes(String searchParam, LocalDate fromDate, LocalDate toDate, UUID supplierId, List<CreditNoteStatus> orderStatuses, Boolean tagged,UUID purchaseOrderPublicId, Pageable pageable);

    CreditNote raiseCreditForSupplier(UUID supplierPublicId, @Valid CreditNoteRequest creditNoteRequest, String name);

    void raiseRefundRequest(UUID purchaseOrderPublicId, String name);
}
