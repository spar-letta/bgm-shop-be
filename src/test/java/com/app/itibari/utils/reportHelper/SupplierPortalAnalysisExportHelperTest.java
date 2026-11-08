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
import com.app.itibari.utils.helpers.ExcelUtils;
import com.itextpdf.io.source.ByteArrayOutputStream;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Smoke tests for SupplierPortalAnalysisExportHelper.
 * Verifies that Excel and PDF output is non-empty and structurally valid
 * for one product analysis view and one customer analysis view.
 */
class SupplierPortalAnalysisExportHelperTest {

    private static final UUID SUPPLIER_ID = UUID.fromString("aaaaaaaa-0000-4000-8000-000000000001");

    private SupplierPortalProductAnalysisService productService;
    private SupplierPortalCustomerAnalysisService customerService;
    private PersonRepository personRepo;
    private SupplierPortalAnalysisExportHelper helper;

    private Suppliers supplier;
    private Principal principal;
    private final LocalDate START = LocalDate.of(2026, 1, 1);
    private final LocalDate END = LocalDate.of(2026, 3, 31);

    @BeforeEach
    void setUp() {
        productService = mock(SupplierPortalProductAnalysisService.class);
        customerService = mock(SupplierPortalCustomerAnalysisService.class);
        personRepo = mock(PersonRepository.class);
        SupplierRepository supplierRepo = mock(SupplierRepository.class);
        principal = mock(Principal.class);
        when(principal.getName()).thenReturn("exporter@example.com");

        Person exporter = new Person();
        exporter.setEmail("exporter@example.com");
        exporter.setFirstName("Jane");
        exporter.setLastName("Doe");
        when(personRepo.findByEmailAndDeletedIsFalse("exporter@example.com")).thenReturn(Optional.of(exporter));

        helper = new SupplierPortalAnalysisExportHelper(
                new ExcelUtils(), personRepo, supplierRepo, productService, customerService);

        supplier = new Suppliers();
        supplier.setPublicId(SUPPLIER_ID);
        supplier.setSupplierName("Test Supplier");
    }

    // ── Product Analysis — Excel smoke test ──────────────────────────────────

