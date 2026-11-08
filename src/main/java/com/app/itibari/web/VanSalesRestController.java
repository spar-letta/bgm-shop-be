package com.app.itibari.web;

import com.app.itibari.dataTypes.ActionType;
import com.app.itibari.dataTypes.VanSaleStatus;
import com.app.itibari.docs.Examples;
import com.app.itibari.models.entities.ProductItem;
import com.app.itibari.models.entities.VanSales;
import com.app.itibari.models.request.UpdateVanSales;
import com.app.itibari.models.request.VanSaleRequest;
import com.app.itibari.services.VanSalesService;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@Tag(name = "VanSales", description = "VanSales API")
@RestController
@RequestMapping("/vanSales")
public class VanSalesRestController {

    private final VanSalesService vanSalesService;

    public VanSalesRestController(VanSalesService vanSalesService) {

        this.vanSalesService = vanSalesService;
    }

    @PreAuthorize("hasAuthority('CREATE_VANSALE')")
    @JsonView(BaseView.VanSalesView.class)
    @PostMapping
    @Operation(summary = "Create a new van sales", description = "Create a new van sales",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET VAN SALES", value = Examples.GET_VAN_SALES)})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "CREATE_VAN_SALE", value = Examples.CREATE_VANSALE),
    })))
    public VanSales createVanSale(@RequestBody @Valid VanSaleRequest vanSaleRequest,
                                  @Parameter(hidden = true) Principal principal) {

        return vanSalesService.createVanSale(vanSaleRequest, principal.getName());
    }
    // update

    @PreAuthorize("hasAuthority('UPDATE_VANSALE')")
    @PatchMapping("/{publicId}")
    @Operation(summary = "Update a van sales", description = "Update a van sales",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "ADD PRODUCT ACTION", value = Examples.ADD_PRODUCT_ACTION),
            @ExampleObject(name = "REMOVE PRODUCT ACTION", value = Examples.REMOVE_PRODUCT_ACTION),
    })))
    public void updateVanSale(@PathVariable(name = "publicId") UUID publicId,
                              @RequestBody @Valid UpdateVanSales vanSaleRequest,
                              @RequestParam(name = "action") ActionType actionType,
                              @Parameter(hidden = true) Principal principal) {

        vanSalesService.updateVanSale(publicId, actionType, vanSaleRequest, principal.getName());
    }

    @JsonView(BaseView.VanSalesView.class)
    @PreAuthorize("hasAuthority('VIEW_VANSALE')")
    @GetMapping("/{publicId}")
    @Operation(summary = "Get a van sales", description = "Get a van sales",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET VAN SALES", value = Examples.GET_VAN_SALES)})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public VanSales getVanSale(@PathVariable(name = "publicId") UUID publicId,
                               @Parameter(hidden = true) Principal principal) {

        return vanSalesService.getVanSale(publicId, principal.getName());
    }

    @JsonView(BaseView.VanSalesView.class)
    @PreAuthorize("hasAuthority('VIEW_VANSALE')")
    @GetMapping
    @Operation(summary = "Get all van sales", description = "Get all van sales",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET VAN SALES", value = Examples.GET_ALL_VAN_SALES)})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<VanSales> getVanSales(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(name = "deliveryAgentPublicId", required = false) UUID deliveryAgentPublicId,
            @RequestParam(name = "status", required = false) VanSaleStatus status,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return vanSalesService.getVanSales(searchParam, deliveryAgentPublicId, status, from, to, pageable);
    }

    @PreAuthorize("hasAuthority('DELETE_VANSALE')")
    @DeleteMapping("/{publicId}")
    @Operation(summary = "Delete a van sales", description = "Delete a van sales",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void deleteVanSale(@PathVariable(name = "publicId") UUID publicId,
                              @Parameter(hidden = true) Principal principal) {

        vanSalesService.deleteVanSale(publicId, principal.getName());
    }

    // get van sales productItems

    @JsonView(BaseView.VanSalesView.class)
    @PreAuthorize("hasAuthority('VIEW_VANSALE')")
    @GetMapping("/{publicId}/productItems")
    @Operation(summary = "Get all product items for a van sales", description = "Get all product items for a van sales",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET VAN SALE PRODUCT ITEMS", value = Examples.GET_VAN_SALES_PRODUCT_ITEMS)})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Page<ProductItem> getVanSalesProductItems(@PathVariable(name = "publicId") UUID publicId,
                                                     @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return vanSalesService.getVanSalesProductItems(publicId, pageable);
    }

    // get route van sales products

    @PreAuthorize("hasAnyAuthority('VIEW_VANSALE')")
    @JsonView(BaseView.VanSalesView.class)
    @GetMapping("/routePlanning/{publicId}/vanSalesProducts")
    @Operation(summary = "Get all van sales products in a route planning", description = "Get all van sales products in a route planning",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET_ROUTE_PLANNING_VANS_SALES_PRODUCTS", value = Examples.GET_VAN_SALES_PRODUCT_ITEMS)})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Page<ProductItem> getRoutePlanningVanSalesProducts(@PathVariable(name = "publicId") UUID publicId,
                                                              @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return vanSalesService.getRoutePlanningVanSalesProducts(publicId, pageable);
    }
}
