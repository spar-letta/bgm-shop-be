package com.app.itibari.web;

import com.app.itibari.ItibariApplicationTests;
import com.app.itibari.dataTypes.ProductDisposalType;
import com.app.itibari.dataTypes.QuarantineAction;
import com.app.itibari.dataTypes.StatementExportType;
import com.app.itibari.models.request.DisposalRequest;
import com.app.itibari.models.request.QuarantineProductRequest;
import com.app.itibari.models.request.QuarantineRequest;
import com.app.itibari.scheduler.Scheduler;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;

@Slf4j
public class QuarantineRestControllerTests extends ItibariApplicationTests {

    @Autowired
    private Scheduler scheduler;

    //    @Ignore  // runs alone
    @Test
    public void testCreateQuarantineProductWorks() throws IOException {
//        scheduler.productStockUpdate(LocalDate.now());
        UUID productId = UUID.fromString("581c5c73-afc0-4b89-bafb-e160011f2715");
        UUID inventoryProductId = UUID.fromString("94a50a32-7e62-4aad-84d9-8383cc78cd1b");
        UUID mainWarehouseId = UUID.fromString("3849b1be-a9dd-4f11-abbf-229ad2780a56");
        UUID quarantineWarehouseId = UUID.fromString("6bd6caf4-f11b-4eba-bc9d-419d4668d182");
        UUID reasonId = UUID.fromString("632b6fda-e23c-4a5f-bec0-a0f9e1f0e293");
        UUID rejectionCodePublicId = UUID.fromString("632b6fda-e23c-4a5f-bec0-a0f9e1f0e293");

        given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .queryParam("productPublicId", productId)
                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
                .then()
                .statusCode(200)
                .body("content[0].availableStock", equalTo(20))
                .body("content[0].reservedStock", equalTo(0))
                .body("content[0].quarantinedStock", equalTo(0))
                .body("content[0].totalStock", equalTo(20));

        QuarantineRequest quarantineRequest = new QuarantineRequest();
        quarantineRequest.setMainWarehousePublicId(mainWarehouseId);
        quarantineRequest.setQuarantineWarehousePublicId(quarantineWarehouseId);

        QuarantineProductRequest quarantineProductRequest = new QuarantineProductRequest();
        quarantineProductRequest.setInventoryProductPublicId(inventoryProductId);
        quarantineProductRequest.setQuantity(6);
        quarantineProductRequest.setReasonPublicId(reasonId);
        quarantineRequest.setQuarantineProducts(Arrays.asList(quarantineProductRequest));

        String quarantinePublicId = given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .body(quarantineRequest)
                .post("/quarantine")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .body(quarantineRequest)
                .queryParam("action", QuarantineAction.Approve)
                .patch("/quarantine/{publicId}/actions", UUID.fromString(quarantinePublicId))
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .get("/quarantine/{publicId}", UUID.fromString(quarantinePublicId))
                .then().log().all()
                .statusCode(200)
                .body("transferType", equalToIgnoringCase("TO_QUARANTINE_WAREHOUSE"))
                .body("quarantineStatus", equalToIgnoringCase("Approved"));

        String quarantineProductId = given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .get("/quarantine/{publicId}/quarantineProducts", UUID.fromString(quarantinePublicId))
                .then().log().all()
                .statusCode(200)
                .body("content[0].quantity", equalTo(6))
                .body("content[0].productName", equalToIgnoringCase("Biscuite 50G x 200"))
                .body("content[0].reasonDescription", equalToIgnoringCase("Shop Closed"))
                .extract().path("content[0].publicId");

        given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .queryParam("productPublicId", productId)
                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
                .then()
                .statusCode(200)
                .body("content[0].availableStock", equalTo(14))
                .body("content[0].reservedStock", equalTo(0))
                .body("content[0].quarantinedStock", equalTo(6))
                .body("content[0].totalStock", equalTo(20));

        given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .queryParam("quantity", 2)
                .pathParam("publicId", UUID.fromString(quarantinePublicId))
                .pathParam("quarantineProductId", quarantineProductId)
                .post("/quarantine/{publicId}/returnProduct/{quarantineProductId}")
                .then()
                .statusCode(200);

        String quarantineProductPublicId = given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .get("/quarantine/{publicId}/quarantineProducts", UUID.fromString(quarantinePublicId))
                .then().log().all()
                .statusCode(200)
                .body("content[0].quantity", equalTo(4))
                .extract().path("content[0].publicId");

        given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .queryParam("productPublicId", productId)
                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
                .then()
                .statusCode(200)
                .body("content[0].availableStock", equalTo(16))
                .body("content[0].reservedStock", equalTo(0))
                .body("content[0].quarantinedStock", equalTo(4))
                .body("content[0].totalStock", equalTo(20));

        String liveDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .queryParam("warehouseId", mainWarehouseId)
                .queryParam("liveDate", liveDate)
                .queryParam("searchParam", "bisc")
                .get("/reports/liveStockReport")
                .then().log().all()
                .statusCode(200);

        log.info("-------from here-------");

        DisposalRequest disposalRequest = DisposalRequest.builder()
                .comment("This product is disposed")
                .build();
        given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .body(quarantineRequest)
                .queryParam("disposalType", ProductDisposalType.Dispose)
                .queryParam("reasonCodePublicId", rejectionCodePublicId)
                .body(disposalRequest).log().all()
                .patch("/quarantine/{publicId}/disposeQuarantineProduct/{quarantineProductId}", UUID.fromString(quarantinePublicId), UUID.fromString(quarantineProductPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .get("/quarantine/{publicId}/quarantineProducts", UUID.fromString(quarantinePublicId))
                .then().log().all()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .body(quarantineRequest)
                .queryParam("disposalType", ProductDisposalType.Approve)
                .queryParam("reasonCodePublicId", rejectionCodePublicId)
                .patch("/quarantine/{publicId}/disposeQuarantineProduct/{quarantineProductId}", UUID.fromString(quarantinePublicId), UUID.fromString(quarantineProductPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .get("/quarantine/{publicId}/quarantineProducts", UUID.fromString(quarantinePublicId))
                .then().log().all()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .queryParam("warehouseId", mainWarehouseId)
                .queryParam("liveDate", liveDate)
                .queryParam("searchParam", "bisc")
                .get("/reports/liveStockReport")
                .then().log().all()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .get("/quarantine/quarantineApprovedProducts")
                .then().log().all()
                .statusCode(200);

        Response response_1 = given()
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .queryParam("exportType", StatementExportType.PDF)
                .get("/reports/get-disposed-quarantine-product-export")
                .then()
                .statusCode(200)
                .extract().response();

        saveExport(response_1, "disposed_quarantine_product_export.pdf");
    }
}
