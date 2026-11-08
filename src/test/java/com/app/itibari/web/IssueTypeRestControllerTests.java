package com.app.itibari.web;

import com.app.itibari.ItibariApplicationTests;
import com.app.itibari.dataTypes.SlaType;
import com.app.itibari.models.request.*;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class IssueTypeRestControllerTests extends ItibariApplicationTests {

    public String createPriority() {
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

        return given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/helpTickets/priorities")
                .then().log().all()
                .statusCode(200)
                .extract().path("content[0].publicId");
    }

    @Test
    public void createIssueTypeTest() {
        UUID departmentId = UUID.fromString("15284538-302e-4920-a91e-f3dca0c164a3");
        String priorityPublicId = createPriority();

        IssueTypeRequest request = IssueTypeRequest.builder().name("Bad Packaging")
                .departmentPublicId(departmentId)
                .customerType(TicketDto.CustomerType.Existing_Customer)
                .priorityPublicId(UUID.fromString(priorityPublicId))
                .build();

        String issueTypePublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request).log().all()
                .post("/issueTypes")
                .then().log().all()
                .statusCode(200)
                .extract().path("publicId");

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
//                .queryParam("searchParam", "issue three")
//                .queryParam("departmentId", "b0683e0f-a839-4e26-bb1b-7f4510defec6")
                .queryParam("customerType", TicketDto.CustomerType.Existing_Customer)
                .get("/issueTypes")
                .then().log().all()
                .statusCode(200)
                .body("content.size()", equalTo(1));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/issueTypes/{publicId}", "9941542d-4108-4e4f-8f16-1f352ba4568a")
                .then().log().all()
                .statusCode(200);

        request = IssueTypeRequest.builder().name("Bad tEST Packaging")
                .departmentPublicId(departmentId)
                .priorityPublicId(UUID.fromString(priorityPublicId))
                .customerType(TicketDto.CustomerType.New_Customer)
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request).log().all()
                .put("/issueTypes/{publicId}", UUID.fromString(issueTypePublicId))
                .then().log().all()
                .statusCode(200);
    }

    @Test
    public void createFaqsTest() {
        UUID departmentId = UUID.fromString("15284538-302e-4920-a91e-f3dca0c164a3");
        String priorityPublicId = createPriority();

        IssueTypeRequest request = IssueTypeRequest.builder().name("Bad Packaging")
                .departmentPublicId(departmentId)
                .priorityPublicId(UUID.fromString(priorityPublicId))
                .customerType(TicketDto.CustomerType.New_Customer)
                .build();

        String issueTypePublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .post("/issueTypes")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        FaqRequest.FaqInner faqInner = FaqRequest.FaqInner.builder()
                .question("message for testing")
                .answer("answer for testing")
                .issueTypePublicId(UUID.fromString(issueTypePublicId))
                .build();
        FaqRequest faqRequest = FaqRequest.builder()
                .departmentPublicId(departmentId)
                .faqInnerList(List.of(faqInner))
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(faqRequest).log().all()
                .post("/helpTickets/faqs")
                .then().log().all()
                .statusCode(200);

        String customerFaqNotePublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/helpTickets/faqs")
                .then().log().all()
                .statusCode(200)
                .extract().path("content[0].noteList[0].publicId");

        FaqUpdateRequest faqUpdateRequest = FaqUpdateRequest.builder()
                .issueTypePublicId(UUID.fromString(issueTypePublicId))
                .answer("This my update")
                .question("Whose update is this")
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(faqUpdateRequest).log().all()
                .put("/helpTickets/faqs/{customerFaqNotePublicId}", customerFaqNotePublicId)
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/helpTickets/faqs")
                .then().log().all()
                .statusCode(200);
    }
}
