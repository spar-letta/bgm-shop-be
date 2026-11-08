package com.app.itibari.web;

import com.app.itibari.ItibariApplicationTests;
import com.app.itibari.dataTypes.*;
import com.app.itibari.models.entities.Transition;
import com.app.itibari.models.entities.TumaSmsDto;
import com.app.itibari.models.entities.TumaSmsKwaMtejaDto;
import com.app.itibari.models.entities.WorkflowStatus;
import com.app.itibari.models.request.*;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.*;

@Slf4j
public class HelpDeskRestControllerTests extends ItibariApplicationTests {

    @Test
    public void testCreateTicket() throws IOException {
        TestPayload testPayload = createWorkflowCustomerExperienceFormAndExport();

        UUID customerPublicId = UUID.fromString("11264d01-098e-407a-978d-2296b12425b6");
        UUID departmentId = UUID.fromString("b0683e0f-a839-4e26-bb1b-7f4510defec6");
        UUID issueId = UUID.fromString("cb67b08f-0281-4297-a45f-f70d139cb3f9");
        UUID assignedTo = UUID.fromString("5446a000-627c-4a83-b26e-2d01c692df91");

        TicketDto ticketDtoRequest = TicketDto.builder()
                .entityUserPublicId(customerPublicId)
                .departmentPublicId(departmentId)
                .issueTypeId(issueId)
                .content("Start your Free Trial with 300 in credit")
                .storeName("store one")
                .assignedToId(assignedTo)
                .customerType(TicketDto.CustomerType.Existing_Customer)
                .productPublicIds(List.of(UUID.fromString("581c5c73-afc0-4b89-bafb-e160011f2715")))
                .customerPublicIds(List.of(UUID.fromString("d7c5bf34-fda1-46e2-b5ee-40db98aed06d")))
                .templatePublicId(UUID.fromString(testPayload.templatePublicId()))
                .build();

        String helpDeskPublicId = given()
                .auth().oauth2(SUPPLIER_USER_TOKEN)
                .contentType(ContentType.JSON)
                .body(ticketDtoRequest).log().all()
                .post("/helpTickets")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        given()
                .auth().oauth2(SUPPLIER_USER_TOKEN)
                .contentType(ContentType.JSON)
                .body(ticketDtoRequest).log().all()
                .put("/helpTickets/{ticketPublicId}/actionRecordTicketViewer", helpDeskPublicId)
                .then()
                .statusCode(200);

        TicketActionDto ticketActionDto = TicketActionDto.builder()
                .ticketActionType(TicketActionDto.TicketActionType.Assign)
                .assignedToId(UUID.fromString("ee7226c2-9358-411a-b02b-2a77f8a882e8"))
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .body(ticketActionDto).log().all()
                .put("/helpTickets/{ticketPublicId}/actions", helpDeskPublicId)
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(SUPPLIER_USER_TOKEN)
//                .queryParam("searchParam", "TCK-100000")
//                .queryParam("priority", TicketPriority.High)
                .get("/helpTickets")
                .then().log().all()
                .statusCode(200);

        Response responseCustomer = given()
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .queryParam("exportType", StatementExportType.EXCEL)
                .get("/reports/helpTickets")
                .then()
                .statusCode(200)
                .extract().response();

        saveExport(responseCustomer, "ticket_report.xlsx");

//        given()
//                .contentType("application/json")
//                .auth().oauth2(ADMIN_TOKEN)
//                .get("/helpTickets/{publicId}", UUID.fromString(helpDeskPublicId))
//                .then().log().all()
//                .statusCode(200)
//                .body("workflowStatus", equalToIgnoringCase("NEW_TICKET"));
//
//        TicketUpdateDto ticketUpdateDto = TicketUpdateDto.builder()
//                .title("Second Title")
//                .content("This this this")
//                .ticketPriority(TicketPriority.Low)
//                .build();
//
//        given()
//                .contentType("application/json")
//                .auth().oauth2(SUPPLIER_USER_TOKEN)
//                .body(ticketUpdateDto).log().all()
//                .put("/helpTickets/{publicId}", helpDeskPublicId)
//                .then()
//                .statusCode(200)
//                .body("title", equalToIgnoringCase("issue three"))
//                .body("content", equalToIgnoringCase(ticketUpdateDto.content()));


        given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .get("/workflowEngine/{ticketPublicId}/transtionMinified", helpDeskPublicId)
                .then().log().all()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .get("/workflowEngine/{ticketPublicId}/transtionLinePoints", helpDeskPublicId)
                .then().log().all()
                .statusCode(200);


        given()
                .contentType("application/json")
                .auth().oauth2(SUPPLIER_USER_TOKEN)
                .get("/helpTickets/{ticketPublicId}/history", helpDeskPublicId)
                .then().log().all()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(SUPPLIER_USER_TOKEN)
                .get("/helpTickets/{ticketPublicId}/firstViewers", helpDeskPublicId)
                .then().log().all()
                .statusCode(200);

//        given()
//                .contentType("application/json")
//                .auth().oauth2(ADMIN_TOKEN)
//                .post("/workflowEngine/ticket/{ticketId}/transition/{statusPublicId}", helpDeskPublicId, "TRANSIT_NEW_TICKET_STEP_TWO")
//                .then()
//                .statusCode(200);
//
//        given()
//                .contentType("application/json")
//                .auth().oauth2(ADMIN_TOKEN)
//                .get("/helpTickets/{publicId}", UUID.fromString(helpDeskPublicId))
//                .then().log().all()
//                .statusCode(200)
////                .body("status", equalToIgnoringCase(HelpDeskStatus.In_Progress.name()))
//                .body("workflowStatus", equalToIgnoringCase("STEP_TWO"))
//                .body("assignedToName", notNullValue());
//
//        given()
//                .contentType("application/json")
//                .auth().oauth2(ADMIN_TOKEN)
//                .get("/workflowEngine/{ticketPublicId}/transtions", helpDeskPublicId)
//                .then().log().all()
//                .statusCode(200);

//        String notificationPublicId = given()
//                .contentType("application/json")
//                .auth().oauth2(ADMIN_TOKEN)
//                .get("/helpTickets/ticketNotifications")
//                .then().log().all()
//                .statusCode(200)
//                .body("count", equalTo(1))
//                .body("deskDtoList[0].read", equalTo(Boolean.FALSE))
//                .extract().path("deskDtoList[0].publicId");
//
//        TicketCommentDto ticketCommentDto = TicketCommentDto.builder()
//                .content("This is needed")
//                .build();
//        given()
//                .contentType("application/json")
//                .auth().oauth2(SUPPLIER_USER_TOKEN)
//                .body(ticketCommentDto)
//                .post("/helpTickets/{publicId}/comment", helpDeskPublicId)
//                .then()
//                .statusCode(200);
//
//        given()
//                .contentType("application/json")
//                .auth().oauth2(ADMIN_TOKEN)
//                .get("/helpTickets/{publicId}/comments", helpDeskPublicId)
//                .then().log().all()
//                .statusCode(200);
//
//        ticketCommentDto = TicketCommentDto.builder()
//                .content("This has been resolved")
//                .build();
//        given()
//                .contentType("application/json")
//                .auth().oauth2(ADMIN_TOKEN)
//                .body(ticketCommentDto)
//                .post("/helpTickets/{publicId}/comment", helpDeskPublicId)
//                .then()
//                .statusCode(200);
//
//        given()
//                .contentType("application/json")
//                .auth().oauth2(ADMIN_TOKEN)
//                .get("/helpTickets/{publicId}/comments", helpDeskPublicId)
//                .then().log().all()
//                .statusCode(200);
//
//        given()
//                .contentType("application/json")
//                .auth().oauth2(SUPPLIER_USER_TOKEN)
//                .get("/helpTickets/{publicId}/comments", helpDeskPublicId)
//                .then().log().all()
//                .statusCode(200);
//
//        ticketActionDto = TicketActionDto.builder()
//                .ticketActionType(TicketActionDto.TicketActionType.Resolve)
//                .build();
//
//        given()
//                .contentType("application/json")
//                .auth().oauth2(ADMIN_TOKEN)
//                .body(ticketActionDto).log().all()
//                .put("/helpTickets/{ticketPublicId}/actions", helpDeskPublicId)
//                .then()
//                .statusCode(200);
//
//        given()
//                .contentType("application/json")
//                .auth().oauth2(ADMIN_TOKEN)
//                .get("/helpTickets/{publicId}", UUID.fromString(helpDeskPublicId))
//                .then().log().all()
//                .statusCode(200);
//
//        given()
//                .contentType("application/json")
//                .auth().oauth2(ADMIN_TOKEN)
//                .get("/helpTickets/summary")
//                .then().log().all()
//                .statusCode(200);
//
//        log.info("------customerPublicId {}");
//        String customerId = given()
//                .contentType("application/json")
//                .auth().oauth2(SUPPLIER_USER_TOKEN)
//                .get("/helpTickets/customerWithSummarisedTickets")
//                .then().log().all()
//                .statusCode(200)
//                .extract().path("content[0].publicId");
//
//        given()
//                .contentType("application/json")
//                .auth().oauth2(SUPPLIER_USER_TOKEN)
//                .queryParam("customerPublicId", UUID.fromString("5446a000-627c-4a83-b26e-2d01c692df91"))
//                .get("/helpTickets")
//                .then().log().all()
//                .statusCode(200);
    }

