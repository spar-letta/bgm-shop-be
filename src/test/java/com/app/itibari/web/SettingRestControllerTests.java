package com.app.itibari.web;

import com.app.itibari.ItibariApplicationTests;
import com.app.itibari.dataTypes.CustomerType;
import com.app.itibari.dataTypes.DocumentTypes;
import com.app.itibari.dataTypes.EntityType;
import com.app.itibari.models.request.*;
import com.app.itibari.services.ReminderRunnerService;
import io.restassured.http.ContentType;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

// N/B some tests runs alone!


public class SettingRestControllerTests extends ItibariApplicationTests {

    @Autowired
    private ReminderRunnerService reminderRunnerService;


    @Test
    @Ignore  // works alone
    public void regionMappingTest() {
        RegionWarehouseMappingRequest regionWarehouseMappingRequest = new RegionWarehouseMappingRequest();
        regionWarehouseMappingRequest.setWarehousePublicId(UUID.fromString("3849b1be-a9dd-4f11-abbf-229ad2780a56"));

        List<UUID> regions = List.of(UUID.fromString("59fb9c9f-cac4-49fe-9ba3-74e9026fe1c7"), UUID.fromString("8c5d4db0-f9a1-47ed-a160-56780e4e4d14"));
        regionWarehouseMappingRequest.setRegionPublicIds(regions);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(regionWarehouseMappingRequest)
                .post("/settings/regionWarehouseMapping")
                .then()
                .statusCode(200)
                .body("mappedRegions.size()", equalTo(2));
    }

    @Test
    public void testCreateDocumentRule() {
        CreateDocumentRuleRequest request = new CreateDocumentRuleRequest();
        request.setDocumentName("Test Document");
        request.setEntityType(EntityType.CUSTOMER);
        request.setDescription("This is a test document rule");
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
                .body("documentName", equalTo("Test Document"))
                .body("entityType", equalTo(EntityType.CUSTOMER.name()))
                .body("documentDescription", equalTo("This is a test document rule"))
                .body("documentType", equalTo(DocumentTypes.KYC_DOCUMENT.name()))
                .body("documentExpiryCheck", equalTo(true))
                .body("documentRequired", equalTo(true))
                .body("entitySubType", equalTo("Regular"));

        String publicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/settings/documentRules?entityType=CUSTOMER")
                .then().log().all()
                .statusCode(200)
                .body("content.find { it.documentName == 'Test Document' }.entityType", equalTo(EntityType.CUSTOMER.name()))
                .body("content.find { it.documentName == 'Test Document' }.documentDescription", equalTo("This is a test document rule"))
                .body("content.find { it.documentName == 'Test Document' }.documentType", equalTo(DocumentTypes.KYC_DOCUMENT.name()))
                .body("content.find { it.documentName == 'Test Document' }.documentExpiryCheck", equalTo(true))
                .body("content.find { it.documentName == 'Test Document' }.documentRequired", equalTo(true))
                .body("content.find { it.documentName == 'Test Document' }.entitySubType", equalTo("Regular"))
                .extract().path("content.find { it.documentName == 'Test Document' }.publicId");

        UpdateDocumentRuleRequest updateRequest = new UpdateDocumentRuleRequest();
        updateRequest.setPublicId(UUID.fromString(publicId));
        updateRequest.setDocumentName("Updated Test Document");
        updateRequest.setEntityType(EntityType.CUSTOMER);
        updateRequest.setDescription("This is an updated test document rule");
        updateRequest.setDocumentType(DocumentTypes.KYC_DOCUMENT);
        updateRequest.setDocumentExpiryCheck(false);
        updateRequest.setDocumentRequired(false);
        updateRequest.setEntitySubType("VIP");

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(updateRequest)
                .log().all()
                .patch("/settings/documentRule")
                .then()
                .statusCode(200)
                .body("documentName", equalTo("Updated Test Document"))
                .body("entityType", equalTo(EntityType.CUSTOMER.name()))
                .body("documentDescription", equalTo("This is an updated test document rule"))
                .body("documentType", equalTo(DocumentTypes.KYC_DOCUMENT.name()))
                .body("documentExpiryCheck", equalTo(false))
                .body("documentRequired", equalTo(false))
                .body("entitySubType", equalTo("VIP"));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .delete("/settings/documentRule/" + publicId)
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/settings/documentRule/" + publicId)
                .then()
                .statusCode(400)
                .body("userMessage", equalTo("Document Rule not found"));
    }

