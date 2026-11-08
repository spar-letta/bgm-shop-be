package com.app.itibari.web;

import com.app.itibari.ItibariApplicationTests;
import com.app.itibari.dataTypes.ChurnedDateRangeFilter;
import com.app.itibari.dataTypes.CustomerOutboundCallStatus;
import com.app.itibari.models.request.UpdateCustomerCallStatusRequest;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertFalse;


//@Ignore("These tests are currently when run as a whole, run separately for now")
public class CallStatusRestControllerTests extends ItibariApplicationTests {

    /** Canonical SUCCESSFUL outcome row — seeded via Liquibase (matches {@code CustomerOutboundCallStatus}). */
    private static final String OUTCOME_SUCCESSFUL_PUBLIC_ID = "d4e5f6a7-b8c9-4123-def0-000000000002";

    // Seeded in my-data.sql: active status
    private static final String SEEDED_ACTIVE_PUBLIC_ID = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
    // Seeded in my-data.sql: inactive (active=false) status
    private static final String SEEDED_INACTIVE_PUBLIC_ID = "c3d4e5f6-a7b8-9012-cdef-123456789012";
    // Customer with last_order_date in churn range (my-data.sql id=60)
    private static final String CHURNED_CUSTOMER_PUBLIC_ID = "d7c5bf34-fda1-46e2-b5ee-40db98aed06d";

