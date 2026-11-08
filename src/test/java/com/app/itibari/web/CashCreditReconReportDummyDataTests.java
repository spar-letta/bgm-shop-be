package com.app.itibari.web;

import com.app.itibari.ItibariApplicationTests;
import com.app.itibari.dataTypes.CashCreditReconAdjustmentType;
import com.app.itibari.dataTypes.CashCreditReconAdjustmentStatus;
import com.app.itibari.dataTypes.CashCreditReconLineStatus;
import com.app.itibari.dataTypes.CashCreditReconReportStatus;
import com.app.itibari.dataTypes.StatementExportType;
import com.app.itibari.models.request.CashCreditReconAdjustmentRequest;
import com.app.itibari.models.response.CashCreditReconAdjustmentDTO;
import com.app.itibari.models.response.CashCreditReconPendingAdjustmentsDTO;
import com.app.itibari.models.response.CashCreditReconReportDTO;
import com.app.itibari.models.response.CashCreditReconReportLineDTO;
import com.app.itibari.services.ReconciliationService;
import com.app.itibari.services.ReportService;
import com.app.itibari.utils.reportHelper.CashCreditReconReportExport;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CashCreditReconReportDummyDataTests extends ItibariApplicationTests {

    private static final UUID REPORT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BALANCED_LINE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID UNDER_LINE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID OVER_LINE_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final LocalDate REPORT_DATE = LocalDate.of(2026, 7, 9);
    private static final UUID ADJUSTMENT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @MockBean
    private ReconciliationService reconciliationService;

    @MockBean
    private ReportService reportService;

    @Autowired
    private CashCreditReconReportExport reportExport;

    private CashCreditReconReportDTO generatedReport;
    private CashCreditReconReportDTO reconciledReport;

    @Before
    public void configureDummyData() {
        generatedReport = dummyReport(CashCreditReconReportStatus.GENERATED);
        reconciledReport = dummyReport(CashCreditReconReportStatus.RECONCILED);

        when(reconciliationService.generateCashCreditReconReport(eq(REPORT_DATE), anyString()))
                .thenReturn(generatedReport);
        when(reconciliationService.getCashCreditReconReport(REPORT_ID)).thenReturn(generatedReport);
        when(reconciliationService.refreshCashCreditReconReport(eq(REPORT_ID), anyString()))
                .thenReturn(generatedReport);
        when(reconciliationService.updateCashCreditReconAdjustment(eq(REPORT_ID), eq(BALANCED_LINE_ID), any(), anyString()))
                .thenReturn(generatedReport);
        when(reconciliationService.reconcileCashCreditReconReport(eq(REPORT_ID), anyString(), anyString()))
                .thenReturn(reconciledReport);
        when(reconciliationService.getCashCreditReconReportEvents(eq(REPORT_ID), any()))
                .thenReturn(new PageImpl<>(List.of()));
        when(reconciliationService.getCashCreditReconAdjustments(any(),any(),any(),any(),any()))
                .thenReturn(new PageImpl<>(List.of(pendingAdjustments())));
        when(reconciliationService.reviewCashCreditReconAdjustment(eq(ADJUSTMENT_ID),
                eq(CashCreditReconAdjustmentStatus.APPROVED), any(), anyString()))
                .thenReturn(logisticsExpense(CashCreditReconAdjustmentStatus.APPROVED));

        when(reportService.downloadCashCreditReconReport(any(CashCreditReconReportDTO.class),
                eq(StatementExportType.EXCEL), anyString()))
                .thenAnswer(invocation -> reportExport.generateCashCreditReconExcel(
                        invocation.getArgument(0), "Dummy Finance Manager"));
        when(reportService.downloadCashCreditReconReport(any(CashCreditReconReportDTO.class),
                eq(StatementExportType.PDF), anyString()))
                .thenAnswer(invocation -> reportExport.generateCashCreditReconPdf(
                        invocation.getArgument(0), "Dummy Finance Manager"));
    }

    @Ignore
    @Test
    public void testCashCreditEndpointsWithDummyData() {
        given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("date", REPORT_DATE.toString())
                .post("/reconciliation/cash-credit-reports")
                .then()
                .statusCode(200)
                .log().all()
                .body("publicId", equalTo(REPORT_ID.toString()))
                .body("status", equalTo("GENERATED"))
                .body("lines", hasSize(3))
                .body("lines[0].deliveryAgentName", equalTo("Abdallah Rajabu Omary"))
                .body("lines[1].status", equalTo("UNDER"))
                .body("lines[2].status", equalTo("OVER"));

        given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/reconciliation/cash-credit-reports/{publicId}", REPORT_ID)
                .then()
                .statusCode(200)
                .log().all()
                .body("totalGmvDelivered", equalTo(12000000))
                .body("lines", hasSize(3));

        given()
                .auth().oauth2(ADMIN_TOKEN)
                .patch("/reconciliation/cash-credit-reports/{publicId}/refresh", REPORT_ID)
                .then()
                .log().all()
                .statusCode(200)
                .body("publicId", equalTo(REPORT_ID.toString()));

        CashCreditReconAdjustmentRequest request = new CashCreditReconAdjustmentRequest();
        request.setType(CashCreditReconAdjustmentType.PARKING_FEE);
        request.setAmount(new BigDecimal("15000"));
        request.setNote("Dummy parking receipt");
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .patch("/reconciliation/cash-credit-reports/{reportId}/lines/{lineId}/adjustments",
                        REPORT_ID, BALANCED_LINE_ID)
                .then()
                .statusCode(200)
                .body("lines[0].adjustments[0].type", equalTo("PARKING_FEE"));

        given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/reconciliation/cash-credit-reports/{publicId}/events", REPORT_ID)
                .then()
                .statusCode(200)
                .log().all()
                .body("content", notNullValue());

        given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("comment", "Approved dummy report")
                .patch("/reconciliation/cash-credit-reports/{publicId}/reconcile", REPORT_ID)
                .then()
                .statusCode(200)
                .body("status", equalTo("RECONCILED"))
                .body("reconciliationComment", equalTo("Approved dummy report"));
    }

    @Ignore
    @Test
    public void testListSubmittedLogisticsExpenses() {
        given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/reconciliation/cash-credit-reports/adjustments")
                .then()
                .statusCode(200)
                .body("content", hasSize(1))
                .body("content[0].reportLinePublicId", equalTo(BALANCED_LINE_ID.toString()))
                .body("content[0].reportDate", equalTo(REPORT_DATE.toString()))
                .body("content[0].pendingAdjustments[0].publicId", equalTo(ADJUSTMENT_ID.toString()))
                .body("content[0].pendingAdjustments[0].approvalStatus", equalTo("PENDING"))
                .body("content[0].pendingAdjustments[0].attachmentUrl", equalTo("https://files.example/parking-receipt.pdf"));

        verify(reconciliationService).getCashCreditReconAdjustments(null,null,null,null,any());
    }

    @Ignore
    @Test
    public void testApproveSubmittedLogisticsExpense() {
        given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("status", "APPROVED")
                .queryParam("comment", "Receipt verified")
                .patch("/reconciliation/cash-credit-reports/adjustments/{publicId}/review", ADJUSTMENT_ID)
                .then()
                .statusCode(200)
                .body("publicId", equalTo(ADJUSTMENT_ID.toString()))
                .body("approvalStatus", equalTo("APPROVED"));

        verify(reconciliationService).reviewCashCreditReconAdjustment(eq(ADJUSTMENT_ID),
                eq(CashCreditReconAdjustmentStatus.APPROVED), eq("Receipt verified"), anyString());
    }

    @Test
    @Ignore
    public void testExcelAndPdfDownloadsWithDummyData() throws IOException {
        Response excel = given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("exportType", StatementExportType.EXCEL)
                .get("/reports/cash-credit-reports/{publicId}/download", REPORT_ID)
                .then()
                .statusCode(200)
                .contentType(StatementExportType.EXCEL.contentType)
                .extract().response();
        saveExport(excel, "cash_credit_reconciliation_dummy.xlsx");
        assertDownload("cash_credit_reconciliation_dummy.xlsx", "PK");

        Response pdf = given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("exportType", StatementExportType.PDF)
                .get("/reports/cash-credit-reports/{publicId}/download", REPORT_ID)
                .then()
                .statusCode(200)
                .contentType(StatementExportType.PDF.contentType)
                .extract().response();
        saveExport(pdf, "cash_credit_reconciliation_dummy.pdf");
        assertDownload("cash_credit_reconciliation_dummy.pdf", "%PDF");
    }

    private CashCreditReconReportDTO dummyReport(CashCreditReconReportStatus status) {
        List<CashCreditReconReportLineDTO> lines = List.of(
                line(BALANCED_LINE_ID, "Abdallah Rajabu Omary", new BigDecimal("6888500"),
                        new BigDecimal("55000"), new BigDecimal("6833500"), BigDecimal.ZERO,
                        new BigDecimal("6833500"), BigDecimal.ZERO, CashCreditReconLineStatus.BALANCED, true),
                line(UNDER_LINE_ID, "Ibrahimu Abdala Juma", new BigDecimal("3179500"),
                        new BigDecimal("2500"), new BigDecimal("3177000"), new BigDecimal("-100000"),
                        new BigDecimal("3077000"), new BigDecimal("-100000"), CashCreditReconLineStatus.UNDER, false),
                line(OVER_LINE_ID, "Revocatus Lauliani Longino", new BigDecimal("1932000"),
                        new BigDecimal("20000"), new BigDecimal("1912000"), BigDecimal.ZERO,
                        new BigDecimal("1912500"), new BigDecimal("500"), CashCreditReconLineStatus.OVER, false));

        return CashCreditReconReportDTO.builder()
                .publicId(REPORT_ID)
                .reportDate(REPORT_DATE)
                .status(status)
                .generatedAt(LocalDateTime.of(2026, 7, 9, 12, 0))
                .reconciledAt(status == CashCreditReconReportStatus.RECONCILED
                        ? LocalDateTime.of(2026, 7, 9, 13, 0) : null)
                .reconciledBy(status == CashCreditReconReportStatus.RECONCILED ? "Dummy Finance Manager" : null)
                .reconciliationComment(status == CashCreditReconReportStatus.RECONCILED
                        ? "Approved dummy report" : null)
                .totalGmvDelivered(new BigDecimal("12000000"))
                .totalDiscounts(new BigDecimal("77500"))
                .totalComplementaries(BigDecimal.ZERO)
                .totalNetGmv(new BigDecimal("11922500"))
                .totalLoadingOffloading(new BigDecimal("-100000"))
                .totalCreditOrders(BigDecimal.ZERO)
                .totalParkingFee(new BigDecimal("15000"))
                .totalFerryCost(BigDecimal.ZERO)
                .totalDepositedAmount(new BigDecimal("11822800"))
                .totalDifference(new BigDecimal("-99500"))
                .totalMetaOutstanding(BigDecimal.ZERO)
                .lines(lines)
                .build();
    }

    private CashCreditReconAdjustmentDTO logisticsExpense(CashCreditReconAdjustmentStatus status) {
        return CashCreditReconAdjustmentDTO.builder()
                .publicId(ADJUSTMENT_ID)
                .type(CashCreditReconAdjustmentType.PARKING_FEE)
                .amount(new BigDecimal("15000"))
                .note("Parking receipt")
                .attachmentUrl("https://files.example/parking-receipt.pdf")
                .approvalStatus(status)
                .reviewComment(status == CashCreditReconAdjustmentStatus.APPROVED ? "Receipt verified" : null)
                .build();
    }

    private CashCreditReconPendingAdjustmentsDTO pendingAdjustments() {
        return CashCreditReconPendingAdjustmentsDTO.builder()
                .reportLinePublicId(BALANCED_LINE_ID)
                .routePublicId("route-public-id")
                .routeId("RT-2222")
                .deliveryAgentPublicId("delivery-agent-public-id")
                .deliveryAgentName("Abdallah Rajabu Omary")
                .reportDate(REPORT_DATE)
                .vehicleNumber("T123 ABC")
                .pendingAdjustments(List.of(logisticsExpense(CashCreditReconAdjustmentStatus.PENDING)))
                .build();
    }

    private CashCreditReconReportLineDTO line(UUID publicId, String agent, BigDecimal gmv,
                                               BigDecimal discounts, BigDecimal netGmv,
                                               BigDecimal loadingOffloading, BigDecimal deposited,
                                               BigDecimal difference, CashCreditReconLineStatus status,
                                               boolean includeAdjustment) {
        List<CashCreditReconAdjustmentDTO> adjustments = includeAdjustment
                ? List.of(CashCreditReconAdjustmentDTO.builder()
                .publicId(UUID.fromString("55555555-5555-5555-5555-555555555555"))
                .type(CashCreditReconAdjustmentType.PARKING_FEE)
                .amount(new BigDecimal("15000"))
                .note("Dummy parking receipt")
                .build()) : List.of();

        return CashCreditReconReportLineDTO.builder()
                .publicId(publicId)
                .routePublicId(UUID.randomUUID().toString())
                .routeId("RT-" + publicId.toString().substring(0, 4))
                .deliveryAgentPublicId(UUID.randomUUID().toString())
                .deliveryAgentName(agent)
                .vehicleNumber("T123 ABC")
                .gmvDelivered(gmv)
                .discounts(discounts)
                .complementaries(BigDecimal.ZERO)
                .netGmv(netGmv)
                .loadingOffloading(loadingOffloading)
                .creditOrders(BigDecimal.ZERO)
                .parkingFee(includeAdjustment ? new BigDecimal("15000") : BigDecimal.ZERO)
                .ferryCost(BigDecimal.ZERO)
                .depositedAmount(deposited)
                .difference(difference)
                .metaOutstanding(BigDecimal.ZERO)
                .status(status)
                .sourceReconciliationPublicId(UUID.randomUUID().toString())
                .sourceProductReconPublicId(UUID.randomUUID().toString())
                .adjustments(adjustments)
                .build();
    }

    private void assertDownload(String fileName, String signature) throws IOException {
        File file = new File("src/test/resources/downloads", fileName);
        Assert.assertTrue("Expected downloaded file " + file.getAbsolutePath(), file.isFile());
        Assert.assertTrue("Expected downloaded file to contain data", file.length() > 0);
        byte[] bytes = Files.readAllBytes(file.toPath());
        String actual = new String(bytes, 0, Math.min(bytes.length, signature.length()), StandardCharsets.US_ASCII);
        Assert.assertTrue("Unexpected signature for " + fileName, actual.startsWith(signature));
    }
}
