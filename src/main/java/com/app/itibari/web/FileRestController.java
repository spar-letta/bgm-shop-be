package com.app.itibari.web;

import com.app.itibari.dataTypes.BannerAction;
import com.app.itibari.dataTypes.ReferenceType;
import com.app.itibari.dataTypes.StatementExportType;
import com.app.itibari.models.entities.FileData;
import com.app.itibari.models.request.BannerRequest;
import com.app.itibari.models.request.FileReceiptDTO;
import com.app.itibari.models.response.*;
import com.app.itibari.services.FileService;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import com.itextpdf.io.source.ByteArrayOutputStream;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Tag(name = "Files", description = "File management")
@RestController
@RequestMapping("/files")
public class FileRestController {

    private final FileService fileService;

    public FileRestController(FileService fileService) {

        this.fileService = fileService;
    }

    @JsonView({BaseView.FileDataView.class})
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload File", description = "Upload File",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(example = ""))),
                    @ApiResponse(responseCode = "404", description = "Input Error", content = @Content(mediaType = "application/json", schema = @Schema(example = "")))
            }
    )
    public FileData uploadFile(@RequestParam("file") MultipartFile file,
                               @RequestParam("referenceId") UUID referenceId,
                               @RequestParam("referenceType") ReferenceType referenceType,
                               @RequestParam(value = "description", required = false) String description,
                               @RequestParam(value = "receipt", required = false) String receipt,
                               @RequestParam(value = "amount", required = false) String amount,
                               @RequestParam(value = "documentRulePublicId", required = false) UUID documentRulePublicId,
                               @RequestParam(value = "expiryDate", required = false) String expiryDate,
                               @RequestParam(value = "supplierDocumentInfo", required = false) String supplierDocumentInfo,
                               @RequestParam(value = "uploadedFromWeb", required = false, defaultValue = "false") boolean uploadedFromWeb,
                               @Parameter(hidden = true) Principal principal) {

        return fileService.uploadFile(file, referenceId, referenceType, description, receipt, amount, documentRulePublicId, expiryDate, uploadedFromWeb, principal.getName(), supplierDocumentInfo);
    }

    //download file

    @GetMapping("/download/{publicId}")
    @Operation(summary = "Download File", description = "Download File",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(example = ""))),
                    @ApiResponse(responseCode = "404", description = "Input Error", content = @Content(mediaType = "application/json", schema = @Schema(example = "")))
            }
    )
    public FileResponse downloadFile(@PathVariable("publicId") UUID referenceId) throws IOException {

        return fileService.downloadFile(referenceId);
    }

    //swap
    @PutMapping(path = "/{publicId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Swap File", description = "Swap File",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(example = ""))),
                    @ApiResponse(responseCode = "404", description = "Input Error", content = @Content(mediaType = "application/json", schema = @Schema(example = "")))
            }
    )
    public void swapFile(@PathVariable("publicId") UUID publicId,
                         @RequestParam("file") MultipartFile file,
                         @Parameter(hidden = true) Principal principal) throws IOException {

        fileService.swapFile(publicId, file, principal.getName());
    }

    @DeleteMapping("/{publicId}")
    @Operation(summary = "Delete File", description = "Delete File",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(example = ""))),
                    @ApiResponse(responseCode = "404", description = "Input Error", content = @Content(mediaType = "application/json", schema = @Schema(example = "")))
            }
    )
    public void deleteFile(@PathVariable("publicId") UUID referenceId,
                           @RequestParam(name = "referenceType") ReferenceType referenceType) throws IOException {

        fileService.deleteFile(referenceId);
    }

    @JsonView({BaseView.FileDataView.class})
    @GetMapping
    @Operation(summary = "Get File", description = "Get File",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(example = ""))),
                    @ApiResponse(responseCode = "404", description = "Input Error", content = @Content(mediaType = "application/json", schema = @Schema(example = "")))
            }
    )
    public Page<FileData> getFile(@RequestParam(name = "referenceType") ReferenceType referenceType,
                                  @RequestParam(name = "referenceId") UUID referenceId,
                                  @RequestParam(name = "receipt", required = false) String receipt,
                                  @RequestParam(name = "orderNumber", required = false) String orderNumber,
                                  @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return fileService.getFile(referenceId, referenceType, receipt, orderNumber, pageable);
    }

    @GetMapping("/export")
    @Operation(summary = "Export Files", description = "Export files for a reference (e.g. RECEIPT rows shown on cash recon) to Excel or PDF",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "404", description = "Input Error", content = @Content(mediaType = "application/json", schema = @Schema(example = "")))
            }
    )
    public ResponseEntity<?> exportFiles(@RequestParam(name = "referenceType") ReferenceType referenceType,
                                         @RequestParam(name = "referenceId") UUID referenceId,
                                         @RequestParam(name = "receipt", required = false) String receipt,
                                         @RequestParam(name = "orderNumber", required = false) String orderNumber,
                                         @RequestParam(name = "exportType") StatementExportType exportType,
                                         @Parameter(hidden = true) Principal principal) {
        ByteArrayOutputStream byteArrayOutputStream = fileService.exportFiles(
                referenceId, referenceType, receipt, orderNumber, exportType, principal.getName());
        String contentDisposition = String.format("attachment; filename=\"Receipts Report - %s.%s\"",
                LocalDateTime.now(),
                exportType.extension);
        return ResponseEntity.ok()
                .contentLength(byteArrayOutputStream.size())
                .contentType(MediaType.parseMediaType(exportType.contentType))
                .cacheControl(CacheControl.noCache())
                .header("Content-Disposition", contentDisposition)
                .body(byteArrayOutputStream.toByteArray());
    }

    @GetMapping("/productImage/{referenceId}")
    @Operation(summary = "Get Product Images", description = "Get Product Images",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(example = ""))),
                    @ApiResponse(responseCode = "404", description = "Input Error", content = @Content(mediaType = "application/json", schema = @Schema(example = "")))
            }
    )
    public ResponseEntity<FileResponseDTO> getProductImages(@PathVariable("referenceId") UUID referenceId) throws IOException {

        FileResponseDTO fileResponseDTO = fileService.getImages(referenceId, ReferenceType.PRODUCT);
        return ResponseEntity.ok(fileResponseDTO);
    }

    @GetMapping("/image/{referenceId}")
    @Operation(summary = "Get Product Images", description = "Get Product Images",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(example = ""))),
                    @ApiResponse(responseCode = "404", description = "Input Error", content = @Content(mediaType = "application/json", schema = @Schema(example = "")))
            }
    )
    public ResponseEntity<FileResponseDTO> getImages(@PathVariable UUID referenceId,
                                                     @RequestParam(name = "referenceType") ReferenceType referenceType) throws IOException {

        FileResponseDTO fileResponseDTO = fileService.getImages(referenceId, referenceType);
        return ResponseEntity.ok(fileResponseDTO);
    }

    // check if is duplicate
    @GetMapping("/isDuplicate")
    @Operation(summary = "Check Duplicate Receipt", description = "Check if the receipt is a duplicate",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(example = ""))),
                    @ApiResponse(responseCode = "404", description = "Input Error", content = @Content(mediaType = "application/json", schema = @Schema(example = "")))
            }
    )
    public ResponseEntity<DuplicateReceiptResponse> isDuplicate(@RequestParam("receipt") String receipt,
                                                                @RequestParam("referenceId") UUID referenceId,
                                                                @RequestParam("referenceType") ReferenceType referenceType) {

        DuplicateReceiptResponse fileResponse = fileService.isDuplicate(receipt, referenceId, referenceType);
        return ResponseEntity.ok(fileResponse);
    }

    // delete receipt by accountant
    @DeleteMapping("/receipt/{publicId}")
    @Operation(summary = "Delete Receipt", description = "Delete a receipt by public ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(example = ""))),
                    @ApiResponse(responseCode = "404", description = "Input Error", content = @Content(mediaType = "application/json", schema = @Schema(example = "")))
            }
    )
    public void deleteReceipt(@PathVariable("publicId") UUID publicId,
                              @RequestParam("comment") String comment,
                              @Parameter(hidden = true) Principal principal) {

        fileService.deleteReceipt(publicId, comment, principal.getName());
    }

    // delete receipt by delivery agent

    @DeleteMapping("/agent/receipt/{publicId}")
    @Operation(summary = "Delete Receipt by Delivery Agent", description = "Delete a receipt by public ID by delivery agent",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(example = ""))),
                    @ApiResponse(responseCode = "404", description = "Input Error", content = @Content(mediaType = "application/json", schema = @Schema(example = "")))
            })
    public void deleteReceiptByAgent(@PathVariable("publicId") UUID publicId,
                                     @RequestParam("comment") String comment,
                                     @Parameter(hidden = true) Principal principal) {

        fileService.deleteReceiptByAgent(publicId, comment, principal.getName());
    }


    // get bnpl kyc documents


    @GetMapping("/bnpl/{customerPublicId}")
    @Operation(summary = "Get BNPL KYC Documents", description = "Get BNPL KYC Documents",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(example = ""))),
                    @ApiResponse(responseCode = "404", description = "Input Error", content = @Content(mediaType = "application/json", schema = @Schema(example = "")))
            }
    )
    public ResponseEntity<List<BNPLFileResponseDTO>> getBNPLKYCDocuments(@PathVariable("customerPublicId") UUID customerPublicId,
                                                                         @RequestParam(name = "documentRulePublicId") UUID documentRulePublicId
    ) {
        List<BNPLFileResponseDTO> bnplFileResponseDTO = fileService.getBNPLKYCDocuments(customerPublicId, documentRulePublicId);
        return ResponseEntity.ok(bnplFileResponseDTO);
    }

    // approve a file

    @PatchMapping("/{publicId}/verify")
    @Operation(summary = "Approve File", description = "Approve a file",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(example = ""))),
                    @ApiResponse(responseCode = "404", description = "Input Error", content = @Content(mediaType = "application/json", schema = @Schema(example = "")))
            }
    )
    public void approveFile(@PathVariable UUID publicId,
                            @Parameter(hidden = true) Principal principal) {
        fileService.approveFile(publicId, principal.getName());
    }

    // reject a file with reason
    @PatchMapping("/{publicId}/reject")
    @Operation(summary = "Approve File", description = "Approve a file",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(example = ""))),
                    @ApiResponse(responseCode = "404", description = "Input Error", content = @Content(mediaType = "application/json", schema = @Schema(example = "")))
            }
    )
    public void rejectFile(@PathVariable UUID publicId,
                           @RequestParam("reason") String reason,
                           @Parameter(hidden = true) Principal principal) {
        fileService.rejectFile(publicId, reason, principal.getName());
    }

    // get customer shop profile image

    @GetMapping("/customerShopProfile/{referenceId}")
    @Operation(summary = "Get Customer Shop Profile Image", description = "Get Customer Shop Profile Image",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(example = ""))),
                    @ApiResponse(responseCode = "404", description = "Input Error", content = @Content(mediaType = "application/json", schema = @Schema(example = "")))
            }
    )
    public ResponseEntity<FileResponseDTO> getCustomerShopProfileImage(@PathVariable UUID referenceId) throws IOException {
        FileResponseDTO fileResponseDTO = fileService.getImages(referenceId, ReferenceType.CUSTOMER_SHOP_PROFILE);
        return ResponseEntity.ok(fileResponseDTO);
    }


    // upload mobile banner image

    @JsonView({BaseView.FileDataView.class})
    @PostMapping(value = "/uploadMobileBanner", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload Mobile Banner Image", description = "Upload Mobile Banner Image",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(example = ""))),
                    @ApiResponse(responseCode = "404", description = "Input Error", content = @Content(mediaType = "application/json", schema = @Schema(example = "")))
            }
    )
    public FileData uploadMobileBannerImage(@RequestParam("file") MultipartFile file,
                                            @RequestPart(value = "bannerRequest", required = false) BannerRequest bannerRequest,
                                            @RequestParam(value = "description", required = false) String description,
                                            @Parameter(hidden = true) Principal principal) {
        return fileService.uploadMobileBannerImage(file, bannerRequest, description, principal.getName());
    }

    // fetch mobile banner image

    @GetMapping("/mobileBanner")
    @Operation(summary = "Get Mobile Banner Image", description = "Get Mobile Banner Image",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(example = ""))),
                    @ApiResponse(responseCode = "404", description = "Input Error", content = @Content(mediaType = "application/json", schema = @Schema(example = "")))
            })
    public ResponseEntity<List<BannerResponseDTO>> getMobileBannerImage(@RequestParam(name = "isActive", required = false, defaultValue = "true") Boolean isActive) {
        List<BannerResponseDTO> fileResponseDTO = fileService.getMobileBanner(false, isActive);
        return ResponseEntity.ok(fileResponseDTO);
    }

    // fetch customer receipt files

    @GetMapping("/customerReceipts")
    @Operation(summary = "Get Customer Receipt Files", description = "Get Customer Receipt Files",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(example = ""))),
                    @ApiResponse(responseCode = "404", description = "Input Error", content = @Content(mediaType = "application/json", schema = @Schema(example = "")))
            }
    )
    public ResponseEntity<List<FileReceiptDTO>> getCustomerReceiptFiles(@Parameter(hidden = true) Principal principal) {
        List<FileReceiptDTO> fileResponseDTO = fileService.getCustomerReceiptFiles(principal.getName());
        return ResponseEntity.ok(fileResponseDTO);
    }

    // actions on banners

    @PatchMapping("/banner/{publicId}/action")
    @Operation(summary = "Approve or Reject Banner Image", description = "Approve or Reject Banner Image",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(example = ""))),
                    @ApiResponse(responseCode = "404", description = "Input Error", content = @Content(mediaType = "application/json", schema = @Schema(example = "")))
            }
    )
    public void approveOrRejectBannerImage(@PathVariable UUID publicId,
                                           @RequestParam("action") BannerAction action,
                                           @Parameter(hidden = true) Principal principal) {

        fileService.approveOrRejectBannerImage(publicId, action, principal.getName());
    }


    @GetMapping("/{routePublicId}/depositedAmount")
    @Operation(summary = "Get Deposited Amount", description = "Get Deposited Amount for a receipt",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(example = ""))),
                    @ApiResponse(responseCode = "404", description = "Input Error", content = @Content(mediaType = "application/json", schema = @Schema(example = "")))
            }
    )
    public ResponseEntity<BigDecimal> getDepositedAmount(@PathVariable UUID routePublicId) {
        BigDecimal depositedAmount = fileService.getDepositedAmount(routePublicId);
        return ResponseEntity.ok(depositedAmount);
    }

    //TODO after test we delete
    @PostMapping("/sampleRunDocumentExpiry")
    public void runDocumentExpiry(@Parameter(hidden = true) Principal principal) {
        fileService.runDocumentExpiry();
    }

    // image depending on network strength
}
