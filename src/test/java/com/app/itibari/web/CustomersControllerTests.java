package com.app.itibari.web;

import com.app.itibari.ItibariApplicationTests;
import com.app.itibari.dataTypes.*;
import com.app.itibari.models.entities.TumaSmsDto;
import com.app.itibari.models.request.*;
import com.app.itibari.services.CustomerExperienceService;
import com.app.itibari.services.ItibariSmsService;
import com.app.itibari.services.JobService;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Slf4j
public class CustomersControllerTests extends ItibariApplicationTests {

    @Autowired
    private ItibariSmsService itibariSmsService;
    @Autowired
    private JobService jobService;
    @Autowired
    private CustomerExperienceService customerExperienceService;

    @Ignore
    @Test
    public void fetchCustomersTestWorks() {
        String publicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/customers")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].publicId");


        String body = """
                {
                    "address": "kigamboni ",
                    "area": "Kinondoni",
                    "landmark": "Redhill - Nairobi",
                    "latitude": -1.2918418829728668,
                    "location": "kigamboni ",
                    "longitude": 36.825836934149265,
                    "name": "Antony Test",
                    "organizationLocationId": "90ad01f1-4a13-41f7-b825-c428b41f35d0",
                    "organizationRegionId": "8c5d4db0-f9a1-47ed-a160-56780e4e4d14",
                    "phoneNumber": "+255762385818",
                    "saleAgentRegistered": true,
                    "shopKeeperNumber":"+255762385877"
                }
                """;