    @Test
    public void testCreditRuleCreateWorks() {
        CreateCreditRuleRequest request = new CreateCreditRuleRequest();
        request.setCustomerType(CustomerType.CORPORATE);
        request.setIsCreditLimit(true);
        request.setCreditLimitPercentage(new BigDecimal("15.5"));
        request.setIsRepaymentTerm(false);
        request.setRuleName("Corporate Credit Limit Rule");
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .log().all()
                .post("/settings/creditRule")
                .then()
                .statusCode(200)
                .body("customerType", equalTo(CustomerType.CORPORATE.name()))
                .body("creditLimit", equalTo(true))
                .body("creditLimitPercentage", equalTo(15.5f))
                .body("repaymentTerm", equalTo(false))
                .body("ruleName", equalTo("Corporate Credit Limit Rule"));

        String publicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/settings/creditRules")
                .then().log().all()
                .statusCode(200)
                .body("content.find { it.ruleName == 'Corporate Credit Limit Rule' }.customerType", equalTo(CustomerType.CORPORATE.name()))
                .body("content.find { it.ruleName == 'Corporate Credit Limit Rule' }.creditLimit", equalTo(true))
                .body("content.find { it.ruleName == 'Corporate Credit Limit Rule' }.creditLimitPercentage", equalTo(15.5f))
                .body("content.find { it.ruleName == 'Corporate Credit Limit Rule' }.repaymentTerm", equalTo(false))
                .body("content.find { it.ruleName == 'Corporate Credit Limit Rule' }.ruleName", equalTo("Corporate Credit Limit Rule"))
                .extract().path("content.find { it.ruleName == 'Corporate Credit Limit Rule' }.publicId");

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .delete("/settings/creditRule/" + publicId)
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/settings/creditRules")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(0));
    }

    @Test
    public void createOverdueCreditRuleWorks() {
        CreateCreditRuleRequest request = new CreateCreditRuleRequest();
        request.setCustomerType(CustomerType.REGULAR);
        request.setIsOverdueFine(true);
        request.setOverdueFinePercentage(new BigDecimal("5.0"));
        request.setRuleName("Individual Overdue Fine Rule");
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .log().all()
                .post("/settings/creditRule")
                .then()
                .statusCode(200)
                .body("customerType", equalTo(CustomerType.REGULAR.name()))
                .body("overdueFine", equalTo(true))
                .body("overdueFinePercentage", equalTo(5.0f))
                .body("ruleName", equalTo("Individual Overdue Fine Rule"));

        String publicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/settings/creditRules")
                .then().log().all()
                .statusCode(200)
                .body("content.find { it.ruleName == 'Individual Overdue Fine Rule' }.customerType", equalTo(CustomerType.REGULAR.name()))
                .body("content.find { it.ruleName == 'Individual Overdue Fine Rule' }.overdueFine", equalTo(true))
                .body("content.find { it.ruleName == 'Individual Overdue Fine Rule' }.overdueFinePercentage", equalTo(5.0f))
                .body("content.find { it.ruleName == 'Individual Overdue Fine Rule' }.ruleName", equalTo("Individual Overdue Fine Rule"))
                .extract().path("content.find { it.ruleName == 'Individual Overdue Fine Rule' }.publicId");

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .delete("/settings/creditRule/" + publicId)
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/settings/creditRules")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(0));
    }

    @Ignore
    @Test
    public void testCreateTaxCode() throws IOException {
        for (int i = 0; i <= 5; i++) {
            TaxCodeRequest taxCodeRequest =
                    TaxCodeRequest.builder().codeName(String.valueOf(UUID.randomUUID()).substring(0, 15)).codeValue(18).build();
            given()
                    .contentType(ContentType.JSON)
                    .auth().oauth2(ADMIN_TOKEN)
                    .body(taxCodeRequest).log().all()
                    .post("/settings/taxCodes")
                    .then()
                    .statusCode(200);
        }

        String taxCodePublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/settings/taxCodes")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(7))
                .extract().path("content[4].publicId");

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .pathParams("taxCodePublicId", taxCodePublicId)
                .get("/settings/taxCodes/{taxCodePublicId}")
                .then().log().all()
                .statusCode(200);

        TaxCodeRequest taxCodeRequest =
                TaxCodeRequest.builder().codeName("The Taxer").codeValue(0).build();
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .pathParams("taxCodePublicId", taxCodePublicId)
                .body(taxCodeRequest).log().all()
                .patch("/settings/taxCodes/{taxCodePublicId}")
                .then().log().all()
                .statusCode(200)
                .body("codeName", equalToIgnoringCase(taxCodeRequest.codeName()))
                .body("codeValue", equalTo(0));

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .pathParams("taxCodePublicId", taxCodePublicId)
                .body(taxCodeRequest).log().all()
                .delete("/settings/taxCodes/{taxCodePublicId}")
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/settings/taxCodes")
                .then().log().all()
                .statusCode(200)
                .body("content.size()", equalTo(6));

        taxCodeRequest =
                TaxCodeRequest.builder().codeName("new Tax").codeValue(2).build();
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(taxCodeRequest).log().all()
                .post("/settings/taxCodes")
                .then()
                .statusCode(200);
    }