    @Ignore
    @Test
    public void testCreateTicketWithNewCustomer() {
        TestPayload testPayload = createWorkflowCustomerExperienceFormAndExport();

        UUID issueId = UUID.fromString("cb67b08f-0281-4297-a45f-f70d139cb3f9");
        List<Map<String, Object>> customFields = new ArrayList<>();

        Map<String, Object> orderTypes = new HashMap<>();
        orderTypes.put("sortOrder", 0);
        orderTypes.put("label", "Order Types");
        orderTypes.put("fieldType", "Dropdown");
        orderTypes.put("mandatory", false);

        customFields.add(orderTypes);

        TicketDto ticketDtoRequest = TicketDto.builder()
                .issueTypeId(issueId)
                .content("Start your Free Trial with 300 in credit")
                .customerType(TicketDto.CustomerType.New_Customer)
                .customerName("Martin Kisia")
                .phone("+255676826608")
                .district("8c5d4db0-f9a1-47ed-a160-56780e4e4d14")
                .ward("90ad01f1-4a13-41f7-b825-c428b41f35d0")
                .postalAddress("P.O Box 2133")
                .customFields(customFields)
                .institution("institution test")
                .templatePublicId(UUID.fromString(testPayload.templatePublicId()))
                .build();

        String helpDeskPublicId = given()
                .auth().oauth2(SUPPLIER_USER_TOKEN)
                .contentType(ContentType.JSON)
                .body(ticketDtoRequest).log().all()
                .post("/helpTickets")
                .then().log().all()
                .statusCode(200)
                .extract().path("publicId");

        given()
                .contentType("application/json")
                .auth().oauth2(SUPPLIER_USER_TOKEN)
//                .queryParam("searchParam", "TCK-100000")
//                .queryParam("priority", TicketPriority.High)
                .get("/helpTickets")
                .then().log().all()
                .statusCode(200);

        log.info("-----------check j");
        given()
                .contentType("application/json")
                .auth().oauth2(SUPPLIER_USER_TOKEN)
                .get("/helpTickets/slaDetails/{ticketPublicId}", UUID.fromString(helpDeskPublicId))
                .then().log().all()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/helpTickets/{publicId}", UUID.fromString(helpDeskPublicId))
                .then().log().all()
                .statusCode(200)
                .body("workflowStatus", equalToIgnoringCase("NEW_TICKET"));
//                .body("assignedToName", equalToIgnoringCase(""));

        TicketUpdateDto ticketUpdateDto = TicketUpdateDto.builder()
                .title("Second Title")
                .content("This this this")
                .ticketPriority(TicketPriority.Low)
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(SUPPLIER_USER_TOKEN)
                .body(ticketUpdateDto).log().all()
                .put("/helpTickets/{publicId}", helpDeskPublicId)
                .then()
                .statusCode(200)
//                .body("title", equalToIgnoringCase("issue three"))
                .body("content", equalToIgnoringCase(ticketUpdateDto.content()));
//                .body("ticketPriority", equalToIgnoringCase(ticketUpdateDto.ticketPriority().name()));

        TicketActionDto ticketActionDto = TicketActionDto.builder()
                .ticketActionType(TicketActionDto.TicketActionType.Viewed)
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(ticketActionDto).log().all()
                .put("/helpTickets/{ticketPublicId}/actions", helpDeskPublicId)
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/helpTickets/{publicId}", UUID.fromString(helpDeskPublicId))
                .then().log().all()
                .statusCode(200)
                .body("assignedToName", notNullValue());

        String notificationPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/helpTickets/ticketNotifications")
                .then().log().all()
                .statusCode(200)
                .body("count", greaterThanOrEqualTo(1))
                .body("deskDtoList[0].read", equalTo(Boolean.FALSE))
                .extract().path("deskDtoList[0].publicId");

        TicketCommentDto ticketCommentDto = TicketCommentDto.builder()
                .content("This is needed")
                .build();
        given()
                .contentType("application/json")
                .auth().oauth2(SUPPLIER_USER_TOKEN)
                .body(ticketCommentDto)
                .post("/helpTickets/{publicId}/comment", helpDeskPublicId)
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/helpTickets/{publicId}/comments", helpDeskPublicId)
                .then().log().all()
                .statusCode(200);

        ticketCommentDto = TicketCommentDto.builder()
                .content("This has been resolved")
                .build();
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(ticketCommentDto)
                .post("/helpTickets/{publicId}/comment", helpDeskPublicId)
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/helpTickets/{publicId}/comments", helpDeskPublicId)
                .then().log().all()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(SUPPLIER_USER_TOKEN)
                .get("/helpTickets/{publicId}/comments", helpDeskPublicId)
                .then().log().all()
                .statusCode(200);

        ticketActionDto = TicketActionDto.builder()
                .ticketActionType(TicketActionDto.TicketActionType.Resolve)
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(ticketActionDto).log().all()
                .put("/helpTickets/{ticketPublicId}/actions", helpDeskPublicId)
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/helpTickets/{publicId}", UUID.fromString(helpDeskPublicId))
                .then().log().all()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/helpTickets/summary")
                .then().log().all()
                .statusCode(200);
    }

