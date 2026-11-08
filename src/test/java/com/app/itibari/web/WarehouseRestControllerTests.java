package com.app.itibari.web;

import com.app.itibari.ItibariApplicationTests;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

public class WarehouseRestControllerTests extends ItibariApplicationTests {

    @Test
    public void fetchAllWarehouses() {
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/warehouse")
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));
    }

    @Test
    public void fetchStagings(){
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/warehouse/staged")
                .then()
                .statusCode(200);
    }
}
