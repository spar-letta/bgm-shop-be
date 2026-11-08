package com.app.itibari.web;

import com.app.itibari.dataTypes.DiscountApprovalStatus;
import com.app.itibari.dataTypes.DiscountStatus;
import com.app.itibari.dataTypes.DiscountType;
import com.app.itibari.docs.Examples;
import com.app.itibari.models.entities.DiscountApproval;
import com.app.itibari.models.entities.Discounts;
import com.app.itibari.models.entities.VolumeRange;
import com.app.itibari.models.request.ApprovalRequest;
import com.app.itibari.models.request.DiscountCreateRequest;
import com.app.itibari.models.request.VolumeRangeCreateRequest;
import com.app.itibari.services.DiscountService;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Tag(name = "Discounts", description = "Discounts API")
@RestController
@RequestMapping("/discounts")
public class DiscountRestController {

    private final DiscountService discountService;

    public DiscountRestController(DiscountService discountService) {

        this.discountService = discountService;
    }

    @PostMapping
    @Operation(summary = "Create discounts", responses = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Validation Error", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.DISCOUNT_VALIDATION_ERROR))),
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "Special Discount", value = Examples.SPECIAL_DISCOUNT_REQUEST_EXAMPLE),
            @ExampleObject(name = "Volume Discount", value = Examples.VOLUME_DISCOUNT_REQUEST_EXAMPLE),
            @ExampleObject(name = "Schedule Discount", value = Examples.SCHEDULE_DISCOUNT_REQUEST_EXAMPLE),
            @ExampleObject(name = "Invoice Discount", value = Examples.INVOICE_DISCOUNT_REQUEST_EXAMPLE),
    })))
    public void createDiscount(@RequestBody @Valid DiscountCreateRequest discountCreateRequest,
                               @Parameter(hidden = true) Principal principal) {

        discountService.createUpdateDiscount(discountCreateRequest, principal.getName());
    }

    // disa
    @PatchMapping("/{publicId}")
    // @PreAuthorize("hasAuthority('CREATE_DISCOUNT')")
    @Operation(summary = "Create discounts", responses = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Validation Error", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.DISCOUNT_VALIDATION_ERROR))),
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "Special Discount", value = Examples.SPECIAL_DISCOUNT_REQUEST_EXAMPLE),
            @ExampleObject(name = "Volume Discount", value = Examples.VOLUME_DISCOUNT_REQUEST_EXAMPLE),
            @ExampleObject(name = "Schedule Discount", value = Examples.SCHEDULE_DISCOUNT_REQUEST_EXAMPLE),
            @ExampleObject(name = "Invoice Discount", value = Examples.INVOICE_DISCOUNT_REQUEST_EXAMPLE),
    })))
    public void updateDiscount(@PathVariable(name = "publicId") UUID publicId,
                               @RequestBody @Valid DiscountCreateRequest discountCreateRequest,
                               @Parameter(hidden = true) Principal principal) {

        discountService.update(publicId, discountCreateRequest, principal.getName());
    }

    @PatchMapping("/{publicId}/suspend")
    // @PreAuthorize("hasAuthority('SUSPEND_DISCOUNT')")
    @Operation(summary = "Suspend discounts", responses = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Validation Error", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.DISCOUNT_VALIDATION_ERROR))),
    })
    public void suspendDiscount(@PathVariable(name = "publicId") UUID publicId,
                                @Parameter(hidden = true) Principal principal) {

        discountService.suspend(publicId, principal.getName());
    }

    @PatchMapping("/{publicId}/approve")
    @Operation(summary = "Approve discounts", responses = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Validation Error", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.DISCOUNT_VALIDATION_ERROR))),
    })
    public void approveDiscount(@PathVariable(name = "publicId") UUID publicId,
                                @Parameter(hidden = true) Principal principal) {

        discountService.approve(publicId, principal.getName());
    }

    @JsonView(BaseView.DiscountView.class)
    @GetMapping
    @Operation(summary = "Get all discounts", description = "Fetch all discounts with pagination",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "FETCH_DISCOUNTS", value = Examples.FETCH_DISCOUNTS)})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<Discounts> getAllDiscounts(@RequestParam(name = "discountType") DiscountType discountType,
                                           @RequestParam(name = "searchParam", required = false) String searchParam,
                                           @RequestParam(name = "status", required = false) DiscountStatus status,
                                           @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                           @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                           @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return discountService.getAllDiscounts(discountType, searchParam, startDate, endDate, status, pageable);
    }

    // get all discountApprovals

    @JsonView(BaseView.DiscountApprovalView.class)
    @GetMapping("/discountApprovals")
    @Operation(summary = "Get all discount approvals", description = "Fetch all discount approvals with pagination",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "FETCH_DISCOUNT_APPROVALS", value = Examples.FETCH_DISCOUNT_APPROVALS)})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<DiscountApproval> getAllDiscountApprovals(@RequestParam(name = "searchParam", required = false) String searchParam,
                                                          @RequestParam(name = "salesAgentPublicId", required = false) UUID salesAgentPublicId,
                                                          @RequestParam(name = "customerPublicId", required = false) UUID customerPublicId,
                                                          @RequestParam(name = "status", required = false) DiscountApprovalStatus status,
                                                          @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                          @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                          @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return discountService.getAllDiscountApprovals(searchParam, salesAgentPublicId, customerPublicId, status, startDate, endDate, pageable);
    }

    @DeleteMapping("/{publicId}")
    //@PreAuthorize("hasAuthority('DELETE_DISCOUNT')")
    @Operation(summary = "Delete discounts", responses = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Validation Error", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.DISCOUNT_VALIDATION_ERROR))),
    })
    public void deleteDiscount(@PathVariable(name = "publicId") UUID publicId,
                               @Parameter(hidden = true) Principal principal) {

        discountService.delete(publicId, principal.getName());
    }

    // approve special discount request

    @PostMapping("/{publicId}/firstApproval")
    //  @PreAuthorize("hasAuthority('APPROVE_SPECIAL_DISCOUNT')")
    @Operation(summary = "First approval for special discount", responses = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Validation Error", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.DISCOUNT_VALIDATION_ERROR))),
    })
    public void firstApproval(@PathVariable(name = "publicId") UUID publicId,
                              @RequestParam(name = "comment") String comment,
                              @RequestParam(name = "approve") boolean approve,
                              @Parameter(hidden = true) Principal principal) {

        discountService.firstApproval(publicId, comment, approve, principal.getName());
    }

    // final approval for special discount
    @PostMapping("/{publicId}/finalApproval")
    // @PreAuthorize("hasAuthority('FINAL_APPROVE_SPECIAL_DISCOUNT')")
    @Operation(summary = "Final approval for special discount", responses = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Validation Error", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.DISCOUNT_VALIDATION_ERROR))),
    },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
                    @ExampleObject(name = "Final Approval Request", value = Examples.SPECIAL_DISCOUNT_FINAL_APPROVAL_REQUEST_EXAMPLE),
            })))
    public void finalApproval(@PathVariable(name = "publicId") UUID publicId,
                              @RequestBody ApprovalRequest request,
                              @Parameter(hidden = true) Principal principal) {

        discountService.finalApproval(publicId, request, principal.getName());
    }

    // add volume range

    @PatchMapping("/{discountPublicId}/addVolumeRange")
    @Operation(summary = "Add volume range to volume discount", responses = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Validation Error", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.DISCOUNT_VALIDATION_ERROR))),
    },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
                    @ExampleObject(name = "Volume Range Create Request", value = Examples.VOLUME_RANGE_CREATE_REQUEST_EXAMPLE),
            })))
    public void addVolumeRange(@PathVariable(name = "discountPublicId") UUID discountPublicId,
                               @RequestBody @Valid VolumeRangeCreateRequest volumeRangeCreateRequest,
                               @Parameter(hidden = true) Principal principal) {

        discountService.addVolumeRange(discountPublicId, volumeRangeCreateRequest, principal.getName());
    }

    @JsonView(BaseView.DiscountApprovalView.class)
    @GetMapping("/{discountPublicId}/volumeRanges")
    @Operation(summary = "Get volume ranges for a volume discount", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "FETCH_VOLUME_RANGES", value = "")})),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
    })
    public List<VolumeRange> getVolumeRanges(@PathVariable(name = "discountPublicId") UUID discountPublicId) {

        return discountService.getDiscountVolumeRanges(discountPublicId);
    }

    // delete volume range

    @DeleteMapping("/{discountPublicId}/volumeRange/{publicId}")
    @Operation(summary = "Delete volume range from a volume discount", responses = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Validation Error", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.DISCOUNT_VALIDATION_ERROR))),
    })
    public void deleteVolumeRange(@PathVariable(name = "discountPublicId") UUID discountPublicId,
                                  @PathVariable(name = "publicId") UUID publicId,
                                  @Parameter(hidden = true) Principal principal) {

        discountService.deleteVolumeRange(discountPublicId, publicId, principal.getName());
    }
}