    @Test
    public void testCreateCustomerExperienceForm() {
        UUID departmentId = UUID.fromString("15284538-302e-4920-a91e-f3dca0c164a3");
        String issueTypeId = createIssueType(departmentId);

        List<Map<String, Object>> customFields = new ArrayList<>();

        Map<String, Object> orderTypes = new HashMap<>();
        orderTypes.put("sortOrder", 0);
        orderTypes.put("label", "Order Types");
        orderTypes.put("fieldType", "Dropdown");
        orderTypes.put("mandatory", false);
        orderTypes.put("options", Arrays.asList("Late", "OnTime", "OverDue"));

        Map<String, Object> orderNumber = new HashMap<>();
        orderNumber.put("sortOrder", 1);
        orderNumber.put("label", "Order Number");
        orderNumber.put("fieldType", "Number");
        orderNumber.put("mandatory", false);

        Map<String, Object> orderName = new HashMap<>();
        orderName.put("sortOrder", 2);
        orderName.put("label", "Order Name");
        orderName.put("fieldType", "Text");
        orderName.put("mandatory", false);

        customFields.add(orderTypes);
        customFields.add(orderNumber);
        customFields.add(orderName);
        CustomerExperienceRequest request = CustomerExperienceRequest.builder()
                .templateName("Template test Name")
                .issueTypePublicId(UUID.fromString(issueTypeId))
                .departmentInCopyPublicId(Collections.singletonList(departmentId))
                .assignTo(AssignToEnum.poc)
                .pocEmail("qopc@yopmail.com")
                .customFields(customFields)
                .emailInCopy(List.of("email@gmail.com", "email2@gmail.com", "email3@gmail.com", "email4@gmail.com"))
                .allowNotification(true)
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request).log().all()
                .post("/helpTickets/customerExperience")
                .then().log().all()
                .statusCode(200);

        String templatePublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("issueTypePublicId", UUID.fromString(issueTypeId))
                .get("/helpTickets/customerExperience")
                .then().log().all()
                .statusCode(200)
                .extract().path("content[0].publicId");

