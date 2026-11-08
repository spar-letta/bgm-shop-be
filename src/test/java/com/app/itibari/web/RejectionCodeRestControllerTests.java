package com.app.itibari.web;

import com.app.itibari.ItibariApplicationTests;
import com.app.itibari.dataTypes.Module;
import com.app.itibari.dataTypes.SourceChannelEnum;
import com.app.itibari.models.request.DepartmentRequest;
import com.app.itibari.models.request.SalesOrderItemsRequest;
import com.app.itibari.models.request.SalesOrdersRequest;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collections;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

public class RejectionCodeRestControllerTests extends ItibariApplicationTests {

    @Test
    public void getAllRejectionCodes() {
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("module", Module.Warehouse)
                .queryParam("searchParam", "description")
                .get("/rejectionCodes")
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));
    }


    @Test
    @Ignore
    public void testCreateCustomerRejectionCode() {
        String requestBody = """
                {
                    "code": "CUST_REJ_001",
                    "description": "Customer Rejection Code 001",
                    "module": "Customer"
                }
                """;

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(requestBody)
                .post("/rejectionCodes")
                .then()
                .statusCode(200)
                .body("code", org.hamcrest.Matchers.equalTo("CUST_REJ_001"))
                .body("description", org.hamcrest.Matchers.equalTo("Customer Rejection Code 001"))
                .body("module", org.hamcrest.Matchers.equalTo("Customer"));


        // fetch and verify

        String code = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("module", Module.Customer)
                .get("/rejectionCodes")
                .then()
                .statusCode(200)
                .body("content.size()", org.hamcrest.Matchers.equalTo(1))
                .body("content[0].code", org.hamcrest.Matchers.equalTo("CUST_REJ_001"))
                .body("content[0].description", org.hamcrest.Matchers.equalTo("Customer Rejection Code 001"))
                .body("content[0].module", org.hamcrest.Matchers.equalTo("Customer"))
                .extract().path("content[0].publicId");


        // create order then cancel with rejection code

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

        String orderId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .post("/salesOrders")
                .then().log().all()
                .statusCode(200)
                .extract().path("publicId");


        // cancel order with rejection code

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("rejectionReasonPublicId", UUID.fromString(code))
                .patch("/salesOrders/{salesOrderPublicId}/cancel", orderId)
                .then().log().all()
                .statusCode(200);


        // fetch order and verify rejection code

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders/{salesOrderPublicId}", orderId)
                .then().log().all()
                .statusCode(200)
                .body("salesOrderStatus", org.hamcrest.Matchers.equalTo("CANCELLED"))
                .body("rejectionReason.code", org.hamcrest.Matchers.equalTo("CUST_REJ_001"))
                .body("rejectionReason.description", org.hamcrest.Matchers.equalTo("Customer Rejection Code 001"));
    }

    @Ignore
    @Test
    public void testRejectionWithDepartment() {

        DepartmentRequest departmentRequest = DepartmentRequest.builder()
                .name("Logistics")
                .build();

        String departmentPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(departmentRequest).log().all()
                .post("/departments")
                .then().log().all()
                .statusCode(200)
                .extract().path("publicId");

        String requestBody = """
                {
                    "code": "CUST_REJ_001",
                    "description": "Customer Rejection Code 001",
                    "module": "Crm_CS_Notes",
                    "departmentPublicId": "%s"
                }
                """.formatted(departmentPublicId);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(requestBody)
                .post("/rejectionCodes")
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("departmentPublicId", UUID.fromString(departmentPublicId))
                .get("/rejectionCodes")
                .then().log().all()
                .statusCode(200);
    }

}
