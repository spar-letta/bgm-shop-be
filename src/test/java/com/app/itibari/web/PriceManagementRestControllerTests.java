package com.app.itibari.web;

import com.app.itibari.ItibariApplicationTests;
import com.app.itibari.models.request.PriceManagementRequest;
import com.app.itibari.models.request.PriceManagementRequestItem;
import org.junit.Ignore;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static io.restassured.RestAssured.given;

public class PriceManagementRestControllerTests extends ItibariApplicationTests {
    @Test
    @Ignore
    public void testPriceManagementController() {
        PriceManagementRequest request = new PriceManagementRequest();
        PriceManagementRequestItem item = new PriceManagementRequestItem();

        item.setProductName("Bwana Sukari Brown Sugar 5KG x 4");
        item.setSku("SKU100020");
        item.setPrice(BigDecimal.valueOf(99.99));
        item.setProductId(UUID.fromString("08ebb084-2633-428f-82b9-e8d98a22ec53"));

        request.getItems().add(item);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .post("/priceManagement")
                .then()
                .statusCode(200);

    }
}
