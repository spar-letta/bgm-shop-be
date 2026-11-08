package com.app.itibari.web;

import com.app.itibari.ItibariApplicationTests;
import org.junit.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

public class PersonRestControllerTests extends ItibariApplicationTests {

    @Test
    public void testGetPermissionGroup() {

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/system/permissionGroup")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));

    }

    @Test
    public void testGetRole() {
     String publicId =    given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/system/roles")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract()
                .path("content[0].publicId");

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/system/{rolePublicId}/rolePermissions", UUID.fromString(publicId))
                .then().log().all()
                .statusCode(200);

    }


    @Test
    public void getPersonsTest(){
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/system/persons")
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));
    }

    @Test
    public void getPersonsInRoleTest(){
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("roleName", "ROLE_SALES_AGENT")
                .get("/system/personInRole")
                .then().log().all()
                .statusCode(200);
    }
}
