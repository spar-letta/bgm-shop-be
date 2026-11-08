package com.app.itibari.web;

import com.app.itibari.dataTypes.*;
import com.app.itibari.docs.Examples;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.report.*;
import com.app.itibari.models.request.TicketDto;
import com.app.itibari.models.response.*;
import com.app.itibari.services.*;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.formatter.DateFormatter;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import com.itextpdf.io.IOException;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.text.DocumentException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Tag(name = "Reports")
@RestController
@RequestMapping("/reports")
public class ReportRestController {

    private final ReportService reportService;

    private final LogisticService logisticService;

    private final SalesOrdersService salesOrdersService;

    private final CustomersService customersService;

    private final ProductService productService;
    private final WarehouseService warehouseService;
    private final SalesAgentsService salesAgentsService;
    private final BNPLService bNPLService;
    private final ReconciliationService reconciliationService;
    private final DeliveryAgentsService deliveryAgentsService;
    private final HelpDeskService helpDeskService;
    private final BnplCrmService bnplCrmService;
    private final QuarantineService quarantineService;
    private final BnplCreditLimitAdjustmentService creditLimitAdjustmentService;

    public ReportRestController(ReportService reportService,
                                LogisticService logisticService,
                                SalesOrdersService salesOrdersService,
                                CustomersService customersService,
                                ProductService productService, WarehouseService warehouseService, SalesAgentsService salesAgentsService, BNPLService bNPLService, ReconciliationService reconciliationService, DeliveryAgentsService deliveryAgentsService, HelpDeskService helpDeskService, BnplCrmService bnplCrmService, QuarantineService quarantineService, BnplCreditLimitAdjustmentService creditLimitAdjustmentService) {

        this.reportService = reportService;
        this.logisticService = logisticService;
        this.salesOrdersService = salesOrdersService;
        this.customersService = customersService;
        this.productService = productService;
        this.warehouseService = warehouseService;
        this.salesAgentsService = salesAgentsService;
        this.bNPLService = bNPLService;
        this.reconciliationService = reconciliationService;
        this.deliveryAgentsService = deliveryAgentsService;
        this.helpDeskService = helpDeskService;
        this.bnplCrmService = bnplCrmService;
        this.quarantineService = quarantineService;
        this.creditLimitAdjustmentService = creditLimitAdjustmentService;
    }

