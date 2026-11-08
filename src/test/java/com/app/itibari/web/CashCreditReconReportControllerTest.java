package com.app.itibari.web;

import com.app.itibari.ItibariApplicationTests;
import org.junit.Ignore;
import org.junit.Test;

import java.time.LocalDate;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;

public class CashCreditReconReportControllerTest extends ItibariApplicationTests {

    private static final LocalDate REPORT_DATE = LocalDate.of(2026, 7, 8);
    private static String reportPublicId;

    @Test
    public void test1GenerateCashCreditReconReport() {
        reportPublicId = given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("date", REPORT_DATE.toString())
                .post("/reconciliation/cash-credit-reports")
                .then()
                .statusCode(200)
                .log().all()
                .body("publicId", notNullValue())
                .body("reportDate", equalTo(REPORT_DATE.toString()))
                .body("status", equalTo("GENERATED"))
                .extract().path("publicId");
    }

    @Test
    public void test2GetCashCreditReconReport() {
        given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/reconciliation/cash-credit-reports/{publicId}", reportPublicId)
                .then()
                .statusCode(200)
                .body("publicId", equalTo(reportPublicId))
                .body("reportDate", equalTo(REPORT_DATE.toString()))
                .body("status", equalTo("GENERATED"))
                .body("lines", notNullValue());
    }

    @Test
    public void test3RefreshCashCreditReconReport() {
        given()
                .auth().oauth2(ADMIN_TOKEN)
                .patch("/reconciliation/cash-credit-reports/{publicId}/refresh", reportPublicId)
                .then()
                .statusCode(200)
                .body("publicId", equalTo(reportPublicId))
                .body("status", equalTo("GENERATED"));
    }

    @Test
    public void test4UpdateCashCreditReconAdjustmentRejectsInvalidRequest() {
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body("{}")
                .patch("/reconciliation/cash-credit-reports/{reportPublicId}/lines/{linePublicId}/adjustments",
                        reportPublicId, UUID.randomUUID())
                .then()
                .statusCode(400);
    }

    @Ignore
    @Test
    public void test5GetCashCreditReconReportEvents() {
        given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/reconciliation/cash-credit-reports/{publicId}/events", reportPublicId)
                .then()
                .statusCode(200)
                .body("content.size()", greaterThan(0))
                .body("content[0].referenceId", equalTo(reportPublicId));
    }

    @Test
    public void test6ReconcileCashCreditReconReport() {
        given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("comment", "Approved by finance integration test")
                .patch("/reconciliation/cash-credit-reports/{publicId}/reconcile", reportPublicId)
                .then()
                .statusCode(200)
                .body("publicId", equalTo(reportPublicId))
                .body("status", equalTo("RECONCILED"))
                .body("reconciliationComment", equalTo("Approved by finance integration test"));
    }

    @Test
    public void test7ListAdjustmentsWithoutSearchParamReturnsOk() {
        given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .get("/reconciliation/cash-credit-reports/adjustments")
                .then()
                .statusCode(200)
                .body("content", notNullValue());
    }

}
