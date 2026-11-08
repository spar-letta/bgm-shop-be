package com.app.itibari.web;

import com.app.itibari.ItibariApplicationTests;
import com.app.itibari.dataTypes.DiscountStatus;
import com.app.itibari.dataTypes.DiscountType;
import com.app.itibari.dataTypes.OrderType;
import com.app.itibari.dataTypes.SourceChannelEnum;
import com.app.itibari.models.request.*;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

@Slf4j
public class WalkInSalesTests extends ItibariApplicationTests {

    @Test
    @Ignore("Okay, when run alone!")
    public void testRunDiscount() {
        UUID productId = UUID.fromString("7c4a6d52-5da2-47fe-92a8-ea2fce2602af");
        DiscountCreateRequest discountCreateRequest = new DiscountCreateRequest();
        discountCreateRequest.setDiscountType(DiscountType.VolumeDiscount);
        discountCreateRequest.setProductIds(Arrays.asList(productId));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(discountCreateRequest)
                .post("/discounts")
                .then().log().all()
                .statusCode(200);

        String publicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("discountType", DiscountType.VolumeDiscount)
                .get("/discounts")
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].status", equalTo(DiscountStatus.Incomplete.name()))
                .extract().path("content[0].publicId");

        VolumeRangeCreateRequest volumeRangeCreateRequest = new VolumeRangeCreateRequest();
        volumeRangeCreateRequest.setMinVolume(5);
        volumeRangeCreateRequest.setMaxVolume(10);
        volumeRangeCreateRequest.setDiscountAmount(new BigDecimal(20));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(volumeRangeCreateRequest)
                .log().all()
                .patch("/discounts/{discountPublicId}/addVolumeRange", UUID.fromString(publicId))
                .then().log().all()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/discounts/{discountPublicId}/volumeRanges", UUID.fromString(publicId))
                .then().log().all()
                .statusCode(200)
                .body("content.size()", equalTo(1));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("discountType", DiscountType.VolumeDiscount)
                .get("/discounts")
                .then().log().all()
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
                .then().log().all()
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
                .then().log().all()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/products")
                .then().log().all()
                .statusCode(200);
    }

    @Test
    public void testCreateSalesOrder_WalkIn_Works() throws IOException {
        UUID productId = UUID.fromString("7c4a6d52-5da2-47fe-92a8-ea2fce2602af");
        UUID customerPublicId = UUID.fromString("d7c5bf34-fda1-46e2-b5ee-40db98aed06d");

        WalkInSalesOrdersRequest.OrderItemsRequest orderItems = WalkInSalesOrdersRequest.OrderItemsRequest
                .builder()
                .productId(productId)
                .quantity(5)
                .volumeDiscount(BigDecimal.valueOf(5 * 10))  // 5 * 10
                .specialDiscount(BigDecimal.valueOf(5 * 5)) // 5 * 5
                .build();

        WalkInSalesOrdersRequest request = WalkInSalesOrdersRequest.builder()
                .customerId(customerPublicId)
                .sourceChannel(SourceChannelEnum.BACK_OFFICE_APP.name())
                .orderItemsRequest(Collections.singleton(orderItems))
                .build();

        String orderPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request).log().all()
                .post("/salesOrders/walk-in-order")
                .then().log().all()
                .statusCode(200)
                .extract().path("publicId");

        log.info("-sales order--");
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("isWalkIn", true)
                .get("/salesOrders")
                .then().log().all()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders/{salesOrderPublicId}/items", UUID.fromString(orderPublicId))
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
//                .body("content[0].quantity")
                .extract().path("content[0].publicId");

        log.info("-------recon wrapper---");
        String productReconId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders/walk-in-product-reconciliations")
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].publicId");

        log.info("-------recon items---");
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/warehouse/productRecons/{publicId}/items", UUID.fromString(productReconId))
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("walkIn", Boolean.TRUE)
                .get("/reconciliation")
                .then().log().all()
                .statusCode(200);

//        Response response = given()
//                .auth().oauth2(ADMIN_TOKEN)
//                .queryParam("orderType", OrderType.CASH)
//                .get("/reports/{salesOrderPublic}/customerInvoiceReceipt", UUID.fromString(orderPublicId))
//                .then()
//                .statusCode(200)
//                .extract().response();
//
//        saveExport(response, "walk_in_tra_receipt.pdf");
    }
}
