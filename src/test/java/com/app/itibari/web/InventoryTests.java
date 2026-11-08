package com.app.itibari.web;

import com.app.itibari.ItibariApplicationTests;
import com.app.itibari.models.request.PayloadCarrierDto;
import io.restassured.http.ContentType;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

@Slf4j
public class InventoryTests extends ItibariApplicationTests {

    @Ignore
    @Test
    public void testInventory() throws IOException {

        UUID warehouseId = UUID.fromString("3849b1be-a9dd-4f11-abbf-229ad2780a56");
        UUID productId = UUID.fromString("94c964b0-7a94-4f64-9b8a-96a365584409");
        UUID productId2 = UUID.fromString("7c4a6d52-5da2-47fe-92a8-ea2fce2602af");
        UUID inventoryPublicId = UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386");
        UUID supplierPublicId = UUID.fromString("0bacd072-df86-4d05-bf86-27a1d38cacfb");
        UUID inventoryProductPublicId = UUID.fromString("c9191b06-f3d9-464f-be23-9d0c414895a2");

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/warehouse/{id}", warehouseId)
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/inventory/{id}/products", inventoryPublicId)
                .then()
                .statusCode(200)
                .body("content.product.availableStock", containsInAnyOrder(500, 249))
                .body("content.availableStock", containsInAnyOrder(500, 250))
                .body("content.reservedStock", containsInAnyOrder(0, 0))
                .body("content.onHoldStock", containsInAnyOrder(0, 0))
                .body("content.quarantinedStock", containsInAnyOrder(0, 0))
                .body("content.totalStock", containsInAnyOrder(500, 250))
                .body("content.closingStock", containsInAnyOrder(500, 250));

        // 500 + 500 = 1000
        PayloadCarrierDto payloadCarrierDto = runPurchaseOrder(warehouseId, productId, supplierPublicId, productId2);
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/inventory/{id}/products", inventoryPublicId)
                .then()
                .statusCode(200)
                .body("content.product.availableStock", containsInAnyOrder(249, 1000))
                .body("content.availableStock", containsInAnyOrder(250, 1000))
                .body("content.reservedStock", containsInAnyOrder(0, 0))
                .body("content.onHoldStock", containsInAnyOrder(0, 0))
                .body("content.quarantinedStock", containsInAnyOrder(0, 0))
                .body("content.totalStock", containsInAnyOrder(250, 1000))
                .body("content.closingStock", containsInAnyOrder(250, 500));

        // 1000 + 70 = 1070
        runPromoGoods(UUID.fromString(payloadCarrierDto.shipmentReceivedNotePublicId()), productId, payloadCarrierDto.purchaseOrderNumber());
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/inventory/{id}/products", inventoryPublicId)
                .then()
                .statusCode(200)
                .body("content.product.availableStock", containsInAnyOrder(249, 1070))
                .body("content.availableStock", containsInAnyOrder(250, 1070))
                .body("content.reservedStock", containsInAnyOrder(0, 0))
                .body("content.onHoldStock", containsInAnyOrder(0, 0))
                .body("content.quarantinedStock", containsInAnyOrder(0, 0))
                .body("content.totalStock", containsInAnyOrder(250, 1070))
                .body("content.closingStock", containsInAnyOrder(250, 500));

        // 1070 - 10 = 1060
        runGoodsReturns(UUID.fromString(payloadCarrierDto.shipmentReceivedNotePublicId()), UUID.fromString(payloadCarrierDto.itemReceivedPublicId()));
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/inventory/{id}/products", inventoryPublicId)
                .then()
                .statusCode(200)
                .body("content.product.availableStock", containsInAnyOrder(249, 1060))
                .body("content.availableStock", containsInAnyOrder(250, 1060))
                .body("content.reservedStock", containsInAnyOrder(0, 0))
                .body("content.onHoldStock", containsInAnyOrder(0, 0))
                .body("content.quarantinedStock", containsInAnyOrder(0, 0))
                .body("content.totalStock", containsInAnyOrder(250, 1060))
                .body("content.closingStock", containsInAnyOrder(250, 500));


        runOrderPlaceBySA(productId, inventoryPublicId, warehouseId, inventoryProductPublicId, productId2);
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/inventory/{id}/products", inventoryPublicId)
                .then()
                .statusCode(200)
                .body("content[0].product.availableStock", equalTo(561))
                .body("content[0].availableStock", equalTo(561))
                .body("content[0].reservedStock", equalTo(0))
                .body("content[0].onHoldStock", equalTo(0))
                .body("content[0].quarantinedStock", equalTo(0))
                .body("content[0].totalStock", equalTo(561))
                .body("content[0].closingStock", equalTo(500));

        testCreateQuarantineProductWorks(productId, inventoryPublicId, warehouseId,
                inventoryProductPublicId);
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/inventory/{id}/products", inventoryPublicId)
                .then()
                .statusCode(200)
                .body("content[0].product.availableStock", equalTo(559))
                .body("content[0].availableStock", equalTo(559))
                .body("content[0].reservedStock", equalTo(0))
                .body("content[0].onHoldStock", equalTo(0))
                .body("content[0].quarantinedStock", equalTo(2))
                .body("content[0].totalStock", equalTo(561))
                .body("content[0].closingStock", equalTo(500));
    }
}