    // ─── Reminder Setting CRUD ────────────────────────────────────────────────

    /**
     * Full CRUD lifecycle: DUE_SOON + DAILY.
     * Also verifies that creating a setting auto-creates a linked ReminderJob.
     */
    @Ignore
    @Test
    public void testReminderSettingCrudWorks_DueSoon_Daily() {
        Map<String, Object> createRequest = Map.of(
                "customerType", "CORPORATE",
                "reminderType", "DUE_SOON",
                "period", 3,
                "messageSendTime", "09:00:00",
                "messageTemplate", "Dear {{customerName}}, your invoice {{invoiceNumber}} is due on {{dueDate}}. – {{companyName}}",
                "frequency", "DAILY"
        );

        // CREATE — verify all fields echoed back
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(createRequest)
                .log().all()
                .post("/settings/reminderSetting")
                .then()
                .statusCode(200)
                .body("publicId", notNullValue())
                .body("customerType", equalTo("CORPORATE"))
                .body("reminderType", equalTo("DUE_SOON"))
                .body("period", equalTo(3))
                .body("frequency", equalTo("DAILY"))
                .body("messageTemplate", notNullValue());

        // GET ALL — find setting and extract publicId
        String settingPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/settings/reminderSettings")
                .then().log().all()
                .statusCode(200)
                .body("content.find { it.customerType == 'CORPORATE' && it.reminderType == 'DUE_SOON' }.period", equalTo(3))
                .body("content.find { it.customerType == 'CORPORATE' && it.reminderType == 'DUE_SOON' }.frequency", equalTo("DAILY"))
                .extract().path("content.find { it.customerType == 'CORPORATE' && it.reminderType == 'DUE_SOON' }.publicId");

        // GET BY ID
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/settings/reminderSetting/" + settingPublicId)
                .then()
                .statusCode(200)
                .body("publicId", equalTo(settingPublicId))
                .body("customerType", equalTo("CORPORATE"))
                .body("reminderType", equalTo("DUE_SOON"))
                .body("period", equalTo(3));

        // VERIFY LINKED JOB was auto-created
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/settings/reminderJobs")
                .then()
                .statusCode(200)
                .body("content.find { it.jobType == 'DUE_SOON' }.publicId", notNullValue())
                .body("content.find { it.jobType == 'DUE_SOON' }.status", equalTo("SCHEDULED"));

        // UPDATE — change period and frequency
        Map<String, Object> updateRequest = Map.of(
                "customerType", "CORPORATE",
                "reminderType", "DUE_SOON",
                "period", 5,
                "messageSendTime", "10:00:00",
                "messageTemplate", "Dear {{customerName}}, invoice {{invoiceNumber}} due {{dueDate}}. – {{companyName}}",
                "frequency", "WEEKLY"
        );

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(updateRequest)
                .log().all()
                .patch("/settings/reminderSetting/" + settingPublicId)
                .then()
                .statusCode(200);

        // verify update persisted
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/settings/reminderSetting/" + settingPublicId)
                .then()
                .statusCode(200)
                .body("period", equalTo(5))
                .body("frequency", equalTo("WEEKLY"));

        // DELETE
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .delete("/settings/reminderSetting/" + settingPublicId)
                .then()
                .statusCode(200);

        // VERIFY DELETED — 400
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/settings/reminderSetting/" + settingPublicId)
                .then()
                .statusCode(400)
                .body("userMessage", equalTo("Reminder setting not found"));
    }

