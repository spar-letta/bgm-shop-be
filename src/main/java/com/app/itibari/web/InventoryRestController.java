package com.app.itibari.web;

import com.app.itibari.docs.Examples;
import com.app.itibari.models.entities.Inventory;
import com.app.itibari.models.entities.InventoryProducts;
import com.app.itibari.models.request.ClosingUpdateRequest;
import com.app.itibari.services.InventoryService;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
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

@Tag(name = "Inventory", description = "Inventory API")
@RestController
@RequestMapping("/inventory")
public class InventoryRestController {

    private final InventoryService inventoryService;

    public InventoryRestController(InventoryService inventoryService) {

        this.inventoryService = inventoryService;
    }

    // get inventory per warehouse

    @PreAuthorize("hasAuthority('VIEW_INVENTORY')")
    @JsonView(BaseView.InventoryView.class)
    @GetMapping
    @Operation(summary = "Get Inventory per Warehouse", description = "Get Inventory per Warehouse",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Inventory getInventoryPerWarehouse(
            @RequestParam(name = "warehousePublicId", required = false) UUID warehousePublicId) {

        return inventoryService.getInventoryPerWarehouse(warehousePublicId);
    }

    // get inventory products

    @PreAuthorize("hasAuthority('VIEW_INVENTORY')")
    @JsonView(BaseView.InventorySummaryView.class)
    @GetMapping("/{inventoryPublicId}/products")
    @Operation(summary = "Get Inventory Products", description = "Get Inventory Products",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET_INVENTORY_PRODUCTS", value = Examples.GET_INVENTORY_PRODUCTS)})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Page<InventoryProducts> getInventoryProducts(@PathVariable(name = "inventoryPublicId") UUID inventoryPublicId,
                                                        @RequestParam(name = "productPublicId", required = false) UUID productPublicId,
                                                        @RequestParam(name = "searchParam", required = false) String searchParam,
                                                        @PageableDefault(size = 20, sort = "product.productName", direction = Sort.Direction.ASC) Pageable pageable) {

        return inventoryService.getInventoryProducts(inventoryPublicId, productPublicId, searchParam, pageable);
    }

    // get all inventories

    @PreAuthorize("hasAuthority('VIEW_INVENTORY')")
    @JsonView(BaseView.InventoryView.class)
    @GetMapping("/all")
    @Operation(summary = "Get All Inventories", description = "Get All Inventories",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET_ALL_INVENTORIES", value = Examples.GET_ALL_INVENTORIES)})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Page<Inventory> getAllInventories(@RequestParam(name = "searchParam", required = false) String searchParam,
                                             @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return inventoryService.getAllInventories(searchParam, pageable);
    }

    // fetch inventory products by warehouse public id

    @PreAuthorize("hasAuthority('VIEW_INVENTORY')")
    @JsonView(BaseView.InventorySummaryView.class)
    @GetMapping("/warehouse/{warehousePublicId}/products")
    @Operation(summary = "Get Inventory Products by Warehouse", description = "Get Inventory Products by Warehouse",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET_INVENTORY_PRODUCTS_BY_WAREHOUSE", value = "")})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Page<InventoryProducts> getInventoryProductsByWarehouse(
            @PathVariable UUID warehousePublicId,
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @PageableDefault(size = 20, sort = "product.productName", direction = Sort.Direction.ASC) Pageable pageable) {

        return inventoryService.getInventoryProductsByWarehouse(warehousePublicId, searchParam, pageable);
    }

    // update closing stock
    @PreAuthorize("hasAuthority('VIEW_INVENTORY')")
    @PatchMapping("/updateClosingStock")
    @Operation(summary = "Update Closing Stock", description = "Update Closing Stock",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void updateClosingStock(@RequestBody @Valid ClosingUpdateRequest request,
                                   @Parameter(hidden = true) Principal principal) {

        inventoryService.updateClosingStock(request, principal.getName());
    }

    // fetch inventory

    @PreAuthorize("hasAuthority('VIEW_INVENTORY')")
    @GetMapping("/{inventoryPublicId}/v2")
    @Operation(summary = "Get Inventory", description = "Get Inventory",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET_INVENTORY", value = "")})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Inventory getInventory(@PathVariable(name = "inventoryPublicId") UUID inventoryPublicId) {
        return inventoryService.getInventory(inventoryPublicId);
    }
}
