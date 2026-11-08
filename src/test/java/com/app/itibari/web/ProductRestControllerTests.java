package com.app.itibari.web;

import com.app.itibari.ItibariApplicationTests;
import io.restassured.http.ContentType;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Slf4j
public class ProductRestControllerTests extends ItibariApplicationTests {

    @Test
    public void fetchProductCategories() {
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/products/categories")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));
    }

    @Test
    public void fetchListProducts() {
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("productPublicId", List.of(UUID.fromString("94c964b0-7a94-4f64-9b8a-96a365584409"), UUID.fromString("581c5c73-afc0-4b89-bafb-e160011f2715")))
                .get("/products/get-products")
                .then().log().all()
                .statusCode(200);
    }

    @Test
    public void testFetchProductClasses() {
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/products/classes")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));
    }

    @Test
    public void testFetchProductSubClasses() {
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/products/class/subClasses")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));
    }

    @Test
    public void testFetchSupplierProducts() {
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/products/supplier/{supplierPublicId}", UUID.fromString("e94a7afb-9560-4498-a789-e8197a9e12fd"))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));
    }


    @Test
    public void markProductAsBestSellerTest() {
        UUID productId = UUID.fromString("7c4a6d52-5da2-47fe-92a8-ea2fce2602af");

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("isBestSeller", true)
                .patch("/products/{productId}/markAsBestSeller", productId)
                .then()
                .statusCode(200);


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/products/{publicId}", productId)
                .then()
                .statusCode(200)
                .body("productBestSelling", equalTo(true));

    }


    @Test
    @Ignore
    public void markProductAsInActiveTest() {

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/products")
                .then().log().all()
                .statusCode(200)
                .body("content.size()", equalTo(5));


        UUID productId = UUID.fromString("7c4a6d52-5da2-47fe-92a8-ea2fce2602af");

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("isInactive", true)
                .patch("/products/{productId}/markAsActive", productId)
                .then()
                .statusCode(200);


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/products/{publicId}", productId)
                .then()
                .statusCode(200)
                .body("inActive", equalTo(true));


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("isActive", true)
                .get("/products")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(4));


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("isActive", false)
                .get("/products")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1));
    }

    @Test
    @Ignore
    public void testCreateProduct_withDescription() throws IOException {
        String payload = productPayload("String description", UUID.fromString("a6f320b2-ad75-42a7-859e-253505242736"));
        String publicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(payload).log().all()
                .post("/products")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/products/{publicId}", publicId)
                .then().log().all()
                .statusCode(200)
                .body("description", equalToIgnoringCase("String description"));

        payload = productPayload("description updated", UUID.fromString("a6f320b2-ad75-42a7-859e-253505242736"));
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(payload)
                .patch("/products/{publicId}", publicId)
                .then().log().all()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/products/{publicId}", publicId)
                .then().log().all()
                .statusCode(200)
                .body("description", equalToIgnoringCase("description updated"));
    }

    @Test
    public void testFetchProducts() {

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/products")
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));
    }

    @Test
    public void testFetchProductDetails() {
        given()
                .contentType("application/json")
                .get("/authentication/v2/products")
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/products/v2")
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));
    }

    @Test
    @Ignore
    public void testCreateProduct_and_addToInventory() throws IOException {
        String payload = supplierProductPayload("String description", UUID.fromString("a6f320b2-ad75-42a7-859e" +
                        "-253505242736"),
                "Sabuni ya kibuyu");
        String publicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(payload).log().all()
                .post("/products")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/products/{publicId}", publicId)
                .then().log().all()
                .statusCode(200)
                .body("description", equalToIgnoringCase("String description"));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
                .then().log().all()
                .statusCode(200)
                .body("content.size()", equalTo(5));
    }

    @Test
    public void testCreateProduct_and_supplierTarget() throws IOException {


        String payload = supplierProductPayload("String description", UUID.fromString("a6f320b2-ad75-42a7-859e" +
                        "-253505242736"),
                "Sabuni ya kibuyu");
        String publicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(payload).log().all()
                .post("/products")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/products/{publicId}", publicId)
                .then().log().all()
                .statusCode(200);


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/products/supplierTarget")
                .then().log().all()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/products/{publicId}", publicId)
                .then().log().all()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(payload).log().all()
                .patch("/products/{id}", publicId)
                .then()
                .statusCode(200);

        log.info("--------here");
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/products/{publicId}", publicId)
                .then().log().all()
                .statusCode(200);
    }

    @Test
    public void testGetBrands() throws IOException {
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("searchParam", "bread")
                .get("/products/brands")
                .then().log().all()
                .statusCode(200);
    }
}
