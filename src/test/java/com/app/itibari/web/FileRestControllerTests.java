package com.app.itibari.web;

import com.app.itibari.ItibariApplicationTests;
import com.app.itibari.dataTypes.BannerAction;
import com.app.itibari.dataTypes.DocumentTypes;
import com.app.itibari.dataTypes.EntityType;
import com.app.itibari.dataTypes.ReferenceType;
import com.app.itibari.models.request.BannerRequest;
import com.app.itibari.models.request.CreateDocumentRuleRequest;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * All file upload related test should be ignored to avoid cluttering the file storage with test files.
 */

public class FileRestControllerTests extends ItibariApplicationTests {
    @Test
    @Ignore
    public void testWorks() throws IOException {
        File sampleFile = new ClassPathResource("documents/image1.png", this.getClass().getClassLoader()).getFile();

        given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("referenceId", UUID.randomUUID())
                .queryParam("referenceType", ReferenceType.RECEIPT)
                .queryParam("description", "description")
                .queryParam("receipt", "receipt")
                .multiPart("file", sampleFile)
                .queryParam("amount", "400")
                .post("/files/upload")
                .then().log().all()
                .statusCode(200);
    }

    @Test
    @Ignore
    public void testKYCDocumentUploadAndRetrieval() throws IOException {

        CreateDocumentRuleRequest request = new CreateDocumentRuleRequest();
        request.setDocumentName("Rent lease agreement");
        request.setEntityType(EntityType.CUSTOMER);
        request.setDescription("Please upload your rent lease agreement");
        request.setDocumentType(DocumentTypes.KYC_DOCUMENT);
        request.setDocumentExpiryCheck(true);
        request.setDocumentRequired(true);
        request.setEntitySubType("Regular");

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .log().all()
                .post("/settings/documentRule")
                .then()
                .statusCode(200)
                .body("documentName", equalTo("Rent lease agreement"))
                .body("entityType", equalTo(EntityType.CUSTOMER.name()))
                .body("documentDescription", equalTo("Please upload your rent lease agreement"))
                .body("documentType", equalTo(DocumentTypes.KYC_DOCUMENT.name()))
                .body("documentExpiryCheck", equalTo(true))
                .body("documentRequired", equalTo(true))
                .body("entitySubType", equalTo("Regular"));


        // get the document rule

        String publicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/settings/documentRules?entityType=CUSTOMER")
                .then().log().all()
                .statusCode(200)
                .extract().path("content.find { it.documentName == 'Rent lease agreement' }.publicId");


        File sampleFile = new ClassPathResource("documents/image1.png", this.getClass().getClassLoader()).getFile();

        // Upload KYC Document for customer
        UUID customerId = UUID.fromString("d7c5bf34-fda1-46e2-b5ee-40db98aed06d");
        given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("referenceId", customerId)
                .queryParam("referenceType", ReferenceType.KYC_DOCUMENT)
                .queryParam("description", "KYC Document Upload Test")
                .queryParam("documentRulePublicId", UUID.fromString(publicId))
                .queryParam("expiryDate", "2026-12-31")
                .multiPart("file", sampleFile)
                .post("/files/upload")
                .then().log().all()
                .statusCode(200)
                .body("referenceId", equalTo(customerId.toString()))
                .body("referenceType", equalTo(ReferenceType.KYC_DOCUMENT.toString()));


        // Retrieve KYC Documents for customer

        given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("documentRulePublicId", UUID.fromString(publicId))
                .get("/files/bnpl/{customerPublicId}", customerId)
                .then().log().all()
                .statusCode(200)
                .body("content.size()", equalTo(1));

    }


    // upload and fetch mobile banner image

    @Test
    @Ignore
    public void testMobileBannerUploadAndRetrieval() throws IOException {
        File sampleFile = new ClassPathResource("documents/image1.png", this.getClass().getClassLoader()).getFile();

        // Upload Mobile Banner Image
        given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("description", "Mobile Banner Image Test")
                .multiPart("file", sampleFile)
                .post("/files/uploadMobileBanner")
                .then().log().all()
                .statusCode(200)
                .body("description", equalTo("Mobile Banner Image Test"));


        given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/files/mobileBanner")
                .then().log().all()
                .statusCode(200);


    }

    @Test
    public void testMobileBannerRetrieval() throws IOException {
        // Retrieve Mobile Banner Image
        given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/files/mobileBanner")
                .then().log().all()
                .statusCode(200);
    }

    @Ignore
    @Test
    public void testRunExpiryDocuments() throws IOException {
        given()
                .auth().oauth2(ADMIN_TOKEN)
                .post("/files/sampleRunDocumentExpiry")
                .then().log().all()
                .statusCode(200);
    }

    @Test
    @Ignore
    public void testUploadBannerWithBannerRequest() throws IOException {
        File sampleFile = new ClassPathResource("documents/image1.png", this.getClass().getClassLoader()).getFile();

        BannerRequest bannerRequest = new BannerRequest();
        bannerRequest.setHasProducts(Boolean.TRUE);
        bannerRequest.setProducts(Collections.singletonList(UUID.fromString("534cf5d4-f8c3-495c-a813-9a42f338c466")));

        given()
                .auth().oauth2(ADMIN_TOKEN)
                .multiPart("file", sampleFile)
                .multiPart("bannerRequest", bannerRequest, "application/json")
                .log().all()
                .queryParam("description", "Banner With Request Test")
                .post("/files/uploadMobileBanner")
                .then().log().all()
                .statusCode(200)
                .body("description", equalTo("Banner With Request Test"))
                .body("publicId", notNullValue())
                .body("referenceType", equalTo("MOBILE_BANNER"));
    }

    @Test
    @Ignore
    public void testUploadBannerActivateAndFetchImages() throws IOException {
        File sampleFile = new ClassPathResource("documents/image1.png", this.getClass().getClassLoader()).getFile();

        // Upload banner image
        String publicId = given()
                .auth().oauth2(ADMIN_TOKEN)
                .multiPart("file", sampleFile)
                .queryParam("description", "Banner Activate Fetch Test")
                .post("/files/uploadMobileBanner")
                .then().log().all()
                .statusCode(200)
                .body("description", equalTo("Banner Activate Fetch Test"))
                .body("publicId", notNullValue())
                .body("referenceType", equalTo("MOBILE_BANNER"))
                .extract().path("publicId");

        // Activate the uploaded banner
        given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("action", BannerAction.ACTIVATE.name())
                .patch("/files/banner/{publicId}/action", publicId)
                .then().log().all()
                .statusCode(200);

        // Fetch active banners and verify the uploaded banner is present
        given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("isActive", true)
                .get("/files/mobileBanner")
                .then().log().all()
                .statusCode(200)
                .body("$", hasSize(greaterThan(0)))
                .body("publicId", hasItem(publicId));

        // Deactivate the banner
        given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("action", BannerAction.DEACTIVATE.name())
                .patch("/files/banner/{publicId}/action", publicId)
                .then().log().all()
                .statusCode(200);

        // Fetch inactive banners and verify banner moved to inactive list
        given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("isActive", false)
                .get("/files/mobileBanner")
                .then().log().all()
                .statusCode(200)
                .body("publicId", hasItem(publicId));
    }

    @Test
    @Ignore
    public void testFetchInactiveBanners() {
        given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("isActive", false)
                .get("/files/mobileBanner")
                .then().log().all()
                .statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }
}
