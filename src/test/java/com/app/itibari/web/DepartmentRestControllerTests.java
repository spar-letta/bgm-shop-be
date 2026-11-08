package com.app.itibari.web;

import com.app.itibari.ItibariApplicationTests;
import com.app.itibari.models.request.DepartmentRequest;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalToIgnoringCase;

public class DepartmentRestControllerTests extends ItibariApplicationTests {

    @Test
    public void markProductAsBestSellerTest() {

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

        departmentRequest = DepartmentRequest.builder()
                .name("Logistics-2")
                .build();
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(departmentRequest).log().all()
                .put("/departments/{publicId}", departmentPublicId)
                .then().log().all()
                .statusCode(200)
                .body("name", equalToIgnoringCase(departmentRequest.name()));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/departments")
                .then().log().all()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/departments/{publicId}", departmentPublicId)
                .then().log().all()
                .statusCode(200);
    }
}
