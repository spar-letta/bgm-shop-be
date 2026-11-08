package com.app.itibari.web;

import com.app.itibari.dataTypes.RefundAction;
import com.app.itibari.dataTypes.RemittanceStatus;
import com.app.itibari.docs.Examples;
import com.app.itibari.models.entities.Remittances;
import com.app.itibari.models.entities.TaxMapping;
import com.app.itibari.models.request.PurchaseOrderApprovalRequest;
import com.app.itibari.models.request.RemittanceRequest;
import com.app.itibari.models.request.TaxMappingRequest;
import com.app.itibari.services.FinancialService;
import com.app.itibari.services.RemittanceService;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@Tag(name = "Financial", description = "The Financial API")
@RestController
@RequestMapping("/financial")
public class FinancialRestController {

    private final FinancialService financialService;

    private final RemittanceService remittanceService;

    public FinancialRestController(FinancialService financialService, RemittanceService remittanceService) {

        this.financialService = financialService;
        this.remittanceService = remittanceService;
    }

    @JsonView(BaseView.purchaseOrderSummaryView.class)
    @PreAuthorize("hasAuthority('FINANCE_APPROVE_PURCHASE_ORDER')")
    @PatchMapping("/purchaseOrder/{purchaseOrderPublicId}/approve")
    @Operation(summary = "Accountant approve purchase order", description = "Admin approve purchase order",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
                    @ExampleObject(name = "APPROVE", value = Examples.ACCOUNTANT_APPROVE_PURCHASE_ORDER),
            })))
    public void accountantApprovePurchaseOrder(@PathVariable UUID purchaseOrderPublicId,
                                               @RequestBody @Valid PurchaseOrderApprovalRequest purchaseOrderApprovalRequest,
                                               @Parameter(hidden = true) Principal principal) {

        financialService.accountantApprovePurchaseOrder(purchaseOrderPublicId, purchaseOrderApprovalRequest, principal.getName());
    }

    // remittance. create
    @Hidden
    @JsonView(BaseView.RemittanceSummaryView.class)
    @PostMapping("/remittance")
    @Operation(summary = "Create Remittance", description = "Create Remittance",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET_REMITTANCE", value = Examples.GET_REMITTANCE)})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
                    @ExampleObject(name = "CREATE", value = Examples.CREATE_REMITTANCE),
            })))
    public Remittances createRemittance(@RequestBody @Valid RemittanceRequest remittanceRequest,
                                        @Parameter(hidden = true) Principal principal) {

        return remittanceService.createRemittance(remittanceRequest, principal.getName());
    }

    // remittance. update
    @JsonView(BaseView.RemittanceSummaryView.class)
    @PutMapping("/remittance/{remittancePublicId}")
    @Operation(summary = "Update Remittance", description = "Update Remittance",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
                    @ExampleObject(name = "UPDATE", value = Examples.UPDATE_REMITTANCE),
            })))
    public void updateRemittance(@PathVariable UUID remittancePublicId, @RequestBody @Valid RemittanceRequest remittanceRequest,
                                 @Parameter(hidden = true) Principal principal) {

        remittanceService.updateRemittance(remittancePublicId, remittanceRequest, principal.getName());
    }

    //get remittance by public id

    @JsonView(BaseView.RemittanceSummaryView.class)
    @GetMapping("/remittance/{remittancePublicId}")
    @Operation(summary = "Get Remittance by Public Id", description = "Get Remittance by Public Id",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Remittances getRemittanceByPublicId(@PathVariable UUID remittancePublicId) {

        return remittanceService.getRemittanceByPublicId(remittancePublicId);
    }

    // get all remittances

    @JsonView(BaseView.RemittanceSummaryView.class)
    @GetMapping("/remittance")
    @Operation(summary = "Get All Remittances", description = "Get All Remittances",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET_REMITTANCES", value = Examples.GET_REMITTANCES)})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Page<Remittances> getRemittances(@RequestParam(name = "invoice", required = false) String invoiceNumber,
                                            @RequestParam(name = "purchaseOrder", required = false) String purchaseOrderNumber,
                                            @RequestParam(name = "supplier", required = false) UUID supplierId,
                                            @RequestParam(name = "status", required = false) RemittanceStatus status,
                                            @RequestParam(name = "paymentMethod", required = false) String paymentMethod,
                                            @RequestParam(required = false, name = "to") String to,
                                            @RequestParam(required = false, name = "from") String from,
                                            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return remittanceService.getRemittances(invoiceNumber, purchaseOrderNumber, supplierId, status, paymentMethod, to, from, pageable);
    }

    // delete remittance

    @DeleteMapping("/remittance/{remittancePublicId}")
    @Operation(summary = "Delete Remittance", description = "Delete Remittance",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void deleteRemittance(@PathVariable UUID remittancePublicId,
                                 @Parameter(hidden = true) Principal principal) {

        remittanceService.deleteRemittance(remittancePublicId, principal.getName());
    }

    @JsonView(BaseView.TaxMappingSummaryView.class)
    @PostMapping("/taxMapping")
    @Operation(summary = "Create Tax Mapping", description = "Create Tax Mapping",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET_TAX_MAPPING", value = Examples.GET_TAX_MAPPING)})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
                    @ExampleObject(name = "CREATE", value = Examples.CREATE_TAX_MAPPING),
            })))

    public TaxMapping createTaxMapping(@RequestBody @Valid TaxMappingRequest taxMappingRequest,
                                       @Parameter(hidden = true) Principal principal) {

        return financialService.createTaxMapping(taxMappingRequest, principal.getName());
    }

    // update

    @JsonView(BaseView.TaxMappingSummaryView.class)
    @PutMapping("/taxMapping/{taxMappingPublicId}")
    @Operation(summary = "Update Tax Mapping", description = "Update Tax Mapping",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
                    @ExampleObject(name = "UPDATE", value = Examples.CREATE_TAX_MAPPING),
            })))
    public void updateTaxMapping(@PathVariable UUID taxMappingPublicId, @RequestBody @Valid TaxMappingRequest taxMappingRequest,
                                 @Parameter(hidden = true) Principal principal) {

        financialService.updateTaxMapping(taxMappingPublicId, taxMappingRequest, principal.getName());
    }

    // get tax mapping by public id

    @JsonView(BaseView.TaxMappingSummaryView.class)
    @GetMapping("/taxMapping/{taxMappingPublicId}")
    @Operation(summary = "Get Tax Mapping by Public Id", description = "Get Tax Mapping by Public Id",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public TaxMapping getTaxMappingByPublicId(@PathVariable UUID taxMappingPublicId) {

        return financialService.getTaxMappingByTaxId(taxMappingPublicId);
    }

    // get all tax mappings

    @JsonView(BaseView.TaxMappingSummaryView.class)
    @GetMapping("/taxMapping")
    @Operation(summary = "Get All Tax Mappings", description = "Get All Tax Mappings",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET_TAX_MAPPINGS", value = Examples.GET_TAX_MAPPINGS)})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Page<TaxMapping> getTaxMappings(@RequestParam(name = "taxName", required = false) String taxName,
                                           @RequestParam(name = "taxRate", required = false) String taxRate,
                                           @RequestParam(name = "taxType", required = false) String taxType,
                                           @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return financialService.getTaxMappings(taxName, taxRate, taxType, pageable);
    }

    @DeleteMapping("/taxMapping/{taxMappingPublicId}")
    @Operation(summary = "Delete Tax Mapping", description = "Delete Tax Mapping",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void deleteTaxMapping(@PathVariable UUID taxMappingPublicId,
                                 @Parameter(hidden = true) Principal principal) {

        financialService.deleteTaxMapping(taxMappingPublicId, principal.getName());
    }

    @JsonView(BaseView.purchaseOrderSummaryView.class)
    @PreAuthorize("hasAuthority('FINANCE_APPROVE_PURCHASE_ORDER')")
    @PatchMapping("/purchaseOrder/{purchaseOrderPublicId}/update")
    @Operation(summary = "Accountant update purchase order", description = "Admin update purchase order",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK")},
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
                    @ExampleObject(name = "UPDATE PO PAYMENT", value = Examples.ACCOUNTANT_UPDATE_PURCHASE_ORDER),
            })))
    public void accountantUpdatePurchaseOrderPayment(@PathVariable UUID purchaseOrderPublicId,
                                                     @RequestBody @Valid PurchaseOrderApprovalRequest purchaseOrderApprovalRequest,
                                                     @Parameter(hidden = true) Principal principal) {

        financialService.accountantUpdatePurchaseOrderPayment(purchaseOrderPublicId, purchaseOrderApprovalRequest, principal.getName());
    }

    // po refund

    @PreAuthorize("hasAuthority('FINANCE_APPROVE_PURCHASE_ORDER')")
    @PatchMapping("/purchaseOrder/{purchaseOrderPublicId}/refundApprove")
    @Operation(summary = "Accountant refund purchase order", description = "Admin refund purchase order",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK")},
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
                    @ExampleObject(name = "REFUND PO", value = ""),
            })))
    public void accountantRefundPurchaseOrder(@PathVariable UUID purchaseOrderPublicId,
                                              @RequestParam(value = "rejectionCodePublicId") UUID rejectionCodePublicId,
                                              @RequestParam(name = "action") RefundAction action,
                                              @Parameter(hidden = true) Principal principal) {
        financialService.processPurchaseOrderRefund(purchaseOrderPublicId, action,rejectionCodePublicId,principal.getName());
    }
}