        StatusRequest statusRequest = StatusRequest.builder()
                .names(List.of("MY first one", "status next", "and the last status"))
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("departmentPublicId", departmentId)
                .body(statusRequest).log().all()
                .post("/workflowEngine/addTicketStatus/{templatePublicId}", UUID.fromString(templatePublicId))
                .then().log().all()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/workflowEngine/ticketStatusLists/{templatePublicId}", UUID.fromString(templatePublicId))
                .then().log().all()
                .statusCode(200);
    }

    public String createIssueType(UUID departmentId) {
        PrioritySettingRequest prioritySettingRequest = PrioritySettingRequest.builder()
                .priorityName("Low")
                .slaType(SlaType.Day)
                .slaDuration("4")
                .colorName("Rainbow")
                .firstTimeResponse(5)
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(prioritySettingRequest).log().all()
                .post("/helpTickets/priorities")
                .then().log().all()
                .statusCode(200);

        String priorityPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/helpTickets/priorities")
                .then().log().all()
                .statusCode(200)
                .extract().path("content[0].publicId");


        IssueTypeRequest request = IssueTypeRequest.builder().name("Bad Packaging")
                .departmentPublicId(departmentId)
                .priorityPublicId(UUID.fromString(priorityPublicId))
                .customerType(TicketDto.CustomerType.Existing_Customer)
                .build();

        return given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request).log().all()
                .post("/issueTypes")
                .then().log().all()
                .statusCode(200)
                .extract().path("publicId");
    }

    @Test
    public void testTumaSms() {
        TumaSmsDto tumaSmsDto = TumaSmsDto.builder()
                .messageBody("Dear {firstName}, Is this your store name {storeName} for This text is so mixed up as your last name is {lastName}. Hope you are fine {email} okay.")
                .multiplePhoneNumber(Set.of("+2557625818"))
                .isCustomer(Boolean.TRUE)
                .isGroup(Boolean.FALSE)
                .isDeliveryAgent(Boolean.FALSE)
                .isSalesAgent(Boolean.FALSE)
                .customVariable(Set.of("storeName", "firstName", "lastName", "email"))
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(SALES_AGENT_TOKEN)
                .body(tumaSmsDto).log().all()
                .post("/helpTickets/tuma-sms")
                .then().log().all()
                .statusCode(200);

        given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/helpTickets/tuma-sms")
                .then().log().all()
                .statusCode(200);
    }

    @Test
    public void testFetchFields() {
        given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/helpTickets/tuma-sms/fields")
                .then().log().all()
                .statusCode(200);
    }

    @Test
    public void testCreateSmsGroupTemplate() throws IOException {
        Response response = given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/helpTickets/smsGroupTemplate")
                .then()
                .statusCode(200)
                .extract().response();
        saveExport(response, "sms_group_template.xlsx");
    }


    public String testCreateSmsGroup() throws IOException {
        File sampleFile = new ClassPathResource("downloads/sms_group_template.xlsx", this.getClass().getClassLoader()).getFile();

        given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("group_name", "Chaned_Customer_2026_March")
                .multiPart("file", sampleFile)
                .post("/helpTickets/upload")
                .then()
                .statusCode(200);

        return given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/helpTickets/tuma-sms/groups")
                .then().log().all()
                .statusCode(200)
                .extract().path("content[0].publicId");
    }

    @Ignore
    @Test
    public void testTumaSmsKwaGroups() throws IOException {
        String groupPublicId = testCreateSmsGroup();
        TumaSmsDto tumaSmsDto = TumaSmsDto.builder()
                .messageBody("Dear {Name}, Is this your store name for This text is so mixed up as your last name is. Hope you are fine okay.")
                .isCustomer(Boolean.FALSE)
                .isGroup(Boolean.TRUE)
                .isDeliveryAgent(Boolean.FALSE)
                .isSalesAgent(Boolean.FALSE)
                .customVariable(Set.of("Name"))
                .groupPublicId(UUID.fromString(groupPublicId))
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(SALES_AGENT_TOKEN)
                .body(tumaSmsDto).log().all()
                .post("/helpTickets/tuma-sms")
                .then().log().all()
                .statusCode(200);

        given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/helpTickets/tuma-sms")
                .then().log().all()
                .statusCode(200);
    }

    @Test
    public void updatePrioritySetting() {
        PrioritySettingRequest prioritySettingRequest = PrioritySettingRequest.builder()
                .priorityName("Low")
                .slaType(SlaType.Day)
                .slaDuration("4")
                .colorName("Rainbow")
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(prioritySettingRequest).log().all()
                .post("/helpTickets/priorities")
                .then().log().all()
                .statusCode(200);

        String priorityPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/helpTickets/priorities")
                .then().log().all()
                .statusCode(200)
                .extract().path("content[0].publicId");

        prioritySettingRequest = PrioritySettingRequest.builder()
                .priorityName("Low Test")
                .slaType(SlaType.Hour)
                .slaDuration("3")
                .colorName("Brown")
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(prioritySettingRequest).log().all()
                .put("/helpTickets/priorities/{publicId}", UUID.fromString(priorityPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/helpTickets/priorities/{publicId}", UUID.fromString(priorityPublicId))
                .then().log().all()
                .statusCode(200);
    }

    @Test
    public void test_add_update_fetch_customer_notes() {
        UUID customerPublicId = UUID.fromString("11264d01-098e-407a-978d-2296b12425b6");
        CustomerNotesRequest customerNotesRequest = CustomerNotesRequest.builder()
                .noteType(NoteType.CS_NOTES)
                .content("Customer notes")
                .reasonCodePublicId(UUID.fromString("632b6fda-e23c-4a5f-bec0-a0f9e1f0e293"))
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(customerNotesRequest).log().all()
                .post("/helpTickets/customerNotes/{customerPublicId}", customerPublicId)
                .then().log().all()
                .statusCode(200);

        String customerNotePublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/helpTickets/customerNotes/{customerPublicId}/listings", customerPublicId)
                .then().log().all()
                .statusCode(200)
                .extract().path("content[0].publicId");

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/helpTickets/customerNotes/{publicId}", customerNotePublicId)
                .then().log().all()
                .statusCode(200);

        customerNotesRequest = CustomerNotesRequest.builder()
                .noteType(NoteType.CUSTOMER_NOTES)
                .content("SC notes")
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(customerNotesRequest).log().all()
                .put("/helpTickets/customerNotes/{publicId}", customerNotePublicId)
                .then().log().all()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/helpTickets/customerNotes/{publicId}", customerNotePublicId)
                .then().log().all()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .delete("/helpTickets/customerNotes/{publicId}", customerNotePublicId)
                .then().log().all()
                .statusCode(200);
    }

    //    @Ignore
    @Test
    public void test_tuma_ujumbe_kwa_customer() {
        UUID customerPublicId = UUID.fromString("11264d01-098e-407a-978d-2296b12425b6");
        TumaSmsKwaMtejaDto tumaSmsKwaMtejaDto = TumaSmsKwaMtejaDto.builder()
                .messageBody("Uko freshi🙋, karibu itibari🤓 kwa manunuzi ya bwana singida🤣")
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(tumaSmsKwaMtejaDto).log().all()
                .post("/helpTickets/tuma-sms-kwa-kastoma/{customerPublicId}", customerPublicId)
                .then().log().all()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("searchParam", "255764840059")
                .get("/helpTickets/tuma-sms")
                .then().log().all()
                .statusCode(200);
    }

    @Ignore
    @Test
    public void testRunTicketWorkflow() {
        String payload = String.format("{\n" +
                "  \"id\": \"123\",\n" +
                "  \"transitions\": [\n" +
                "    {\"name\": \"Validate Ticket\", \"from\": \"NEW_TICKET\", \"to\": \"VALIDATED\"},\n" +
                "    {\"name\": \"Assign to Logistics\", \"from\": \"VALIDATED\", \"to\": \"ASSIGNED_TO_LOGISTICS\"},\n" +
                "    {\"name\": \"Ship Order\", \"from\": \"ASSIGNED_TO_LOGISTICS\", \"to\": \"IN_TRANSIT\"},\n" +
                "    {\"name\": \"Confirm Delivery\", \"from\": \"IN_TRANSIT\", \"to\": \"DELIVERED\"},\n" +
                "    {\"name\": \"Close Ticket\", \"from\": \"DELIVERED\", \"to\": \"CLOSED\"}\n" +
                "  ]\n" +
                "}");

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(payload).log().all()
                .post("/workflowEngine/define")
                .then().log().all()
                .statusCode(200);

        given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/workflowEngine/define")
                .then().log().all()
                .statusCode(200);
    }

    @Ignore
    @Test
    public void testDeletes() {
        UUID departmentId = UUID.fromString("15284538-302e-4920-a91e-f3dca0c164a3");
        String issueTypeId = createIssueType(departmentId);

        List<Map<String, Object>> customFields = new ArrayList<>();

        Map<String, Object> orderTypes = new HashMap<>();
        orderTypes.put("sortOrder", 0);
        orderTypes.put("label", "Order Types");
        orderTypes.put("fieldType", "Dropdown");
        orderTypes.put("mandatory", false);
        orderTypes.put("options", Arrays.asList("Late", "OnTime", "OverDue"));

        Map<String, Object> orderNumber = new HashMap<>();
        orderNumber.put("sortOrder", 1);
        orderNumber.put("label", "Order Number");
        orderNumber.put("fieldType", "Number");
        orderNumber.put("mandatory", false);

        Map<String, Object> orderName = new HashMap<>();
        orderName.put("sortOrder", 2);
        orderName.put("label", "Order Name");
        orderName.put("fieldType", "Text");
        orderName.put("mandatory", false);

        customFields.add(orderTypes);
        customFields.add(orderNumber);
        customFields.add(orderName);
        CustomerExperienceRequest request = CustomerExperienceRequest.builder()
                .templateName("Template test Name")
                .issueTypePublicId(UUID.fromString(issueTypeId))
                .departmentInCopyPublicId(Collections.singletonList(departmentId))
                .assignTo(AssignToEnum.department)
                .pocEmail("qopc@yopmail.com")
                .customFields(customFields)
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request).log().all()
                .post("/helpTickets/customerExperience")
                .then().log().all()
                .statusCode(200);

        String templatePublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("issueTypePublicId", UUID.fromString(issueTypeId))
                .get("/helpTickets/customerExperience")
                .then().log().all()
                .statusCode(200)
                .extract().path("content[0].publicId");

        StatusRequest statusRequest = StatusRequest.builder()
                .names(List.of("NEW_TICKET", "step two", "step three"))
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("departmentPublicId", departmentId)
                .body(statusRequest).log().all()
                .post("/workflowEngine/addTicketStatus/{templatePublicId}", UUID.fromString(templatePublicId))
                .then().log().all()
                .statusCode(200);

        List<WorkflowStatus> workflowStatuses = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/workflowEngine/ticketStatusLists/{templatePublicId}", UUID.fromString(templatePublicId))
                .then().log().all()
                .statusCode(200)
                .extract().body().jsonPath().getList("content", WorkflowStatus.class);


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(statusRequest).log().all()
                .delete("/workflowEngine/ticketStatus/{publicId}", workflowStatuses.get(0).getPublicId())
                .then()
                .statusCode(200);

        log.info("----------cccc----");
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/helpTickets/ticketStatuses")
                .then().log().all()
                .statusCode(200);


        WorkflowStatus workflowStatus = workflowStatuses.get(0);
        WorkflowStatus workflowStatus2 = workflowStatuses.get(1);
        WorkflowStatus workflowStatus3 = workflowStatuses.get(2);

        WorkflowRequest.Transitions transitions1 = WorkflowRequest.Transitions.builder().from(workflowStatus.getPublicId()).to(workflowStatus2.getPublicId()).build();
        WorkflowRequest.Transitions transitions2 = WorkflowRequest.Transitions.builder().from(workflowStatus2.getPublicId()).to(workflowStatus3.getPublicId()).build();

        WorkflowRequest request1 = WorkflowRequest.builder()
                .templatePublicId(UUID.fromString(templatePublicId))
                .transitionsList(List.of(transitions1, transitions2))
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request1).log().all()
                .post("/workflowEngine/define")
                .then().log().all()
                .statusCode(200);
    }

    //    @Test
    public TestPayload createWorkflowCustomerExperienceFormAndExport() {
        UUID departmentId = UUID.fromString("b0683e0f-a839-4e26-bb1b-7f4510defec6");
        String issueTypeId = createIssueType(departmentId);

        List<Map<String, Object>> customFields = new ArrayList<>();

        Map<String, Object> orderTypes = new HashMap<>();
        orderTypes.put("sortOrder", 0);
        orderTypes.put("label", "Order Types");
        orderTypes.put("fieldType", "Dropdown");
        orderTypes.put("mandatory", false);
        orderTypes.put("options", Arrays.asList("Late", "OnTime", "OverDue"));

        Map<String, Object> orderNumber = new HashMap<>();
        orderNumber.put("sortOrder", 1);
        orderNumber.put("label", "Order Number");
        orderNumber.put("fieldType", "Number");
        orderNumber.put("mandatory", false);

        Map<String, Object> orderName = new HashMap<>();
        orderName.put("sortOrder", 2);
        orderName.put("label", "Order Name");
        orderName.put("fieldType", "Text");
        orderName.put("mandatory", false);

        customFields.add(orderTypes);
        customFields.add(orderNumber);
        customFields.add(orderName);
        CustomerExperienceRequest request = CustomerExperienceRequest.builder()
                .templateName("Template test Name")
                .issueTypePublicId(UUID.fromString(issueTypeId))
                .departmentInCopyPublicId(Collections.singletonList(departmentId))
                .assignTo(AssignToEnum.poc)
                .pocEmail("qopc@yopmail.com")
                .customFields(customFields)
                .emailInCopy(List.of("email2@yopmail.com", "email2@gmail.com", "email3@gmail.com", "email4@gmail.com"))
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request).log().all()
                .post("/helpTickets/customerExperience")
                .then().log().all()
                .statusCode(200);

        String templatePublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("issueTypePublicId", UUID.fromString(issueTypeId))
                .get("/helpTickets/customerExperience")
                .then().log().all()
                .statusCode(200)
                .extract().path("content[0].publicId");

        StatusRequest statusRequest = StatusRequest.builder()
                .names(List.of("NEW_TICKET", "step two", "step three"))
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(statusRequest).log().all()
                .queryParam("departmentPublicId", departmentId)
                .post("/workflowEngine/addTicketStatus/{templatePublicId}", UUID.fromString(templatePublicId))
                .then().log().all()
                .statusCode(200);

        List<WorkflowStatus> workflowStatuses = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