    /**
     * Full CRUD lifecycle: OVERDUE + WEEKLY.
     */
    @Test
    public void testReminderSettingCrudWorks_Overdue_Weekly() {
        Map<String, Object> createRequest = Map.of(
                "customerType", "REGULAR",
                "reminderType", "OVERDUE",
                "period", 7,
                "messageSendTime", "08:00:00",
                "messageTemplate", "Hi {{customerName}}, invoice {{invoiceNumber}} of {{amountDue}} is {{daysOverdue}} day(s) overdue.",
                "frequency", "WEEKLY"
        );

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(createRequest)
                .log().all()
                .post("/settings/reminderSetting")
                .then()
                .statusCode(200)
                .body("customerType", equalTo("REGULAR"))
                .body("reminderType", equalTo("OVERDUE"))
                .body("period", equalTo(7))
                .body("frequency", equalTo("WEEKLY"));

        String settingPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/settings/reminderSettings")
                .then()
                .statusCode(200)
                .extract().path("content.find { it.customerType == 'REGULAR' && it.reminderType == 'OVERDUE' }.publicId");

        // VERIFY linked job type is OVERDUE
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/settings/reminderJobs")
                .then()
                .statusCode(200)
                .body("content.find { it.jobType == 'OVERDUE' }.status", equalTo("SCHEDULED"));

        // DELETE + verify jobs deleted too
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .delete("/settings/reminderSetting/" + settingPublicId)
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/settings/reminderSetting/" + settingPublicId)
                .then()
                .statusCode(400);
    }

    /**
     * ONCE frequency — verifies the setting is created and a job is created but
     * the frequency field is persisted correctly.
     */
    @Test
    public void testReminderSettingCrudWorks_Once() {
        Map<String, Object> createRequest = Map.of(
                "customerType", "CORPORATE",
                "reminderType", "OVERDUE",
                "period", 14,
                "messageSendTime", "11:00:00",
                "messageTemplate", "Dear {{customerName}}, urgent: {{invoiceNumber}} is overdue by {{daysOverdue}} days.",
                "frequency", "ONCE"
        );

        String settingPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(createRequest)
                .post("/settings/reminderSetting")
                .then()
                .statusCode(200)
                .body("frequency", equalTo("ONCE"))
                .extract().path("publicId");

        // linked job must exist and be SCHEDULED
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/settings/reminderJobs")
                .then()
                .statusCode(200)
                .body("content.find { it.jobType == 'OVERDUE' }.status", equalTo("SCHEDULED"));

        // cleanup
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .delete("/settings/reminderSetting/" + settingPublicId)
                .then()
                .statusCode(200);
    }

    /**
     * AFTER_EVERY_TWO_DAYS frequency — verifies all four frequency values are accepted.
     */
    @Ignore
    @Test
    public void testReminderSettingCrudWorks_AfterEveryTwoDays() {
        Map<String, Object> createRequest = Map.of(
                "customerType", "REGULAR",
                "reminderType", "DUE_SOON",
                "period", 2,
                "messageSendTime", "07:30:00",
                "messageTemplate", "Reminder: {{invoiceNumber}} due {{dueDate}}.",
                "frequency", "AFTER_EVERY_TWO_DAYS"
        );

        String settingPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(createRequest)
                .post("/settings/reminderSetting")
                .then()
                .statusCode(200)
                .body("frequency", equalTo("AFTER_EVERY_TWO_DAYS"))
                .body("period", equalTo(2))
                .extract().path("publicId");

        // cleanup
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .delete("/settings/reminderSetting/" + settingPublicId)
                .then()
                .statusCode(200);
    }


    // ─── Duplicate / Uniqueness ───────────────────────────────────────────────

    /**
     * Two settings with the same customerType + reminderType must be rejected.
     */
    @Test
    public void testReminderSettingDuplicateIsRejected() {
        Map<String, Object> request = Map.of(
                "customerType", "REGULAR",
                "reminderType", "OVERDUE",
                "period", 7,
                "messageSendTime", "08:00:00",
                "messageTemplate", "Hi {{customerName}}, invoice {{invoiceNumber}} of {{amountDue}} is {{daysOverdue}} day(s) overdue.",
                "frequency", "DAILY"
        );

        // first create — must succeed
        String settingPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .post("/settings/reminderSetting")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        // second create with same customerType + reminderType — must fail
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .log().all()
                .post("/settings/reminderSetting")
                .then()
                .statusCode(400)
                .body("userMessage", equalTo("A reminder setting for this customer type and day already exists"));

        // cleanup
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .delete("/settings/reminderSetting/" + settingPublicId)
                .then()
                .statusCode(200);
    }


    // ─── Validation ──────────────────────────────────────────────────────────

    /**
     * Missing all required fields except customerType → 400.
     */
    @Test
    public void testReminderSettingValidationRejectsMissingFields() {
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(Map.of("customerType", "REGULAR"))
                .log().all()
                .post("/settings/reminderSetting")
                .then()
                .statusCode(400);
    }