        given()
                .contentType("application/json")
                .auth().oauth2(SALES_AGENT_TOKEN)
                .body(body).log().all()
                .patch("/customers/{publicId}", UUID.fromString(publicId))
                .then().log().all()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/customers/{publicId}", UUID.fromString(publicId))
                .then().log().all()
                .statusCode(200);
    }

    @Test
    @Ignore
    public void testFetchCustomer_withSamePhone() throws Exception {

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/customers")
                .then().log().all()
                .statusCode(200)
                .body("content.size()", equalTo(5));

        SalesOrderItemsRequest orderItems = SalesOrderItemsRequest
                .builder()
                .productId(UUID.fromString("08ebb084-2633-428f-82b9-e8d98a22ec53"))
                .quantity(5)
                .build();
        SalesOrdersRequest request = SalesOrdersRequest.builder()
                .customerId(UUID.fromString("6701ef3b-6e22-496e-9739-2e6fdc61cb74"))
                .bookedBy("Itibari1 Test1")
                .sourceChannel(SourceChannelEnum.BACK_OFFICE_APP.name())
                .salesOrderItems(Collections.singleton(orderItems))
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .post("/salesOrders")
                .then()
                .statusCode(200);

        orderItems = SalesOrderItemsRequest
                .builder()
                .productId(UUID.fromString("7c4a6d52-5da2-47fe-92a8-ea2fce2602af"))
                .quantity(1)
                .build();
        request = SalesOrdersRequest.builder()
                .customerId(UUID.fromString("63ef7d9b-8ca6-462f-a177-843a82abf900"))
                .bookedBy("Itibari2 Test2")
                .sourceChannel(SourceChannelEnum.BACK_OFFICE_APP.name())
                .salesOrderItems(Collections.singleton(orderItems))
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .post("/salesOrders")
                .then()
                .statusCode(200);


        orderItems = SalesOrderItemsRequest
                .builder()
                .productId(UUID.fromString("7c4a6d52-5da2-47fe-92a8-ea2fce2602af"))
                .quantity(1)
                .build();
        request = SalesOrdersRequest.builder()
                .customerId(UUID.fromString("02f965b5-819b-406a-b8b7-b610cb738296"))
                .bookedBy("Itibari3 Test3")
                .sourceChannel(SourceChannelEnum.BACK_OFFICE_APP.name())
                .salesOrderItems(Collections.singleton(orderItems))
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .post("/salesOrders")
                .then()
                .statusCode(200);

        given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders")
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/customers/fixDuplicate")
                .then().log().all()
                .statusCode(200);


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("customerId", UUID.fromString("6701ef3b-6e22-496e-9739-2e6fdc61cb74"))
                .get("/salesOrders")
                .then().log().all()
                .statusCode(200)
                .body("content.size()", equalTo(3));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("customerId", UUID.fromString("63ef7d9b-8ca6-462f-a177-843a82abf900"))
                .get("/salesOrders")
                .then().log().all()
                .statusCode(200)
                .body("content.size()", equalTo(0));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("customerId", UUID.fromString("02f965b5-819b-406a-b8b7-b610cb738296"))
                .get("/salesOrders")
                .then().log().all()
                .statusCode(200)
                .body("content.size()", equalTo(0));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/customers")
                .then().log().all()
                .statusCode(200)
                .body("content.size()", equalTo(3));
    }

    @Test
    public void fetchCustomers() {
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("phoneNumber", "+255772345345")
                .get("/customers")
                .then().log().all()
                .statusCode(200);
    }

    @Test
    public void fetchCustomerByPhoneTestWorks() {
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("phoneNumber", "+255772345345")
                .get("/customers/naPoneNumber")
                .then().log().all()
                .statusCode(200);
    }

    @Test
    @Ignore
    public void testCreateChildCustomerWithParentCustomerPublicIdWorks() {
        UUID parentCustomerPublicId = UUID.fromString("ef2f4703-b478-4ca6-98d1-bc3aa88e1c3f");
        String phoneNumber = "07" + String.valueOf(System.currentTimeMillis()).substring(3, 11);

        String body = String.format("""
                {
                    "name": "Child Customer Test",
                    "firstName": "Child",
                    "lastName": "Customer",
                    "email": "%s@gmail.com",
                    "nationalId": "12345678901",
                    "phoneNumber": "%s",
                    "sourceChannel": "%s",
                    "latitude": -1.2918418829728668,
                    "longitude": 36.825836934149265,
                    "address": "p.o box 12-nai",
                    "landmark": "Redhill - Nairobi",
                    "tin": "123452345",
                    "customerType": "%s",
                    "organizationRegionId": "ae9df8ce-6ad0-4829-8082-95fb4c491e95",
                    "organizationLocationId": "0e0b9c9c-3dfd-43c5-8be7-60a28e1ef2a0",
                    "selfRegistered": false,
                    "parentCustomerRegistered": true,
                    "saleAgentRegistered": false,
                    "storeName": "Child Customer Test",
                    "parentCustomerPublicId": "%s"
                }
                """,
                phoneNumber,
                phoneNumber,
                SourceChannelEnum.CUSTOMER_CARE_APP.name(),
                CustomerType.REGULAR.name(),
                parentCustomerPublicId
        );

        String createdCustomerPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(body)
                .log().all()
                .post("/customers")
                .then().log().all()
                .statusCode(200)
                .body("publicId", notNullValue())
                .extract().path("publicId");

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("phoneNumber", phoneNumber)
                .get("/customers")
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].publicId", equalTo(createdCustomerPublicId))
                .body("content[0].hasParentCustomer", equalTo(true));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/wallets")
                .then().log().all()
                .statusCode(200);
    }

    @Ignore
    @Test
    public void testCustomerCreateProcess() {
        String customerOne = createCustomerPayload("Dirah Saahar", "Dirah", "Saahar", "0708543212",
                SourceChannelEnum.CUSTOMER_CARE_APP.name(),
                "ae9df8ce-6ad0-4829-8082-95fb4c491e95"
                , "0e0b9c9c-3dfd-43c5-8be7-60a28e1ef2a0", Boolean.TRUE, Boolean.FALSE, CustomerType.REGULAR);

        String tokenGen = given()
                .contentType(ContentType.JSON)
                .body(customerOne)
                .post("/authentication/mobilePlatform/customerRegister")
                .then().log().all()
                .statusCode(200)
                .extract().path("token");

        // has no mapped by
        String customerSelfCreatedPublicId = given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("phoneNumber", "0708543212")
                .get("/customers")
                .then().log().all()
                .statusCode(200)
                .body("content[0].servedBy", nullValue())
                .extract().path("content[0].publicId");

        String salesAgentPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("organisationRegionPublicId", UUID.fromString("ae9df8ce-6ad0-4829-8082-95fb4c491e95"))
                .queryParam("organisationLocationPublicId", UUID.fromString("8946aa9d-396a-41f6-881e-8819386ec1b3"))
                .get("/salesAgents")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .extract().path("content[0].publicId");


        AgentAction agentAction = new AgentAction(AgentAction.ActionType.Map_Customer, Collections.singleton(UUID.fromString(customerSelfCreatedPublicId)),
                null);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(agentAction).log().all()
                .patch("/salesAgents/{salesAgentPublicId}/actions", UUID.fromString(salesAgentPublicId))
                .then()
                .statusCode(200);

        given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("phoneNumber", "0708543212")
                .get("/customers")
                .then().log().all()
                .statusCode(200)
                .body("content[0].servedBy", notNullValue());

        //create order
        UUID productId = UUID.fromString("534cf5d4-f8c3-495c-a813-9a42f338c466");

        SalesOrderItemsRequest orderItems = SalesOrderItemsRequest
                .builder()
                .productId(productId)
                .quantity(3)
                .build();
        SalesOrdersRequest request = SalesOrdersRequest.builder()
                .customerId(UUID.fromString("d7c5bf34-fda1-46e2-b5ee-40db98aed06d"))
                .bookedBy("Itibari Test")
                .sourceChannel(SourceChannelEnum.CUSTOMER_CARE_APP.name())
                .salesOrderItems(Collections.singleton(orderItems))
                .build();

        int expectedQuantity = orderItems.getQuantity();
        double expectedPricePerUnit = 36000.00;
        double expectedTotalAmount = expectedQuantity * expectedPricePerUnit;

        String orderPublicId = given()
                .contentType("application/json")
                .auth().oauth2(tokenGen)
                .body(request)
                .post("/salesOrders/customer/placeOrder")
                .then()
                .statusCode(200)
                .body("salesOrderItems[0].quantity", equalTo(orderItems.getQuantity()))
                .body("salesOrderItems[0].totalAmount", equalTo((float) expectedTotalAmount))
                .extract().path("publicId");

        given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders/{salesOrderPublicId}", orderPublicId)
                .then().log().all()
                .statusCode(200);
    }

    @Test
    public void testCustomerFetchLocations() {
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(SALES_AGENT_TOKEN)
                .queryParam("isFromSA", Boolean.TRUE)
                .get("/customers")
                .then().log().all()
                .statusCode(200);
    }

    @Test
    public void fetchCustomersBySupplier() throws IOException {
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/customers/supplierCustomers")
                .then().log().all()
                .statusCode(200);

    }

    @Test
    public void testDeActivateCustomersTestWorks() {

        UUID customerId = UUID.fromString("d7c5bf34-fda1-46e2-b5ee-40db98aed06d");
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .pathParams("customerPublicId", customerId)
                .get("/customers/{customerPublicId}")
                .then()
                .statusCode(200)
                .body("active", equalTo(true));

        CustomerAction customerAction = CustomerAction.builder()
                .actionType(CustomerAction.ActionType.De_Activate)
                .rejectionCode("Wrong SKU")
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(customerAction).log().all()
                .pathParams("customerPublicId", customerId)
                .patch("/customers/{customerPublicId}/actions")
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .pathParams("customerPublicId", customerId)
                .get("/customers/{customerPublicId}")
                .then()
                .statusCode(200)
                .body("active", equalTo(false))
                .body("status", equalToIgnoringCase("Pending_DeActivation"));

        customerAction = CustomerAction.builder()
                .actionType(CustomerAction.ActionType.Approve_DeActivate)
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(customerAction).log().all()
                .pathParams("customerPublicId", customerId)
                .patch("/customers/{customerPublicId}/actions")
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .pathParams("customerPublicId", customerId)
                .get("/customers/{customerPublicId}")
                .then()
                .statusCode(200)
                .body("active", equalTo(false))
                .body("status", equalToIgnoringCase("De_Activated"));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("active", Boolean.FALSE)
                .get("/customers")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1));

        customerAction = CustomerAction.builder()
                .actionType(CustomerAction.ActionType.Activate)
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(customerAction).log().all()
                .pathParams("customerPublicId", customerId)
                .patch("/customers/{customerPublicId}/actions")
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("active", Boolean.FALSE)
                .pathParams("customerPublicId", customerId)
                .get("/customers/{customerPublicId}")
                .then()
                .statusCode(200)
                .body("active", equalTo(true))
                .body("status", equalToIgnoringCase("Active"));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("active", Boolean.TRUE)
                .get("/customers")
                .then().log().all()
                .statusCode(200);
    }

    @Ignore
    @Test
    public void testCustomerCreateAndUpdateProcess() {
        String customerOne = createCustomerPayload("JJ shop", "", "", "0708543212",
                SourceChannelEnum.CUSTOMER_CARE_APP.name(),
                "ae9df8ce-6ad0-4829-8082-95fb4c491e95"
                , "0e0b9c9c-3dfd-43c5-8be7-60a28e1ef2a0", Boolean.TRUE, Boolean.FALSE, CustomerType.REGULAR);

        given()
                .contentType(ContentType.JSON)
                .body(customerOne).log().all()
                .post("/authentication/mobilePlatform/customerRegister")
                .then()
                .statusCode(200);

        String customerPublicId = given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("phoneNumber", "0708543212")
                .get("/customers")
                .then().log().all()
                .statusCode(200)
                .extract().path("content[0].publicId");

        customerOne = createCustomerUpdatePayload("JJ shop", "Alfatuu", "jemmy", "0708543212",
                SourceChannelEnum.CUSTOMER_CARE_APP.name(),
                "ae9df8ce-6ad0-4829-8082-95fb4c491e95"
                , "0e0b9c9c-3dfd-43c5-8be7-60a28e1ef2a0", Boolean.TRUE, Boolean.FALSE);

        given()
                .auth().oauth2(ADMIN_TOKEN)
                .contentType(ContentType.JSON)
                .body(customerOne).log().all()
                .patch("/customers/{customerPublicId}", UUID.fromString(customerPublicId))
                .then()
                .statusCode(200);

        given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("phoneNumber", "0708543212")
                .get("/customers")
                .then().log().all()
                .statusCode(200);

        given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/customers")
                .then().log().all()
                .statusCode(200);
    }

    @Test
    public void test_ListChurnedCustoms_Works() throws IOException { // 2026-02-20 17:32:25
        UUID customerPublicId = UUID.fromString("37946b2b-b08d-4117-9927-f8adcf32292f");
        CustomerNotesRequest customerNotesRequest = CustomerNotesRequest.builder()
                .noteType(NoteType.CS_NOTES)
                .content("Customer notes")
                .reasonCodePublicId(UUID.fromString("632b6fda-e23c-4a5f-bec0-a0f9e1f0e293"))
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .body(customerNotesRequest).log().all()
                .post("/helpTickets/customerNotes/{customerPublicId}", customerPublicId)
                .then().log().all()
                .statusCode(200);

        String publicId = given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
//                .queryParam("rangeFilter", ChurnedDateRangeFilter.Range_30_120)
//                .queryParam("customerType", CustomerType.REGULAR)
                .queryParam("departmentPublicId", "b0683e0f-a839-4e26-bb1b-7f4510defec6")
                .get("/customers/churnedCustomers")
                .then().log().all()
                .statusCode(200)
                .extract().path("content[0].publicId");

//        customerExperienceService.sendCustomerNotesToDepartmentOfficers();

//        given()
//                .contentType("application/json")
//                .auth().oauth2(ADMIN_TOKEN)
//                .get("/salesOrders/customerOrders/{customerPublicId}", UUID.fromString("37946b2b-b08d-4117-9927-f8adcf32292f"))
//                .then().log().all()
//                .statusCode(200);
//
//        given()
//                .contentType(ContentType.JSON)
//                .auth().oauth2(ADMIN_TOKEN)
//                .post("/helpTickets/{customerPublicId}/lastContactDate", UUID.fromString(publicId))
//                .then()
//                .statusCode(200);
//
//        given()
//                .contentType("application/json")
//                .auth().oauth2(ADMIN_TOKEN)
//                .get("/customers/{publicId}", UUID.fromString(publicId))
//                .then().log().all()
//                .statusCode(200);
//
        Response response = given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("rangeFilter", ChurnedDateRangeFilter.Range_30_120)
//                .queryParam("customerType", CustomerType.REGULAR)
                .queryParam("exportType", StatementExportType.EXCEL)
                .get("/reports/exportChurnedCustomers")
                .then()
                .statusCode(200)
                .extract().response();

        saveExport(response, "churned_export.xlsx");
    }


    @Test
    @Ignore
    public void testSendSmsViaBeem() {
        itibariSmsService.send("+255676826608", "Test message");
    }

//    @Ignore
    @Test
    public void testRunActiveCustomersWorks() throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, InterruptedException, JobRestartException {
//        jobService.runActiveCustomerJob("Hello {firstName} your store name is {storeName}!- Campaign Alert Test", Set.of("storeName", "firstName"), "camp name");

        given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/helpTickets/tuma-sms")
                .then().log().all()
                .statusCode(200);

    }
}
