package com.app.itibari.web;

import com.app.itibari.ItibariApplicationTests;
import com.app.itibari.dataTypes.StatementExportType;
import io.restassured.response.Response;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;

public class SalesAgentOrdersControllerTests extends ItibariApplicationTests {

    private final String SALES_AGENT_PUBLIC_ID = "e388bdc8-5de4-41b3-91f5-35ef26c51200";

    @Test
    public void testGetSalesAgentOrdersForSA_returns200() {
        given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders/salesAgentOrders/{salesAgentPublicId}", SALES_AGENT_PUBLIC_ID)
                .then()
                .statusCode(200)
                .body("content", notNullValue());
    }

    @Test
    public void testExportSalesAgentOrdersPdf() throws IOException {
        Response response = given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("exportType", StatementExportType.PDF)
                .get("/reports/salesAgentOrders/{salesAgentPublicId}/export", SALES_AGENT_PUBLIC_ID)
                .then()
                .statusCode(200)
                .contentType(StatementExportType.PDF.contentType)
                .header("Content-Disposition", containsString(".pdf"))
                .extract().response();

       // saveExport(response, "sales_agent_orders.pdf");
       // assertDownloadedFile("sales_agent_orders.pdf", "%PDF");
    }

    private void assertDownloadedFile(String fileName, String signature) throws IOException {
        File file = new File("src/test/resources/downloads", fileName);
        org.junit.Assert.assertTrue("Expected downloaded file " + file.getAbsolutePath(), file.isFile());
        org.junit.Assert.assertTrue("Expected downloaded file to contain data", file.length() > 0);

        byte[] bytes = Files.readAllBytes(file.toPath());
        String actualSignature = new String(bytes, 0, Math.min(bytes.length, signature.length()), StandardCharsets.US_ASCII);
        org.junit.Assert.assertTrue("Unexpected file signature for " + fileName, actualSignature.startsWith(signature));
    }
}