    @Test
    void exportProductAnalysis_wardPerformance_excel_returnsNonEmptyWorkbook() throws Exception {
        when(productService.getWardPerformanceForExport(
                any(), any(), any(), eq(START), eq(END), eq(principal), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(wardPerformanceRows(3)));

        ByteArrayOutputStream bos = helper.exportProductAnalysis(
                SupplierPortalProductAnalysisView.WARD_PERFORMANCE,
                StatementExportType.EXCEL,
                SUPPLIER_ID, List.of(), List.of(), START, END, null,
                supplier, principal);

        assertNotNull(bos);
        assertTrue(bos.toByteArray().length > 0, "Excel output must not be empty");

        XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bos.toByteArray()));
        assertEquals(1, workbook.getNumberOfSheets());
        // Header row + 3 data rows, offset by title/date rows
        assertTrue(workbook.getSheetAt(0).getLastRowNum() > 4, "Workbook must contain data rows");
        assertTrue(workbook.getSheetAt(0).getRow(6).getCell(0).getStringCellValue().contains("exporter@example.com"),
                "Excel export must include exporter email");
        workbook.close();
    }

    // ── Product Analysis — PDF smoke test ────────────────────────────────────

    @Test
    void exportProductAnalysis_wardPerformance_pdf_returnsNonEmptyBytes() {
        when(productService.getWardPerformanceForExport(
                any(), any(), any(), eq(START), eq(END), eq(principal), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(wardPerformanceRows(2)));

        ByteArrayOutputStream bos = helper.exportProductAnalysis(
                SupplierPortalProductAnalysisView.WARD_PERFORMANCE,
                StatementExportType.PDF,
                SUPPLIER_ID, List.of(), List.of(), START, END, null,
                supplier, principal);

        assertNotNull(bos);
        byte[] bytes = bos.toByteArray();
        assertTrue(bytes.length > 0, "PDF output must not be empty");
        // PDF magic bytes: %PDF
        assertEquals('%', (char) bytes[0]);
        assertEquals('P', (char) bytes[1]);
        assertEquals('D', (char) bytes[2]);
        assertEquals('F', (char) bytes[3]);
    }

    // ── Customer Analysis — Excel smoke test ─────────────────────────────────

    @Test
    void exportCustomerAnalysis_customers_excel_returnsNonEmptyWorkbook() throws Exception {
        when(customerService.getCustomersForExport(
                any(), any(), any(), eq(START), eq(END), any(), eq(principal), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(customerRows(4)));

        ByteArrayOutputStream bos = helper.exportCustomerAnalysis(
                SupplierPortalCustomerAnalysisView.CUSTOMERS,
                StatementExportType.EXCEL,
                null, null, List.of(), START, END, null, null,
                supplier, principal);

        assertNotNull(bos);
        assertTrue(bos.toByteArray().length > 0, "Excel output must not be empty");

        XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bos.toByteArray()));
        assertEquals(1, workbook.getNumberOfSheets());
        assertTrue(workbook.getSheetAt(0).getLastRowNum() > 4, "Workbook must contain data rows");
        assertEquals("Exported By: exporter@example.com",
                workbook.getSheetAt(0).getRow(6).getCell(0).getStringCellValue());

        org.apache.poi.ss.usermodel.Row headerRow = workbook.getSheetAt(0).getRow(8);
        assertEquals("Shop Name", headerRow.getCell(0).getStringCellValue());
        assertEquals("Quantity", headerRow.getCell(2).getStringCellValue());
        assertEquals("Order Date", headerRow.getCell(3).getStringCellValue());
        assertEquals("Purchase Tonnes", headerRow.getCell(8).getStringCellValue());
        assertEquals(9, headerRow.getLastCellNum());
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            assertFalse(headerRow.getCell(i).getStringCellValue().contains("Purchase Dates"),
                    "Customer list export must not include Purchase Dates");
            assertFalse(headerRow.getCell(i).getStringCellValue().equals("Settled Tonnes"),
                    "Customer list export must use Purchase Tonnes, not Settled Tonnes");
        }
        assertEquals("15/01/2026", workbook.getSheetAt(0).getRow(9).getCell(3).getStringCellValue());
        workbook.close();
    }

    // ── Customer Analysis — PDF smoke test ───────────────────────────────────

    @Test
    void exportCustomerAnalysis_customers_pdf_returnsNonEmptyBytes() {
        when(customerService.getCustomersForExport(
                any(), any(), any(), eq(START), eq(END), any(), eq(principal), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(customerRows(2)));

        ByteArrayOutputStream bos = helper.exportCustomerAnalysis(
                SupplierPortalCustomerAnalysisView.CUSTOMERS,
                StatementExportType.PDF,
                null, null, List.of(), START, END, null, null,
                supplier, principal);

        assertNotNull(bos);
        byte[] bytes = bos.toByteArray();
        assertTrue(bytes.length > 0, "PDF output must not be empty");
        assertEquals('%', (char) bytes[0]);
        assertEquals('P', (char) bytes[1]);
        assertEquals('D', (char) bytes[2]);
        assertEquals('F', (char) bytes[3]);
    }

    @Test
    void exportProductAnalysis_allProductsProductRankingCogs_excel_includesExportedBy() throws Exception {
        when(productService.getAllProductsProductRankingCogsForExport(
                any(), any(), eq(START), eq(END), eq(principal), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        when(principal.getName()).thenReturn("exporter@example.com");
        Person exporter = new Person();
        exporter.setEmail("exporter@example.com");
        exporter.setFirstName("Jane");
        exporter.setLastName("Doe");
        when(personRepo.findByEmailAndDeletedIsFalse("exporter@example.com")).thenReturn(Optional.of(exporter));

        ByteArrayOutputStream bos = helper.exportProductAnalysis(
                SupplierPortalProductAnalysisView.ALL_PRODUCTS_PRODUCT_RANKING_COGS,
                StatementExportType.EXCEL,
                null, List.of(), List.of(), START, END, null,
                supplier, principal);

        XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bos.toByteArray()));
        assertEquals("Exported By: exporter@example.com", workbook.getSheetAt(0).getRow(6).getCell(0).getStringCellValue());
        workbook.close();
    }

    // ── exportFilename ────────────────────────────────────────────────────────

    @Test
    void exportFilename_excel_matchesExpectedPattern() {
        String name = helper.exportFilename("Acme Ltd", "Ward Performance", StatementExportType.EXCEL);
        assertTrue(name.startsWith("Acme Ltd_Ward Performance_"), "Filename must start with supplier + view");
        assertTrue(name.endsWith(".xlsx"), "Excel filename must end with .xlsx");
    }

    @Test
    void exportFilename_pdf_matchesExpectedPattern() {
        String name = helper.exportFilename("Acme Ltd", "Ward Performance", StatementExportType.PDF);
        assertTrue(name.endsWith(".pdf"), "PDF filename must end with .pdf");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<ProductAnalysisWardPerformanceRowDTO> wardPerformanceRows(int count) {
        List<ProductAnalysisWardPerformanceRowDTO> rows = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            ProductAnalysisWardPerformanceRowDTO dto = new ProductAnalysisWardPerformanceRowDTO();
            dto.setRank(i);
            dto.setOrganizationLocationName("Ward " + i);
            dto.setTotalCogsTzs(BigDecimal.valueOf(100_000L * i));
            dto.setSettledTonnes(BigDecimal.valueOf(i * 10));
            dto.setDistinctCustomerCount(i * 5L);
            dto.setDeliveredOrderCount(i * 3L);
            dto.setAverageOrderValueTzs(BigDecimal.valueOf(50_000L));
            rows.add(dto);
        }
        return rows;
    }

    private List<CustomerAnalysisCustomerRowV2DTO> customerRows(int count) {
        List<CustomerAnalysisCustomerRowV2DTO> rows = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            CustomerAnalysisCustomerRowV2DTO dto = new CustomerAnalysisCustomerRowV2DTO();
            dto.setShopName("Shop " + i);
            dto.setRegionName("Region A");
            dto.setOrganizationLocationName("Ward " + i);
            dto.setDeliveredOrderCount(i * 2L);
            dto.setTotalCogsTzs(BigDecimal.valueOf(80_000L * i));
            dto.setSettledTonnes(BigDecimal.valueOf(i * 5));
            dto.setProductNames("Product " + i);
            dto.setProductPurchaseQuantities(String.valueOf(i * 10));
            dto.setProductPurchaseDates("Product " + i + ": 01/01/2026");
            dto.setLastOrderDate(LocalDate.of(2026, 1, 15));
            rows.add(dto);
        }
        return rows;
    }
}