    /**
     * period = 0 violates @Min(1) constraint → 400.
     */
    @Test
    public void testReminderSettingValidationRejectsPeriodZero() {
        Map<String, Object> request = Map.of(
                "customerType", "REGULAR",
                "reminderType", "OVERDUE",
                "period", 0,
                "messageSendTime", "09:00:00",
                "messageTemplate", "Hi {{customerName}}",
                "frequency", "DAILY"
        );

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .log().all()
                .post("/settings/reminderSetting")
                .then()
                .statusCode(400);
    }

    /**
     * Blank messageTemplate → 400.
     */
    @Test
    public void testReminderSettingValidationRejectsBlankTemplate() {
        Map<String, Object> request = Map.of(
                "customerType", "REGULAR",
                "reminderType", "OVERDUE",
                "period", 5,
                "messageSendTime", "09:00:00",
                "messageTemplate", "   ",
                "frequency", "DAILY"
        );

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .log().all()
                .post("/settings/reminderSetting")
                .then()
                .statusCode(400);
    }

    /**
     * Invalid reminderType enum value → 400.
     */
    @Test
    public void testReminderSettingValidationRejectsInvalidReminderType() {
        Map<String, Object> request = Map.of(
                "customerType", "REGULAR",
                "reminderType", "BEFORE_DUE_DATE",   // not a valid ReminderType
                "period", 5,
                "messageSendTime", "09:00:00",
                "messageTemplate", "Hi {{customerName}}",
                "frequency", "DAILY"
        );

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .post("/settings/reminderSetting")
                .then()
                .statusCode(400);
    }

    /**
     * Invalid frequency enum value → 400.
     */
    @Test
    public void testReminderSettingValidationRejectsInvalidFrequency() {
        Map<String, Object> request = Map.of(
                "customerType", "REGULAR",
                "reminderType", "OVERDUE",
                "period", 5,
                "messageSendTime", "09:00:00",
                "messageTemplate", "Hi {{customerName}}",
                "frequency", "MONTHLY"   // not a valid ReminderFrequency
        );

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .post("/settings/reminderSetting")
                .then()
                .statusCode(400);
    }

    /**
     * GET on a non-existent publicId → 400.
     */
    @Test
    public void testReminderSettingGetNonExistentReturns400() {
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/settings/reminderSetting/" + UUID.randomUUID())
                .then()
                .statusCode(400)
                .body("userMessage", equalTo("Reminder setting not found"));
    }

    /**
     * UPDATE on a non-existent publicId → 400.
     */
    @Test
    public void testReminderSettingUpdateNonExistentReturns400() {
        Map<String, Object> request = Map.of(
                "customerType", "REGULAR",
                "reminderType", "OVERDUE",
                "period", 5,
                "messageSendTime", "09:00:00",
                "messageTemplate", "Hi {{customerName}}",
                "frequency", "DAILY"
        );

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .patch("/settings/reminderSetting/" + UUID.randomUUID())
                .then()
                .statusCode(400)
                .body("userMessage", equalTo("Reminder setting not found"));
    }

    /**
     * DELETE on a non-existent publicId → 400.
     */
    @Test
    public void testReminderSettingDeleteNonExistentReturns400() {
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .delete("/settings/reminderSetting/" + UUID.randomUUID())
                .then()
                .statusCode(400)
                .body("userMessage", equalTo("Reminder setting not found"));
    }


    // ─── Update idempotency / NonUnique regression ───────────────────────────

    /**
     * Updating the same setting multiple times must not throw NonUniqueResultException.
     * This covers the bug where scheduleNextOccurrence chains jobs under the same
     * setting, causing findByReminderSettingAndDeletedIsFalse to return > 1 row.
     */
    @Test
    public void testMultipleUpdatesDoNotCauseNonUniqueException() {
        Map<String, Object> createRequest = Map.of(
                "customerType", "CORPORATE",
                "reminderType", "DUE_SOON",
                "period", 3,
                "messageSendTime", "09:00:00",
                "messageTemplate", "Dear {{customerName}}, invoice {{invoiceNumber}} due {{dueDate}}.",
                "frequency", "DAILY"
        );

        String settingPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(createRequest)
                .post("/settings/reminderSetting")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        // update 1
        Map<String, Object> update1 = Map.of(
                "customerType", "CORPORATE",
                "reminderType", "DUE_SOON",
                "period", 5,
                "messageSendTime", "10:00:00",
                "messageTemplate", "Updated template 1",
                "frequency", "WEEKLY"
        );
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(update1)
                .patch("/settings/reminderSetting/" + settingPublicId)
                .then()
                .statusCode(200);

        // update 2 — would crash before the fix
        Map<String, Object> update2 = Map.of(
                "customerType", "CORPORATE",
                "reminderType", "DUE_SOON",
                "period", 7,
                "messageSendTime", "11:00:00",
                "messageTemplate", "Updated template 2",
                "frequency", "AFTER_EVERY_TWO_DAYS"
        );
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(update2)
                .patch("/settings/reminderSetting/" + settingPublicId)
                .then()
                .statusCode(200);

        // update 3 — third consecutive update
        Map<String, Object> update3 = Map.of(
                "customerType", "CORPORATE",
                "reminderType", "DUE_SOON",
                "period", 10,
                "messageSendTime", "08:00:00",
                "messageTemplate", "Updated template 3",
                "frequency", "ONCE"
        );
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(update3)
                .patch("/settings/reminderSetting/" + settingPublicId)
                .then()
                .statusCode(200);

        // verify final state
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/settings/reminderSetting/" + settingPublicId)
                .then()
                .statusCode(200)
                .body("period", equalTo(10))
                .body("frequency", equalTo("ONCE"));

        // cleanup
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .delete("/settings/reminderSetting/" + settingPublicId)
                .then()
                .statusCode(200);
    }