//                .queryParam("searchParam", "step")
                .queryParam("departmentPublicId", UUID.fromString("b0683e0f-a839-4e26-bb1b-7f4510defec6"))
                .get("/workflowEngine/ticketStatusLists/{templatePublicId}", UUID.fromString(templatePublicId))
                .then().log().all()
                .statusCode(200)
                .extract().body().jsonPath().getList("content", WorkflowStatus.class);


        WorkflowStatus workflowStatus = workflowStatuses.get(0);
        WorkflowStatus workflowStatus2 = workflowStatuses.get(1);
        WorkflowStatus workflowStatus3 = workflowStatuses.get(2);

        WorkflowRequest.Transitions transitions1 = WorkflowRequest.Transitions.builder().from(workflowStatus.getPublicId()).to(workflowStatus2.getPublicId()).durationNumber("2").build();
        WorkflowRequest.Transitions transitions2 = WorkflowRequest.Transitions.builder().from(workflowStatus2.getPublicId()).to(workflowStatus3.getPublicId()).durationNumber("4").build();

        WorkflowRequest request1 = WorkflowRequest.builder()
                .templatePublicId(UUID.fromString(templatePublicId))
                .transitionsList(List.of(transitions1, transitions2))
                .assignedDepartmentPublicId(departmentId)
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request1).log().all()
                .post("/workflowEngine/define")
                .then().log().all()
                .statusCode(200);

        return TestPayload.builder().templatePublicId(templatePublicId).transitionName(String.valueOf(workflowStatus3.getName())).build();
    }

    @Ignore
    @Test
    public void createWorkflowAndUpdate() {
        UUID departmentId = UUID.fromString("15284538-302e-4920-a91e-f3dca0c164a3");
        String issueTypeId = createIssueType(departmentId);

        List<Map<String, Object>> customFields = new ArrayList<>();

        Map<String, Object> orderTypes = new HashMap<>();
        orderTypes.put("sortOrder", 0);
        orderTypes.put("label", "Order Types");
        orderTypes.put("fieldType", "Dropdown");
        orderTypes.put("mandatory", false);
        orderTypes.put("options", Arrays.asList("Late", "OnTime", "OverDue"));

        Map<String, Object> orderNumber = new HashMap<>();
        orderNumber.put("sortOrder", 1);
        orderNumber.put("label", "Order Number");
        orderNumber.put("fieldType", "Number");
        orderNumber.put("mandatory", false);

        Map<String, Object> orderName = new HashMap<>();
        orderName.put("sortOrder", 2);
        orderName.put("label", "Order Name");
        orderName.put("fieldType", "Text");
        orderName.put("mandatory", false);

        customFields.add(orderTypes);
        customFields.add(orderNumber);
        customFields.add(orderName);
        CustomerExperienceRequest request = CustomerExperienceRequest.builder()
                .templateName("Template test Name")
                .issueTypePublicId(UUID.fromString(issueTypeId))
                .departmentInCopyPublicId(Collections.singletonList(departmentId))
                .assignTo(AssignToEnum.department)
                .pocEmail("qopc@yopmail.com")
                .customFields(customFields)
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request).log().all()
                .post("/helpTickets/customerExperience")
                .then().log().all()
                .statusCode(200);

        String templatePublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("issueTypePublicId", UUID.fromString(issueTypeId))
                .get("/helpTickets/customerExperience")
                .then().log().all()
                .statusCode(200)
                .extract().path("content[0].publicId");

        StatusRequest statusRequest = StatusRequest.builder()
                .names(List.of("new", "in progress", "pending", "approved", "close"))
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(statusRequest).log().all()
                .queryParam("departmentPublicId", departmentId)
                .post("/workflowEngine/addTicketStatus/{templatePublicId}", UUID.fromString(templatePublicId))
                .then().log().all()
                .statusCode(200);

        log.info("--------------list status-----------");
        List<WorkflowStatus> workflowStatuses = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("departmentPublicId", departmentId)
                .get("/workflowEngine/ticketStatusLists/{templatePublicId}", UUID.fromString(templatePublicId))
                .then().log().all()
                .statusCode(200)
                .extract().body().jsonPath().getList("content", WorkflowStatus.class);


        WorkflowStatus workflowStatus = workflowStatuses.get(0);
        WorkflowStatus workflowStatus2 = workflowStatuses.get(1);
        WorkflowStatus workflowStatus3 = workflowStatuses.get(2);

        StatusUpdateRequest statusUpdateRequest = StatusUpdateRequest.builder()
                .statusName("RESOLVED")
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(statusUpdateRequest).log().all()
                .put("/workflowEngine/updateTicketStatus/{statusPublicId}/{templatePublicId}", workflowStatus2.getPublicId(), UUID.fromString(templatePublicId))
                .then().log().all()
                .statusCode(200);


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/workflowEngine/ticketStatusLists/{templatePublicId}", UUID.fromString(templatePublicId))
                .then().log().all()
                .statusCode(200);

        WorkflowRequest.Transitions transitions1 = WorkflowRequest.Transitions.builder().from(workflowStatus.getPublicId()).to(workflowStatus2.getPublicId()).build();
        WorkflowRequest.Transitions transitions2 = WorkflowRequest.Transitions.builder().from(workflowStatus2.getPublicId()).to(workflowStatus3.getPublicId()).build();

        WorkflowRequest request1 = WorkflowRequest.builder()
                .templatePublicId(UUID.fromString(templatePublicId))
                .transitionsList(List.of(transitions1, transitions2))
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request1).log().all()
                .post("/workflowEngine/define")
                .then().log().all()
                .statusCode(200);


        String stepPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/workflowEngine/define")
                .then().log().all()
                .statusCode(200)
                .extract().path("content[0].transitions[0].publicId");

        WorkflowStepRequest workflowStepRequest = WorkflowStepRequest.builder()
                .transitionsList(List.of(WorkflowStepRequest.Transitions.builder()
                        .from(workflowStatus3.getPublicId())
                        .to(workflowStatus.getPublicId())
                        .build()))
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(workflowStepRequest).log().all()
                .put("/workflowEngine/define/{transitionPublicId}/updateStep", stepPublicId)
                .then().log().all()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/workflowEngine/define")
                .then().log().all()
                .statusCode(200);
    }

    @Ignore
    @Test
    public void createStatusAndWorkflowWorks() {
        UUID departmentId = UUID.fromString("15284538-302e-4920-a91e-f3dca0c164a3");
        String issueTypeId = createIssueType(departmentId);

        List<Map<String, Object>> customFields = new ArrayList<>();

        Map<String, Object> orderTypes = new HashMap<>();
        orderTypes.put("sortOrder", 0);
        orderTypes.put("label", "Order Types");
        orderTypes.put("fieldType", "Dropdown");
        orderTypes.put("mandatory", false);
        orderTypes.put("options", Arrays.asList("Late", "OnTime", "OverDue"));

        Map<String, Object> orderNumber = new HashMap<>();
        orderNumber.put("sortOrder", 1);
        orderNumber.put("label", "Order Number");
        orderNumber.put("fieldType", "Number");
        orderNumber.put("mandatory", false);

        Map<String, Object> orderName = new HashMap<>();
        orderName.put("sortOrder", 2);
        orderName.put("label", "Order Name");
        orderName.put("fieldType", "Text");
        orderName.put("mandatory", false);

        customFields.add(orderTypes);
        customFields.add(orderNumber);
        customFields.add(orderName);
        CustomerExperienceRequest request = CustomerExperienceRequest.builder()
                .templateName("Template test Name")
                .issueTypePublicId(UUID.fromString(issueTypeId))
                .departmentInCopyPublicId(Collections.singletonList(departmentId))
                .assignTo(AssignToEnum.department)
                .pocEmail("qopc@yopmail.com")
                .customFields(customFields)
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request).log().all()
                .post("/helpTickets/customerExperience")
                .then().log().all()
                .statusCode(200);

        String templatePublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("issueTypePublicId", UUID.fromString(issueTypeId))
                .get("/helpTickets/customerExperience")
                .then().log().all()
                .statusCode(200)
                .extract().path("content[0].publicId");

        StatusRequest statusRequest = StatusRequest.builder()
                .names(List.of("new", "in progress", "pending", "approved", "close"))
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(statusRequest).log().all()
                .queryParam("departmentPublicId", departmentId)
                .post("/workflowEngine/addTicketStatus/{templatePublicId}", UUID.fromString(templatePublicId))
                .then().log().all()
                .statusCode(200);

        List<WorkflowStatus> workflowStatuses = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("departmentPublicId", departmentId)
                .get("/workflowEngine/ticketStatusLists/{templatePublicId}", UUID.fromString(templatePublicId))
                .then().log().all()
                .statusCode(200)
                .extract().body().jsonPath().getList("content", WorkflowStatus.class);


        WorkflowStatus workflowStatus = workflowStatuses.get(0);
        WorkflowStatus workflowStatus2 = workflowStatuses.get(1);
        WorkflowStatus workflowStatus3 = workflowStatuses.get(2);

        WorkflowRequest.Transitions transitions1 = WorkflowRequest.Transitions.builder().from(workflowStatus.getPublicId()).to(workflowStatus2.getPublicId()).durationNumber("2").index(0).build();
        WorkflowRequest.Transitions transitions2 = WorkflowRequest.Transitions.builder().from(workflowStatus2.getPublicId()).to(workflowStatus3.getPublicId()).durationNumber("3").index(1).build();

        WorkflowRequest request1 = WorkflowRequest.builder()
                .templatePublicId(UUID.fromString(templatePublicId))
                .transitionsList(List.of(transitions1, transitions2))
                .assignedDepartmentPublicId(departmentId)
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request1).log().all()
                .post("/workflowEngine/define")
                .then().log().all()
                .statusCode(200);

        String workflowPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/workflowEngine/define")
                .then().log().all()
                .statusCode(200)
                .body("content[0].transitions.size()", Matchers.equalTo(3))
                .body("content[0].name", equalToIgnoringCase("Template test Name"))
                .extract().path("content[0].publicId");

        List<Transition> transitionList = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/workflowEngine/define")
                .then().log().all()
                .statusCode(200)
                .body("content[0].transitions.size()", Matchers.equalTo(3))
                .body("content[0].name", equalToIgnoringCase("Template test Name"))
                .extract().body().jsonPath().getList("content[0].transitions", Transition.class);

        Transition transition = transitionList.get(0);
        Transition transition1 = transitionList.get(1);

        UUID transitionPublicId1 = transition.getPublicId();
        UUID transitionPublicId2 = transition1.getPublicId();

        WorkflowStatus workflowStatus5 = workflowStatuses.get(4);

        AddWorkflowRequest.Transitions transitionToUpdate = AddWorkflowRequest.Transitions.builder().oldPublicId(transition.getPublicId()).from(transition.getStatusFrom().getPublicId()).to(transition.getStatusTo().getPublicId()).index(1).durationNumber("2").build();
        AddWorkflowRequest.Transitions transitionToUpdate1 = AddWorkflowRequest.Transitions.builder().oldPublicId(transition1.getPublicId()).from(transition1.getStatusFrom().getPublicId()).to(transition1.getStatusTo().getPublicId()).index(1).durationNumber("3").build();
        AddWorkflowRequest.Transitions transitionToUpdate2 = AddWorkflowRequest.Transitions.builder().from(workflowStatus5.getPublicId()).to(workflowStatus5.getPublicId()).index(1).durationNumber("4").build();


        AddWorkflowRequest request2 = AddWorkflowRequest.builder()
                .transitionsList(List.of(transitionToUpdate, transitionToUpdate1, transitionToUpdate2))
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request2).log().all()
                .put("/workflowEngine/define/{workflowPublicId}/update", UUID.fromString(workflowPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/workflowEngine/define")
                .then().log().all()
                .body("content[0].transitions.size()", Matchers.equalTo(4))
                .body("content[0].transitions.publicId", hasItems(transitionPublicId1.toString(), transitionPublicId2.toString()))
                .statusCode(200);
    }

    @Ignore
    @Test
    public void testSendCustomerLink() {
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("customerPhone", "255764840059")
                .post("/helpTickets/share-app-link")
                .then().log().all()
                .statusCode(200);
    }

    @Test
    public void testCreateSmsTemplateWorks() {
        SmsTemplateRequest request = SmsTemplateRequest.builder()
                .name("name1 name1")
                .message("Sample sms template")
                .smsChannel(SmsChannel.Sms)
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request).log().all()
                .post("/helpTickets/sms-template")
                .then().log().all()
                .statusCode(200);

        String templatePublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/helpTickets/sms-template")
                .then().log().all()
                .statusCode(200)
                .extract().path("content[0].publicId");

        request = SmsTemplateRequest.builder()
                .name("name1 name1")
                .message("Sample sms template updated")
                .smsChannel(SmsChannel.Sms)
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request).log().all()
                .put("/helpTickets/sms-template/{publicId}", UUID.fromString(templatePublicId))
                .then().log().all()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/helpTickets/sms-template")
                .then().log().all()
                .statusCode(200);
    }
}
