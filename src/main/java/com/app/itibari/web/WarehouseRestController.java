package com.app.itibari.web;

import com.app.itibari.dataTypes.*;
import com.app.itibari.docs.Examples;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.*;
import com.app.itibari.models.response.ReconRouteSummary;
import com.app.itibari.repository.ProductStockRapperResponse;
import com.app.itibari.services.ProductService;
import com.app.itibari.services.WarehouseService;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.formatter.DateFormatter;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Slf4j
@Tag(name = "Warehouse", description = "Warehouse API")
@RestController
@RequestMapping("/warehouse")
public class WarehouseRestController {

    private final WarehouseService warehouseService;
    private final ProductService productService;

    public WarehouseRestController(WarehouseService warehouseService, ProductService productService) {

        this.warehouseService = warehouseService;
        this.productService = productService;
    }

    @PreAuthorize("hasAuthority('CREATE_WAREHOUSE')")
    @JsonView(BaseView.WarehouseDetailView.class)
    @PostMapping
    @Operation(summary = "Create a new warehouse", description = "Create a new warehouse",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "CREATE_WAREHOUSE", value = Examples.CREATE_WAREHOUSE),
    })))
    public Warehouse createWarehouse(@RequestBody @Valid WarehouseRequest warehouseRequest,
                                     @Parameter(hidden = true) Principal principal) {

        return warehouseService.createWarehouse(warehouseRequest, principal.getName());
    }

    //update

    @PreAuthorize("hasAuthority('CREATE_WAREHOUSE')")
    @PatchMapping("/{warehousePublicId}")
    @Operation(summary = "Update a warehouse", description = "Update a warehouse",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "UPDATE_WAREHOUSE", value = Examples.CREATE_WAREHOUSE),
    })))
    public void updateWarehouse(@PathVariable UUID warehousePublicId, @RequestBody @Valid WarehouseRequest warehouseRequest,
                                @Parameter(hidden = true) Principal principal) {

        warehouseService.updateWarehouse(warehousePublicId, warehouseRequest, principal.getName());
    }

    // get

    @PreAuthorize("hasAuthority('GET_WAREHOUSE')")
    @JsonView(BaseView.WarehouseDetailView.class)
    @GetMapping("/{warehousePublicId}")
    @Operation(summary = "Get a warehouse", description = "Get a warehouse",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Warehouse getWarehouse(@PathVariable UUID warehousePublicId) {

        return warehouseService.getWarehouse(warehousePublicId);
    }

    @PreAuthorize("hasAuthority('GET_WAREHOUSE')")
    @JsonView(BaseView.WarehouseDetailView.class)
    @GetMapping
    @Operation(summary = "Get all warehouses", description = "Get all warehouses",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<Warehouse> getAllWarehouses(@RequestParam(name = "searchParam", required = false) String searchParam,
                                            @RequestParam(name = "warehouseType", required = false) WarehouseType warehouseType,
                                            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return warehouseService.getAllWarehouses(searchParam, warehouseType, pageable);
    }

    @PreAuthorize("hasAuthority('DELETE_WAREHOUSE')")
    @DeleteMapping("/{warehousePublicId}")
    @Operation(summary = "Delete a warehouse", description = "Delete a warehouse",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void deleteWarehouse(@PathVariable UUID warehousePublicId,
                                @Parameter(hidden = true) Principal principal) {

        warehouseService.deleteWarehouse(warehousePublicId, principal.getName());
    }

    // get mobile warehouse per main

    @PreAuthorize("hasAuthority('GET_WAREHOUSE')")
    @JsonView(BaseView.WarehouseDetailView.class)
    @GetMapping("/{warehousePublicId}/childWarehouse")
    @Operation(summary = "Get mobile warehouse per main", description = "Get mobile warehouse per main",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Page<Warehouse> getMobileWarehousePerMain(@PathVariable UUID warehousePublicId,
                                                     @RequestParam(name = "type", required = false) WarehouseType type,
                                                     @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return warehouseService.getMobileWarehousePerMain(warehousePublicId, type, pageable);
    }

    // actions, remove or add mobile

    @PatchMapping("/{warehousePublicId}/mobileWarehouse")
    @Operation(summary = "Add or remove mobile warehouse", description = "Add or remove mobile warehouse",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "ADD_REMOVE_MOBILE_WAREHOUSE", value = Examples.ADD_REMOVE_MOBILE_WAREHOUSE),
    })))
    public void addRemoveMobileWarehouse(@PathVariable UUID warehousePublicId, @RequestBody @Valid WarehouseRequest warehouseRequest,
                                         @RequestParam(name = "action") ActionType action,
                                         @Parameter(hidden = true) Principal principal) {

        warehouseService.addRemoveMobileWarehouse(warehousePublicId, warehouseRequest, action, principal.getName());
    }

    @PreAuthorize("hasAuthority('LOAD_PRODUCTS')")
    @PostMapping("/agent/{agentPublicId}/loading")
    @Operation(summary = "Load warehouse products", description = "Load warehouse",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "Load Products from the warehouse request", value = Examples.LOAD_WAREHOUSE),
    })))
    public void loadWarehouseProducts(@PathVariable UUID agentPublicId,
                                      @RequestBody @Valid LoadingRequest loadingRequest,
                                      @Parameter(hidden = true) Principal principal) {

        warehouseService.loadWarehouseProducts(agentPublicId, loadingRequest, principal.getName());
    }

    @JsonView(BaseView.ProductReconView.class)
    @GetMapping("/agent/{agentPublicId}/items")
    @PreAuthorize("hasAuthority('LOAD_PRODUCTS')")
    @Operation(summary = "Get all product items from loaded route", description = "Get all product items from loaded route",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET PRODUCT LOADED ITEMS", value = Examples.GET_LOADED_ITEMS)})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<ProductItem> getAllProductItemsFromLoaded(@PathVariable(name = "agentPublicId") UUID agentPublicId,
                                                          @RequestParam(name = "deliveryId") String deliveryId,
                                                          @RequestParam(name = "searchParam", required = false) String searchParam,
                                                          @PageableDefault(size = 20, sort = "productName", direction = Sort.Direction.ASC) Pageable pageable) {

        if (deliveryId == null || deliveryId.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Delivery id is required")
                    .userMessage("Delivery id is required");
        }
        return warehouseService.getAllProductItemsFromLoaded(agentPublicId, searchParam, deliveryId, pageable);
    }

    // fetch product recons

    @JsonView(BaseView.ProductReconView.class)
    @GetMapping("/productRecons")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT_RECONCILIATIONS')")
    @Operation(summary = "Get all product recons", description = "Get all product recons",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET PRODUCT RECONCILIATIONS", value = Examples.GET_RECONS_PRODUCT_OK)})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<ProductRecon> getAllProductRecons(@RequestParam(name = "searchParam", required = false) String searchParam,
                                                  @RequestParam(name = "deliveryAgentPublicId", required = false) String deliveryAgentPublicId,
                                                  @RequestParam(name = "deliveryId", required = false) String deliveryId,
                                                  @RequestParam(name = "vehicleNumber", required = false) String vehicleNumber,
                                                  @RequestParam(name = "reconciled", required = false) Boolean reconciled,
                                                  @RequestParam(name = "isWalkIn", required = false) Boolean isWalkIn,
                                                  @RequestParam(name = "warehousePublicId", required = false) String warehousePublicId,
                                                  @RequestParam(name = "status", required = false) ReconStatus status,
                                                  @RequestParam(name = "createdBy", required = false) UUID createdBy,
                                                  @RequestParam(name = "from", required = false) String from,
                                                  @RequestParam(name = "to", required = false) String to,
                                                  @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return warehouseService.getAllProductRecons(searchParam,
                deliveryAgentPublicId,
                deliveryId,
                vehicleNumber,
                reconciled,
                warehousePublicId,
                from,
                to,
                status,
                createdBy,
                isWalkIn,
                pageable);
    }

    // fetch product items from recon

    @JsonView(BaseView.ProductReconView.class)
    @GetMapping("/productRecons/{publicId}/items")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT_RECONCILIATIONS')")
    @Operation(summary = "Get all product items from recon", description = "Get all product items from recon",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET PRODUCT RECONCILIATIONS ITEMS", value = Examples.GET_RECONS_ITEMS_OK)})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<ProductItem> getAllProductItemsFromRecon(@PathVariable(name = "publicId") UUID publicId,
                                                         @RequestParam(name = "searchParam", required = false) String searchParam,
                                                         @PageableDefault(size = 20, sort = "productName", direction = Sort.Direction.ASC) Pageable pageable) {

        return warehouseService.getAllProductItemsFromRecon(publicId, searchParam, pageable);
    }

    // actions, reconcile

    @PreAuthorize("hasAuthority('UPDATE_PRODUCT_RECONCILIATIONS')")
    @PatchMapping("/productRecons/{publicId}")
    @Operation(summary = "Reconcile product recon", description = "Reconcile product recon",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "RECONCILE_PRODUCT_RECON", value = Examples.RECONCILE_PRODUCT_RECON),
    })))
    public void reconcileProductRecon(@PathVariable(name = "publicId") UUID publicId,
                                      @RequestBody @Valid ProductReconAction productReconAction,
                                      @Parameter(hidden = true) Principal principal) {

        productReconAction.setProductReconPublicId(publicId);
        warehouseService.reconcileProductRecon(publicId, productReconAction, principal.getName());
    }

    @PreAuthorize("hasAuthority('UPDATE_PRODUCT_RECONCILIATIONS')")
    @PatchMapping("/productRecons/{publicId}/reconcile")
    @Operation(summary = "Reconcile product recon", description = "Reconcile product recon",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void reconcileProductReconAction(@PathVariable(name = "publicId") UUID publicId,
                                            @Parameter(hidden = true) Principal principal) {

        warehouseService.reconcileProductReconAction(publicId, principal.getName());
    }

    // create quarantine warehouse

    @PreAuthorize("hasAuthority('CREATE_WAREHOUSE')")
    @JsonView(BaseView.WarehouseDetailView.class)
    @PostMapping("/{warehousePublicId}/quarantine")
    @Operation(summary = "Create a quarantine warehouse", description = "Create a quarantine warehouse",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Warehouse createQuarantineWarehouse(@PathVariable UUID warehousePublicId,
                                               @Parameter(hidden = true) Principal principal) {

        return warehouseService.createQuarantineWarehouse(warehousePublicId, principal.getName());
    }

    // transfer products to quarantine warehouse
    @Deprecated
    @PreAuthorize("hasAuthority('CREATE_WAREHOUSE')")
    @PostMapping("/{warehousePublicId}/quarantine/transfer")
    @Operation(summary = "Transfer products to quarantine warehouse", description = "Transfer products to quarantine warehouse",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "TRANSFER_TO_QUARANTINE_WAREHOUSE", value = Examples.TRANSFER_TO_QUARANTINE_WAREHOUSE),
    })))
    public void transferProductsToQuarantineWarehouse(@PathVariable UUID warehousePublicId,
                                                      @RequestParam(name = "transferType") TransferType transferType,
                                                      @RequestBody @Valid TransferProductsToQuarantineWarehouseRequest transferProductsToQuarantineWarehouseRequest,
                                                      @Parameter(hidden = true) Principal principal) {

        warehouseService.transferProductsToQuarantineWarehouse(warehousePublicId,
                transferType,
                transferProductsToQuarantineWarehouseRequest,
                principal.getName());
    }

    // update product recon sold product.

    @Deprecated
    @PreAuthorize("hasAuthority('UPDATE_PRODUCT_RECONCILIATIONS')")
    @PatchMapping("/productRecons/{publicId}/updateProduct")
    @Operation(summary = "Update product recon sold product", description = "Update product recon sold product",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "UPDATE_PRODUCT_RECON_SOLD_PRODUCT", value = Examples.UPDATE_PRODUCT_RECON_SOLD_PRODUCT),
    })))
    public void updateProductReconSoldProduct(@PathVariable(name = "publicId") UUID publicId,
                                              @RequestBody @Valid UpdateProductReconSoldProductRequest updateProductReconSoldProductRequest,
                                              @Parameter(hidden = true) Principal principal) {

        warehouseService.updateProductReconSoldProduct(publicId, updateProductReconSoldProductRequest, principal.getName());
    }

    // stage returns

    @PatchMapping("/{productReconPublicId}/stageReturns")
    @Operation(summary = "Update product returns", description = "Update product returns",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "UPDATE_PRODUCT_RETURNS", value = Examples.UPDATE_PRODUCT_RETURNS),
    })))
    public void stageReturns(@PathVariable(name = "productReconPublicId") UUID productReconPublicId,
                             @RequestBody @Valid CreateStageRequest stageReturnsRequest,
                             @Parameter(hidden = true) Principal principal) {

        warehouseService.stageReturns(productReconPublicId, stageReturnsRequest, principal.getName());
    }

    // fetch

    @JsonView(BaseView.ReturnView.class)
    @GetMapping("/staged")
    @Operation(summary = "Get all staged", description = "Get all staged",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<ReturnStaging> getAllReturnStaging(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return warehouseService.getAllReturnStaging(searchParam, pageable);
    }

    @PatchMapping("/{returnStagePublicId}/approveReturn")
    @Operation(summary = "approve", description = "approve",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void approveStagedReturn(@PathVariable(name = "returnStagePublicId") UUID returnStagePublicId,
                                    @Parameter(hidden = true) Principal principal) {
        warehouseService.approveStagedReturn(returnStagePublicId, principal.getName());
    }

    @JsonView(BaseView.ReturnView.class)
    @GetMapping("/stagedForApproval")
    @Operation(summary = "Get all staged", description = "Get all staged",
            responses = {@ApiResponse(responseCode = "200", description = "OK", content = @Content)})
    @PageableAsQueryParam
    public Page<ReconciliationStaging> getReconciliationStagingItems(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") @Parameter(example = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") @Parameter(example = "yyyy-MM-dd") LocalDate endDate,
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return warehouseService.getReconciliationStagingItems(searchParam, startDate, endDate, pageable);
    }

    @JsonView(BaseView.ReturnView.class)
    @GetMapping("/stagedForApproval/{publicId}/items")
    @Operation(summary = "Get all staged items", description = "Get all staged items",
            responses = {@ApiResponse(responseCode = "200", description = "OK", content = @Content)})
    @PageableAsQueryParam
    public Page<ReconciliationStagingItem> getReconciliationStagingItem(
            @PathVariable(name = "publicId") UUID publicId,
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return warehouseService.getStagingItems(publicId, pageable);
    }

    @PreAuthorize("hasAuthority('UPDATE_PRODUCT_RECONCILIATIONS')")
    @PatchMapping("/stagedForApproval/{publicId}")
    @Operation(summary = "Reconcile product recon", description = "Reconcile product recon",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "APPROVE STAGING", value = Examples.APPROVE_STAGING),
            @ExampleObject(name = "APPROVE STAGING", value = Examples.REJECT_STAGING),
            @ExampleObject(name = "Edit Item Quantity", value = Examples.EDIT_ITEM_QUANTITY),
    })))
    public void stagingActions(@PathVariable UUID publicId,
                               @RequestParam(name = "itemPublicId", required = false) UUID itemPublicId,
                               @RequestBody @Valid StagingActionRequest stagingActionRequest,
                               @Parameter(hidden = true) Principal principal) {

        warehouseService.stagingActions(publicId, stagingActionRequest, principal.getName(), itemPublicId);
    }

    // TO BE REMOVED AFTER TESTS
    @Deprecated
    @PatchMapping("/runStaticStock")
    @Operation(summary = "Run Static Stock", description = "Run Static Stock",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "RECONCILE_PRODUCT_RECON", value = Examples.RECONCILE_PRODUCT_RECON),})))
    public void runStaticStock(@RequestParam(value = "runDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") @Parameter(example = "yyyy-MM-dd") LocalDate runDate,
                               @Parameter(hidden = true) Principal principal) {

        productService.updateProductStockManualRun(runDate);
    }

    // get stock movements

    @JsonView(BaseView.StockMovementView.class)
    @GetMapping("/stockMovements")
    @Operation(summary = "Get all stock movements", description = "Get all stock movements",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<StockMovementItem> getAllStockMovements(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(name = "warehousePublicId", required = false) UUID warehousePublicId,
            @RequestParam(name = "transactionType", required = false) StockMovementTransactionType transactionType,
            @RequestParam(name = "transactionTypes", required = false) List<StockMovementTransactionType> transactionTypes,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {
        LocalDate startDate = DateFormatter.parseDate(from);
        LocalDate endDate = DateFormatter.parseDate(to);

        return warehouseService.getAllStockMovements(searchParam, warehousePublicId, transactionType, transactionTypes, startDate, endDate, pageable);
    }

    @JsonView(BaseView.ProductStockView.class)
    @GetMapping("/productStock")
    @Operation(summary = "Get all product stock", description = "Get all product stock",
            responses = {@ApiResponse(responseCode = "200", description = "OK", content = @Content),})
    @PageableAsQueryParam
    public Page<ProductStock> getAllProductStocks(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(name = "warehousePublicId", required = false) UUID warehousePublicId,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "activeType", required = false) ActiveType activeType,
            @PageableDefault(size = 20, sort = "productName", direction = Sort.Direction.ASC) Pageable pageable) {
        LocalDate startDate = DateFormatter.parseDate(from);
        LocalDate endDate = DateFormatter.parseDate(to);

        return warehouseService.getAllProductStocks(searchParam, warehousePublicId, startDate, endDate, activeType, pageable);
    }

    @GetMapping("/productStockRapper")
    @Operation(summary = "Get all product stock rapper", description = "Get all product stock rapper",
            responses = {@ApiResponse(responseCode = "200", description = "OK", content = @Content),})
    @PageableAsQueryParam
    public Page<ProductStockRapperResponse> getProductStockRapper(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(name = "warehousePublicId", required = false) UUID warehousePublicId,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @PageableDefault(size = 20) Pageable pageable) {
        LocalDate startDate = DateFormatter.parseDate(from);
        LocalDate endDate = DateFormatter.parseDate(to);

        return warehouseService.getProductStockRapper(searchParam, warehousePublicId, startDate, endDate, pageable);
    }

    @GetMapping("/downloadTemplate")
    public void getProductImportTemplate(@RequestParam(name = "warehousePublicId", required = false) UUID warehousePublicId,
                                         @Parameter(hidden = true) Principal principal,
                                         HttpServletResponse response) throws IOException {
        String prefix = "PRODUCT_STOCK_IMPORT_TEMPLATE_";
        warehouseService.downloadProductStockTemplate(warehousePublicId, response, prefix, principal);
    }

    @PostMapping(value = "/productStockBulkUpload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Bulk Product Upload", description = "Bulk Product Upload",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
            })
    public void importProductStock(@RequestParam("file") MultipartFile file,
                                   @Parameter(hidden = true) Principal loggedInUserDetails) throws Exception {

        if (file.isEmpty()) {
            throw new Exception("You must upload a file");
        }

        if (!StringUtils.isEmpty(file.getOriginalFilename())) {
            warehouseService.uploadProductStockExcel(file.getInputStream(), loggedInUserDetails);
        } else {
            throw new BadRequestRestApiException()
                    .developerMessage("You must upload a file")
                    .userMessage("Sorry. File missing");
        }
    }

    @PatchMapping("/runProductStock")
    public void runStaticStock(@Parameter(hidden = true) Principal principal) {

        productService.runProductStock(principal);
    }

    // First One
    @PatchMapping("/runNextDayProductStock")
    public void runNextDayProductStock(@Parameter(hidden = true) Principal principal) {

        productService.runProductStockByNextDay(principal);
    }

    @PreAuthorize("hasAuthority('UPDATE_PRODUCT_RECONCILIATIONS')")
    @PatchMapping("/productRecons/{publicId}/revert")
    @Operation(summary = "Revert reconciled product recon", description = "Revert Reconciled product recon",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void revertReconciledProductRecon(@PathVariable(name = "publicId") UUID publicId,
                                             @RequestParam(name = "reasonPublicId") UUID reasonPublicId,
                                             @Parameter(hidden = true) Principal principal) {

        warehouseService.revertReconcileProductRecon(publicId, reasonPublicId, principal.getName());
    }

    @GetMapping("/productReconSummary")
    @Operation(summary = "Get route summary", description = "Get route summary",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "Get Route Summary", value = Examples.ROUTE_SUMMARY_RESPONSE)})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),})
    public ReconRouteSummary getAllProductReconSummary(@RequestParam(name = "routeId") String routeId) {

        return warehouseService.getAllProductReconSummary(routeId);
    }

    // First second
    @PatchMapping("/runManuallyStaticStock")
    public void runManuallyStaticStock(@Parameter(hidden = true) Principal principal) {

        productService.updateProductStock(LocalDate.now(ZoneId.of("Africa/Nairobi")));
    }
}
