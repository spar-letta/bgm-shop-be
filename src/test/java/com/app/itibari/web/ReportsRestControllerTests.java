package com.app.itibari.web;

import com.app.itibari.ItibariApplicationTests;
import com.app.itibari.dataTypes.*;
import com.app.itibari.models.request.*;
import com.app.itibari.models.entities.BnplCreditLimitAdjustment;
import com.app.itibari.models.entities.Customers;
import com.app.itibari.repository.BnplCreditLimitAdjustmentRepository;
import com.app.itibari.repository.CustomersRepository;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.CellType;
import com.app.itibari.scheduler.Scheduler;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;
import java.util.Map;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@Slf4j
public class ReportsRestControllerTests extends ItibariApplicationTests {

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private BnplCreditLimitAdjustmentRepository creditLimitAdjustmentRepository;

    @Autowired
    private CustomersRepository reportCustomersRepository;

    @Autowired
    private JdbcTemplate reportJdbcTemplate;

    @Test
    public void testLeadExportIncludesAllQualifiedLeads() throws IOException {
        String search = "LeadExport-" + UUID.randomUUID();
        List<Customers> customers = new ArrayList<>();
        try {
            for (int i = 0; i < 3; i++) {
                Customers customer = new Customers();
                customer.setName(search + "-" + i);
                customer.setPhoneNumber("+255" + (700100000 + i));
                customer.setCustomerType(CustomerType.REGULAR);
                customer.setPreliminaryValue(new BigDecimal("500000"));
                customer.setBnplAccountCreated(false);
                customer = reportCustomersRepository.saveAndFlush(customer);
                customers.add(customer);
                reportJdbcTemplate.update("""
                        INSERT INTO itibari.bnpl_lead
                            (public_id, deleted, creation_time, customer_id, customer_public_id,
                             phone_number, potential_credit_limit, funnel_stage, total_call_attempts)
                        VALUES (?, false, ?, ?, ?, ?, 200000, 'PROSPECT', 0)
                        """, UUID.randomUUID(), LocalDateTime.now(ZoneId.of("Africa/Nairobi")),
                        customer.getId(), customer.getPublicId().toString(), customer.getPhoneNumber());

                // Only the first two customers have three delivered orders in each 30-day period.
                if (i < 2) {
                    LocalDateTime now = LocalDateTime.now(ZoneId.of("Africa/Nairobi"));
                    for (int period = 0; period < 3; period++) {
                        for (int order = 0; order < 3; order++) {
                            reportJdbcTemplate.update("""
                                    INSERT INTO itibari.sales_orders
                                        (public_id, deleted, creation_time, sales_order_customer_fk, loading_status)
                                    VALUES (?, false, ?, ?, 'DELIVERED')
                                    """, UUID.randomUUID(), now.minusDays(period * 30L + 10 + order), customer.getId());
                        }
                    }
                }
            }

            Response excel = downloadLeadExport(search, StatementExportType.EXCEL);
            saveExport(excel, "lead_report.xlsx");
            try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excel.asByteArray()))) {
                var sheet = workbook.getSheetAt(0);
                assertEquals("BNPL Leads Report", sheet.getSheetName());
                assertEquals("Customer Name", sheet.getRow(6).getCell(0).getStringCellValue());
                assertEquals("Both qualified leads must be exported despite size=1 and page=1",
                        9, sheet.getLastRowNum());
                for (int i = 0; i < 2; i++) {
                    assertEquals(customers.get(i).getName(), sheet.getRow(8 + i).getCell(0).getStringCellValue());
                    assertEquals(customers.get(i).getPhoneNumber(), sheet.getRow(8 + i).getCell(1).getStringCellValue());
                }
            }

            Response pdf = downloadLeadExport(search, StatementExportType.PDF);
            saveExport(pdf, "lead_report.pdf");
            PdfReader reader = new PdfReader(pdf.asByteArray());
            try {
                StringBuilder text = new StringBuilder();
                for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                    text.append(PdfTextExtractor.getTextFromPage(reader, page, new SimpleTextExtractionStrategy()));
                }
                // Phone numbers can wrap within the narrow PDF column.
                String pdfText = text.toString().replaceAll("\\s+", "");
                assertTrue("Missing first qualified lead in PDF: " + text, pdfText.contains(customers.get(0).getPhoneNumber()));
                assertTrue("Missing second qualified lead in PDF: " + text, pdfText.contains(customers.get(1).getPhoneNumber()));
                assertFalse("Unqualified leads must be excluded", pdfText.contains(customers.get(2).getPhoneNumber()));
            } finally {
                reader.close();
            }
        } finally {
            for (Customers customer : customers) {
                reportJdbcTemplate.update("DELETE FROM itibari.bnpl_lead WHERE customer_id = ?", customer.getId());
                reportJdbcTemplate.update("DELETE FROM itibari.sales_orders WHERE sales_order_customer_fk = ?", customer.getId());
                reportCustomersRepository.deleteById(customer.getId());
            }
        }
    }

    private Response downloadLeadExport(String search, StatementExportType exportType) {
        return given().auth().oauth2(ADMIN_TOKEN)
                .queryParam("search", search)
                .queryParam("exportType", exportType.name())
                .queryParam("page", 1).queryParam("size", 1).queryParam("sort", "customer.name,asc")
                .get("/reports/leadExport")
                .then().statusCode(200)
                .contentType(exportType.contentType)
                .header("Content-Disposition", containsString("attachment; filename=\"Lead Report - "))
                .header("Content-Disposition", containsString("." + exportType.extension + "\""))
                .extract().response();
    }

    private static final List<String> CREDIT_ADJUSTMENT_HEADERS = List.of(
            "Customer Name", "Customer Phone Number", "Adjustment Type", "Percentage",
            "Old Credit Limit", "New Credit Limit", "Old Credit Limit Balance", "New Credit Limit Balance",
            "Period Start", "Period End");

    @Test
    public void testCreditLimitAdjustmentsExcelAndPdfDownloads() throws IOException {
        UUID customerId = UUID.fromString("d7c5bf34-fda1-46e2-b5ee-40db98aed06d");
        var customer = reportCustomersRepository.findByPublicIdAndDeletedIsFalse(customerId).orElseThrow();
        BnplCreditLimitAdjustment adjustment = new BnplCreditLimitAdjustment();
        adjustment.setCustomer(customer);
        adjustment.setAdjustmentType(BnplCreditLimitAdjustmentType.INCREASE);
        adjustment.setPercentage(new BigDecimal("20.00"));
        adjustment.setOldCreditLimit(new BigDecimal("1000.25"));
        adjustment.setNewCreditLimit(new BigDecimal("1200.30"));
        adjustment.setOldCreditLimitBalance(new BigDecimal("600.25"));
        adjustment.setNewCreditLimitBalance(new BigDecimal("800.30"));
        adjustment.setPeriodStart(LocalDate.of(2026, 7, 1));
        adjustment.setPeriodEnd(LocalDate.of(2026, 9, 30));
        adjustment = creditLimitAdjustmentRepository.saveAndFlush(adjustment);
        try {
            Map<String, Object> filters = Map.of(
                    "startDate", adjustment.getCreationTime().toLocalDate().toString(),
                    "endDate", adjustment.getCreationTime().toLocalDate().toString(),
                    "customerPublicId", customerId.toString(),
                    "searchParam", customer.getPhoneNumber(),
                    "page", 0, "size", 1, "sort", "id,desc");
            // Confirm the report selects the same record as the BNPL listing endpoint.
            given().auth().oauth2(ADMIN_TOKEN).queryParams(filters)
                    .get("/bnpl/creditLimitAdjustments")
                    .then().statusCode(200)
                    .body("content.size()", equalTo(1))
                    .body("content[0].publicId", equalTo(adjustment.getPublicId().toString()));

            Response excel = downloadCreditLimitAdjustments(StatementExportType.EXCEL, filters);
            saveExport(excel, "credit_limit_adjustments.xlsx");
            try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excel.asByteArray()))) {
                var sheet = workbook.getSheetAt(0);
                assertEquals(10, sheet.getRow(4).getLastCellNum());
                for (int i = 0; i < CREDIT_ADJUSTMENT_HEADERS.size(); i++) {
                    assertEquals(CREDIT_ADJUSTMENT_HEADERS.get(i), sheet.getRow(4).getCell(i).getStringCellValue());
                }
                assertEquals("Only the requested page should be exported", 5, sheet.getLastRowNum());
                var row = sheet.getRow(5);
                assertEquals(customer.getName(), row.getCell(0).getStringCellValue());
                assertEquals(customer.getPhoneNumber(), row.getCell(1).getStringCellValue());
                assertEquals("INCREASE", row.getCell(2).getStringCellValue());
                double[] amounts = {20, 1000.25, 1200.30, 600.25, 800.30};
                for (int i = 0; i < amounts.length; i++) {
                    assertEquals(CellType.NUMERIC, row.getCell(i + 3).getCellType());
                    assertEquals(amounts[i], row.getCell(i + 3).getNumericCellValue(), 0.00001);
                }
                assertEquals(adjustment.getPeriodStart(), row.getCell(8).getLocalDateTimeCellValue().toLocalDate());
                assertEquals(adjustment.getPeriodEnd(), row.getCell(9).getLocalDateTimeCellValue().toLocalDate());
            }

            Response pdf = downloadCreditLimitAdjustments(StatementExportType.PDF, filters);
            saveExport(pdf, "credit_limit_adjustments.pdf");
            PdfReader reader = new PdfReader(pdf.asByteArray());
            try {
                assertTrue(reader.getPageSizeWithRotation(1).getWidth() > reader.getPageSizeWithRotation(1).getHeight());
                String text = PdfTextExtractor.getTextFromPage(reader, 1).replaceAll("\\s+", " ");
                // PDF headings may wrap over several lines in the ten-column table.
                for (String heading : CREDIT_ADJUSTMENT_HEADERS) {
                    for (String word : heading.split(" ")) {
                        assertTrue("Missing PDF heading word: " + word, text.contains(word));
                    }
                }
                for (String value : List.of(customer.getName(), customer.getPhoneNumber(), "INCREASE", "20.00",
                        "1000.25", "1200.30", "600.25", "800.30", "2026-07-01", "2026-09-30")) {
                    assertTrue("Missing PDF value: " + value, text.contains(value));
                }
            } finally {
                reader.close();
            }
        } finally {
            creditLimitAdjustmentRepository.deleteById(adjustment.getId());
        }
    }

    @Test
    public void testCreditLimitAdjustmentsEmptyExports() throws IOException {
        Map<String, Object> filters = Map.of("customerPublicId", UUID.randomUUID().toString());
        Response excel = downloadCreditLimitAdjustments(StatementExportType.EXCEL, filters);
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excel.asByteArray()))) {
            assertEquals(4, workbook.getSheetAt(0).getLastRowNum());
            assertEquals(10, workbook.getSheetAt(0).getRow(4).getLastCellNum());
        }
        PdfReader reader = new PdfReader(downloadCreditLimitAdjustments(StatementExportType.PDF, filters).asByteArray());
        try {
            assertTrue(PdfTextExtractor.getTextFromPage(reader, 1).contains("No credit limit adjustments found."));
        } finally {
            reader.close();
        }
    }

    @Test
    public void testCreditLimitAdjustmentsRejectsInvalidDateRange() {
        given().auth().oauth2(ADMIN_TOKEN)
                .queryParam("exportType", "EXCEL")
                .queryParam("startDate", "2026-09-30")
                .queryParam("endDate", "2026-09-01")
                .get("/reports/creditLimitAdjustments")
                .then().statusCode(400);
    }

    @Test
    public void testCreditLimitAdjustmentsRequiresValidExportType() {
        given().auth().oauth2(ADMIN_TOKEN)
                .get("/reports/creditLimitAdjustments")
                .then().statusCode(400);
        given().auth().oauth2(ADMIN_TOKEN).queryParam("exportType", "CSV")
                .get("/reports/creditLimitAdjustments")
                .then().statusCode(400);
    }

    @Test
    public void testCreditLimitAdjustmentsRequiresAuthentication() {
        given().queryParam("exportType", "PDF")
                .get("/reports/creditLimitAdjustments")
                .then().statusCode(401);
    }

    private Response downloadCreditLimitAdjustments(StatementExportType exportType, Map<String, Object> filters) {
        return given().auth().oauth2(ADMIN_TOKEN).queryParams(filters)
                .queryParam("exportType", exportType.name())
                .get("/reports/creditLimitAdjustments")
                .then().statusCode(200)
                .contentType(exportType.contentType)
                .header("Content-Disposition", containsString("attachment;"))
                .header("Content-Disposition", containsString(exportType.extension + "\""))
                .extract().response();
    }

    @Ignore
    @Test
    public void testCashCreditReconciliationReportExcelAndPdfDownloads() throws IOException {
        LocalDate reportDate = LocalDate.of(2026, 7, 9);
        String reportPublicId = given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("date", reportDate.toString())
                .post("/reconciliation/cash-credit-reports")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        Response excelResponse = given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("exportType", StatementExportType.EXCEL)
                .get("/reports/cash-credit-reports/{publicId}/download", reportPublicId)
                .then()
                .statusCode(200)
                .contentType(StatementExportType.EXCEL.contentType)
                .header("Content-Disposition", containsString(".xlsx"))
                .extract().response();
        saveExport(excelResponse, "cash_credit_reconciliation_report.xlsx");
        assertDownloadedFile("cash_credit_reconciliation_report.xlsx", "PK");

        Response pdfResponse = given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("exportType", StatementExportType.PDF)
                .get("/reports/cash-credit-reports/{publicId}/download", reportPublicId)
                .then()
                .statusCode(200)
                .contentType(StatementExportType.PDF.contentType)
                .header("Content-Disposition", containsString(".pdf"))
                .extract().response();
        saveExport(pdfResponse, "cash_credit_reconciliation_report.pdf");
        assertDownloadedFile("cash_credit_reconciliation_report.pdf", "%PDF");
    }

    private void assertDownloadedFile(String fileName, String signature) throws IOException {
        File file = new File("src/test/resources/downloads", fileName);
        org.junit.Assert.assertTrue("Expected downloaded file " + file.getAbsolutePath(), file.isFile());
        org.junit.Assert.assertTrue("Expected downloaded file to contain data", file.length() > 0);

        byte[] bytes = Files.readAllBytes(file.toPath());
        String actualSignature = new String(bytes, 0, Math.min(bytes.length, signature.length()), StandardCharsets.US_ASCII);
        org.junit.Assert.assertTrue("Unexpected file signature for " + fileName, actualSignature.startsWith(signature));
    }


    @Test
    public void testProductReport() throws IOException {
        Response response = given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/reports/productsReport")
                .then()
                .statusCode(200)
                .extract().response();

//        saveExport(response, "productR.xlsx");
    }

    @Ignore
    @Test
    public void testProductReport2() throws IOException {
        UUID productId = UUID.fromString("7c4a6d52-5da2-47fe-92a8-ea2fce2602af");
        DiscountCreateRequest discountCreateRequest = new DiscountCreateRequest();
        discountCreateRequest.setDiscountType(DiscountType.VolumeDiscount);
        discountCreateRequest.setProductIds(Collections.singletonList(productId));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(discountCreateRequest)
                .log().all()
                .post("/discounts")
                .then()
                .statusCode(200);


        String publicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("discountType", DiscountType.VolumeDiscount)
                .get("/discounts")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].status", equalTo(DiscountStatus.Incomplete.name()))
                .extract().path("content[0].publicId");


        VolumeRangeCreateRequest volumeRangeCreateRequest = new VolumeRangeCreateRequest();
        volumeRangeCreateRequest.setMinVolume(2);
        volumeRangeCreateRequest.setMaxVolume(10);
        volumeRangeCreateRequest.setDiscountAmount(new BigDecimal(786));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(volumeRangeCreateRequest)
                .log().all()
                .patch("/discounts/{discountPublicId}/addVolumeRange", UUID.fromString(publicId))
                .then()
                .statusCode(200);


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/discounts/{discountPublicId}/volumeRanges", UUID.fromString(publicId))
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1));


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("discountType", DiscountType.VolumeDiscount)
                .get("/discounts")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].status", equalTo(DiscountStatus.Pending.name()));


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .patch("/discounts/{publicId}/approve", UUID.fromString(publicId))
                .then()
                .statusCode(200);


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("discountType", DiscountType.VolumeDiscount)
                .get("/discounts")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].status", equalTo(DiscountStatus.Active.name()));

        CartDiscountRequest cartDiscountRequest = new CartDiscountRequest();
        CartRequestItem cartRequestItem = new CartRequestItem();
        cartRequestItem.setProductPublicId(productId);
        cartRequestItem.setQuantity(5);

        cartDiscountRequest.setCartRequestItems(Collections.singletonList(cartRequestItem));


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(cartDiscountRequest)
                .log().all()
                .post("/salesOrders/cartDiscountCheck")
                .then()
                .statusCode(200);

        SalesOrderItemsRequest orderItems = SalesOrderItemsRequest
                .builder()
                .productId(productId)
                .quantity(5)
                .build();
        SalesOrdersRequest request = SalesOrdersRequest.builder()
                .customerId(UUID.fromString("d7c5bf34-fda1-46e2-b5ee-40db98aed06d"))
                .bookedBy("Itibari Test")
                .sourceChannel(SourceChannelEnum.BACK_OFFICE_APP.name())
                .salesOrderItems(Collections.singleton(orderItems))
                .build();

        String orderPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .post("/salesOrders")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        String orderItemPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders/{salesOrderPublicId}/items", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].discount", notNullValue())
                .body("content[0].hasDiscount", equalTo(true))
                .extract().path("content[0].publicId");

        checkInventoryNegativeTests();

        scheduler.runOrderPlanning();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200)
                .body("salesOrderStatus", equalTo(LoadingStatusEnum.PLANNING.name()));


        checkInventoryNegativeTests();

        RoutePlanningRequest routePlanningRequest = new RoutePlanningRequest();
        routePlanningRequest.setLocationPublicId(UUID.fromString("90ad01f1-4a13-41f7-b825-c428b41f35d0"));
        routePlanningRequest.setRegionPublicId(UUID.fromString("8c5d4db0-f9a1-47ed-a160-56780e4e4d14"));
        routePlanningRequest.setVehiclePublicId(UUID.fromString("fa4bf2ee-6665-493a-8dbd-5d56dc8a7d93"));
        routePlanningRequest.setDeliveryAgentPublicId(UUID.fromString("af485765-69c5-4dd0-8fa0-560058b85c65"));
        routePlanningRequest.setWarehousePublicId(UUID.fromString("3849b1be-a9dd-4f11-abbf-229ad2780a56"));
        routePlanningRequest.setSalesOrdersPublicIds(Collections.singletonList(UUID.fromString(orderPublicId)));


        String routePublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(routePlanningRequest)
                .post("/logistics/routePlanning")
                .then()
                .statusCode(200)
                .extract().path("publicId");


        String deliveryId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/logistics/routePlanning/{routePlanningPublicId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .extract().path("routeId");


        String productItemPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/logistics/routePlanning/{routePlanningPublicId}/loadingProducts", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].productItemPublicId");


        checkInventoryNegativeTests();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/logistics/routePlanning/{routePlanningPublicId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .body("routeStatus", equalTo(RouteStatus.CREATED.name()));

        LoadingRequest loadingRequest = new LoadingRequest();
        LoadingDTO loadingDTO = new LoadingDTO();
        loadingDTO.setLoadingType(LoadingType.OrderSale);
        loadingDTO.setProductItemPublicId(UUID.fromString(productItemPublicId));
        loadingRequest.setLoadingDTOList(Collections.singletonList(loadingDTO));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(loadingRequest)
                .post("warehouse/agent/{agentPublicId}/loading", UUID.fromString("af485765-69c5-4dd0-8fa0-560058b85c65"))
                .then()
                .statusCode(200);


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/logistics/routePlanning/{routePlanningPublicId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .body("routeStatus", equalTo(RouteStatus.Loaded.name()));


        checkInventoryNegativeTests();

        given()
                .contentType("application/json")
                .auth().oauth2(DELIVERY_AGENT_TOKEN)
                .header("X-App-Version", "1.0.0")
                .queryParam("acceptType", AcceptTypeEnum.AcceptRoute)
                .patch("/deliveryAgents/accept/{routeId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200);

        checkInventoryNegativeTests();


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/logistics/routePlanning/{routePlanningPublicId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .body("routeStatus", equalTo(RouteStatus.ReadyToGo.name()));

        given()
                .contentType("application/json")
                .auth().oauth2(DELIVERY_AGENT_TOKEN)
                .header("X-App-Version", "1.0.0")
                .queryParam("routeId", UUID.fromString(routePublicId))
                .queryParam("action", RouteAction.Start)
                .patch("/deliveryAgents/routeActions")
                .then()
                .statusCode(200);


        String productReconId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/warehouse/productRecons")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].publicId");


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/warehouse/productRecons/{publicId}/items", UUID.fromString(productReconId))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));


        checkInventoryNegativeTests();

        // partial deliver products

        DeliverItemRequest deliverItemRequest = new DeliverItemRequest();
        deliverItemRequest.setSalesOrderPublicId(UUID.fromString(orderPublicId));
        deliverItemRequest.setRoutePublicId(UUID.fromString(routePublicId));

        ItemRequest itemRequest = new ItemRequest();
        itemRequest.setProductItemPublicId(UUID.fromString(orderItemPublicId));
        itemRequest.setProductReconItemPublicId(UUID.fromString(orderItemPublicId));
        itemRequest.setQuantitySold(3);
        deliverItemRequest.setItems(Collections.singletonList(itemRequest));


        given()
                .contentType("application/json")
                .auth().oauth2(DELIVERY_AGENT_TOKEN)
                .header("X-App-Version", "1.0.0")
                .body(deliverItemRequest)
                .patch("/deliveryAgents/markOrderItemAsDelivered")
                .then()
                .statusCode(200);

        // mark order as delivered

        given()
                .contentType("application/json")
                .auth().oauth2(DELIVERY_AGENT_TOKEN)
                .header("X-App-Version", "1.0.0")
                .patch("/deliveryAgents/markOrderAsDelivered/{orderPublicId}", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200);

        checkInventoryNegativeTests();


        // fetch order check

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200)
                .body("salesOrderStatus", equalTo(LoadingStatusEnum.DELIVERED.name()));


        given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("startDate", "2025-06-01")
                .queryParam("endDate", "2025-06-30")
                .get("/reports/productDistribution")
                .then().log().all()
                .statusCode(200);
    }

    private void checkInventoryNegativeTests() {

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].availableStock", greaterThanOrEqualTo(0));
    }


    @Test
    public void testDistributionReport() {
        given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("startDate", "2025-06-01")
                .queryParam("endDate", "2025-06-30")
                .get("/reports/productDistribution")
                .then().log().all()
                .statusCode(200);
    }

    @Test
    public void testInventoryDashboardEndpoints() {
        given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/reports/inventoryDashboard/summary")
                .then().log().all()
                .statusCode(200)
                .body("totalQuantity", notNullValue())
                .body("products", notNullValue())
                .body("regions", notNullValue());

        given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/reports/inventoryDashboard/quantityByProduct")
                .then().log().all()
                .statusCode(200);

        given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/reports/inventoryDashboard/distributionByRegion")
                .then().log().all()
                .statusCode(200);

        given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/reports/inventoryDashboard/details")
                .then().log().all()
                .statusCode(200)
                .body("content", notNullValue());
    }


    @Test
    @Ignore
    public void testSupplierDashboardAnalysis() {

        SalesOrderItemsRequest orderItems = SalesOrderItemsRequest
                .builder()
                .productId(UUID.fromString("08ebb084-2633-428f-82b9-e8d98a22ec53"))
                .quantity(5)
                .build();
        SalesOrdersRequest request = SalesOrdersRequest.builder()
                .customerId(UUID.fromString("d7c5bf34-fda1-46e2-b5ee-40db98aed06d"))
                .bookedBy("Itibari Test")
                .sourceChannel(SourceChannelEnum.BACK_OFFICE_APP.name())
                .salesOrderItems(Collections.singleton(orderItems))
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .post("/salesOrders")
                .then().log().all()
                .statusCode(200);

//        given()
//                .auth().oauth2(SUPPLIER_USER_TOKEN)
//                .get("/reports/supplierStatisticsReport")
//                .then().log().all()
//                .statusCode(200);
//
//
//        given()
//                .auth().oauth2(SUPPLIER_USER_TOKEN)
//                .get("/reports/supplierProductDistributionReport")
//                .then().log().all()
//                .statusCode(200);

//        given()
//                .auth().oauth2(SUPPLIER_USER_TOKEN)
//                .get("/reports/supplierQuantitySoldPerRegion")
//                .then().log().all()
//                .statusCode(200);


        log.info("------------rtf");
        given()
                .auth().oauth2(SUPPLIER_USER_TOKEN)
//                .queryParam("monthly", true)
                .get("/reports/supplierSalesRevenue")
                .then().log().all()
                .statusCode(200);

//        given()
//                .auth().oauth2(SUPPLIER_USER_TOKEN)
//                .queryParam("quarterly", true)
//                .get("/reports/supplierSalesRevenue")
//                .then().log().all()
//                .statusCode(200);
//
//        given()
//                .auth().oauth2(SUPPLIER_USER_TOKEN)
//                .queryParam("yearly", true)
//                .get("/reports/supplierSalesRevenue")
//                .then().log().all()
//                .statusCode(200);
    }


    @Test
   @Ignore
    public void testSupplierProductPerformanceReport() throws IOException {

        SalesOrderItemsRequest orderItems = SalesOrderItemsRequest
                .builder()
                .productId(UUID.fromString("08ebb084-2633-428f-82b9-e8d98a22ec53"))
                .quantity(5)
                .build();
        SalesOrdersRequest request = SalesOrdersRequest.builder()
                .customerId(UUID.fromString("d7c5bf34-fda1-46e2-b5ee-40db98aed06d"))
                .bookedBy("Itibari Test")
                .sourceChannel(SourceChannelEnum.BACK_OFFICE_APP.name())
                .salesOrderItems(Collections.singleton(orderItems))
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .post("/salesOrders")
                .then().log().all()
                .statusCode(200);

//
//        UUID productId = UUID.fromString("08ebb084-2633-428f-82b9-e8d98a22ec53");
//        UUID regionId = UUID.fromString("8c5d4db0-f9a1-47ed-a160-56780e4e4d14");
//
//
//        given()
//                .auth().oauth2(SUPPLIER_USER_TOKEN)
//                .queryParam("productId", productId)
//                .queryParam("regionId", regionId)
//                .get("/reports/productPerformanceReport")
//                .then().log().all()
//                .statusCode(200);
//
//
//        given()
//                .auth().oauth2(SUPPLIER_USER_TOKEN)
//                .queryParam("productId", productId)
//                .queryParam("regionId", regionId)
//                .get("/reports/productPerformancePerZoneReport")
//                .then().log().all()
//                .statusCode(200);
//
//
//        given()
//                .auth().oauth2(SUPPLIER_USER_TOKEN)
////                .queryParam("productId", productId)
////                .queryParam("regionId", regionId)
//                .queryParam("startDate", "2026-01-01")
//                .queryParam("endDate", "2026-01-31")
//                .get("/reports/productCustomerPerformanceReport")
//                .then().log().all()
//                .statusCode(200);
//
//        Response response = given()
//                .auth().oauth2(ADMIN_TOKEN)
////                .queryParam("productId", productId)
////                .queryParam("regionId", regionId)
//                .queryParam("startDate", "2026-01-01")
//                .queryParam("endDate", "2026-01-31")
//                .get("/reports/productCustomerPerformanceExport")
//                .then()
//                .statusCode(200)
//                .extract().response();

//        saveExport(response, "customer_performance.xlsx");

//        log.info("-----------her");
//        given()
//                .auth().oauth2(SUPPLIER_USER_TOKEN)
////                .queryParam("productId", productId)
////                .queryParam("regionId", regionId)
////                .queryParam("startDate", "2026-01-01")
////                .queryParam("endDate", "2026-01-31")
//                .get("/reports/purchaseReport")
//                .then().log().all()
//                .statusCode(200);

        Response response = given()
                .auth().oauth2(SUPPLIER_USER_TOKEN)
                .queryParam("exportType", StatementExportType.EXCEL)
                .get("/reports/purchaseReport/export")
                .then()
                .statusCode(200)
                .extract().response();

      saveExport(response, "supp_purchase_report.xlsx");


        Response responsePdf = given()
                .auth().oauth2(SUPPLIER_USER_TOKEN)
                .queryParam("exportType", StatementExportType.PDF)
                .get("/reports/purchaseReport/export")
                .then()
                .statusCode(200)
                .extract().response();

        saveExport(responsePdf, "supp_purchase_report.pdf");


        Response responseCustomer = given()
                .auth().oauth2(SUPPLIER_USER_TOKEN)
                .queryParam("exportType", StatementExportType.EXCEL)
                .get("/reports/supplierCustomersReport")
                .then()
                .statusCode(200)
                .extract().response();

        saveExport(responseCustomer, "supp_customer_purchase_report.xlsx");


        Response responseCustomerPdf = given()
                .auth().oauth2(SUPPLIER_USER_TOKEN)
                .queryParam("exportType", StatementExportType.PDF)
                .get("/reports/supplierCustomersReport")
                .then()
                .statusCode(200)
                .extract().response();

        saveExport(responseCustomerPdf, "supp_customer_purchase_report.pdf");
    }

    @Test
    public void testLoanManagementReport() throws IOException {
//        Response response = given()
//                .auth().oauth2(ADMIN_TOKEN)
//                .queryParam("exportType", StatementExportType.EXCEL)
//                .get("/reports/loanManagementReportExport")
//                .then()
//                .statusCode(200)
//                .extract().response();

//       saveExport(response, "loanManahementExcel.xlsx");

        Response responsePdf = given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("exportType", StatementExportType.PDF)
                .get("/reports/loanManagementReportExport")
                .then()
                .statusCode(200)
                .extract().response();

       // saveExport(responsePdf, "loanManahementPdf.pdf");
    }

    @Test
    public void testBnplCustomerListReport() throws IOException {
        Response response = given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("exportType", StatementExportType.EXCEL)
                .get("/reports/bnplCustomerListReportExport")
                .then()
                .statusCode(200)
                .extract().response();

//        saveExport(response, "bnplCustomerListExcel.xlsx");


        Response responsePdf = given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("exportType", StatementExportType.PDF)
                .get("/reports/bnplCustomerListReportExport")
                .then()
                .statusCode(200)
                .extract().response();

//        saveExport(responsePdf, "bnplCustomerListPdf.pdf");
    }

    // eligible customers report test is covered in BNPLServiceTests as it uses the same service method

    @Test
    public void testBnplEligibleCustomerTopReport() throws IOException {
        Response response = given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("exportType", StatementExportType.EXCEL)
                .get("/reports/eligibleBNPLCustomers")
                .then()
                .statusCode(200)
                .extract().response();

//        saveExport(response, "bnplEligibleCustomerTopExcel.xlsx");

        Response responsePdf = given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("exportType", StatementExportType.PDF)
                .get("/reports/eligibleBNPLCustomers")
                .then()
                .statusCode(200)
                .extract().response();

//        saveExport(responsePdf, "bnplEligibleCustomerTopPdf.pdf");
    }


    @Test
