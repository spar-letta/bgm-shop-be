package com.app.itibari.web;

import com.app.itibari.ItibariApplicationTests;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

public class OrganizationsControllerTests extends ItibariApplicationTests {

    @Test
    public void getAllRegions() {
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/organizationRegions")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));

    }

    @Test
    public void getAllLocations() {
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/organizationLocations")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));
    }

    @Test
    public void testGetAllCountries(){
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/organizationCountries")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));
    }

    @Test
    public void getOrganizationCities() {
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/organizationCities")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));
    }

}