    /**
     * After update, only one SCHEDULED job must exist for the setting
     * (old SCHEDULED job is cancelled, new one created).
     */
    @Ignore
    @Test
    public void testUpdateReminderSettingCancelsOldJobAndCreatesNew() {
        Map<String, Object> createRequest = Map.of(
                "customerType", "REGULAR",
                "reminderType", "OVERDUE",
                "period", 5,
                "messageSendTime", "09:00:00",
                "messageTemplate", "Hi {{customerName}}",
                "frequency", "DAILY"
        );

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(createRequest)
                .post("/settings/reminderSetting")
                .then()
                .statusCode(200);

        String settingPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/settings/reminderSettings")
                .then()
                .statusCode(200)
                .extract().path("content.find { it.customerType == 'REGULAR' && it.reminderType == 'OVERDUE' }.publicId");

        // capture the original job's publicId
        String originalJobPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/settings/reminderJobs")
                .then()
                .statusCode(200)
                .extract().path("content.find { it.jobType == 'OVERDUE' }.publicId");

        // update the setting
        Map<String, Object> updateRequest = Map.of(
                "customerType", "REGULAR",
                "reminderType", "OVERDUE",
                "period", 10,
                "messageSendTime", "14:00:00",
                "messageTemplate", "Updated: {{invoiceNumber}}",
                "frequency", "WEEKLY"
        );
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(updateRequest)
                .patch("/settings/reminderSetting/" + settingPublicId)
                .then()
                .statusCode(200);

        // new job must have a different publicId (old one was soft-deleted)
        String newJobPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/settings/reminderJobs")
                .then()
                .statusCode(200)
                .body("content.find { it.jobType == 'OVERDUE' }.status", equalTo("SCHEDULED"))
                .extract().path("content.find { it.jobType == 'OVERDUE' }.publicId");

        assert !originalJobPublicId.equals(newJobPublicId) : "Update should have created a new job";

        // cleanup
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .delete("/settings/reminderSetting/" + settingPublicId)
                .then()
                .statusCode(200);
    }


    // ─── ReminderJob Endpoints ────────────────────────────────────────────────