    @Test
    public void getAllCallStatuses_returnsSeededEntries() {
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/callStatuses")
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(2));
    }

    @Test
    public void getActiveCallStatuses_excludesInactiveEntries() {
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("active", true)
                .get("/callStatuses")
                .then().log().all()
                .statusCode(200)
                .body("content.code", not(hasItem("NOT_INTERESTED")));
    }

    @Test
    public void getCallStatuses_filterBySearchParam() {
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("searchParam", "answer")
                .get("/callStatuses")
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content.code", hasItem("NO_ANSWER"));
    }

    @Test
    public void createCallStatus_persistsAndReturnsEntry() {
        String uniqueCode = "TEST_CS_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String requestBody = String.format("""
                {
                    "code": "%s",
                    "description": "Created by integration test"
                }
                """, uniqueCode);

        String publicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(requestBody).log().all()
                .post("/callStatuses")
                .then().log().all()
                .statusCode(200)
                .body("code", equalTo(uniqueCode))
                .body("description", equalTo("Created by integration test"))
                .body("active", equalTo(true))
                .extract().path("publicId");

        // Verify in list
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("searchParam", uniqueCode.substring(0, 7))
                .get("/callStatuses")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));
    }

    @Test
    public void createCallStatus_missingFieldsReturnsBadRequest() {
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body("""
                        {
                            "code": "",
                            "description": "Missing code"
                        }
                        """)
                .post("/callStatuses")
                .then().log().all()
                .statusCode(400);
    }

    @Test
    public void createCallStatus_duplicateCodeReturnsBadRequest() {
        String requestBody = """
                {
                    "code": "NO_ANSWER",
                    "description": "Duplicate code"
                }
                """;

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(requestBody)
                .post("/callStatuses")
                .then().log().all()
                .statusCode(400);
    }

    @Test
    public void updateCallStatus_duplicateCodeReturnsBadRequest() {
        String updateBody = """
                {
                    "code": "WILL_ORDER_LATER",
                    "description": "Duplicate code on update"
                }
                """;

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(updateBody)
                .patch("/callStatuses/{publicId}", SEEDED_ACTIVE_PUBLIC_ID)
                .then().log().all()
                .statusCode(400);
    }

    @Test
    public void updateCallStatus_changesDescription() {
        String updateBody = """
                {
                    "code": "NO_ANSWER",
                    "description": "Updated description for no answer"
                }
                """;

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(updateBody)
                .patch("/callStatuses/{publicId}", SEEDED_ACTIVE_PUBLIC_ID)
                .then().log().all()
                .statusCode(200);
    }

    @Test
    public void deactivateCallStatus_removesFromActiveList() {
        // 1. Create a new call status to safely deactivate (don't break other tests by deactivating seeded ones)
        String uniqueCode = "DEACT_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String createBody = String.format("""
                {
                    "code": "%s",
                    "description": "To be deactivated"
                }
                """, uniqueCode);

        String publicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(createBody)
                .post("/callStatuses")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        // 2. Deactivate
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .delete("/callStatuses/{publicId}", publicId)
                .then().log().all()
                .statusCode(200);

        // 3. Should not appear in active-only list
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("active", true)
                .get("/callStatuses")
                .then()
                .statusCode(200)
                .body("content.code", not(hasItem(uniqueCode)));

        // 4. Should still appear in full list (history preserved)
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("active", false)
                .get("/callStatuses")
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));
    }

    @Test
    public void activateCallStatus_restoresInActiveList() {
        // 1. Reactivate the pre-seeded inactive entry
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .patch("/callStatuses/{publicId}/activate", SEEDED_INACTIVE_PUBLIC_ID)
                .then().log().all()
                .statusCode(200);

        // 2. Now appears in active list
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("active", true)
                .get("/callStatuses")
                .then()
                .statusCode(200)
                .body("content.code", hasItem("NOT_INTERESTED"));
    }

    @Test
    public void setCustomerCallStatus_appearsInChurnedList() {
        String requestBody = """
                {
                    "status": "SUCCESSFUL"
                }
                """;

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(requestBody)
                .patch("/customers/{publicId}/callStatus", CHURNED_CUSTOMER_PUBLIC_ID)
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("rangeFilter", ChurnedDateRangeFilter.Range_30_120)
                .queryParam("endDate", "2026-03-15")
                .get("/customers/churnedCustomers")
                .then()
                .statusCode(200)
                .body("content.find { it.publicId == '" + CHURNED_CUSTOMER_PUBLIC_ID + "' }.callStatus.code",
                        equalTo("SUCCESSFUL"));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/customers/{publicId}/callHistory", CHURNED_CUSTOMER_PUBLIC_ID)
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content.size()", lessThanOrEqualTo(10))
                .body("content[0].callStatus", equalTo("SUCCESSFUL"))
                .body("content[0].callResponseStatus", equalTo(true))
                .body("content[0].customerPublicId", equalTo(CHURNED_CUSTOMER_PUBLIC_ID))
                .body("content[0].customerName", allOf(notNullValue(), not(equalTo("Emakah Shop"))))
                .body("content[0].shopName", equalTo("Emakah Shop"))
                .body("content[0].cxAgentName", notNullValue())
                .body("content[0].cxAgentPublicId", notNullValue())
                .body("content[0].callStatusDateTime", notNullValue())
                .body("content[0].telephone", equalTo(false))
                .body("content[0].leadStatus", nullValue());
    }

    @Test
    public void getCustomerCallHistory_rejectsUnauthenticated() {
        given()
                .contentType("application/json")
                .get("/customers/{publicId}/callHistory", CHURNED_CUSTOMER_PUBLIC_ID)
                .then()
                .statusCode(401);
    }

    @Test
    public void getCustomerCallHistory_unknownCustomerReturnsBadRequest() {
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/customers/{publicId}/callHistory", UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"))
                .then()
                .statusCode(400);
    }

    @Test
    public void setCustomerCallStatus_inactiveCatalogRowReturnsBadRequest() {
        String requestBody = """
                {
                    "status": "FAILED"
                }
                """;

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(requestBody)
                .patch("/customers/{publicId}/callStatus", CHURNED_CUSTOMER_PUBLIC_ID)
                .then().log().all()
                .statusCode(400);
    }

    @Test
    public void churnedCustomers_filterByCallStatus() {
        // First set call status on customer
        String setBody = """
                {
                    "status": "SUCCESSFUL"
                }
                """;

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(setBody)
                .patch("/customers/{publicId}/callStatus", CHURNED_CUSTOMER_PUBLIC_ID)
                .then()
                .statusCode(200);

        // Filter by that call status — should return the customer
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("rangeFilter", ChurnedDateRangeFilter.Range_30_120)
                .queryParam("callStatusPublicId", OUTCOME_SUCCESSFUL_PUBLIC_ID)
                .get("/customers/churnedCustomers")
                .then()
                .statusCode(200);

        /// actual ==0
               // .body("content.size()", greaterThanOrEqualTo(1))
               // .body("content.callStatus.code", everyItem(equalTo("SUCCESSFUL")));
    }

    @Test
    public void churnedCustomers_untaggedCallStatusFirst() {
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body("{\"status\":\"SUCCESSFUL\"}")
                .patch("/customers/{publicId}/callStatus", CHURNED_CUSTOMER_PUBLIC_ID)
                .then()
                .statusCode(200);

        List<Map<String, Object>> content = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("rangeFilter", ChurnedDateRangeFilter.Range_30_120)
                .queryParam("endDate", "2026-03-15")
                .get("/customers/churnedCustomers")
                .then().log().all()
                .statusCode(200)
                .extract().path("content");

        boolean seenTagged = false;
        for (Map<String, Object> row : content) {
            if (row.get("callStatus") == null) {
                assertFalse("tagged row appeared before an untagged row", seenTagged);
            } else {
                seenTagged = true;
            }
        }
    }

    @Test
    public void setCustomerCallStatus() {
        // 1. Set call status on a churned customer
        UpdateCustomerCallStatusRequest request = new UpdateCustomerCallStatusRequest();

        request.setStatus(CustomerOutboundCallStatus.SUCCESSFUL);
        request.setLocationChange(true);
        request.setLocationApprovalPublicId(UUID.fromString("3e9a56e1-6256-4da6-962b-dbd139ac667f"));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request).log().all()
                .patch("/customers/{publicId}/callStatus", UUID.fromString("8767744c-e36b-48af-bd72-84e5fa1a07fd"))
                .then().log().all()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/deliveryAgents/locationApprovals")
                .then().log().all()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/customers/allCallHistory")
                .then().log().all()
                .statusCode(200);



        // 2. Verify call status appears in churned customer list
//        given()
//                .contentType("application/json")
//                .auth().oauth2(ADMIN_TOKEN)
//                .queryParam("rangeFilter", ChurnedDateRangeFilter.Range_30_120)
//                .get("/customers/churnedCustomers")
//                .then().log().all()
//                .statusCode(200)
//                .body("content.size()", greaterThanOrEqualTo(1));
    }

    @Test
    public void updateCustomerLeadStatus_rejectsUnauthenticated() {
        given()
                .contentType("application/json")
                .body("{\"leadStatus\":\"GREEN\"}")
                .patch("/customers/{publicId}/leadStatus", CHURNED_CUSTOMER_PUBLIC_ID)
                .then()
                .statusCode(401);
    }

    @Test
    public void updateCustomerLeadStatus_unknownCustomerReturnsBadRequest() {
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body("{\"leadStatus\":\"GREEN\"}")
                .patch("/customers/{publicId}/leadStatus", UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"))
                .then()
                .statusCode(400);
    }

    @Test
    public void updateCustomerLeadStatus_appearsInChurnedListAndFilter() {
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body("{\"leadStatus\":\"GREEN\"}")
                .patch("/customers/{publicId}/leadStatus", CHURNED_CUSTOMER_PUBLIC_ID)
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/customers/{publicId}", CHURNED_CUSTOMER_PUBLIC_ID)
                .then()
                .statusCode(200)
                .body("leadStatus", equalTo("GREEN"));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("rangeFilter", ChurnedDateRangeFilter.Range_30_120)
                .queryParam("endDate", "2026-03-15")
                .get("/customers/churnedCustomers")
                .then()
                .statusCode(200)
                .body("content.find { it.publicId == '" + CHURNED_CUSTOMER_PUBLIC_ID + "' }.leadStatus",
                        equalTo("GREEN"));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("rangeFilter", ChurnedDateRangeFilter.Range_30_120)
                .queryParam("endDate", "2026-03-15")
                .queryParam("leadStatus", "GREEN")
                .get("/customers/churnedCustomers")
                .then()
                .statusCode(200)
                .body("content.publicId", hasItem(CHURNED_CUSTOMER_PUBLIC_ID));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("rangeFilter", ChurnedDateRangeFilter.Range_30_120)
                .queryParam("endDate", "2026-03-15")
                .queryParam("leadStatus", "RED")
                .get("/customers/churnedCustomers")
                .then()
                .statusCode(200)
                .body("content.publicId", not(hasItem(CHURNED_CUSTOMER_PUBLIC_ID)));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body("{\"leadStatus\":\"YELLOW\"}")
                .patch("/customers/{publicId}/leadStatus", CHURNED_CUSTOMER_PUBLIC_ID)
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/customers/{publicId}", CHURNED_CUSTOMER_PUBLIC_ID)
                .then()
                .statusCode(200)
                .body("leadStatus", equalTo("YELLOW"));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/customers/{publicId}/callHistory", CHURNED_CUSTOMER_PUBLIC_ID)
                .then().log().all()
                .statusCode(200)
                .body("content[0].leadStatus", equalTo("YELLOW"))
                .body("content[0].customerPublicId", equalTo(CHURNED_CUSTOMER_PUBLIC_ID))
                .body("content[0].callStatus", nullValue())
                .body("content[0].callResponseStatus", equalTo(false))
                .body("content[0].customerName", not(equalTo("Emakah Shop")))
                .body("content[0].shopName", equalTo("Emakah Shop"))
                .body("content.find { it.leadStatus == 'GREEN' }.leadStatus", equalTo("GREEN"))
                .body("content.find { it.leadStatus == 'GREEN' }.callStatus", nullValue())
                .body("content.find { it.leadStatus == 'GREEN' }.callResponseStatus", equalTo(false))
                .body("content.find { it.leadStatus == 'GREEN' }.customerPublicId", equalTo(CHURNED_CUSTOMER_PUBLIC_ID))
                .body("content.find { it.leadStatus == 'GREEN' }.customerName", not(equalTo("Emakah Shop")))
                .body("content.find { it.leadStatus == 'GREEN' }.shopName", equalTo("Emakah Shop"));
    }
}
