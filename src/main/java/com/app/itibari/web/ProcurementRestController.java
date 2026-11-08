package com.app.itibari.web;

import com.app.itibari.dataTypes.CreditNoteApproveAction;
import com.app.itibari.dataTypes.CreditNoteStatus;
import com.app.itibari.dataTypes.PurchaseOrderStatus;
import com.app.itibari.dataTypes.SupplierStatus;
import com.app.itibari.docs.Examples;
import com.app.itibari.models.entities.CreditNote;
import com.app.itibari.models.entities.PurchaseOrder;
import com.app.itibari.models.entities.PurchaseOrderItem;
import com.app.itibari.models.entities.Suppliers;
import com.app.itibari.models.request.*;
import com.app.itibari.services.ProcurementService;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Tag(name = "Procurement", description = "Procurement API")
@RestController
@RequestMapping("/procurement")
public class ProcurementRestController {

    private final ProcurementService procurementService;

    public ProcurementRestController(ProcurementService procurementService) {

        this.procurementService = procurementService;
    }

/*
supplier details
 */

    @PreAuthorize("hasAuthority('CREATE_SUPPLIER')")
    @JsonView(BaseView.SupplierSummaryView.class)
    @PostMapping("/supplier")
    @Operation(summary = "Create a new supplier", description = "Create a new supplier",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET_SUPPLIER", value = Examples.FETCH_SUPPLIER_OK)})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "CREATE_SUPPLIER", value = Examples.CREATE_SUPPLIER),
    })))
    public Suppliers createSupplier(@RequestBody @Valid SupplierRequest supplierRequest,
                                    @Parameter(hidden = true) Principal principal) {

        return procurementService.createSupplier(supplierRequest, principal.getName());
    }

    @PreAuthorize("hasAuthority('UPDATE_SUPPLIER')")
    @PatchMapping("/supplier/{supplierPublicId}")
    @Operation(summary = "Update supplier details", description = "Update supplier details",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "UPDATE_SUPPLIER", value = Examples.CREATE_SUPPLIER),
    })))
    public void updateSupplier(@PathVariable UUID supplierPublicId, @RequestBody @Valid SupplierRequest supplierRequest,
                               @Parameter(hidden = true) Principal principal) {

        procurementService.updateSupplier(supplierPublicId, supplierRequest, principal.getName());
    }

    @PreAuthorize("hasAuthority('VIEW_SUPPLIER')")
    @JsonView(BaseView.SupplierSummaryView.class)
    @GetMapping("/supplier/{supplierPublicId}")
    @Operation(summary = "Get supplier details", description = "Get supplier details",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET_SUPPLIER", value = Examples.FETCH_SUPPLIER_OK)})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Suppliers getSupplier(@PathVariable UUID supplierPublicId) {

        return procurementService.getSupplier(supplierPublicId);
    }

    @PreAuthorize("hasAuthority('VIEW_SUPPLIER')")
    @JsonView(BaseView.SupplierSummaryView.class)
    @GetMapping("/suppliers")
    @Operation(summary = "Get all suppliers", description = "Get all suppliers",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<Suppliers> getSuppliers(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(name = "status", required = false) SupplierStatus supplierStatus,
            @RequestParam(name = "maxOrder", required = false) Integer maxOrder,
            @RequestParam(name = "minOrder", required = false) Integer minOrder,
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return procurementService.getSuppliers(searchParam, supplierStatus, maxOrder, minOrder, pageable);
    }

    @PreAuthorize("hasAuthority('DELETE_SUPPLIER')")
    @DeleteMapping("/supplier/{supplierPublicId}")
    @Operation(summary = "Delete supplier", description = "Delete supplier",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void deleteSupplier(@PathVariable UUID supplierPublicId,
                               @Parameter(hidden = true) Principal principal) {

        procurementService.deleteSupplier(supplierPublicId, principal.getName());
    }

    //actions

    @PreAuthorize("hasAuthority('UPDATE_SUPPLIER')")
    @PatchMapping("/supplier/{supplierPublicId}/actions")
    @Operation(summary = "Update supplier actions", description = "Update supplier actions",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "ADD_PRODUCTS", value = Examples.ADD_PRODUCTS_TO_SUPPLIER),
            @ExampleObject(name = "REMOVE_PRODUCTS", value = Examples.REMOVE_PRODUCTS_FROM_SUPPLIER),
    })))
    public void updateSupplierActions(@PathVariable UUID supplierPublicId,
                                      @RequestBody @Valid SupplierActionRequest supplierActionRequest,
                                      @Parameter(hidden = true) Principal principal) {

        procurementService.updateSupplierActions(supplierPublicId, supplierActionRequest, principal.getName());
    }

    // purchase order list details

    @PreAuthorize("hasAuthority('CREATE_PURCHASE_ORDER')")
    @JsonView(BaseView.purchaseOrderSummaryView.class)
    @PostMapping("/purchaseOrder")
    @Operation(summary = "Create a new purchase order", description = "Create a new purchase order",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET_PURCHASE_ORDER", value = Examples.GET_PURCHASE_ORDER)})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "CREATE_PURCHASE_ORDER", value = Examples.CREATE_PURCHASE_ORDER),})))
    public PurchaseOrder createPurchaseOrder(@RequestBody @Valid PurchaseOrderRequest purchaseOrderRequest,
                                             @Parameter(hidden = true) Principal principal) {

        return procurementService.createPurchaseOrder(purchaseOrderRequest, principal.getName());
    }

    @PreAuthorize("hasAuthority('UPDATE_PURCHASE_ORDER')")
    @PatchMapping("/purchaseOrder/{purchaseOrderPublicId}")
    @Operation(summary = "Update purchase order details", description = "Update purchase order details",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "UPDATE_PURCHASE_ORDER", value = Examples.UPDATE_PURCHASE_ORDER),
    })))
    public void updatePurchaseOrder(@PathVariable UUID purchaseOrderPublicId,
                                    @RequestBody @Valid PurchaseOrderRequest purchaseOrderRequest,
                                    @Parameter(hidden = true) Principal principal) {

        procurementService.updatePurchaseOrder(purchaseOrderPublicId, purchaseOrderRequest, principal.getName());
    }

    @PreAuthorize("hasAuthority('VIEW_PURCHASE_ORDER')")
    @JsonView(BaseView.purchaseOrderSummaryView.class)
    @GetMapping("/purchaseOrders")
    @Operation(summary = "Get all purchase orders", description = "Get all purchase orders",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })

    @PageableAsQueryParam
    public Page<PurchaseOrder> getAllPurchaseOrders(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(name = "fromDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fromDate,
            @RequestParam(name = "toDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate toDate,
            @RequestParam(name = "supplier", required = false) UUID supplierId,
            @RequestParam(name = "orderNumber", required = false) String orderNumber,
            @RequestParam(name = "status", required = false) PurchaseOrderStatus orderStatus,
            @RequestParam(name = "statuses", required = false) List<PurchaseOrderStatus> orderStatuses,
            @RequestParam(name = "rebate", required = false) Boolean rebate,
            @RequestParam(name = "replacement", required = false) Boolean replacement,
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return procurementService.getAllPurchaseOrders(searchParam,
                fromDate,
                toDate,
                supplierId,
                orderNumber,
                orderStatus,
                orderStatuses,
                rebate,
                replacement,
                pageable);
    }

    @PreAuthorize("hasAuthority('UPDATE_PURCHASE_ORDER')")
    @JsonView(BaseView.purchaseOrderSummaryView.class)
    @PatchMapping("/purchaseOrder/{purchaseOrderPublicId}/actions")
    @Operation(summary = "Update purchase order details", description = "Update purchase order details",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "ADD_PRODUCTS", value = Examples.ADD_PRODUCTS),
            @ExampleObject(name = "REMOVE_PRODUCTS", value = Examples.REMOVE_PRODUCTS),
            @ExampleObject(name = "PROCESS", value = Examples.PROCESS_PURCHASE_ORDER),
            @ExampleObject(name = "DISPATCH_PURCHASE_ORDER", value = Examples.DISPATCH_PURCHASE_ORDER),
            @ExampleObject(name = "CANCEL_PURCHASE_ORDER", value = Examples.CANCEL_PURCHASE_ORDER),
            @ExampleObject(name = "ACCOUNTANT_DECLINE_PURCHASE_ORDER_CANCELLATION", value = Examples.CANCELLATION_DECLINED),
            @ExampleObject(name = "ACCOUNTANT_APPROVE_PURCHASE_ORDER_CANCELLATION", value = Examples.CANCELLATION_APPROVED),
            @ExampleObject(name = "DISPATCHED_PURCHASE_ORDER_REVERT", value = Examples.REVERT),
    })))
    public void updatePurchaseOrderActions(@PathVariable UUID purchaseOrderPublicId,
                                           @RequestBody @Valid PurchaseOrderUpdateRequest purchaseOrderUpdateRequest,
                                           @Parameter(hidden = true) Principal principal) {

        procurementService.updatePurchaseOrderActions(purchaseOrderPublicId, purchaseOrderUpdateRequest, principal.getName());
    }

    @PreAuthorize("hasAuthority('VIEW_PURCHASE_ORDER')")
    @JsonView(BaseView.purchaseOrderSummaryView.class)
    @GetMapping("/purchaseOrder/{purchaseOrderPublicId}")
    @Operation(summary = "Get purchase order details", description = "Get purchase order details",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public PurchaseOrder getPurchaseOrder(@PathVariable UUID purchaseOrderPublicId) {

        return procurementService.getPurchaseOrder(purchaseOrderPublicId);
    }

    @PreAuthorize("hasAuthority('CREATE_PURCHASE_ORDER_ITEM')")
    @PostMapping("/purchaseOrder/{purchaseOrderPublicId}/items/{purchaseOrderItemPublicId}")
    @Operation(summary = "Edit item to purchase order", description = "edit item to purchase order",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "EDIT_PURCHASE_ORDER_ITEM", value = Examples.EDIT_PURCHASE_ORDER_ITEM),
    })))

    public void editPurchaseOrderItem(@PathVariable UUID purchaseOrderPublicId, @PathVariable UUID purchaseOrderItemPublicId,
                                      @RequestBody @Valid PurchaseOrderItemRequest purchaseOrderItemRequest,
                                      @Parameter(hidden = true) Principal principal) {

        procurementService.editPurchaseOrderItem(purchaseOrderPublicId,
                purchaseOrderItemPublicId,
                purchaseOrderItemRequest,
                principal.getName());
    }

    @JsonView(BaseView.purchaseOrderSummaryView.class)
    @PreAuthorize("hasAuthority('APPROVE_PURCHASE_ORDER')")
    @PatchMapping("/purchaseOrder/{purchaseOrderPublicId}/adminApprove")
    @Operation(summary = "Admin approve purchase order", description = "Admin approve purchase order",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
                    @ExampleObject(name = "APPROVE_PURCHASE_ORDER", value = Examples.APPROVE_PURCHASE_ORDER),})))
    public void adminApprovePurchaseOrder(@PathVariable UUID purchaseOrderPublicId,
                                          @RequestBody @Valid PurchaseOrderApprovalRequest purchaseOrderApprovalRequest,
                                          @Parameter(hidden = true) Principal principal) {

        procurementService.adminApprovePurchaseOrder(purchaseOrderPublicId, purchaseOrderApprovalRequest, principal.getName());
    }

    // Receive goods

    @JsonView(BaseView.purchaseOrderSummaryView.class)
    @PreAuthorize("hasAuthority('RECEIVE_GOODS')")
    @PatchMapping("/purchaseOrder/{purchaseOrderPublicId}/receiveGoods")
    @Operation(summary = "Receive goods", description = "Receive goods",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
                    @ExampleObject(name = "RECEIVE_GOODS", value = Examples.RECEIVE_GOODS),
            })))
    public String receiveGoods(@PathVariable UUID purchaseOrderPublicId,
                               @RequestBody @Valid PurchaseOrderReceiveRequest purchaseOrderReceiveRequest,
                               @Parameter(hidden = true) Principal principal) {

        return procurementService.receiveGoods(purchaseOrderPublicId, purchaseOrderReceiveRequest, principal.getName());
    }

    // get purchase order items by purchase order public id

    @PreAuthorize("hasAuthority('VIEW_PURCHASE_ORDER')")
    @JsonView(BaseView.purchaseOrderSummaryView.class)
    @GetMapping("/purchaseOrder/{purchaseOrderPublicId}/items")
    @Operation(summary = "Get purchase order items by purchase order public id", description = "Get purchase order items by purchase order public id",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Page<PurchaseOrderItem> getPurchaseOrderItems(@PathVariable UUID purchaseOrderPublicId,
                                                         @RequestParam(name = "searchParam", required = false) String searchParam,
                                                         @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return procurementService.getPurchaseOrderItems(purchaseOrderPublicId, searchParam, pageable);
    }

    // raise a replacement purchase order

    @PreAuthorize("hasAuthority('CREATE_PURCHASE_ORDER')")
    @JsonView(BaseView.purchaseOrderSummaryView.class)
    @PostMapping("/purchaseOrder/{goodReturnNotePublicId}/replacement")
    @Operation(summary = "Create a replacement purchase order", description = "Create a replacement purchase order",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET_PURCHASE_ORDER", value = Examples.GET_PURCHASE_ORDER)})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "CREATE_REPLACEMENT_PURCHASE_ORDER", value = ""),
    })))
    public PurchaseOrder createReplacementPurchaseOrder(@PathVariable UUID goodReturnNotePublicId,
                                                        @Parameter(hidden = true) Principal principal) {

        return procurementService.createReplacementPurchaseOrder(goodReturnNotePublicId, principal.getName());
    }

    @JsonView(BaseView.purchaseOrderSummaryView.class)
    @PreAuthorize("hasAuthority('RECEIVE_GOODS')")
    @PatchMapping("/{purchaseOrderPublicId}/receiveShipment")
    @Operation(summary = "Receive goods", description = "Receive goods",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK")},
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
                    @ExampleObject(name = "RECEIVE_GOODS", value = Examples.RECEIVE_GOODS),
            })))
    public String receiveShipment(@PathVariable UUID purchaseOrderPublicId,
                                  @RequestBody @Valid PurchaseOrderReceiveRequest purchaseOrderReceiveRequest,
                                  @Parameter(hidden = true) Principal principal) {
        return procurementService.receiveShipment(purchaseOrderPublicId, purchaseOrderReceiveRequest, principal.getName());
    }


    // raise credit note from PO

    @JsonView(BaseView.CreditNoteSummaryView.class)
    @PreAuthorize("hasAuthority('UPDATE_PURCHASE_ORDER')")
    @PostMapping("/purchaseOrder/{purchaseOrderPublicId}/creditNote")
    @Operation(summary = "Raise credit note from purchase order", description = "Raise credit note from purchase order",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "RAISE_CREDIT_NOTE_FROM_PO", value = Examples.RAISE_CREDIT_NOTE_FROM_PO),
    })))
    public CreditNote raiseCreditNoteFromPurchaseOrder(@PathVariable UUID purchaseOrderPublicId,
                                                       @RequestBody @Valid CreditNoteRequest creditNoteRequest,
                                                       @Parameter(hidden = true) Principal principal) {
        creditNoteRequest.setPurchaseOrderPublicId(purchaseOrderPublicId);
        return procurementService.raiseCreditNoteFromPurchaseOrder(purchaseOrderPublicId, creditNoteRequest, principal.getName());

    }


    // approve credit note

    @PreAuthorize("hasAuthority('UPDATE_PURCHASE_ORDER')")
    @PatchMapping("/creditNote/{creditNotePublicId}/approve")
    @Operation(summary = "Approve credit note", description = "Approve credit note",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "APPROVE_CREDIT_NOTE", value = ""),
    })))
    public void approveCreditNote(@PathVariable UUID creditNotePublicId,
                                  @RequestParam(name = "action") CreditNoteApproveAction action,
                                  @RequestParam(name = "comment", required = false) String comment,
                                  @Parameter(hidden = true) Principal principal) {
        procurementService.approveCreditNote(creditNotePublicId, action, comment, principal.getName());
    }


    // fetch credit notes
    @JsonView(BaseView.CreditNoteSummaryView.class)
    @PreAuthorize("hasAuthority('VIEW_PURCHASE_ORDER')")
    @GetMapping("/creditNotes")
    @Operation(summary = "Get credit notes", description = "Get credit notes by purchase order public id",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Page<CreditNote> getCreditNotes(@RequestParam(name = "searchParam", required = false) String searchParam,
                                           @RequestParam(name = "fromDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fromDate,
                                           @RequestParam(name = "toDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate toDate,
                                           @RequestParam(name = "supplier", required = false) UUID supplierId,
                                           @RequestParam(name = "statuses", required = false) List<CreditNoteStatus> orderStatuses,
                                           @RequestParam(name = "tagged", required = false) Boolean tagged,
                                           @RequestParam(name = "purchaseOrderPublicId", required = false) UUID purchaseOrderPublicId,
                                           @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return procurementService.getCreditNotes(searchParam, fromDate, toDate, supplierId, orderStatuses, tagged, purchaseOrderPublicId, pageable);
    }

    // raise credit for supplier

    @PreAuthorize("hasAuthority('VIEW_PURCHASE_ORDER')")
    @PatchMapping("/supplier/{supplierPublicId}/raiseCredit")
    @Operation(summary = "Raise credit for supplier", description = "Raise credit for supplier",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "RAISE_CREDIT_FOR_SUPPLIER", value = Examples.RAISE_CREDIT_FOR_SUPPLIER),
    })))
    public CreditNote raiseCreditForSupplier(@PathVariable UUID supplierPublicId,
                                             @RequestBody @Valid CreditNoteRequest creditNoteRequest,
                                             @Parameter(hidden = true) Principal principal) {
        return procurementService.raiseCreditForSupplier(supplierPublicId, creditNoteRequest, principal.getName());
    }


    // raise a refund request on partial and dispatched PO

    @PreAuthorize("hasAuthority('UPDATE_PURCHASE_ORDER')")
    @PatchMapping("/purchaseOrder/{purchaseOrderPublicId}/refund")
    @Operation(summary = "Raise refund request for purchase order", description = "Raise refund request for purchase order",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void raiseRefundRequest(@PathVariable UUID purchaseOrderPublicId,
                                   @Parameter(hidden = true) Principal principal) {
        procurementService.raiseRefundRequest(purchaseOrderPublicId, principal.getName());
    }
}
