package com.app.itibari.utils.reportHelper;

import com.app.itibari.dataTypes.StatementExportType;
import com.app.itibari.dataTypes.SupplierPortalCustomerAnalysisView;
import com.app.itibari.dataTypes.SupplierPortalProductAnalysisView;
import com.app.itibari.models.entities.Person;
import com.app.itibari.models.entities.Suppliers;
import com.app.itibari.models.report.*;
import com.app.itibari.repository.PersonRepository;
import com.app.itibari.repository.SupplierRepository;
import com.app.itibari.services.SupplierPortalCustomerAnalysisService;
import com.app.itibari.services.SupplierPortalProductAnalysisService;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.helpers.ExcelUtils;
import com.app.itibari.utils.helpers.PdfUtil;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * Builds Excel and PDF exports for Supplier Portal Product Analysis and Customer Analysis V2 views.
 * <p>Fetches data via the existing V2 service methods with an export-only PageRequest cap
 * ({@value #EXPORT_MAX} rows), then formats as a titled tabular document.</p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SupplierPortalAnalysisExportHelper {

    /**
     * Hard cap on exported rows for paged views (ward-performance, product-ranking-cogs, customers).
     */
    public static final int EXPORT_MAX = 2000;
    /**
     * Default limit for district ranking/COGS exports when the caller omits limit.
     * Matches the validation class maximum so the export retrieves all ranked rows, not just the UI default of 10.
     */
    private static final int MAX_DISTRICT_EXPORT_LIMIT = 100;

    private static final ZoneId EAT = ZoneId.of("Africa/Dar_es_Salaam");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,##0.00");
    private static final DecimalFormat TONNES_FMT = new DecimalFormat("#,##0.0000");
    private static final DecimalFormat TREND_FMT = new DecimalFormat("#,##0.00");
    private static final BaseColor HEADER_GREEN = new BaseColor(83, 112, 76);

    private final ExcelUtils excelUtils;
    private final PersonRepository personRepository;
    private final SupplierRepository supplierRepository;
    private final SupplierPortalProductAnalysisService productAnalysisService;
    private final SupplierPortalCustomerAnalysisService customerAnalysisService;

    // ── Public API ───────────────────────────────────────────────────────────

    public Suppliers resolveSupplier(Principal principal) {
        Person person = personRepository.findByEmailAndDeletedIsFalse(principal.getName())
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("User not found").userMessage("User not found"));
        String supplierId = Optional.ofNullable(person.getSupplierPublicId())
                .filter(id -> !id.isBlank())
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("No associated supplier").userMessage("No associated supplier"));
        return supplierRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(supplierId))
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Supplier not found").userMessage("Supplier not found"));
    }

    public ByteArrayOutputStream exportProductAnalysis(SupplierPortalProductAnalysisView view,
                                                       StatementExportType exportType,
                                                       UUID productId,
                                                       List<UUID> regionIds,
                                                       List<UUID> locationIds,
                                                       LocalDate startDate,
                                                       LocalDate endDate,
                                                       Integer limit,
                                                       Suppliers supplier,
                                                       Principal principal) {
        String dateContext = dateRangeLabel(startDate, endDate);
        List<String> headers;
        List<List<String>> rows;

        // For district views, default null limit to MAX_DISTRICT_EXPORT_LIMIT so exports don't silently cap at 10.
        int effectiveDistrictLimit = (limit != null) ? limit : MAX_DISTRICT_EXPORT_LIMIT;

        switch (view) {
            case WARD_PERFORMANCE -> {
                Page<ProductAnalysisWardPerformanceRowDTO> page = productAnalysisService.getWardPerformanceForExport(
                        productId, regionIds, locationIds, startDate, endDate, principal,
                        PageRequest.of(0, EXPORT_MAX));
                headers = Arrays.asList("Rank", "Ward / Location", "COGS (TZS)", "Settled Tonnes",
                        "Customers", "Orders", "AOV (TZS)", "Tonnes Trend %");
                rows = toRowsWardPerformance(page.getContent());
            }
            case ALL_PRODUCTS_WARD_PERFORMANCE -> {
                Page<ProductAnalysisWardPerformanceRowDTO> page = productAnalysisService.getAllProductsWardPerformanceForExport(
                        regionIds, locationIds,
                        startDate, endDate, principal, PageRequest.of(0, EXPORT_MAX));
                headers = Arrays.asList("Rank", "Ward / Location", "COGS (TZS)", "Settled Tonnes",
                        "Customers", "Orders", "AOV (TZS)", "Tonnes Trend %");
                rows = toRowsWardPerformance(page.getContent());
            }
            case ALL_PRODUCTS_PRODUCT_RANKING_COGS -> {
                Page<ProductAnalysisProductRankingCogsRowV2DTO> page = productAnalysisService.getAllProductsProductRankingCogsForExport(
                        regionIds, locationIds,
                        startDate, endDate, principal, PageRequest.of(0, EXPORT_MAX));
                headers = Arrays.asList("Rank", "Product", "Total Units Sold", "COGS (TZS)", "COGS Trend %");
                rows = toRowsProductRankingCogs(page.getContent());
            }
            case DISTRICT_RANKING -> {
                ProductAnalysisDistrictRankingV2DTO data = productAnalysisService.getDistrictRanking(
                        productId, regionIds, locationIds, startDate, endDate, effectiveDistrictLimit, principal);
                headers = Arrays.asList("Rank", "Ward / Location", "Settled Tonnes", "COGS (TZS)", "Orders", "AOV (TZS)");
                rows = toRowsDistrictRanking(data.getRows());
            }
            case ALL_PRODUCTS_DISTRICT_RANKING -> {
                ProductAnalysisDistrictRankingV2DTO data = productAnalysisService.getAllProductsDistrictRanking(
                        regionIds, locationIds,
                        startDate, endDate, effectiveDistrictLimit, principal);
                headers = Arrays.asList("Rank", "Ward / Location", "Settled Tonnes", "COGS (TZS)", "Orders", "AOV (TZS)");
                rows = toRowsDistrictRanking(data.getRows());
            }
            case DISTRICT_COGS -> {
                ProductAnalysisDistrictCogsV2DTO data = productAnalysisService.getDistrictCogs(
                        productId, regionIds, locationIds, startDate, endDate, effectiveDistrictLimit, principal);
                headers = Arrays.asList("Rank", "Ward / Location", "COGS (TZS)", "COGS Trend %");
                rows = toRowsDistrictCogs(data.getRows());
            }
            case ALL_PRODUCTS_DISTRICT_COGS -> {
                ProductAnalysisDistrictCogsV2DTO data = productAnalysisService.getAllProductsDistrictCogs(
                        regionIds, locationIds,
                        startDate, endDate, effectiveDistrictLimit, principal);
                headers = Arrays.asList("Rank", "Ward / Location", "COGS (TZS)", "COGS Trend %");
                rows = toRowsDistrictCogs(data.getRows());
            }
            default -> throw new BadRequestRestApiException()
                    .developerMessage("Unsupported view: " + view)
                    .userMessage("Export not available for this view");
        }

        String exportedBy = resolveExporterEmail(principal);
        return exportType == StatementExportType.PDF
                ? buildPdf(supplier, view.displayName, dateContext, headers, rows, exportedBy)
                : buildExcel(supplier, view.displayName, dateContext, headers, rows, exportedBy);
    }

    public ByteArrayOutputStream exportCustomerAnalysis(SupplierPortalCustomerAnalysisView view,
                                                        StatementExportType exportType,
                                                        UUID regionId,
                                                        UUID locationId,
                                                        List<UUID> productIds,
                                                        LocalDate startDate,
                                                        LocalDate endDate,
                                                        Integer limit,
                                                        String search,
                                                        Suppliers supplier,
                                                        Principal principal) {
        String dateContext = dateRangeLabel(startDate, endDate);
        List<String> headers;
        List<List<String>> rows;

        int effectiveCustomerDistrictLimit = (limit != null) ? limit : MAX_DISTRICT_EXPORT_LIMIT;
        List<UUID> products = productIds != null ? productIds : List.of();

        switch (view) {
            case CUSTOMERS -> {
                // Uncapped per-product/order export — page size arg is ignored by getCustomersForExport.
                Page<CustomerAnalysisCustomerRowV2DTO> page = customerAnalysisService.getCustomersForExport(
                        regionId, locationId, products, startDate, endDate, search, principal, PageRequest.of(0, 1));
                headers = Arrays.asList("Shop Name", "Product Name", "Quantity", "Order Date",
                        "Region", "Ward / Location", "Orders", "COGS (TZS)", "Purchase Tonnes");
                rows = toRowsCustomers(page.getContent());
            }
            case DISTRICT_RANKING -> {
                CustomerAnalysisDistrictRankingV2DTO data = customerAnalysisService.getDistrictRanking(
                        optionalUuidList(regionId), optionalUuidList(locationId), products,
                        startDate, endDate, effectiveCustomerDistrictLimit, principal);
                headers = Arrays.asList("Rank", "Ward / Location", "Distinct Customers", "Orders", "AOV (TZS)");
                rows = toRowsCustomerDistrictRanking(data.getRows());
            }
            case DISTRICT_COGS -> {
                CustomerAnalysisDistrictCogsV2DTO data = customerAnalysisService.getDistrictCogs(
                        optionalUuidList(regionId), optionalUuidList(locationId), products,
                        startDate, endDate, effectiveCustomerDistrictLimit, principal);
                headers = Arrays.asList("Ward / Location", "COGS (TZS)");
                rows = toRowsCustomerDistrictCogs(data.getRows());
            }
            default -> throw new BadRequestRestApiException()
                    .developerMessage("Unsupported view: " + view)
                    .userMessage("Export not available for this view");
        }

        String exportedBy = resolveExporterEmail(principal);
        return exportType == StatementExportType.PDF
                ? buildPdf(supplier, view.displayName, dateContext, headers, rows, exportedBy)
                : buildExcel(supplier, view.displayName, dateContext, headers, rows, exportedBy);
    }

    private String resolveExporterEmail(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return "Unknown";
        }
        return personRepository.findByEmailAndDeletedIsFalse(principal.getName())
                .map(Person::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .orElse(principal.getName());
    }

    private List<UUID> optionalUuidList(UUID id) {
        return id == null ? List.of() : List.of(id);
    }

    // ── Row mappers ──────────────────────────────────────────────────────────

    private List<List<String>> toRowsWardPerformance(List<ProductAnalysisWardPerformanceRowDTO> data) {
        List<List<String>> rows = new ArrayList<>();
        for (ProductAnalysisWardPerformanceRowDTO r : data) {
            Double tonnesTrend = r.getPeriodTrends() != null ? r.getPeriodTrends().getSettledTonnesTrendPercentage() : null;
            rows.add(Arrays.asList(
                    String.valueOf(r.getRank()),
                    str(r.getOrganizationLocationName()),
                    money(r.getTotalCogsTzs()),
                    tonnes(r.getSettledTonnes()),
                    String.valueOf(r.getDistinctCustomerCount()),
                    String.valueOf(r.getDeliveredOrderCount()),
                    money(r.getAverageOrderValueTzs()),
                    trend(tonnesTrend)
            ));
        }
        return rows;
    }

    private List<List<String>> toRowsProductRankingCogs(List<ProductAnalysisProductRankingCogsRowV2DTO> data) {
        List<List<String>> rows = new ArrayList<>();
        for (ProductAnalysisProductRankingCogsRowV2DTO r : data) {
            Double cogsTrend = r.getPeriodTrends() != null ? r.getPeriodTrends().getTotalCogsTzsTrendPercentage() : null;
            rows.add(Arrays.asList(
                    String.valueOf(r.getRank()),
                    str(r.getProductName()),
                    units(r.getTotalQuantitySoldUnits()),
                    money(r.getTotalCogsTzs()),
                    trend(cogsTrend)
            ));
        }
        return rows;
    }

    private List<List<String>> toRowsDistrictRanking(List<ProductAnalysisDistrictRankingRowDTO> data) {
        List<List<String>> rows = new ArrayList<>();
        for (ProductAnalysisDistrictRankingRowDTO r : data) {
            rows.add(Arrays.asList(
                    String.valueOf(r.getRank()),
                    str(r.getOrganizationLocationName()),
                    tonnes(r.getSettledTonnes()),
                    money(r.getTotalCogsTzs()),
                    String.valueOf(r.getDeliveredOrderCount()),
                    money(r.getAverageOrderValueTzs())
            ));
        }
        return rows;
    }

    private List<List<String>> toRowsDistrictCogs(List<ProductAnalysisDistrictCogsRowDTO> data) {
        List<List<String>> rows = new ArrayList<>();
        int rank = 1;
        for (ProductAnalysisDistrictCogsRowDTO r : data) {
            Double cogsTrend = r.getPeriodTrends() != null ? r.getPeriodTrends().getTotalCogsTzsTrendPercentage() : null;
            rows.add(Arrays.asList(
                    String.valueOf(rank++),
                    str(r.getOrganizationLocationName()),
                    money(r.getTotalCogsTzs()),
                    trend(cogsTrend)
            ));
        }
        return rows;
    }

    private List<List<String>> toRowsCustomers(List<CustomerAnalysisCustomerRowV2DTO> data) {
        List<List<String>> rows = new ArrayList<>();
        for (CustomerAnalysisCustomerRowV2DTO r : data) {
            rows.add(Arrays.asList(
                    str(r.getShopName()),
                    str(r.getProductNames()),
                    str(r.getProductPurchaseQuantities()),
                    r.getLastOrderDate() != null ? DATE_FMT.format(r.getLastOrderDate()) : "—",
                    str(r.getRegionName()),
                    str(r.getOrganizationLocationName()),
                    String.valueOf(r.getDeliveredOrderCount()),
                    money(r.getTotalCogsTzs()),
                    tonnes(r.getSettledTonnes())
            ));
        }
        return rows;
    }

    private List<List<String>> toRowsCustomerDistrictRanking(List<CustomerAnalysisDistrictRankingRowV2DTO> data) {
        List<List<String>> rows = new ArrayList<>();
        for (CustomerAnalysisDistrictRankingRowV2DTO r : data) {
            rows.add(Arrays.asList(
                    String.valueOf(r.getRank()),
                    str(r.getOrganizationLocationName()),
                    String.valueOf(r.getDistinctCustomerCount()),
                    String.valueOf(r.getDeliveredOrderCount()),
                    money(r.getAverageOrderValueTzs())
            ));
        }
        return rows;
    }

    private List<List<String>> toRowsCustomerDistrictCogs(List<CustomerAnalysisDistrictCogsRowV2DTO> data) {
        List<List<String>> rows = new ArrayList<>();
        for (CustomerAnalysisDistrictCogsRowV2DTO r : data) {
            rows.add(Arrays.asList(
                    str(r.getOrganizationLocationName()),
                    money(r.getTotalCogsTzs())
            ));
        }
        return rows;
    }

    // ── Excel builder ────────────────────────────────────────────────────────

    private ByteArrayOutputStream buildExcel(Suppliers supplier, String viewTitle, String dateContext,
                                             List<String> headers, List<List<String>> rows, String exportedBy) {
        try {
            String supplierName = supplier.getSupplierName() != null ? supplier.getSupplierName() : "Supplier";
            String sheetTitle = "CUSTOMER ANALYSIS REPORT";

            XSSFWorkbook workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet(viewTitle);

            org.apache.poi.ss.usermodel.Font headerFont = excelUtils.getHeaderFont(workbook);
            org.apache.poi.ss.usermodel.Font contentFont = excelUtils.getContentFont(workbook);
            CellStyle headerStyle = excelUtils.getHeaderCellStyle(workbook, headerFont);
            CellStyle centreStyle = excelUtils.getStringDataCellStyleCentreAlign(workbook, contentFont);
            CellStyle leftStyle = excelUtils.getStringDataCellStyleLeftAlign(workbook, contentFont);
            CellStyle rightStyle = excelUtils.getNumericDataCellStyleRightAlign(workbook, contentFont);

            XSSFColor reportColor = new XSSFColor(new byte[]{
                    (byte) HEADER_GREEN.getRed(), (byte) HEADER_GREEN.getGreen(), (byte) HEADER_GREEN.getBlue()
            }, null);
            XSSFFont titleFont = workbook.createFont();
            titleFont.setFontName(FontFactory.HELVETICA);
            titleFont.setFontHeightInPoints((short) 14);
            titleFont.setBold(true);
            titleFont.setColor(reportColor);
            CellStyle titleStyle = excelUtils.getStringDataCellStyleCentreAlign(workbook, titleFont);

            XSSFFont supplierFont = workbook.createFont();
            supplierFont.setFontName(FontFactory.HELVETICA);
            supplierFont.setFontHeightInPoints((short) 10);
            supplierFont.setColor(reportColor);
            CellStyle supplierStyle = excelUtils.getStringDataCellStyleCentreAlign(workbook, supplierFont);

            int rowIdx = 1;
            int colCount = headers.size();

            // Title row
            XSSFRow titleRow = sheet.createRow(++rowIdx);
            titleRow.setHeightInPoints(22f);
            excelUtils.createStringDataCell(titleRow, 0, sheetTitle, titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, colCount - 1));

            for (String detail : Arrays.asList(supplierName, supplier.getSupplierAddress(),
                    supplier.getEmail(), supplier.getPhoneNumber())) {
                if (detail == null || detail.isBlank()) {
                    continue;
                }
                XSSFRow supplierRow = sheet.createRow(++rowIdx);
                excelUtils.createStringDataCell(supplierRow, 0, detail.trim(), supplierStyle);
                sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, colCount - 1));
            }

            // Date context
            XSSFRow dateRow = sheet.createRow(++rowIdx);
            excelUtils.createStringDataCell(dateRow, 0, dateContext, centreStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, colCount - 1));

            // Exported on
            XSSFRow printedRow = sheet.createRow(++rowIdx);
            excelUtils.createStringDataCell(printedRow, 0,
                    "Exported On: " + DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(LocalDateTime.now(EAT)),
                    centreStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, colCount - 1));

            XSSFRow exportedByRow = sheet.createRow(++rowIdx);
            excelUtils.createStringDataCell(exportedByRow, 0, "Exported By: " + exportedBy, centreStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, colCount - 1));

            // Column headers
            Row headerRow = sheet.createRow(rowIdx += 2);
            for (int i = 0; i < colCount; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            for (List<String> dataRow : rows) {
                XSSFRow row = sheet.createRow(++rowIdx);
                for (int i = 0; i < dataRow.size(); i++) {
                    String val = dataRow.get(i);
                    CellStyle style = isNumericLike(val) ? rightStyle : leftStyle;
                    excelUtils.createStringDataCell(row, i, val, style);
                }
            }

            for (int i = 0; i < colCount; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            bos.close();
            return bos;
        } catch (Exception e) {
            log.error("Error building analysis Excel for view {}: {}", viewTitle, e.getMessage(), e);
            throw new BadRequestRestApiException()
                    .developerMessage("Error creating Excel file").userMessage("Sorry, error creating file");
        }
    }

    // ── PDF builder ──────────────────────────────────────────────────────────

    private ByteArrayOutputStream buildPdf(Suppliers supplier, String viewTitle, String dateContext,
                                           List<String> headers, List<List<String>> rows, String exportedBy) {
        Document document = new Document(PageSize.A4.rotate(), 18f, 18f, 24f, 24f);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new PdfUtil());
            document.open();

            // Background banner
            try {
                Image logo2 = Image.getInstance(
                        new ClassPathResource("static/images/logo_2.png").getInputStream().readAllBytes());
                logo2.scaleToFit(630, 200);
                float y = document.getPageSize().getHeight() - document.topMargin() - logo2.getScaledHeight() + 50f;
                logo2.setAbsolutePosition(-22, y);
                writer.getDirectContent().addImage(logo2);
            } catch (Exception ex) {
                log.debug("logo_2.png not found, skipping banner", ex);
            }

            document.add(new Paragraph("\n\n\n"));

            // Supplier logo
            try {
                Image logo = Image.getInstance(
                        new ClassPathResource("static/images/logoImage_1.jpg").getInputStream().readAllBytes());
                logo.scaleToFit(120, 120);
                logo.setAlignment(Image.ALIGN_LEFT);
                logo.setIndentationLeft(-10f);
                document.add(logo);
            } catch (Exception ex) {
                log.debug("logoImage_1.jpg not found, skipping supplier logo", ex);
            }

            // Report title
            String titleName = "CUSTOMER ANALYSIS REPORT";
            String supplierName = supplier.getSupplierName() != null ? supplier.getSupplierName() : "Supplier";
            String address = supplier.getSupplierAddress();
            String email = supplier.getEmail();
            String phoneNumber = supplier.getPhoneNumber();
            Paragraph title = new Paragraph(
                    titleName,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, HEADER_GREEN));
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(6f);
            document.add(title);

            for (String detail : Arrays.asList(supplierName, address, email, phoneNumber)) {
                if (detail == null || detail.isBlank()) {
                    continue;
                }
                Paragraph supplierDetail = new Paragraph(detail.trim(),
                        FontFactory.getFont(FontFactory.HELVETICA, 10, HEADER_GREEN));
                supplierDetail.setAlignment(Element.ALIGN_CENTER);
                supplierDetail.setSpacingBefore(2f);
                document.add(supplierDetail);
            }

            Paragraph companyName = new Paragraph("ITIBARI TECHNOLOGIES LTD",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, HEADER_GREEN));
            companyName.setAlignment(Element.ALIGN_LEFT);
            companyName.setSpacingBefore(8f);
            companyName.setSpacingAfter(2f);
            document.add(companyName);

            Paragraph companyAddress = new Paragraph("Mikocheni light industrial area, Dar es Salaam",
                    FontFactory.getFont(FontFactory.HELVETICA, 10, HEADER_GREEN));
            companyAddress.setAlignment(Element.ALIGN_LEFT);
            companyAddress.setSpacingAfter(2f);
            document.add(companyAddress);

            Paragraph companyEmail = new Paragraph("Email: commercial@itibari.io",
                    FontFactory.getFont(FontFactory.HELVETICA, 10, HEADER_GREEN));
            companyEmail.setAlignment(Element.ALIGN_LEFT);
            companyEmail.setSpacingAfter(2f);
            document.add(companyEmail);

            Paragraph exportedByLine = new Paragraph("Exported By: " + exportedBy,
                    FontFactory.getFont(FontFactory.HELVETICA, 10, HEADER_GREEN));
            exportedByLine.setAlignment(Element.ALIGN_LEFT);
            exportedByLine.setSpacingAfter(8f);
            document.add(exportedByLine);

            LineSeparator separator = new LineSeparator();
            separator.setLineColor(new BaseColor(190, 190, 190));
            document.add(new Chunk(separator));

            // Date context + exported on
            Paragraph meta = new Paragraph(dateContext + "    Exported: "
                    + DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(LocalDateTime.now(EAT)),
                    FontFactory.getFont(FontFactory.HELVETICA, 9, BaseColor.DARK_GRAY));
            meta.setAlignment(Element.ALIGN_LEFT);
            meta.setSpacingBefore(6f);
            meta.setSpacingAfter(6f);
            document.add(meta);

            // Data table
            if (!rows.isEmpty()) {
                com.itextpdf.text.Font pdfHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, BaseColor.WHITE);
                com.itextpdf.text.Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

                PdfPTable table = new PdfPTable(headers.size());
                table.setWidthPercentage(100);
                table.setSpacingBefore(4f);
                table.setSpacingAfter(6f);

                for (String h : headers) {
                    table.addCell(pdfHeaderCell(h, pdfHeaderFont));
                }
                for (List<String> dataRow : rows) {
                    for (String cell : dataRow) {
                        table.addCell(pdfDataCell(cell != null ? cell : "", cellFont));
                    }
                }
                document.add(table);
            } else {
                document.add(new Paragraph("No data for the selected period.",
                        FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, BaseColor.DARK_GRAY)));
            }

            Paragraph footer = new Paragraph(
                    "This is a system generated document. No signature is required. For any inquiries, please contact commercial@itibari.io.",
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, BaseColor.DARK_GRAY));
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(9f);
            document.add(footer);

            document.close();
        } catch (Exception e) {
            log.error("Error building analysis PDF for view {}: {}", viewTitle, e.getMessage(), e);
            throw new RuntimeException("Error generating PDF", e);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
        return out;
    }

    // ── Format helpers ───────────────────────────────────────────────────────

    private static String str(String s) {
        return s != null ? s : "";
    }

    private static String money(java.math.BigDecimal v) {
        return v != null ? MONEY_FMT.format(v) : "0.00";
    }

    private static String units(java.math.BigDecimal v) {
        return v != null ? String.valueOf(v.longValue()) : "0";
    }

    private static String tonnes(java.math.BigDecimal v) {
        return v != null ? TONNES_FMT.format(v) : "0.0000";
    }

    private static String trend(Double v) {
        return v != null ? TREND_FMT.format(v) + "%" : "N/A";
    }

    private static boolean isNumericLike(String val) {
        if (val == null || val.isEmpty()) return false;
        return val.matches("^-?[\\d,]+(\\.\\d+)?%?$");
    }

    private static String dateRangeLabel(LocalDate start, LocalDate end) {
        if (start != null && end != null) {
            return "Period: " + DATE_FMT.format(start) + " — " + DATE_FMT.format(end);
        }
        if (start != null) {
            return "From: " + DATE_FMT.format(start);
        }
        return "All periods";
    }

    private static PdfPCell pdfHeaderCell(String text, com.itextpdf.text.Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5f);
        cell.setBackgroundColor(HEADER_GREEN);
        cell.setBorderColor(new BaseColor(230, 230, 230));
        cell.setBorderWidth(0.8f);
        return cell;
    }

    private static PdfPCell pdfDataCell(String text, com.itextpdf.text.Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(4f);
        cell.setBorderColor(new BaseColor(220, 220, 220));
        cell.setBorderWidth(0.5f);
        return cell;
    }

    public String exportFilename(String supplierName, String viewTitle, StatementExportType exportType) {
        String ts = TIMESTAMP_FMT.format(LocalDateTime.now(EAT));
        String safe = (supplierName != null ? supplierName : "Supplier").replaceAll("[^a-zA-Z0-9 ]", "").trim();
        return safe + "_" + viewTitle.replaceAll("[^a-zA-Z0-9 ]", " ").trim() + "_" + ts + exportType.extension;
    }
}
