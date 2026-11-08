package com.app.itibari.web;

import com.app.itibari.dataTypes.ProductDisposalType;
import com.app.itibari.dataTypes.QuarantineAction;
import com.app.itibari.dataTypes.QuarantineStatus;
import com.app.itibari.dataTypes.TransferType;
import com.app.itibari.docs.Examples;
import com.app.itibari.models.entities.Quarantine;
import com.app.itibari.models.entities.QuarantineProducts;
import com.app.itibari.models.request.DisposalRequest;
import com.app.itibari.models.request.QuarantineRequest;
import com.app.itibari.models.response.DisposedQuarantineProducts;
import com.app.itibari.services.QuarantineService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.UUID;

@Tag(name = "quarantine", description = "Quarantine API")
@RestController
@RequestMapping("/quarantine")
public class QuarantineRestController {

    private final QuarantineService quarantineService;

    public QuarantineRestController(QuarantineService quarantineService) {

        this.quarantineService = quarantineService;
    }

    @PreAuthorize("hasAuthority('CREATE_QUARANTINE')")
    @PostMapping
    @JsonView(BaseView.QuarantineView.class)
    @Operation(summary = "Create a new quarantine", description = "Create a new quarantine",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
                    @ExampleObject(name = "CREATE_QUARANTINE", value = Examples.CREATE_QUARANTINE_EXAMPLE)
            })))
    public Quarantine createQuarantine(@RequestBody @Valid QuarantineRequest quarantineRequest,
                                       @Parameter(hidden = true) Principal principal) {

        return quarantineService.createQuarantine(quarantineRequest, principal.getName());
    }

    // actions for add/remove products to/from quarantine will be added here in the future

    @PreAuthorize("hasAuthority('UPDATE_QUARANTINE')")
    @PostMapping("/{publicId}/addQuarantineProducts")
    @Operation(summary = "Update an existing quarantine", description = "Update an existing quarantine",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
                    @ExampleObject(name = "ADD_QUARANTINE_PRODUCTS", value = Examples.UPDATE_QUARANTINE_EXAMPLE)
            })))
    public void updateQuarantine(@PathVariable("publicId") UUID publicId,
                                 @RequestBody @Valid QuarantineRequest quarantineRequest,
                                 @Parameter(hidden = true) Principal principal) {

        quarantineService.addQuarantineProducts(publicId, quarantineRequest, principal.getName());
    }

    @PreAuthorize("hasAuthority('UPDATE_QUARANTINE')")
    @PatchMapping("/{publicId}/removeQuarantineProducts/{quarantineProductId}")
    @Operation(summary = "Remove products from an existing quarantine", description = "Remove products from an existing quarantine",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
                    @ExampleObject(name = "REMOVE_QUARANTINE_PRODUCTS", value = Examples.REMOVE_QUARANTINE_PRODUCTS_EXAMPLE)
            })))
    public void removeQuarantineProducts(@PathVariable("publicId") UUID publicId,
                                         @PathVariable("quarantineProductId") UUID quarantineProductId,
                                         @Parameter(hidden = true) Principal principal) {

        quarantineService.removeQuarantineProducts(publicId, quarantineProductId, principal.getName());
    }

    //get
    @PreAuthorize("hasAuthority('GET_QUARANTINE')")
    @GetMapping("/{publicId}")
    @JsonView(BaseView.QuarantineView.class)
    @Operation(summary = "Fetch quarantine by public ID", description = "Fetch quarantine by public ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Quarantine getQuarantineByPublicId(@PathVariable("publicId") UUID publicId) {

        return quarantineService.getQuarantineByPublicId(publicId);
    }

    @PreAuthorize("hasAuthority('GET_QUARANTINE')")
    @GetMapping
    @JsonView(BaseView.QuarantineView.class)
    @Operation(summary = "Fetch all quarantines", description = "Fetch all quarantines",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<Quarantine> getAllQuarantines(@RequestParam(name = "searchParam", required = false) String searchParam,
                                              @RequestParam(name = "mainWarehousePublicId", required = false) UUID mainWarehousePublicId,
                                              @RequestParam(name = "quarantineWarehousePublicId", required = false) UUID quarantineWarehousePublicId,
                                              @RequestParam(name = "status", required = false) QuarantineStatus status,
                                              @RequestParam(name = "transferType", required = false) TransferType transferType,
                                              @RequestParam(name = "registeredBy", required = false) UUID registeredBy,
                                              @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                              @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                              @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return quarantineService.getAllQuarantines(searchParam,
                mainWarehousePublicId,
                quarantineWarehousePublicId,
                status,
                transferType,
                startDate,
                endDate,
                pageable);
    }

    @JsonView(BaseView.QuarantineView.class)
    @PreAuthorize("hasAuthority('GET_QUARANTINE')")
    @GetMapping("/{publicId}/quarantineProducts")
    @Operation(summary = "Fetch quarantine products by quarantine public ID", description = "Fetch quarantine products by quarantine public ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<QuarantineProducts> getQuarantineProducts(@PathVariable("publicId") UUID publicId,
                                                          @RequestParam(name = "searchParam", required = false) String searchParam,
                                                          @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return quarantineService.getQuarantineProducts(publicId, searchParam, pageable);
    }

    // delete

    @PreAuthorize("hasAuthority('DELETE_QUARANTINE')")
    @DeleteMapping("/{publicId}")
    @Operation(summary = "Delete a quarantine by public ID", description = "Delete a quarantine by public ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void deleteQuarantine(@PathVariable("publicId") UUID publicId,
                                 @Parameter(hidden = true) Principal principal) {

        quarantineService.deleteQuarantine(publicId, principal.getName());
    }

    // actions for approve/reject

    @PreAuthorize("hasAuthority('APPROVE_QUARANTINE')")
    @PatchMapping("/{publicId}/actions")
    @Operation(summary = "Approve or reject a quarantine", description = "Approve or reject a quarantine",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void approveOrRejectQuarantine(@PathVariable("publicId") UUID publicId,
                                          @RequestParam(name = "action") QuarantineAction action,
                                          @Parameter(hidden = true) Principal principal) {

        quarantineService.approveOrRejectQuarantine(publicId, action, principal.getName());
    }

    // return product to main warehouse

    //@PreAuthorize("hasAuthority('RETURN_QUARANTINE_PRODUCT')")
    @PostMapping("/{publicId}/returnProduct/{quarantineProductId}")
    @Operation(summary = "Return a product from quarantine to main warehouse", description = "Return a product from quarantine to main warehouse",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void returnProductToMainWarehouse(@PathVariable("publicId") UUID publicId,
                                             @PathVariable("quarantineProductId") UUID quarantineProductId,
                                             @RequestParam(name = "quantity") int quantity,
                                             @Parameter(hidden = true) Principal principal) {

        quarantineService.returnProductToMainWarehouse(publicId, quarantineProductId, quantity, principal.getName());
    }

    @PreAuthorize("hasAuthority('UPDATE_QUARANTINE')")
    @PatchMapping("/{publicId}/disposeQuarantineProduct/{quarantineProductId}")
    @Operation(summary = "Remove products from an existing quarantine", description = "Remove products from an existing quarantine",
            responses = {@ApiResponse(responseCode = "200", description = "OK")},
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
                    @ExampleObject(name = "REMOVE_QUARANTINE_PRODUCTS", value = Examples.QUARANTINE_DISPOSAL_REQUEST)})))
    public void disposeQuarantineProduct(@PathVariable UUID publicId,
                                         @PathVariable UUID quarantineProductId,
                                         @RequestParam(name = "reasonCodePublicId", required = false) UUID reasonCodePublicId,
                                         @RequestParam(name = "disposalType") ProductDisposalType disposalType,
                                         @RequestBody(required = false) DisposalRequest disposalRequest,
                                         @Parameter(hidden = true) Principal principal) {

        quarantineService.disposeQuarantineProduct(publicId, quarantineProductId, reasonCodePublicId, disposalType, disposalRequest, principal.getName());
    }

    @JsonView(BaseView.QuarantineView.class)
    @PreAuthorize("hasAuthority('GET_QUARANTINE')")
    @GetMapping("/quarantineProducts")
    @Operation(summary = "Fetch pending disposed quarantine products by quarantine public ID", description = "Fetch pending disposed quarantine products by quarantine public ID",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    @PageableAsQueryParam
    public Page<QuarantineProducts> getPendingDisposedQuarantineProducts(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return quarantineService.getPendingDisposedQuarantineProducts(searchParam, pageable);
    }

    @PreAuthorize("hasAuthority('GET_QUARANTINE')")
    @GetMapping("/quarantineApprovedProducts")
    @Operation(summary = "Fetch approved disposed quarantine products by quarantine public ID", description = "Fetch approved disposed quarantine products by quarantine public ID",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    @PageableAsQueryParam
    public Page<DisposedQuarantineProducts> getApprovedDisposedQuarantineProducts(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return quarantineService.getApprovedDisposedQuarantineProducts(searchParam, startDate, endDate, pageable);
    }
}
