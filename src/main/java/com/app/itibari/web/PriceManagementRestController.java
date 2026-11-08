package com.app.itibari.web;

import com.app.itibari.dataTypes.TransactionType;
import com.app.itibari.docs.Examples;
import com.app.itibari.models.entities.AvcoItems;
import com.app.itibari.models.request.PriceManagementRequest;
import com.app.itibari.services.PriceManagementService;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
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
import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;
import java.util.UUID;

@Tag(name = "Price Management")
@RestController
@RequestMapping("/priceManagement")
public class PriceManagementRestController {

    private final PriceManagementService priceManagementService;

    public PriceManagementRestController(PriceManagementService priceManagementService) {

        this.priceManagementService = priceManagementService;
    }

    @PreAuthorize("hasAuthority('CREATE_PRICE_MANAGEMENT')")
    @JsonView(BaseView.PriceManagementSummaryView.class)
    @PostMapping
    @Operation(summary = "createPriceManagement", description = "Create Price Management",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "CREATE_PRICE_MANAGEMENT", value = Examples.CREATE_PRICE_MANAGEMENT),
    })))
    public void createPriceManagement(@RequestBody @Valid PriceManagementRequest priceManagementRequest,
                                      @Parameter(hidden = true) Principal principal) {

        priceManagementService.createPriceManagement(priceManagementRequest, principal.getName());
    }

    @PreAuthorize("hasAuthority('VIEW_PRICE_MANAGEMENT')")
    @GetMapping("/template")
    public void getPriceManagementTemplate(HttpServletResponse response,
                                           @Parameter(hidden = true) Principal principal) {

        priceManagementService.getPriceManagementTemplate(response, principal.getName());
    }

    //upload

    @PreAuthorize("hasAuthority('UPLOAD_PRICE_MANAGEMENT')")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload Price Management", description = "Upload Price Management",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void uploadPriceManagement(@RequestParam("file") MultipartFile file,
                                      @Parameter(hidden = true) Principal principal) throws IOException {

        if (file.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("File is empty")
                    .userMessage("Please upload a file");
        }

        if (!StringUtils.isEmpty(file.getOriginalFilename())) {
            String fileName = file.getOriginalFilename();
            if (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls")) {
                throw new BadRequestRestApiException()
                        .developerMessage("File is not an Excel file")
                        .userMessage("Please upload an Excel file");
            }
            priceManagementService.uploadPriceManagement(file, principal.getName());
        } else {
            throw new BadRequestRestApiException()
                    .developerMessage("File name is empty")
                    .userMessage("Please upload a file");
        }
    }

    // get all avco

    @PreAuthorize("hasAuthority('VIEW_PRICE_MANAGEMENT')")
    @JsonView(BaseView.AvcoView.class)
    @GetMapping("/avco")
    @Operation(summary = "getAll Avco", description = "Get All Avco",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<AvcoItems> getAllAvco(@RequestParam(name = "searchParam", required = false) String searchParam,
                                      @RequestParam(name = "fromDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fromDate,
                                      @RequestParam(name = "toDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate toDate,
                                      @RequestParam(name = "orderNumber", required = false) String orderNumber,
                                      @RequestParam(name = "productPublicId", required = false) UUID productPublicId,
                                      @RequestParam(name = "transactionType", required = false) TransactionType transactionType,
                                      @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return priceManagementService.getAllAvco(searchParam, fromDate, toDate, orderNumber, productPublicId, transactionType, pageable);
    }
}