//    @Ignore
    public void testInvoiceReport() throws IOException {
        // fetch file for receipt

        UUID orderPublicId = UUID.fromString("5b127535-2e8b-43b9-854b-72d3159609d7");

        Response response = given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("orderType", OrderType.CASH)
                .get("/reports/{salesOrderPublic}/customerInvoiceReceipt", orderPublicId)
                .then()
                .statusCode(200)
                .extract().response();

        saveExport(response, "receipty.pdf");
    }

    @Test
    @Ignore
    public void testCustomerPaymentReport() throws IOException {
        // fetch file for receipt

        Response response = given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("exportType", StatementExportType.EXCEL)
                .get("/reports/creditPaymentReportExport")
                .then()
                .statusCode(200)
                .extract().response();

        // saveExport(response, "customerPaymentReportExcel.xlsx");
    }

    @Test
    @Ignore
    public void testBnplOrderReport() throws IOException {
        // fetch file for receipt

        Response response = given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("exportType", StatementExportType.EXCEL)
                .get("/reports/bnplSalesReportExport")
                .then()
                .statusCode(200)
                .extract().response();

        // saveExport(response, "bnplOrderExcel.xlsx");

        Response responsePdf = given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("exportType", StatementExportType.PDF)
                .get("/reports/bnplSalesReportExport")
                .then()
                .statusCode(200)
                .extract().response();

        // saveExport(responsePdf, "bnplOrderPdf.pdf");
    }

    @Test
    @Ignore
    public void testBnplWalletReport() throws IOException {
        // fetch file for receipt

        Response response = given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("exportType", StatementExportType.EXCEL)
                .get("/reports/walletReportExport")
                .then()
                .statusCode(200)
                .extract().response();

       // saveExport(response, "bnplWalletExcel.xlsx");

        Response responsePdf = given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("exportType", StatementExportType.PDF)
                .get("/reports/walletReportExport")
                .then()
                .statusCode(200)
                .extract().response();

      //  saveExport(responsePdf, "bnplWalletPdf.pdf");
    }

    @Test
    public void test_fetchCreditRecon() throws IOException {
        given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/reconciliation/creditRecons")
                .then().log().all()
                .statusCode(200);

        Response responsePdf = given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("exportType", StatementExportType.PDF)
                .get("/reports/creditReconReportExport")
                .then()
                .statusCode(200)
                .extract().response();

          saveExport(responsePdf, "creditRecons.pdf");
    }

    @Test
    @Ignore
    public void fetchOrderRecon() throws IOException {

    }
}