    @PreAuthorize("hasAuthority('VIEW_PURCHASE_ORDER_REPORT')")
    @GetMapping("/purchaseOrder/{purchaseOrderId}")
    @Operation(summary = "Generate Purchase order report", description = "Generate Purchase order report",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<?> generatePurchaseOrderReport(
            @PathVariable(name = "purchaseOrderId") UUID purchaseOrderId,
            @Parameter(hidden = true) Principal principal) throws IOException, DocumentException, java.io.IOException {

        ByteArrayOutputStream byteArrayOutputStream = reportService.generatePurchaseOrderReport(purchaseOrderId, principal.getName());
        String contentDisposition = String.format("attachment; filename=\"Purchase order - %s.%s\"",
                LocalDateTime.now().toString(),
                StatementExportType.PDF.extension);
        String mediaType = StatementExportType.PDF.contentType;
        return ResponseEntity.ok().contentLength(byteArrayOutputStream.size())
                .contentType(MediaType.parseMediaType(mediaType))
                .cacheControl(CacheControl.noCache())
                .header("Content-Disposition", contentDisposition)
                .body(byteArrayOutputStream.toByteArray());
    }


    @PreAuthorize("hasAuthority('VIEW_RETURN_NOTE_REPORT')")
    @GetMapping("/goodsReturnNote/{publicId}")
    @Operation(summary = "Generate goodsReturnNote report", description = "Generate goodsReturnNote report",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<?> generateReturnNoteReport(
            @PathVariable(name = "publicId") UUID publicId,
            @RequestParam(name = "exportType", required = false) StatementExportType exportType,
            @Parameter(hidden = true) Principal principal) throws IOException, DocumentException, java.io.IOException {
        exportType = StatementExportType.PDF;
        ByteArrayOutputStream byteArrayOutputStream = reportService.generateReturnNoteReport(publicId, principal.getName(), exportType);
        String contentDisposition = String.format("attachment; filename=\"Grtn Report - %s.%s\"",
                LocalDateTime.now().toString(),
                exportType.extension);
        String mediaType = exportType.contentType;
        return ResponseEntity.ok().contentLength(byteArrayOutputStream.size())
                .contentType(MediaType.parseMediaType(mediaType))
                .cacheControl(CacheControl.noCache())
                .header("Content-Disposition", contentDisposition)
                .body(byteArrayOutputStream.toByteArray());
    }

    @PreAuthorize("hasAuthority('VIEW_RETURN_NOTE_REPORT')")
    @GetMapping("/goodsReceiveNote/{publicId}")
    @Operation(summary = "Generate goodsReceiveNote report", description = "Generate goodsReceiveNote report",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<?> generateReceiveNoteReport(
            @PathVariable(name = "publicId") UUID publicId,
            @RequestParam(name = "exportType", required = false) StatementExportType exportType,
            @Parameter(hidden = true) Principal principal) throws IOException, DocumentException, java.io.IOException {

        ByteArrayOutputStream byteArrayOutputStream = reportService.generateReceiveNoteReport(publicId,
                principal.getName(),
                exportType);
        String contentDisposition = String.format("attachment; filename=\"Grn Report - %s.%s\"",
                LocalDateTime.now().toString(),
                exportType.extension);
        String mediaType = exportType.contentType;
        return ResponseEntity.ok().contentLength(byteArrayOutputStream.size())
                .contentType(MediaType.parseMediaType(mediaType))
                .cacheControl(CacheControl.noCache())
                .header("Content-Disposition", contentDisposition)
                .body(byteArrayOutputStream.toByteArray());
    }

    // issusance report excel and pdf
    @GetMapping("/issuanceReport/{routePublicId}")
    @Operation(summary = "Generate Issuance report", description = "Generate Issuance report",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<?> generateIssuanceReport(
            @PathVariable(name = "routePublicId") UUID routePublicId,
            @Parameter(hidden = true) Principal principal,
            @RequestParam(name = "exportType", required = false) StatementExportType exportType) throws IOException, DocumentException, java.io.IOException {

        if (exportType == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Export type is required")
                    .userMessage("Export type is required");
        }

        ByteArrayOutputStream byteArrayOutputStream = reportService.generateIssuanceReport(routePublicId, exportType, principal.getName());
        String contentDisposition = String.format("attachment; filename=\"Issuance Report - %s.%s\"",
                LocalDateTime.now().toString(),
                exportType);
        String mediaType = exportType.contentType;
        return ResponseEntity.ok().contentLength(byteArrayOutputStream.size())
                .contentType(MediaType.parseMediaType(mediaType))
                .cacheControl(CacheControl.noCache())
                .header("Content-Disposition", contentDisposition)
                .body(byteArrayOutputStream.toByteArray());
    }

    // export sales orders report

    // @PreAuthorize("hasAuthority('VIEW_SALES_ORDERS_REPORT')")
    @GetMapping("/salesOrdersReport")
    @Operation(summary = "Generate Sales Orders report", description = "Generate Sales Orders report",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<?> generateSalesOrdersReport(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(name = "salesOrderNumber", required = false) String salesOrderNumber,
            @RequestParam(name = "customerId", required = false) UUID customerId,
            @RequestParam(name = "warehouseId", required = false) UUID warehouseId,
            @RequestParam(name = "organizationRegionId", required = false) UUID organizationRegionId,
            @RequestParam(name = "locationId", required = false) UUID locationId,
            @RequestParam(name = "sourceChannel", required = false) SourceChannelEnum sourceChannel,
            @RequestParam(name = "salesOrderStatus", required = false) LoadingStatusEnum salesOrderStatus,
            @RequestParam(name = "statuses", required = false) List<LoadingStatusEnum> statuses,
            @RequestParam(name = "paymentStatus", required = false) PaymentStatusEnum paymentStatus,
            @RequestParam(name = "invoiceNumber", required = false) String invoiceNumber,
            @RequestParam(name = "bookedBySalesAgent", required = false) Boolean bookedBySalesAgent,
            @RequestParam(name = "salesAgentPublicId", required = false) UUID salesAgentPublicId,
            @RequestParam(name = "deliveryAgentPublicId", required = false) UUID deliveryAgentPublicId,
            @RequestParam(name = "parentCustomerPublicId", required = false) UUID parentCustomerPublicId,
            @RequestParam(name = "isCredit", required = false) Boolean isCredit,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(hidden = true) Principal principal,
            @RequestParam(name = "isWalkIn", required = false) Boolean isWalkIn,
            @RequestParam(name = "bookedByParent", required = false) Boolean bookedByParent,
            @RequestParam(name = "exportType", required = false) StatementExportType exportType,
            @PageableDefault(size = 1000, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) throws IOException,
            DocumentException {

        SalesOrdersPageableDTO salesOrders = salesOrdersService.getAllSalesOrders(searchParam,
                salesOrderNumber,
                customerId,
                warehouseId,
                organizationRegionId,
                sourceChannel,
                salesOrderStatus,
                paymentStatus,
                invoiceNumber,
                startDate,
                endDate,
                locationId,
                bookedBySalesAgent,
                salesAgentPublicId,
                deliveryAgentPublicId,
                parentCustomerPublicId,
                isCredit,
                statuses,
                isWalkIn,
                bookedByParent,
                pageable);
        if (exportType == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Export type is required")
                    .userMessage("Export type is required");
        }

        ByteArrayOutputStream byteArrayOutputStream = reportService.generateSalesOrdersReport(exportType,
                salesOrders.getContent(),
                principal.getName());
        String contentDisposition = String.format("attachment; filename=\"Sales Orders Report - %s.%s\"",
                LocalDateTime.now().toString(),
                exportType);
        String mediaType = exportType.contentType;
        return ResponseEntity.ok().contentLength(byteArrayOutputStream.size())
                .contentType(MediaType.parseMediaType(mediaType))
                .cacheControl(CacheControl.noCache())
                .header("Content-Disposition", contentDisposition)
                .body(byteArrayOutputStream.toByteArray());
    }

    // customers report

    @PreAuthorize("hasAuthority('VIEW_CUSTOMERS_REPORT')")
    @GetMapping("/customersReport")
    @Operation(summary = "Generate Customers report", description = "Generate Customers report",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<?> generateCustomersReport(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "email", required = false) String email,
            @RequestParam(name = "nationalId", required = false) String nationalId,
            @RequestParam(name = "phoneNumber", required = false) String phoneNumber,
            @RequestParam(name = "sourceChannel", required = false) SourceChannelEnum sourceChannel,
            @RequestParam(name = "organizationLocationId", required = false) UUID organizationLocationId,
            @RequestParam(name = "locations", required = false) List<UUID> locations,
            @RequestParam(name = "organizationRegionId", required = false) UUID organizationRegionId,
            @RequestParam(name = "registeredBy", required = false) UUID registeredBy,
            @RequestParam(name = "servedBy", required = false) UUID servedBy,
            @RequestParam(name = "parentCustomerPublicId", required = false) UUID parentCustomerPublicId,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(hidden = true) Principal principal,
            @RequestParam(name = "exportType") StatementExportType exportType,
            @PageableDefault(size = 1000, sort = "registrationDate", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<Customers> customersPage = customersService.getAllCustomers(searchParam,
                name,
                email,
                nationalId,
                phoneNumber,
                sourceChannel,
                organizationLocationId,
                organizationRegionId,
                registeredBy,
                locations,
                startDate,
                endDate,
                servedBy,
                parentCustomerPublicId,
                null,
                null,
                null,
                null,
                pageable);

        ByteArrayOutputStream byteArrayOutputStream = reportService.generateCustomersReport(exportType,
                customersPage.getContent(),
                principal.getName());
        String contentDisposition = String.format("attachment; filename=\"Customers Report - %s.%s\"",
                LocalDateTime.now().toString(),
                exportType);
        String mediaType = exportType.contentType;
        return ResponseEntity.ok().contentLength(byteArrayOutputStream.size())
                .contentType(MediaType.parseMediaType(mediaType))
                .cacheControl(CacheControl.noCache())
                .header("Content-Disposition", contentDisposition)
                .body(byteArrayOutputStream.toByteArray());
    }

    // products
    // @PreAuthorize("hasAuthority('VIEW_PRODUCTS_REPORT')")
    @GetMapping("/productsReport")
    @Operation(summary = "Generate Products report", description = "Generate Products report",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<?> generateProductsReport(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(name = "productName", required = false) String productName,
            @RequestParam(name = "status", required = false) ProductStatus status,
            @RequestParam(name = "supplier", required = false) UUID supplier,
            @RequestParam(name = "category", required = false) UUID category,
            @RequestParam(name = "class", required = false) UUID classPublicId,
            @RequestParam(name = "subClass", required = false) UUID subClassPublicId,
            @RequestParam(name = "isActive", required = false) Boolean isActive,
            @RequestParam(name = "brandPublicId", required = false) UUID brandPublicId,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 100, sort = "productName", direction = Sort.Direction.ASC) Pageable pageable) {

        Page<Product> products = productService.getProducts(searchParam, productName, status, supplier, category, classPublicId,
                subClassPublicId, isActive, null, brandPublicId, pageable);
        ByteArrayOutputStream byteArrayOutputStream = reportService.generateProductsReport(products.getContent(),
                StatementExportType.EXCEL,
                principal.getName());
        String contentDisposition = String.format("attachment; filename=\"products Report - %s.%s\"",
                LocalDateTime.now().toString(),
                StatementExportType.EXCEL);
        String mediaType = StatementExportType.EXCEL.contentType;
        return ResponseEntity.ok().contentLength(byteArrayOutputStream.size())
                .contentType(MediaType.parseMediaType(mediaType))
                .cacheControl(CacheControl.noCache())
                .header("Content-Disposition", contentDisposition)
                .body(byteArrayOutputStream.toByteArray());
    }

    //@PreAuthorize("hasAuthority('VIEW_LIVE_STOCK_REPORT')")
    @GetMapping("/liveStockReport")
    public LiveStockPageable generateLiveStockReport(
            @RequestParam(name = "liveDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate liveDate,
            @RequestParam(name = "warehouseId") UUID warehouseId,
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @PageableDefault(size = 100, sort = "productName", direction = Sort.Direction.DESC) Pageable pageable) {

        LocalDate reportDate = Objects.requireNonNullElse(liveDate, LocalDate.now());
        return logisticService.getLiveStockReport(reportDate, warehouseId, searchParam, pageable);
    }

    @GetMapping("/salesReport")
    @Operation(summary = "Generate Sales report", description = "Generate Sales report",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})

    public SalesAgentReportPegable generateSalesReport(
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "salesAgentPublicId", required = false) UUID salesAgentPublicId,
            @Parameter(hidden = true) Principal principal) {

        return reportService.getSalesReport(startDate, endDate, date, salesAgentPublicId, principal.getName());
    }

    //export

    @GetMapping("/salesReport/export")
    @Operation(summary = "Generate Sales report", description = "Generate Sales report",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity generateSalesReportExport(
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "salesAgentPublicId", required = false) UUID salesAgentPublicId,
            @Parameter(hidden = true) Principal principal) {

        ByteArrayOutputStream byteArrayOutputStream = reportService.generateSalesOrdersReportExport(startDate,
                endDate,
                date,
                salesAgentPublicId,
                StatementExportType.EXCEL,
                principal.getName());
        String contentDisposition = String.format("attachment; filename=\"sa performance Report - %s.%s\"",
                LocalDateTime.now().toString(),
                StatementExportType.EXCEL);
        String mediaType = StatementExportType.EXCEL.contentType;
        return ResponseEntity.ok().contentLength(byteArrayOutputStream.size())
                .contentType(MediaType.parseMediaType(mediaType))
                .cacheControl(CacheControl.noCache())
                .header("Content-Disposition", contentDisposition)
                .body(byteArrayOutputStream.toByteArray());
    }

    @GetMapping("/supplierDashboardAnalysis")
    @Operation(summary = "Generate Supplier Dashboard Analysis report", description = "Generate Supplier Dashboard Analysis report",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public SupplierDashboardAnalysis generateSupplierDashboardAnalysis(@RequestParam(name = "sku", required = false) String sku,
                                                                       @RequestParam(name = "region", required = false) UUID regionPublicId,
                                                                       @RequestParam(name = "location", required = false) UUID locationPublicId,
                                                                       @RequestParam(name = "startDate", required = false) LocalDate startDate,
                                                                       @RequestParam(name = "endDate", required = false) LocalDate endDate,
                                                                       @Parameter(hidden = true) Principal principal,
                                                                       @PageableDefault(size = 100, direction = Sort.Direction.ASC) Pageable pageable) {

        return reportService.getSupplierDashboardAnalysis(sku,
                regionPublicId,
                locationPublicId,
                startDate,
                endDate,
                principal.getName(),
                pageable);
    }

    // product distribution report
    @GetMapping("/productDistribution")
    @Operation(summary = "Generate Product Distribution report", description = "Generate Product Distribution report",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ProductDistributionPageableDTO generateProductDistributionReport(
            @RequestParam(name = "productId", required = false) UUID productId,
            @RequestParam(name = "categoryId", required = false) UUID categoryId,
            @RequestParam(name = "regionId", required = false) UUID regionId,
            @RequestParam(name = "locationId", required = false) UUID locationId,
            @RequestParam(name = "customerId", required = false) UUID customerId,
            @RequestParam(name = "startDate", required = false) String starDate,
            @RequestParam(name = "endDate", required = false) String enDate,
//            @RequestParam(value = "startDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") @Parameter(example = "yyyy-MM-dd") LocalDate startDate,
//            @RequestParam(value = "endDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") @Parameter(example = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 100, direction = Sort.Direction.ASC) Pageable pageable) {

        LocalDate startDate = DateFormatter.parseDate(starDate);
        LocalDate endDate = DateFormatter.parseDate(enDate);

        return reportService.getProductDistributionReport(productId,
                categoryId,
                regionId,
                locationId,
                customerId,
                startDate,
                endDate,
                principal.getName(),
                pageable);
    }

    // supplier customer purchase report
    @GetMapping("/purchaseReport")
    @Operation(summary = "Generate Product purchase report", description = "Generate Product purchase report",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public Page<CustomerOrderDTORapper> getPurchaseReport(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(name = "regionId", required = false) UUID regionId,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 1000, direction = Sort.Direction.ASC) Pageable pageable) {

        return reportService.getPurchaseReport(searchParam, regionId, startDate, endDate, principal.getName(), pageable);
    }

    @GetMapping("/purchaseReport/export")
    @Operation(summary = "Generate Product purchase report export", description = "Generate Product purchase report export",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<?> getPurchaseReportExport(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(name = "regionId", required = false) UUID regionId,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "exportType") StatementExportType exportType,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 1000, direction = Sort.Direction.ASC) Pageable pageable) {

        Page<CustomerOrderDTORapper> customerOrderPageable = reportService.getPurchaseReport(searchParam, regionId, startDate,
                endDate,
                principal.getName(), pageable);
        ByteArrayOutputStream byteArrayOutputStream = reportService.generatePurchaseOrderReportExport(customerOrderPageable.getContent(),
                exportType,
                principal.getName());
        String contentDisposition = String.format("attachment; filename=\"purchase Report - %s.%s\"",
                LocalDateTime.now().toString(),
                exportType.extension);
        String mediaType = exportType.contentType;
        return ResponseEntity.ok().contentLength(byteArrayOutputStream.size())
                .contentType(MediaType.parseMediaType(mediaType))
                .cacheControl(CacheControl.noCache())
                .header("Content-Disposition", contentDisposition)
                .body(byteArrayOutputStream.toByteArray());
    }

    @GetMapping("/inventoryDashboard/summary")
    @Operation(summary = "Get Inventory Dashboard Summary", description = "Get Inventory Dashboard Summary",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public InventoryDashboardSummaryDTO getInventoryDashboardSummary(@RequestParam(name = "regionId", required = false) UUID regionId,
                                                                     @RequestParam(name = "startDate", required = false) String startDate,
                                                                     @RequestParam(name = "endDate", required = false) String endDate) {
        LocalDate start = DateFormatter.parseDate(startDate);
        LocalDate end = DateFormatter.parseDate(endDate);
        return reportService.getInventoryDashboardSummary(regionId, start, end);
    }

    @GetMapping("/inventoryDashboard/quantityByProduct")
    @Operation(summary = "Get Inventory Dashboard Quantity By Product", description = "Get Inventory Dashboard Quantity By Product",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public List<InventoryQuantityByProductDTO> getInventoryDashboardQuantityByProduct(@RequestParam(name = "regionId", required = false) UUID regionId,
                                                                                      @RequestParam(name = "startDate", required = false) String startDate,
                                                                                      @RequestParam(name = "endDate", required = false) String endDate) {
        LocalDate start = DateFormatter.parseDate(startDate);
        LocalDate end = DateFormatter.parseDate(endDate);
        return reportService.getInventoryDashboardQuantityByProduct(regionId, start, end);
    }

    @GetMapping("/inventoryDashboard/distributionByRegion")
    @Operation(summary = "Get Inventory Dashboard Distribution By Region", description = "Get Inventory Dashboard Distribution By Region",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public List<InventoryRegionDistributionDTO> getInventoryDashboardDistributionByRegion(@RequestParam(name = "regionId", required = false) UUID regionId,
                                                                                          @RequestParam(name = "startDate", required = false) String startDate,
                                                                                          @RequestParam(name = "endDate", required = false) String endDate) {
        LocalDate start = DateFormatter.parseDate(startDate);
        LocalDate end = DateFormatter.parseDate(endDate);
        return reportService.getInventoryDashboardDistributionByRegion(regionId, start, end);
    }

    @GetMapping("/inventoryDashboard/details")
    @Operation(summary = "Get Inventory Dashboard Details", description = "Get Inventory Dashboard Details",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public Page<InventoryDashboardDetailDTO> getInventoryDashboardDetails(@RequestParam(name = "regionId", required = false) UUID regionId,
                                                                          @RequestParam(name = "startDate", required = false) String startDate,
                                                                          @RequestParam(name = "endDate", required = false) String endDate,
                                                                          @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {
        LocalDate start = DateFormatter.parseDate(startDate);
        LocalDate end = DateFormatter.parseDate(endDate);
        return reportService.getInventoryDashboardDetails(regionId, start, end, pageable);
    }

    @GetMapping("/supplierStatisticsReport")
    @Operation(summary = "Generate Supplier Statistics report", description = "Generate Supplier Statistics report",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET_STATISTICS", value = Examples.FETCH_STATISTICS)})),
            })
    public ResponseEntity<List<StatisticsDTO>> generateSupplierStatisticsReport(@RequestParam(name = "region", required = false) UUID regionPublicId,
                                                                                @RequestParam(name = "startDate", required = false) String startDate,
                                                                                @RequestParam(name = "endDate", required = false) String endDate,
                                                                                @Parameter(hidden = true) Principal principal,
                                                                                @PageableDefault(size = 100, direction = Sort.Direction.ASC) Pageable pageable) {
        LocalDate start = DateFormatter.parseDate(startDate);
        LocalDate end = DateFormatter.parseDate(endDate);
        return reportService.getSupplierStatisticsReport(regionPublicId, start, end, principal.getName(), pageable);
    }

    @GetMapping("/supplierProductDistributionReport")
    @Operation(summary = "Generate Supplier product Distribution report", description = "Generate Supplier product Distribution report",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET_STATISTICS_product", value = Examples.FETCH_PRODUCT_DISTRIBUTION)})),
            })
    public Page<ProductSaleDistributionDTO> generateSupplierDistributionReport(@RequestParam(name = "region", required = false) UUID regionPublicId,
                                                                               @RequestParam(name = "startDate", required = false) String startDate,
                                                                               @RequestParam(name = "endDate", required = false) String endDate,
                                                                               @Parameter(hidden = true) Principal principal,
                                                                               @PageableDefault(size = 100, direction = Sort.Direction.ASC) Pageable pageable) {
        LocalDate start = DateFormatter.parseDate(startDate);
        LocalDate end = DateFormatter.parseDate(endDate);
        return reportService.generateSupplierDistributionReport(regionPublicId, start, end, principal.getName(), pageable);
    }

    @GetMapping("/supplierQuantitySoldPerRegion")
    @Operation(summary = "Generate Supplier product Distribution per region report", description = "Generate Supplier product Distribution report",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET_Distribution", value = Examples.FETCH_PRODUCT_DISTRIBUTION_PER_REGION)})),
            })
    public ResponseEntity<List<QuantitySoldPerRegionDTO>> generateSupplierProductDistributionReport(@RequestParam(name = "region", required = false) UUID regionPublicId,
                                                                                                    @RequestParam(name = "startDate", required = false) String startDate,
                                                                                                    @RequestParam(name = "endDate", required = false) String endDate,
                                                                                                    @Parameter(hidden = true) Principal principal,
                                                                                                    @PageableDefault(size = 100, direction = Sort.Direction.ASC) Pageable pageable) {
        LocalDate start = DateFormatter.parseDate(startDate);
        LocalDate end = DateFormatter.parseDate(endDate);
        return reportService.generateSupplierProductDistributionReport(regionPublicId, start, end, principal.getName(), pageable);
    }

    @GetMapping("/supplierTotalOrders")
    @Operation(summary = "Generate Supplier product Distribution per region report", description = "Generate Supplier product Distribution report",
            responses = {@ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET_Distribution", value = Examples.FETCH_PRODUCT_DISTRIBUTION_PER_REGION)})),})
    public Map<String, Integer> generateSupplierTotalOrdersReport(@RequestParam(name = "regionId", required = false) UUID regionPublicId,
                                                                  @RequestParam(name = "startDate", required = false) String startDate,
                                                                  @RequestParam(name = "endDate", required = false) String endDate,
                                                                  @Parameter(hidden = true) Principal principal) {
        LocalDate start = DateFormatter.parseDate(startDate);
        LocalDate end = DateFormatter.parseDate(endDate);
        return reportService.generateSupplierTotalOrdersReport(regionPublicId, start, end, principal.getName());
    }

    @GetMapping("/supplierTotalQuantitySold")
    @Operation(summary = "Generate Quantity Sold Report", description = "Generate Quantity Sold Report",
            responses = {@ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET_Distribution", value = Examples.FETCH_PRODUCT_DISTRIBUTION_PER_REGION)})),})
    public Map<String, Integer> getQuantitySoldReport(@RequestParam(name = "regionId", required = false) UUID regionPublicId,
                                                      @RequestParam(name = "startDate", required = false) String startDate,
                                                      @RequestParam(name = "endDate", required = false) String endDate,
                                                      @Parameter(hidden = true) Principal principal) {
        LocalDate start = DateFormatter.parseDate(startDate);
        LocalDate end = DateFormatter.parseDate(endDate);
        return reportService.getQuantitySoldReport(regionPublicId, start, end, principal.getName());
    }

    @GetMapping("/totalActiveCustomers")
    @Operation(summary = "Generate Total Active Customers Report", description = "Generate Total Active Customers Report",
            responses = {@ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET_Distribution", value = Examples.FETCH_PRODUCT_DISTRIBUTION_PER_REGION)})),})
    public Map<String, Integer> getTotalActiveCustomers(@RequestParam(name = "regionId", required = false) UUID regionPublicId,
                                                        @RequestParam(name = "startDate", required = false) String startDate,
                                                        @RequestParam(name = "endDate", required = false) String endDate,
                                                        @Parameter(hidden = true) Principal principal) {
        LocalDate start = DateFormatter.parseDate(startDate);
        LocalDate end = DateFormatter.parseDate(endDate);
        return reportService.getTotalActiveCustomers(regionPublicId, start, end, principal.getName());
    }

    @GetMapping("/supplierSalesRevenue")
    @Operation(summary = "Generate Supplier revenue report", description = "Generate Supplier revenue report",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET_REVENUE_STATS", value = Examples.FETCH_SALES_REVENUE_STATS)})),
            })
    public ResponseEntity<List<SalesRevenueDTO>> generateSupplierRevenueReport(@RequestParam(name = "region", required = false) UUID regionPublicId,
                                                                               @RequestParam(name = "monthly", required = false) Boolean monthly,
                                                                               @RequestParam(name = "quarterly", required = false) Boolean quarterly,
                                                                               @RequestParam(name = "yearly", required = false) Boolean yearly,
                                                                               @Parameter(hidden = true) Principal principal,
                                                                               @PageableDefault(size = 100, direction = Sort.Direction.ASC) Pageable pageable) {
        return reportService.generateSupplierRevenueReport(regionPublicId, monthly, quarterly, yearly, principal.getName(), pageable);
    }

    /**
     * product performance report
     */

    @GetMapping("/productPerformanceReport")
    @Operation(summary = "Generate Product performance report", description = "Generate Product performance report",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<List<ProductPerformanceDTO>> generateProductPerformanceReport(
            @RequestParam(name = "productId", required = false) UUID productId,
            @RequestParam(name = "regionId", required = false) UUID regionId,
            @RequestParam(name = "zones", required = false) List<UUID> zones,
            @RequestParam(name = "startDate", required = false) String starDate,
            @RequestParam(name = "endDate", required = false) String enDate,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 100, direction = Sort.Direction.ASC) Pageable pageable) {
        LocalDate startDate = DateFormatter.parseDate(starDate);
        LocalDate endDate = DateFormatter.parseDate(enDate);
        return reportService.getProductPerformanceReport(productId,
                regionId,
                zones,
                startDate,
                endDate,
                principal.getName(),
                pageable);
    }

    //ProductPerformancePerZone

    @GetMapping("/productPerformancePerZoneReport")
    @Operation(summary = "Generate Product performance per zone report", description = "Generate Product performance per zone report",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<List<ProductPerformancePerZone>> generateProductPerformancePerZoneReport(
            @RequestParam(name = "productId", required = false) UUID productId,
            @RequestParam(name = "regionId", required = false) UUID regionId,
            @RequestParam(name = "zones", required = false) List<UUID> zones,
            @RequestParam(name = "startDate", required = false) String starDate,
            @RequestParam(name = "endDate", required = false) String enDate,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 100, direction = Sort.Direction.ASC) Pageable pageable) {
        LocalDate startDate = DateFormatter.parseDate(starDate);
        LocalDate endDate = DateFormatter.parseDate(enDate);
        return reportService.getProductPerformancePerZoneReport(productId,
                regionId,
                zones,
                startDate,
                endDate,
                principal.getName(),
                pageable);
    }

    //ProductCustomerPerformanceDTO

    @GetMapping("/productCustomerPerformanceReport")
    @Operation(summary = "Generate Product performance per customer report", description = "Generate Product performance per customer report",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public Page<ProductCustomerPerformanceDTO> generateProductCustomerPerformanceReport(
            @RequestParam(name = "productId", required = false) UUID productId,
            @RequestParam(name = "regionId", required = false) UUID regionId,
            @RequestParam(name = "locationId", required = false) UUID locationId,
            @RequestParam(name = "startDate", required = false) String starDate,
            @RequestParam(name = "endDate", required = false) String enDate,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 100, direction = Sort.Direction.ASC) Pageable pageable) {
        LocalDate startDate = DateFormatter.parseDate(starDate);
        LocalDate endDate = DateFormatter.parseDate(enDate);
        return reportService.getProductCustomerPerformanceReport(productId,
                regionId,
                locationId,
                startDate,
                endDate,
                principal.getName(),
                pageable);
    }

    @GetMapping("/warehouse/stagedForApproval")
    @Operation(summary = "Generate staging report", description = "Generate staging report",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity generateSalesReportExport(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") @Parameter(example = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") @Parameter(example = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        ByteArrayOutputStream byteArrayOutputStream = warehouseService.generateStagingReportExport(startDate,
                endDate,
                searchParam,
                StatementExportType.EXCEL,
                principal.getName(), pageable);
        String contentDisposition = String.format("attachment; filename=\"Staging Report - %s.%s\"",
                LocalDateTime.now().toString(),
                StatementExportType.EXCEL);
        String mediaType = StatementExportType.EXCEL.contentType;
        return ResponseEntity.ok().contentLength(byteArrayOutputStream.size())
                .contentType(MediaType.parseMediaType(mediaType))
                .cacheControl(CacheControl.noCache())
                .header("Content-Disposition", contentDisposition)
                .body(byteArrayOutputStream.toByteArray());
    }

    @GetMapping("/salesAgentWithCustomers")
    @Operation(summary = "Generate staging report", description = "Generate staging report",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity generateSalesReportExport(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(name = "organisationRegionPublicId", required = false) UUID organisationRegionPublicId,
            @RequestParam(name = "organisationLocationPublicId", required = false) UUID organisationLocationPublicId,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        ByteArrayOutputStream byteArrayOutputStream =
                salesAgentsService.generateSalesAgentCustomerReportExport(organisationLocationPublicId,
                        organisationRegionPublicId,
                        searchParam,
                        StatementExportType.EXCEL,
                        principal.getName(), pageable);
        String contentDisposition = String.format("attachment; filename=\"Sales_agent_customers Report - %s.%s\"",
                LocalDateTime.now().toString(),
                StatementExportType.EXCEL);
        String mediaType = StatementExportType.EXCEL.contentType;
        return ResponseEntity.ok().contentLength(byteArrayOutputStream.size())
                .contentType(MediaType.parseMediaType(mediaType))
                .cacheControl(CacheControl.noCache())
                .header("Content-Disposition", contentDisposition)
                .body(byteArrayOutputStream.toByteArray());
    }

    @GetMapping("/staticReportExport")
    @Operation(summary = "Generate Sales report", description = "Generate Sales report",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity generateLiveStockReportExport(
            @RequestParam(name = "liveDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate liveDate,
            @RequestParam(name = "warehouseId") UUID warehouseId,
            @RequestParam(name = "productName", required = false) String productName,
            @PageableDefault(size = 1000, sort = "productName", direction = Sort.Direction.DESC) Pageable pageable,
            @Parameter(hidden = true) Principal principal) {

        LocalDate reportDate = Objects.requireNonNullElse(liveDate, LocalDate.now());
        ByteArrayOutputStream byteArrayOutputStream = logisticService.generateStaticReportExport(reportDate,
                warehouseId,
                productName,
                pageable,
                StatementExportType.EXCEL,
                principal.getName());
        String contentDisposition = String.format("attachment; filename=\"static report - %s.%s\"",
                LocalDateTime.now().toString(),
                StatementExportType.EXCEL);
        String mediaType = StatementExportType.EXCEL.contentType;
        return ResponseEntity.ok().contentLength(byteArrayOutputStream.size())
                .contentType(MediaType.parseMediaType(mediaType))
                .cacheControl(CacheControl.noCache())
                .header("Content-Disposition", contentDisposition)
                .body(byteArrayOutputStream.toByteArray());
    }

    @GetMapping("/supplierCustomerReportExport")
    @Operation(summary = "Generate Sales report", description = "Generate Sales report",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity generateSupplierCustomerExport(
            @RequestParam(name = "liveDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate liveDate,
            @RequestParam(name = "warehouseId") UUID warehouseId,
            @RequestParam(name = "productName", required = false) String productName,
            @PageableDefault(size = 100, sort = "productName", direction = Sort.Direction.DESC) Pageable pageable,
            @Parameter(hidden = true) Principal principal) {

        LocalDate reportDate = Objects.requireNonNullElse(liveDate, LocalDate.now());
        ByteArrayOutputStream byteArrayOutputStream = logisticService.generateStaticReportExport(reportDate,
                warehouseId,
                productName,
                pageable,
                StatementExportType.EXCEL,
                principal.getName());
        String contentDisposition = String.format("attachment; filename=\"static report - %s.%s\"",
                LocalDateTime.now().toString(),
                StatementExportType.EXCEL);
        String mediaType = StatementExportType.EXCEL.contentType;
        return ResponseEntity.ok().contentLength(byteArrayOutputStream.size())
                .contentType(MediaType.parseMediaType(mediaType))
                .cacheControl(CacheControl.noCache())
                .header("Content-Disposition", contentDisposition)
                .body(byteArrayOutputStream.toByteArray());
    }

    //    @PreAuthorize("hasAuthority('VIEW_SALES_ORDERS_REPORT')")
    @GetMapping("/supplierCustomersReport")
    @Operation(summary = "Generate Sales Orders report", description = "Generate Sales Orders report",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<?> generateSalesOrdersReport(
            @RequestParam(name = "regionId", required = false) UUID regionId,
            @RequestParam(name = "locationId", required = false) UUID locationId,
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(hidden = true) Principal principal,
            @RequestParam(name = "exportType", required = false) StatementExportType exportType,
            @PageableDefault(size = 1000) Pageable pageable) throws IOException {

        if (exportType == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Export type is required")
                    .userMessage("Export type is required");
        }

        ByteArrayOutputStream byteArrayOutputStream = customersService.getAllSalesOrders(regionId, locationId,
                searchParam,
                exportType,
                startDate,
                endDate,
                pageable, principal);
        String contentDisposition = String.format("attachment; filename=\"Supplier Customers Report - %s.%s\"",
                LocalDateTime.now().toString(),
                exportType);
        String mediaType = exportType.contentType;
        return ResponseEntity.ok().contentLength(byteArrayOutputStream.size())
                .contentType(MediaType.parseMediaType(mediaType))
                .cacheControl(CacheControl.noCache())
                .header("Content-Disposition", contentDisposition)
                .body(byteArrayOutputStream.toByteArray());
    }

    @GetMapping("/productPerformanceDateRangeByZone")
    @Operation(summary = "Get Product distribution by zone", description = "Get Product distribution by zone",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public List<ZoneAverageDTO1> productPerformanceDateRangeByZone(
            @RequestParam(name = "locationId", required = false) UUID zoneId,
            @RequestParam(name = "productId", required = false) UUID productId,
            @PageableDefault(size = 1000, sort = "creationTime",
                    direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(hidden = true) Principal principal) {

        return salesOrdersService.productPerformanceDateRangeByZone(productId, zoneId, startDate, endDate, pageable, principal);
    }

    @GetMapping("/productPerformance")
    @Operation(summary = "Get Product Performance", description = "Get Product Performance",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public List<ZoneNameDateAverageProjection> productPerformance(
            @RequestParam(name = "regionId", required = false) UUID regionId,
            @RequestParam(name = "locationId", required = false) UUID locationId,
            @RequestParam(name = "productId", required = false) UUID productId,
            @PageableDefault(size = 1000, sort = "creationTime",
                    direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(hidden = true) Principal principal) {

        return salesOrdersService.productPerformance(productId, regionId, startDate, endDate, locationId, pageable,
                principal);
    }

    @GetMapping("/orderValuePerZone")
    @Operation(summary = "Get Product distribution by zone", description = "Get Product distribution by zone",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public Page<ZoneNameAverageProjection> orderValuePerZone(
            @RequestParam(name = "regionId", required = false) UUID regionId,
            @RequestParam(name = "locationId", required = false) UUID locationId,
            @RequestParam(name = "productId", required = false) UUID productId,
            @PageableDefault(size = 1000, sort = "creationTime",
                    direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(hidden = true) Principal principal) {

        return salesOrdersService.orderValuePerZone(productId, regionId, startDate, endDate, locationId, pageable,
                principal);
    }

    @GetMapping("/{salesOrderPublic}/invoiceReceipt")
    @Operation(summary = "Generate receipt", description = "Generate receipt",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity generateReceipt(
            @PathVariable(name = "salesOrderPublic") UUID salesOrderPublic,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 1000, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) throws IOException,
            DocumentException {

        StatementExportType exportType = StatementExportType.PDF;
        ByteArrayOutputStream byteArrayOutputStream = salesOrdersService.generateInvoice(salesOrderPublic,
                startDate,
                endDate,
                pageable, principal);
        String contentDisposition = String.format("attachment; filename=\"Invoice - %s.%s\"",
                LocalDateTime.now().toString(),
                exportType);
        String mediaType = exportType.contentType;
        return ResponseEntity.ok().contentLength(byteArrayOutputStream.size())
                .contentType(MediaType.parseMediaType(mediaType))
                .cacheControl(CacheControl.noCache())
                .header("Content-Disposition", contentDisposition)
                .body(byteArrayOutputStream.toByteArray());
    }

    @GetMapping("/{salesOrderPublic}/customerInvoiceReceipt")
    @Operation(summary = "Generate customer receipt", description = "Generate customer receipt",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<?> generateCustomerInvoiceReceipt(@PathVariable UUID salesOrderPublic,
                                                            @RequestParam(name = "orderType") OrderType orderType,
                                                            @Parameter(hidden = true) Principal principal) throws IOException, DocumentException {

        if (orderType == OrderType.COMBO) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer invoice receipt generation is not supported for combo orders")
                    .userMessage("Customer invoice receipt generation is not supported for combo orders");
        }

        SalesOrders order = salesOrdersService.getSalesOrder(salesOrderPublic);

        if (order == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Sales order not found for public id: " + salesOrderPublic)
                    .userMessage("Sales order not found");
        }

        if (order.getTraReceiptGenerated() == null || !order.getTraReceiptGenerated()) {
            throw new BadRequestRestApiException()
                    .developerMessage("TRA receipt not generated for sales order: " + salesOrderPublic)
                    .userMessage("TRA receipt not generated for this sales order");
        }

        StatementExportType exportType = StatementExportType.PDF;
        ByteArrayOutputStream byteArrayOutputStream =
                salesOrdersService.generateCustomerInvoiceReceipt(salesOrderPublic, orderType, principal.getName(),
                        order.getSalesOrderNumber());
        String contentDisposition = String.format("attachment; filename=\"Receipt - %s.%s\"",
                LocalDateTime.now(),
                exportType);
        String mediaType = exportType.contentType;
        return ResponseEntity.ok().contentLength(byteArrayOutputStream.size())
                .contentType(MediaType.parseMediaType(mediaType))
                .cacheControl(CacheControl.noCache())
                .header("Content-Disposition", contentDisposition)
                .body(byteArrayOutputStream.toByteArray());
    }

    @GetMapping("/supplierRevenueReport")
    @Operation(summary = "Supplier Revenue Report", description = "Supplier Revenue Report",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public Map<String, BigDecimal> generateSupplierRevenueReport(
            @RequestParam(name = "regionId", required = false) UUID regionId,
            @RequestParam(name = "startDate", required = false) String starDate,
            @RequestParam(name = "endDate", required = false) String enDate,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 100, direction = Sort.Direction.ASC) Pageable pageable) {
        LocalDate startDate = DateFormatter.parseDate(starDate);
        LocalDate endDate = DateFormatter.parseDate(enDate);
        return reportService.runSupplierRevenueReport(
                regionId,
                startDate,
                endDate,
                principal.getName(),
                pageable);
    }

    @GetMapping("/productCustomerPerformanceExport")
    @Operation(summary = "Customer Performance Export", description = "Customer Performance Export",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<?> productCustomerPerformanceExport(
            @RequestParam(name = "productId", required = false) UUID productId,
            @RequestParam(name = "regionId", required = false) UUID regionId,
            @RequestParam(name = "zones", required = false) UUID locationId,
            @RequestParam(name = "startDate", required = false) String starDate,
            @RequestParam(name = "endDate", required = false) String enDate,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 100, direction = Sort.Direction.ASC) Pageable pageable) {

        LocalDate startDate = DateFormatter.parseDate(starDate);
        LocalDate endDate = DateFormatter.parseDate(enDate);
        ByteArrayOutputStream byteArrayOutputStream = reportService.productCustomerPerformanceExport(productId,
                regionId,
                locationId,
                startDate,
                endDate,
                principal.getName(),
                StatementExportType.EXCEL,
                principal.getName(), pageable);
        String contentDisposition = String.format("attachment; filename=\"Customer Performance Report - %s.%s\"",
                LocalDateTime.now().toString(),
                StatementExportType.EXCEL);
        String mediaType = StatementExportType.EXCEL.contentType;
        return ResponseEntity.ok().contentLength(byteArrayOutputStream.size())
                .contentType(MediaType.parseMediaType(mediaType))
                .cacheControl(CacheControl.noCache())
                .header("Content-Disposition", contentDisposition)
                .body(byteArrayOutputStream.toByteArray());
    }

    // loan management excel report

    @GetMapping("/loanManagementReportExport")
    @Operation(summary = "Loan Management Report Export", description = "Loan Management Report Export",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<?> loanManagementReportExport(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) PaymentStatusEnum paymentStatus,
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "dueDate", required = false) String dueDate,
            @RequestParam(name = "customerPublicId", required = false) UUID customerPublicId,
            @RequestParam(name = "parentCustomerPublicId", required = false) UUID parentCustomerPublicId,
            @RequestParam(name = "exportType") StatementExportType exportType,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 1000) Pageable pageable) {
        BnplLoanManagementPageable bnplLoanManagementPageable = bNPLService.getBNPLLoanManagementData(search, customerPublicId, startDate, dueDate, paymentStatus, parentCustomerPublicId, pageable);
        ByteArrayOutputStream byteArrayOutputStream = reportService.generateLoanManagementReportExport(bnplLoanManagementPageable.getContent(),
                exportType, principal.getName());
        String contentDisposition = String.format("attachment; filename=\"Loan Management Report - %s.%s\"",
                LocalDateTime.now().toString(),
                exportType.extension);
        String mediaType = exportType.contentType;
        return ResponseEntity.ok().contentLength(byteArrayOutputStream.size())
                .contentType(MediaType.parseMediaType(mediaType))
                .cacheControl(CacheControl.noCache())
                .header("Content-Disposition", contentDisposition)
                .body(byteArrayOutputStream.toByteArray());
    }

    // bnpl customerListReport

    @GetMapping("/bnplCustomerListReportExport")
    @Operation(summary = "BNPL Customer List Report Export", description = "BNPL Customer List Report Export",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<?> getBNPLCustomerListExport(@RequestParam(name = "search", required = false) String search,
                                                       @RequestParam(name = "creditStatus", required = false) CreditStatus creditStatus,
                                                       @RequestParam(name = "customerPublicId", required = false) UUID customerPublicId,
                                                       @RequestParam(name = "customerType", required = false) CustomerType customerType,
                                                       @RequestParam(name = "regionPublicId", required = false) UUID regionPublicId,
                                                       @RequestParam(name = "exportType") StatementExportType exportType,
                                                       @Parameter(hidden = true) Principal principal,
                                                       @PageableDefault(size = 1000, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {
        BNPLCustomerResponsePageable bnplCustomerResponsePageable = bNPLService.getBNPLCustomers(search, creditStatus, customerPublicId, customerType, regionPublicId, pageable);
        ByteArrayOutputStream byteArrayOutputStream = reportService.generateBNPLCustomerListReportExport(bnplCustomerResponsePageable.getContent(),
                exportType, principal.getName());

        String contentDisposition = String.format("attachment; filename=\"BNPL Customer List Report - %s.%s\"",
                LocalDateTime.now().toString(),
                exportType.extension);
        String mediaType = exportType.contentType;
        return ResponseEntity.ok().contentLength(byteArrayOutputStream.size())
                .contentType(MediaType.parseMediaType(mediaType))
                .cacheControl(CacheControl.noCache())
                .header("Content-Disposition", contentDisposition)
                .body(byteArrayOutputStream.toByteArray());

    }

    // eligible customers for BNPL report

    @GetMapping("/eligibleBNPLCustomers")
    @Operation(summary = "Get eligible BNPL customers", description = "Get eligible BNPL customers",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<?> getEligibleCustomersForBNPLAccountCreation(@RequestParam(name = "search", required = false) String search,
                                                                        @RequestParam(name = "customerPublicId", required = false) UUID customerPublicId,
                                                                        @RequestParam(name = "customerType", required = false) CustomerType customerType,
                                                                        @RequestParam(name = "regionPublicId", required = false) UUID regionPublicId,
                                                                        @RequestParam(name = "exportType") StatementExportType exportType,
                                                                        @Parameter(hidden = true) Principal principal,
                                                                        @PageableDefault(size = 1000) Pageable pageable) {
        BNPLEligibleCustomerPageable bnplEligibleCustomerPageable = bNPLService.getEligibleCustomersForBNPLAccountCreation(search, customerPublicId, customerType, regionPublicId, pageable);
        ByteArrayOutputStream byteArrayOutputStream = reportService.generateBNPLEligibleCustomerListReportExport(bnplEligibleCustomerPageable.getContent(),
                exportType, principal.getName());

        String contentDisposition = String.format("attachment; filename=\"Eligible BNPL Customers Report - %s.%s\"",
                LocalDateTime.now().toString(),
                exportType.extension);
        String mediaType = exportType.contentType;
        return ResponseEntity.ok().contentLength(byteArrayOutputStream.size())
                .contentType(MediaType.parseMediaType(mediaType))
                .cacheControl(CacheControl.noCache())
                .header("Content-Disposition", contentDisposition)
                .body(byteArrayOutputStream.toByteArray());


    }

    @GetMapping("/creditReconReportExport")
    @Operation(summary = "Credit Recon Report Export", description = "Credit Recon Report Export",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<?> creditReconReportExport(@RequestParam(name = "searchParam", required = false) String searchParam,
                                                     @RequestParam(name = "deliveryAgentPublicId", required = false) String deliveryAgentPublicId,
                                                     @RequestParam(name = "salesAgentPublicId", required = false) String salesAgentPublicId,
                                                     @RequestParam(name = "customerPublicId", required = false) String customerPublicId,
                                                     @RequestParam(name = "orderNumber", required = false) String orderNumber,
                                                     @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                     @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                     @RequestParam(name = "exportType") StatementExportType exportType,
                                                     @Parameter(hidden = true) Principal principal,
                                                     @PageableDefault(size = 1000) Pageable pageable) {

        Page<CreditRecon> creditReconPage = reconciliationService.getAllCreditRecons(searchParam, deliveryAgentPublicId, salesAgentPublicId, customerPublicId, orderNumber, from, to, pageable);
        ByteArrayOutputStream byteArrayOutputStream = reportService.generateCreditReconReportExport(creditReconPage.getContent(),
                exportType, principal.getName());

        String contentDisposition = String.format("attachment; filename=\"Credit Recon Report - %s.%s\"",
                LocalDateTime.now().toString(),
                exportType.extension);
        String mediaType = exportType.contentType;
        return ResponseEntity.ok().contentLength(byteArrayOutputStream.size())
                .contentType(MediaType.parseMediaType(mediaType))
                .cacheControl(CacheControl.noCache())
                .header("Content-Disposition", contentDisposition)
                .body(byteArrayOutputStream.toByteArray());
    }

    // credit items report by walletId

    @GetMapping("/customerCreditItemsReportExport")
    @Operation(summary = "Customer Credit Items Report Export", description = "Customer Credit Items Report Export",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<?> customerCreditItemsReportExport(@RequestParam(name = "walletId") UUID walletId,
                                                             @RequestParam(name = "parentCustomerPublicId", required = false) UUID parentCustomerPublicId,
                                                             @RequestParam(name = "exportType") StatementExportType exportType,
                                                             @Parameter(hidden = true) Principal principal,
                                                             @PageableDefault(size = 1000) Pageable pageable) {
        Page<CreditLimitItem> creditLimitItemPage = bNPLService.getBNPLWalletCredits(walletId, parentCustomerPublicId, pageable);

        ByteArrayOutputStream byteArrayOutputStream = reportService.generateCustomerCreditItemsReportExport(creditLimitItemPage.getContent(),
                exportType, principal.getName());

        String contentDisposition = String.format("attachment; filename=\"Customer Credit Items Report - %s.%s\"",
                LocalDateTime.now().toString(),
                exportType.extension);
        String mediaType = exportType.contentType;
        return ResponseEntity.ok().contentLength(byteArrayOutputStream.size())
                .contentType(MediaType.parseMediaType(mediaType))
                .cacheControl(CacheControl.noCache())
                .header("Content-Disposition", contentDisposition)
                .body(byteArrayOutputStream.toByteArray());

    }

    @GetMapping("/creditPaymentReportExport")
    @Operation(summary = "Customer Credit Payment Report Export", description = "Customer Credit Payment Report Export",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<?> creditPaymentReportExport(@RequestParam(name = "search", required = false) String search,
                                                       @RequestParam(name = "customerPublicId", required = false) UUID customerPublicId,
                                                       @RequestParam(name = "parentCustomerPublicId", required = false) UUID parentCustomerPublicId,
                                                       @RequestParam(name = "paymentStatus", required = false) PaymentStatusEnum paymentStatus,
                                                       @RequestParam(name = "batched", required = false) Batched batched,
                                                       @RequestParam(name = "customerType", required = false) CustomerType customerType,
                                                       @RequestParam(name = "regionPublicId", required = false) UUID regionPublicId,
                                                       @RequestParam(name = "startDate", required = false) String startDate,
                                                       @RequestParam(name = "endDate", required = false) String dueDate,
                                                       @RequestParam(name = "exportType") StatementExportType exportType,
                                                       @Parameter(hidden = true) Principal principal,
                                                       @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {
        BNPLPaymentResponsePageable bnplPaymentResponsePageable = bNPLService.getBNPLCreditPaymentsData(search, customerPublicId, paymentStatus, customerType, regionPublicId, startDate, dueDate, parentCustomerPublicId, batched, pageable);
        ByteArrayOutputStream byteArrayOutputStream = reportService.generateCustomerCreditPaymentReportExport(bnplPaymentResponsePageable.getContent(),
                exportType, principal.getName());

        String contentDisposition = String.format("attachment; filename=\"Customer Credit Payment Report - %s.%s\"",
                LocalDateTime.now().toString(),
                exportType.extension);
        String mediaType = exportType.contentType;
        return ResponseEntity.ok().contentLength(byteArrayOutputStream.size())
                .contentType(MediaType.parseMediaType(mediaType))
                .cacheControl(CacheControl.noCache())
                .header("Content-Disposition", contentDisposition)
                .body(byteArrayOutputStream.toByteArray());

    }

    @GetMapping("/bnplSalesReportExport")
    @Operation(summary = "BNPL Sales Report Export", description = "BNPL Sales Report Export",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<?> bnplSalesReportExport(@RequestParam(name = "searchParam", required = false) String searchParam,
                                                   @RequestParam(name = "salesOrderNumber", required = false) String salesOrderNumber,
                                                   @RequestParam(name = "customerId", required = false) UUID customerId,
                                                   @RequestParam(name = "warehouseId", required = false) UUID warehouseId,
                                                   @RequestParam(name = "organizationRegionId", required = false) UUID organizationRegionId,
                                                   @RequestParam(name = "locationId", required = false) UUID locationId,
                                                   @RequestParam(name = "sourceChannel", required = false) SourceChannelEnum sourceChannel,
                                                   @RequestParam(name = "salesOrderStatus", required = false) LoadingStatusEnum salesOrderStatus,
                                                   @RequestParam(name = "statuses", required = false) List<LoadingStatusEnum> statuses,
                                                   @RequestParam(name = "paymentStatus", required = false) PaymentStatusEnum paymentStatus,
                                                   @RequestParam(name = "invoiceNumber", required = false) String invoiceNumber,
                                                   @RequestParam(name = "bookedBySalesAgent", required = false) Boolean bookedBySalesAgent,
                                                   @RequestParam(name = "salesAgentPublicId", required = false) UUID salesAgentPublicId,
                                                   @RequestParam(name = "deliveryAgentPublicId", required = false) UUID deliveryAgentPublicId,
                                                   @RequestParam(name = "parentCustomerPublicId", required = false) UUID parentCustomerPublicId,
                                                   @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                   @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                   @RequestParam(name = "isWalkIn", required = false) Boolean isWalkIn,
                                                   @RequestParam(name = "bookedByParent", required = false) Boolean bookedByParent,
                                                   @Parameter(hidden = true) Principal principal,
                                                   @RequestParam(name = "exportType", required = false) StatementExportType exportType,
                                                   @PageableDefault(size = 1000, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) throws IOException, DocumentException {

        SalesOrdersPageableDTO salesOrders = salesOrdersService.getAllSalesOrders(searchParam, salesOrderNumber, customerId, warehouseId, organizationRegionId,
                sourceChannel, salesOrderStatus, paymentStatus, invoiceNumber, startDate, endDate, locationId, bookedBySalesAgent, salesAgentPublicId, deliveryAgentPublicId, parentCustomerPublicId,
                Boolean.TRUE, statuses, isWalkIn, bookedByParent, pageable);
        if (exportType == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Export type is required")
                    .userMessage("Export type is required");
        }

        ByteArrayOutputStream byteArrayOutputStream = reportService.generateBnplSalesOrdersReport(exportType, salesOrders.getContent(), principal.getName());
        String contentDisposition = String.format("attachment; filename=\"Sales Orders Report - %s.%s\"",
                LocalDateTime.now().toString(),
                exportType);
        String mediaType = exportType.contentType;
        return ResponseEntity.ok().contentLength(byteArrayOutputStream.size())
                .contentType(MediaType.parseMediaType(mediaType))
                .cacheControl(CacheControl.noCache())
                .header("Content-Disposition", contentDisposition)
                .body(byteArrayOutputStream.toByteArray());
    }

    // wallet report


    @GetMapping("/walletReportExport")
    @Operation(summary = "Wallet Report Export", description = "Wallet Report Export",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})

    public ResponseEntity<?> getWalletReportExport(@RequestParam(name = "search", required = false) String search,
                                                   @RequestParam(name = "customerType", required = false) CustomerType customerType,
                                                   @RequestParam(name = "customerPublicId", required = false) UUID customerPublicId,
                                                   @RequestParam(name = "startDate", required = false) String startDate,
                                                   @RequestParam(name = "dueDate", required = false) String dueDate,
                                                   @RequestParam(name = "creditStatus", required = false) CreditStatus creditStatus,
                                                   @RequestParam(name = "exportType") StatementExportType exportType,
                                                   @Parameter(hidden = true) Principal principal,
                                                   @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {


        Page<WalletResponseDTO> walletPage = bNPLService.getBNPLWallets(search, customerType, customerPublicId, startDate, dueDate, creditStatus, pageable);


        if (exportType == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Export type is required")
                    .userMessage("Export type is required");
        }

        ByteArrayOutputStream byteArrayOutputStream = reportService.generateWalletReportExport(walletPage.getContent(), exportType, principal.getName());

        String contentDisposition = String.format("attachment; filename=\"Wallet Report - %s.%s\"",
                LocalDateTime.now().toString(),
                exportType);

        String mediaType = exportType.contentType;

        return ResponseEntity.ok().contentLength(byteArrayOutputStream.size())
                .contentType(MediaType.parseMediaType(mediaType))
                .cacheControl(CacheControl.noCache())
                .header("Content-Disposition", contentDisposition)
                .body(byteArrayOutputStream.toByteArray());
    }

    @GetMapping("/{customerPublicId}/customerMostBoughtProductReport")
    @Operation(summary = "Most bought product Report", description = "Most bought product Report",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public List<BestProductBoughtDto> getCustomerMostBoughtProductReport(
            @PathVariable(name = "customerPublicId") UUID customerPublicId,
            @Parameter(hidden = true) Principal principal) {
        return reportService.getCustomerMostBoughtProductReport(
                customerPublicId,
                principal.getName());
    }

    // locationApproval export

    @JsonView(BaseView.LocationApprovalView.class)
    @GetMapping("/locationApprovalsReport")
    @Operation(summary = "Fetch location approval data for export", description = "Fetch location approval data for export",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public ResponseEntity<?> getLocationApprovalsExport(@RequestParam(name = "searchParam", required = false) String searchParam,
                                                        @RequestParam(name = "statuses", required = false) List<LocationApprovalStatus> statuses,
                                                        @RequestParam(name = "deliveryAgent", required = false) String deliveryAgent,
                                                        @RequestParam(name = "customer", required = false) String customer,
                                                        @RequestParam(name = "from", required = false) String from,
                                                        @RequestParam(name = "to", required = false) String to,
                                                        @RequestParam(name = "exportType") StatementExportType exportType,
                                                        @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable,
                                                        @Parameter(hidden = true) Principal principal) {
        Page<LocationApproval> locationApprovalPage = deliveryAgentsService.getLocationApprovals(searchParam, statuses, deliveryAgent, customer, from, to, pageable);

        ByteArrayOutputStream byteArrayOutputStream = reportService.generateLocationApprovalExport(locationApprovalPage.getContent(), exportType, principal.getName());

        String contentDisposition = String.format("attachment; filename=\"Location Approvals Report - %s.%s\"",
                LocalDateTime.now().toString(),
                exportType.extension);
        String mediaType = exportType.contentType;
        return ResponseEntity.ok().contentLength(byteArrayOutputStream.size())
                .contentType(MediaType.parseMediaType(mediaType))
                .cacheControl(CacheControl.noCache())
                .header("Content-Disposition", contentDisposition)
                .body(byteArrayOutputStream.toByteArray());

    }


    @GetMapping("/exportChurnedCustomers")
    @Operation(summary = "Churned Customer Export", description = "Churned Customer Export",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<?> runExportChurnedCustomers(
            @RequestParam(name = "rangeFilter", required = false) ChurnedDateRangeFilter rangeFilter,
            @RequestParam(name = "customerType", required = false) CustomerType customerType,
            @RequestParam(name = "callStatusPublicId", required = false) UUID callStatusPublicId,
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(name = "departmentPublicId", required = false) UUID departmentPublicId,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(hidden = true) Principal principal,
            @RequestParam(name = "exportType") StatementExportType exportType,
            @RequestParam(name = "callStatus", required = false) CustomerOutboundCallStatus callStatus,
            @RequestParam(name = "leadStatus", required = false) ChurnedCustomerLeadStatus leadStatus,
            @PageableDefault(size = 1000, direction = Sort.Direction.ASC) Pageable pageable) throws DocumentException, java.io.IOException {

        ByteArrayOutputStream byteArrayOutputStream = customersService.runExportChurnedCustomers(rangeFilter,
                customerType,
                callStatusPublicId,
                principal.getName(),
                exportType,
                startDate,
                endDate,
                searchParam,
                callStatus,
                departmentPublicId,
                leadStatus,
                pageable);
        String contentDisposition = String.format("attachment; filename=\"Churned Customer Report - %s.%s\"",
                LocalDateTime.now().toString(),
                StatementExportType.EXCEL);
        String mediaType = StatementExportType.EXCEL.contentType;
        return ResponseEntity.ok().contentLength(byteArrayOutputStream.size())
                .contentType(MediaType.parseMediaType(mediaType))
                .cacheControl(CacheControl.noCache())
                .header("Content-Disposition", contentDisposition)
                .body(byteArrayOutputStream.toByteArray());
    }

    @GetMapping("/exportNonChurnedInActiveCustomers")
    @Operation(summary = "Churned Customer Export", description = "Churned Customer Export",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<?> runExportNonChurnedInActiveCustomers(
            @RequestParam(name = "customerType", required = false) CustomerType customerType,
            @RequestParam(name = "callStatusPublicId", required = false) UUID callStatusPublicId,
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(hidden = true) Principal principal,
            @RequestParam(name = "inActive", required = false) Boolean inActive,
            @RequestParam(name = "departmentPublicId", required = false) UUID departmentPublicId,
            @RequestParam(name = "exportType") StatementExportType exportType,
            @RequestParam(name = "callStatus", required = false) CustomerOutboundCallStatus callStatus,
            @PageableDefault(size = 1000, direction = Sort.Direction.ASC) Pageable pageable) throws DocumentException, java.io.IOException {

        ByteArrayOutputStream byteArrayOutputStream = customersService.runExportNonChurnedInActiveCustomers(inActive,
                customerType,
                callStatusPublicId,
                principal.getName(),
                exportType,
                startDate,
                endDate,
                searchParam,
                callStatus,
                departmentPublicId,
                pageable);
        String contentDisposition = String.format("attachment; filename=\"Non Churned Customer Report - %s.%s\"",
                LocalDateTime.now().toString(),
                StatementExportType.EXCEL);
        String mediaType = StatementExportType.EXCEL.contentType;
        return ResponseEntity.ok().contentLength(byteArrayOutputStream.size())
                .contentType(MediaType.parseMediaType(mediaType))
                .cacheControl(CacheControl.noCache())
                .header("Content-Disposition", contentDisposition)
                .body(byteArrayOutputStream.toByteArray());
    }

    @GetMapping("/helpTickets")
    @Operation(summary = "Ticket Export", description = "Ticket Export",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<?> runExportChurnedCustomers(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(name = "status", required = false) String helpDeskStatus,
            @RequestParam(name = "priority", required = false) TicketPriority ticketPriority,
            @RequestParam(name = "customerType", required = false) TicketDto.CustomerType customerType,
            @RequestParam(name = "customerPublicId", required = false) UUID customerPublicId,
            @RequestParam(name = "departmentPublicId", required = false) UUID departmentPublicId,
            @RequestParam(name = "existingCustomerPublicId", required = false) UUID existingCustomerPublicId,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "exportType") StatementExportType exportType,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 1000, direction = Sort.Direction.ASC) Pageable pageable) throws DocumentException, java.io.IOException {

        ByteArrayOutputStream byteArrayOutputStream = helpDeskService.runExportTickets(
                searchParam, helpDeskStatus, ticketPriority,
                startDate, endDate, customerPublicId, existingCustomerPublicId, departmentPublicId, customerType,
                exportType, pageable, principal);
        String contentDisposition = String.format("attachment; filename=\"Ticket Report - %s.%s\"",
                LocalDateTime.now().toString(),
                StatementExportType.EXCEL);
        String mediaType = StatementExportType.EXCEL.contentType;
        return ResponseEntity.ok().contentLength(byteArrayOutputStream.size())
                .contentType(MediaType.parseMediaType(mediaType))
                .cacheControl(CacheControl.noCache())
                .header("Content-Disposition", contentDisposition)
                .body(byteArrayOutputStream.toByteArray());
    }

    // lead export

    @GetMapping("/leadExport")
    @Operation(summary = "Lead Export", description = "Lead Export",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<?> getLeads(@RequestParam(name = "search", required = false) String search,
                                      @RequestParam(name = "funnelStage", required = false) BnplFunnelStage funnelStage,
                                      @RequestParam(name = "leadStatus", required = false) BnplLeadStatus leadStatus,
                                      @RequestParam(name = "callStatus", required = false) UUID callStatus,
                                      @RequestParam(name = "districtId", required = false) UUID districtId,
                                      @RequestParam(name = "wardId", required = false) UUID wardId,
                                      @RequestParam(name = "vipOverOneMillion", required = false) Boolean vipOverOneMillion,
                                      @RequestParam(name = "exportType", required = false) StatementExportType exportType,
                                      @Parameter(hidden = true) Principal principal,
                                      @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {
        Pageable exportPageable = PageRequest.of(0, Integer.MAX_VALUE, pageable.getSort());
        BnplLeadPageable bnplLeadPageable = bnplCrmService.getQualifiedLeads(search, funnelStage, leadStatus, callStatus, districtId, wardId, vipOverOneMillion, exportPageable);

        ByteArrayOutputStream byteArrayOutputStream = reportService.getLeadExportReport(bnplLeadPageable.getContent(), exportType, principal.getName());
        String contentDisposition = String.format("attachment; filename=\"Lead Report - %s.%s\"",
                LocalDateTime.now().toString(),
                exportType.extension);
        String mediaType = exportType.contentType;
        return ResponseEntity.ok().contentLength(byteArrayOutputStream.size())
                .contentType(MediaType.parseMediaType(mediaType))
                .cacheControl(CacheControl.noCache())
                .header("Content-Disposition", contentDisposition)
                .body(byteArrayOutputStream.toByteArray());

    }

    // @PreAuthorize("hasAuthority('FINANCE_GET_RECONCILIATION')")
    @GetMapping("/cash-credit-reports/{publicId}/download")
    @Operation(summary = "Download cash and credit reconciliation report")
    public ResponseEntity<byte[]> downloadCashCreditReconReport(
            @PathVariable("publicId") UUID publicId,
            @RequestParam(name = "exportType") StatementExportType exportType,
            @Parameter(hidden = true) Principal principal) {

        CashCreditReconReportDTO cashCreditReconReportDTO = reconciliationService.getCashCreditReconReport(publicId);
        ByteArrayOutputStream outputStream = reportService.downloadCashCreditReconReport(
                cashCreditReconReportDTO, exportType, principal.getName());
        String contentDisposition = String.format(
                "attachment; filename=\"Cash Credit Reconciliation Report - %s%s\"",
                LocalDateTime.now(), exportType.extension);

        return ResponseEntity.ok()
                .contentLength(outputStream.size())
                .contentType(MediaType.parseMediaType(exportType.contentType))
                .cacheControl(CacheControl.noCache())
                .header("Content-Disposition", contentDisposition)
                .body(outputStream.toByteArray());
    }

    @GetMapping("/get-customer-return-export")
    @Operation(summary = "Customer Return Export", description = "Customer Return Export",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<?> runExportChurnedCustomers(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(name = "deliveryAgentPublicId", required = false) UUID deliveryAgentPublicId,
            @RequestParam(name = "status", required = false) CustomerReturnStatus status,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "exportType") StatementExportType exportType,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 1000, direction = Sort.Direction.ASC) Pageable pageable) throws DocumentException, java.io.IOException {

        ByteArrayOutputStream byteArrayOutputStream = salesOrdersService.runExportCustomerReturns(
                searchParam, deliveryAgentPublicId, status,
                startDate, endDate,
                exportType, pageable, principal);
        String contentDisposition = String.format("attachment; filename=\"customer return Report - %s.%s\"",
                LocalDateTime.now().toString(),
                exportType.extension);
        String mediaType = exportType.contentType;
        return ResponseEntity.ok().contentLength(byteArrayOutputStream.size())
                .contentType(MediaType.parseMediaType(mediaType))
                .cacheControl(CacheControl.noCache())
                .header("Content-Disposition", contentDisposition)
                .body(byteArrayOutputStream.toByteArray());
    }

    @GetMapping("/get-disposed-quarantine-product-export")
    @Operation(summary = "Disposed quarantine products Export", description = "Disposed quarantine products",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<?> runExportQuarantineDisposedProducts(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "exportType") StatementExportType exportType,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 1000, direction = Sort.Direction.ASC) Pageable pageable) throws DocumentException, java.io.IOException {

        ByteArrayOutputStream byteArrayOutputStream = quarantineService.runExportQuarantineDisposedProducts(
                searchParam, startDate, endDate,
                exportType, pageable, principal);
        String contentDisposition = String.format("attachment; filename=\"quarantine disposed product Report - %s.%s\"",
                LocalDateTime.now().toString(),
                exportType.extension);
        String mediaType = exportType.contentType;
        return ResponseEntity.ok().contentLength(byteArrayOutputStream.size())
                .contentType(MediaType.parseMediaType(mediaType))
                .cacheControl(CacheControl.noCache())
                .header("Content-Disposition", contentDisposition)
                .body(byteArrayOutputStream.toByteArray());
    }

    @GetMapping("/get-order-amendments-export")
    @Operation(summary = "Order amendment Export", description = "Order amendment Export",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<?> runExportOrderAmendment(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(name = "exportType") StatementExportType exportType,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) throws DocumentException, java.io.IOException {

        ByteArrayOutputStream byteArrayOutputStream = deliveryAgentsService.runExportOrderAmendment(
                searchParam, exportType, pageable, principal);
        String contentDisposition = String.format("attachment; filename=\"order amendment Report - %s.%s\"",
                LocalDateTime.now().toString(),
                exportType.extension);
        String mediaType = exportType.contentType;
        return ResponseEntity.ok().contentLength(byteArrayOutputStream.size())
                .contentType(MediaType.parseMediaType(mediaType))
                .cacheControl(CacheControl.noCache())
                .header("Content-Disposition", contentDisposition)
                .body(byteArrayOutputStream.toByteArray());
    }

    @GetMapping("/salesAgentOrders/{salesAgentPublicId}/export")
    @Operation(
            summary = "Get all Sales Orders for SA export",
            description = "Get all Sales Orders for SA export",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public ResponseEntity<?> getAllSalesOrdersForSAExport(@PathVariable String salesAgentPublicId,
                                                          @RequestParam(name = "searchParam", required = false) String searchParam,
                                                          @RequestParam(name = "salesOrderNumber", required = false) String salesOrderNumber,
                                                          @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                          @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                          @PageableDefault(size = 50, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {
        SalesAgentOrderPageable salesAgentOrderPageable = salesOrdersService.getAllSalesOrdersForSA(searchParam, salesOrderNumber, startDate, endDate, pageable, salesAgentPublicId);
        StatementExportType exportType = StatementExportType.PDF;
        ByteArrayOutputStream byteArrayOutputStream = reportService.getAllSalesOrdersForSAExport(salesAgentOrderPageable, exportType);
        String contentDisposition = String.format("attachment; filename=\"sales order Report - %s.%s\"",
                LocalDateTime.now().toString(),
                exportType.extension);
        String mediaType = exportType.contentType;
        return ResponseEntity.ok().contentLength(byteArrayOutputStream.size())
                .contentType(MediaType.parseMediaType(mediaType))
                .cacheControl(CacheControl.noCache())
                .header("Content-Disposition", contentDisposition)
                .body(byteArrayOutputStream.toByteArray());
    }


    @GetMapping("/creditLimitAdjustments")
    @Operation(summary = "Export BNPL credit limit adjustments as Excel or PDF",
            description = "Exports the requested page. Filters use inclusive creation dates in East Africa Time, "
                    + "customer public ID and customer name, store name or phone search.",
            responses = {@ApiResponse(responseCode = "200", description = "Credit limit adjustments report")})
    @PageableAsQueryParam
    public ResponseEntity<byte[]> exportCreditLimitAdjustments(@RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                               @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                               @RequestParam(name = "customerPublicId", required = false) UUID customerPublicId,
                                                               @RequestParam(name = "searchParam", required = false) String searchParam,
                                                               @RequestParam(name = "exportType") StatementExportType exportType,
                                                               @Parameter(hidden = true) Principal principal,
                                                               @PageableDefault(size = 1000, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<BnplCreditLimitAdjustmentResponse> adjustments = creditLimitAdjustmentService.fetchAdjustments(
                startDate, endDate, customerPublicId, searchParam, pageable);
        ByteArrayOutputStream output = reportService.generateCreditLimitAdjustmentsReportExport(
                adjustments.getContent(), exportType, principal.getName());
        String contentDisposition = String.format("attachment; filename=\"Credit Limit Adjustments Report - %s%s\"",
                LocalDateTime.now(), exportType.extension);
        return ResponseEntity.ok().contentLength(output.size())
                .contentType(MediaType.parseMediaType(exportType.contentType))
                .cacheControl(CacheControl.noCache())
                .header("Content-Disposition", contentDisposition)
                .body(output.toByteArray());
    }

}