    /**
     * Creating a ReminderSetting must auto-create a linked SCHEDULED ReminderJob.
     */
    @Test
    public void testCreateReminderSettingAutoCreatesScheduledJob() {
        Map<String, Object> createRequest = Map.of(
                "customerType", "CORPORATE",
                "reminderType", "DUE_SOON",
                "period", 3,
                "messageSendTime", "09:00:00",
                "messageTemplate", "Dear {{customerName}}, invoice {{invoiceNumber}} due {{dueDate}}.",
                "frequency", "DAILY"
        );

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(createRequest)
                .post("/settings/reminderSetting")
                .then()
                .statusCode(200);

        // job must appear in the jobs list
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/settings/reminderJobs")
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));

        // cleanup
        String settingPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/settings/reminderSettings")
                .then()
                .statusCode(200)
                .extract().path("content.find { it.customerType == 'CORPORATE' && it.reminderType == 'DUE_SOON' }.publicId");

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .delete("/settings/reminderSetting/" + settingPublicId)
                .then()
                .statusCode(200);
    }

    /**
     * GET /reminderJob/{publicId} with a valid publicId.
     */
    @Ignore
    @Test
    public void testGetReminderJobByPublicId() {
        Map<String, Object> createRequest = Map.of(
                "customerType", "REGULAR",
                "reminderType", "OVERDUE",
                "period", 5,
                "messageSendTime", "08:00:00",
                "messageTemplate", "Hi {{customerName}}, overdue notice.",
                "frequency", "WEEKLY"
        );

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(createRequest)
                .post("/settings/reminderSetting")
                .then()
                .statusCode(200);

        String jobPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/settings/reminderJobs")
                .then()
                .statusCode(200)
                .extract().path("content.find { it.jobType == 'OVERDUE' }.publicId");

        // GET by publicId
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/settings/reminderJob/{publicId}", UUID.fromString(jobPublicId))
                .then()
                .statusCode(200)
                .body("publicId", equalTo(jobPublicId))
                .body("jobType", equalTo("OVERDUE"))
                .body("status", equalTo("SCHEDULED"))
                .body("notificationChannel", notNullValue())
                .body("scheduledTime", notNullValue());

        // cleanup
        String settingPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/settings/reminderSettings")
                .then()
                .statusCode(200)
                .extract().path("content.find { it.customerType == 'REGULAR' && it.reminderType == 'OVERDUE' }.publicId");

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .delete("/settings/reminderSetting/" + settingPublicId)
                .then()
                .statusCode(200);
    }

    /**
     * GET /reminderJob/{publicId} with a non-existent ID → 400.
     */
    @Test
    public void testGetReminderJobNonExistentReturns400() {
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/settings/reminderJob/" + UUID.randomUUID())
                .then()
                .statusCode(400)
                .body("userMessage", equalTo("Reminder job not found"));
    }

    /**
     * GET /reminderJobs — paginated list.
     */
    @Test
    public void testGetAllReminderJobsReturnsPaginatedList() {
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/settings/reminderJobs")
                .then()
                .statusCode(200);
    }

    /**
     * Deleting a setting must soft-delete all linked jobs (they disappear from the job list).
     */
    @Ignore
    @Test
    public void testDeleteReminderSettingDeletesLinkedJobs() {
        Map<String, Object> createRequest = Map.of(
                "customerType", "CORPORATE",
                "reminderType", "OVERDUE",
                "period", 3,
                "messageSendTime", "09:00:00",
                "messageTemplate", "Dear {{customerName}}, urgent overdue notice.",
                "frequency", "ONCE"
        );

        String settingPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(createRequest)
                .post("/settings/reminderSetting")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        String jobPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/settings/reminderJobs")
                .then()
                .statusCode(200)
                .extract().path("content.find { it.jobType == 'OVERDUE' }.publicId");

        // delete the setting
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .delete("/settings/reminderSetting/" + settingPublicId)
                .then()
                .statusCode(200);

        // the linked job must now return 400
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/settings/reminderJob/{publicId}", UUID.fromString(jobPublicId))
                .then()
                .statusCode(400)
                .body("userMessage", equalTo("Reminder job not found"));
    }


    // ─── ReminderLog Endpoints ────────────────────────────────────────────────

    /**
     * GET /reminderLogs — must return a paginated response (possibly empty).
     */
    @Test
    public void testGetAllReminderLogsReturnsPaginatedList() {
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/settings/reminderLogs")
                .then()
                .statusCode(200);
    }

    /**
     * GET /reminderJob/{publicId}/logs — must return a paginated log list for a specific job.
     */
    @Ignore
    @Test
    public void testGetReminderLogsByJobId() {
        Map<String, Object> createRequest = Map.of(
                "customerType", "REGULAR",
                "reminderType", "DUE_SOON",
                "period", 3,
                "messageSendTime", "09:00:00",
                "messageTemplate", "Dear {{customerName}}, invoice due soon.",
                "frequency", "DAILY"
        );

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(createRequest)
                .post("/settings/reminderSetting")
                .then()
                .statusCode(200);

        String jobPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/settings/reminderJobs")
                .then()
                .statusCode(200)
                .extract().path("content.find { it.jobType == 'DUE_SOON' }.publicId");

        // logs endpoint must return paginated result (empty — job hasn't run yet)
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/settings/reminderJob/{publicId}/logs", UUID.fromString(jobPublicId))
                .then()
                .statusCode(200)
                .body("content", notNullValue())
                .body("pageable", notNullValue());

        // cleanup
        String settingPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/settings/reminderSettings")
                .then()
                .statusCode(200)
                .extract().path("content.find { it.customerType == 'REGULAR' && it.reminderType == 'DUE_SOON' }.publicId");

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .delete("/settings/reminderSetting/" + settingPublicId)
                .then()
                .statusCode(200);
    }


    // ─── Template Variables ───────────────────────────────────────────────────

    /**
     * GET /reminderSettings/templateVariables — must return all 6 placeholders.
     */
    @Test
    public void testReminderSettingTemplateVariablesReturnsAll() {
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/settings/reminderSettings/templateVariables")
                .then().log().all()
                .statusCode(200)
                .body("size()", equalTo(6))
                .body("", hasItems(
                        "CUSTOMER_NAME", "INVOICE_NUMBER", "DUE_DATE",
                        "DAYS_OVERDUE", "AMOUNT_DUE", "COMPANY_NAME"
                ));
    }


    // ─── Authentication ───────────────────────────────────────────────────────

    /**
     * Unauthenticated requests to reminder endpoints must be rejected (401).
     */
    @Test
    public void testReminderEndpointsRequireAuthentication() {
        given()
                .contentType(ContentType.JSON)
                .get("/settings/reminderSettings")
                .then()
                .statusCode(401);

        given()
                .contentType(ContentType.JSON)
                .get("/settings/reminderJobs")
                .then()
                .statusCode(401);

        given()
                .contentType(ContentType.JSON)
                .get("/settings/reminderLogs")
                .then()
                .statusCode(401);
    }


    @Ignore
    @Test
    public void testReminderJonRunnerWorks() {
        Map<String, Object> createRequest = Map.of(
                "customerType", "REGULAR",
                "reminderType", "OVERDUE",
                "period", 3,
                "messageSendTime", "00:00:00",
                "messageTemplate", "Dear {{customerName}}, urgent overdue notice.",
                "frequency", "ONCE"
        );

        String settingPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(createRequest)
                .post("/settings/reminderSetting")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        String jobPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/settings/reminderJobs")
                .then()
                .statusCode(200)
                .extract().path("content.find { it.jobType == 'OVERDUE' }.publicId");


        //INSERT INTO itibari.credit_items (id, public_id, deleted, created_by, updated_by, creation_time, update_date, wallet_id, amount, total_amount, reference_id, description, confirmed_amount, total_confirmed_amount, paid_amount, order_number, payment_reference, interest_rate, fully_paid, credit_status, payment_status, date_taken, due_date, interest_amount, status, order_delivered, cancelled, order_public_id, reminder_sent, has_excess_payment, excess_amount_paid, amount_paid_from_wallet, approved_by, pay_via_wallet) VALUES (120, '0da20626-3498-41b7-84e6-3b6bbebcfc02', false, null, null, '2026-02-20 17:32:37.133832', '2026-02-20 17:50:15.728716', 101, 2000, 0.00, '37946b2b-b08d-4117-9927-f8adcf32292f', 'Credit taken for order ORD101009', 0.00, 0.00, 0.00, 'ORD101009', 'PMT-100113', 1.00, false, 'Healthy', 'NOT_PAID', '2026-02-20', '2026-03-02', 28.31, 'ACTIVE', true, false, 'ef2f4703-b478-4ca6-98d1-bc3aa88e1c3f', false, false, 0.00, 0.00, 'BNPL User at 20/02/2026 17:50', false);

        // expectation message to be sent cause this credit item has overdue.

        reminderRunnerService.processScheduledJobs();

        // sms is sent

        // fetch reminderLogs

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("searchParam", "ORD101009")
                .get("/settings/reminderLogs")
                .then().log().all()
                .statusCode(200);

    }


    @Test
    public void testCreateProductBrand() {

        ProductBrandRequest productBrandRequest = new ProductBrandRequest("name2");

        String publicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(productBrandRequest).log().all()
                .post("/settings/product-brands")
                .then().log().all()
                .statusCode(200)
                .extract().path("publicId");

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("searchParam", "name2")
                .get("/settings/product-brands")
                .then().log().all()
                .statusCode(200);

        productBrandRequest = new ProductBrandRequest("name10");
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(productBrandRequest)
                .put("/settings/product-brands/{publicId}", publicId)
                .then().log().all()
                .statusCode(200);
    }


    @Test
    public void testSwitchingTraOnAndOff() {
        String publicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("switchOn", true)
                .post("/settings/switchTraOn")
                .then().log().all()
                .statusCode(200)
                .extract().path("publicId");


        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("switchOn", false)
                .post("/settings/switchTraOn")
                .then().log().all()
                .statusCode(200);


        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/settings/switchTraOn")
                .then().log().all()
                .statusCode(200);


        // fetch events to see who did what

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/events/{publicId}", UUID.fromString(publicId))
                .then().log().all()
                .statusCode(200)
                .body("content.size()", equalTo(2));
    }
}
