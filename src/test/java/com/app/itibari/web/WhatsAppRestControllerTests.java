package com.app.itibari.web;

import com.app.itibari.ItibariApplicationTests;
import com.app.itibari.dataTypes.DiscountType;
import com.app.itibari.models.entities.Person;
import com.app.itibari.models.request.WhatsAppOutboundMedia;
import com.app.itibari.models.request.WhatsAppRequest;
import com.app.itibari.repository.PersonRepository;
import com.app.itibari.services.WhatsAppResponseService;
import com.app.itibari.utils.helpers.TumaMessage;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class WhatsAppRestControllerTests extends ItibariApplicationTests {

    @Autowired
    private WhatsAppResponseService whatsAppResponseService;

    @Autowired
    private TumaMessage tumaMessage;

    @Autowired
    private PersonRepository personRepository;


    @Test
    public void testSaveWhatsMessage() {
        String payload = "{\n" +
                "  \"entry\": [\n" +
                "    {\n" +
                "      \"changes\": [\n" +
                "        {\n" +
                "          \"value\": {\n" +
                "            \"messaging_product\": \"whatsapp\",\n" +
                "            \"messages\": [\n" +
                "              {\n" +
                "                \"from\": \"16315551181\",\n" +
                "                \"id\": \"ABGGFlA5Fpa\",\n" +
                "                \"timestamp\": 1504902988,\n" +
                "                \"type\": \"text\",\n" +
                "                \"text\": {\n" +
                "                  \"body\": \"this is a text message\"\n" +
                "                }\n" +
                "              }\n" +
                "            ],\n" +
                "            \"history\": [\n" +
                "              {\n" +
                "                \"metadata\": {\n" +
                "                  \"phase\": 1,\n" +
                "                  \"chunk_order\": 131,\n" +
                "                  \"progress\": 30\n" +
                "                },\n" +
                "                \"threads\": [\n" +
                "                  {\n" +
                "                    \"id\": \"1234567890\",\n" +
                "                    \"messages\": [\n" +
                "                      {\n" +
                "                        \"from\": \"16505551111\",\n" +
                "                        \"id\": \"ABGGFlA5Fpa\",\n" +
                "                        \"timestamp\": 1504902988,\n" +
                "                        \"type\": \"media_placeholder\",\n" +
                "                        \"history_context\": {\n" +
                "                          \"status\": \"read\",\n" +
                "                          \"from_me\": true\n" +
                "                        }\n" +
                "                      },\n" +
                "                      {\n" +
                "                        \"from\": \"16315551181\",\n" +
                "                        \"id\": \"ABGGFlA5Fpa\",\n" +
                "                        \"timestamp\": 1504902988,\n" +
                "                        \"type\": \"text\",\n" +
                "                        \"text\": {\n" +
                "                          \"body\": \"this is a text message\"\n" +
                "                        },\n" +
                "                        \"history_context\": {\n" +
                "                          \"status\": \"delivered\",\n" +
                "                          \"from_me\": false\n" +
                "                        }\n" +
                "                      }\n" +
                "                    ]\n" +
                "                  }\n" +
                "                ]\n" +
                "              }\n" +
                "            ],\n" +
                "            \"metadata\": {\n" +
                "              \"display_phone_number\": \"16505551111\",\n" +
                "              \"phone_number_id\": \"123123123\"\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n";

        given()
                .contentType("application/json")
                .body(payload)
                .post("/itibari-whatsapp")
                .then().log().all()
                .statusCode(200);

        whatsAppResponseService.saveMessage(
                "{\"messages\":[{\"id\":\"normalized-phone-test\",\"message_status\":\"accepted\"}]}",
                "order_planning",
                "+16315551181",
                "Jane Jane",
                "ORD12345");

        given()
                .auth().oauth2(ADMIN_TOKEN)
                .contentType("application/json")
                .get("/itibari-whatsapp/get-all-messages")
                .then().log().all()
                .statusCode(200);

        given()
                .auth().oauth2(ADMIN_TOKEN)
                .contentType("application/json")
                .queryParam("searchParam", "16315551181")
                .get("/itibari-whatsapp/get-all-messages")
                .then().log().all()
                .statusCode(200)
                .body("content.find { it.phone == '+16315551181' }.unreadCount", equalTo(1));

        given()
                .auth().oauth2(ADMIN_TOKEN)
                .contentType("application/json")
                .queryParam("phoneNumber", "16315551181")
                .get("/itibari-whatsapp/get-all-message-items")
                .then().log().all()
                .statusCode(200);
    }

    @Test
    public void testSaveWhatsAppImageMessageWithoutCallingMeta() {
        // media id is fake so download fails gracefully; message body/type must still persist
        String payload = "{\n" +
                "  \"entry\": [{\n" +
                "    \"changes\": [{\n" +
                "      \"value\": {\n" +
                "        \"messaging_product\": \"whatsapp\",\n" +
                "        \"messages\": [{\n" +
                "          \"from\": \"16315551182\",\n" +
                "          \"id\": \"wamid.image.test\",\n" +
                "          \"timestamp\": 1504902988,\n" +
                "          \"type\": \"image\",\n" +
                "          \"image\": {\n" +
                "            \"caption\": \"receipt photo\",\n" +
                "            \"mime_type\": \"image/jpeg\",\n" +
                "            \"id\": \"fake-media-id\"\n" +
                "          }\n" +
                "        }]\n" +
                "      }\n" +
                "    }]\n" +
                "  }]\n" +
                "}";

        given()
                .contentType("application/json")
                .body(payload)
                .post("/itibari-whatsapp")
                .then().log().all()
                .statusCode(200);

        given()
                .auth().oauth2(ADMIN_TOKEN)
                .contentType("application/json")
                .queryParam("phoneNumber", "16315551182")
                .get("/itibari-whatsapp/get-all-message-items")
                .then().log().all()
                .statusCode(200)
                .body("content[0].message", equalTo("receipt photo"))
                .body("content[0].messageType", equalTo("image"));
    }

    @Test
    public void testSaveWhatsOutBound1Message() throws IOException, InterruptedException {
        tumaMessage.whatsAppCustomer("planning", "254708761565", "Jane Jane", "ORD12345");

        given()
                .auth().oauth2(ADMIN_TOKEN)
                .contentType("application/json")
                .get("/itibari-whatsapp/get-all-messages")
                .then().log().all()
                .statusCode(200);
    }

    @Test
    public void testSaveWhatsOutBoundMessage() {
        String body = String.format("{\n" +
                "  \"messaging_product\": \"whatsapp\",\n" +
                "  \"contacts\": [\n" +
                "    {\n" +
                "      \"input\": \"+255747179804\",\n" +
                "      \"wa_id\": \"255747179804\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"messages\": [\n" +
                "    {\n" +
                "      \"id\": \"wamid.HBgMMjU1NzQ3MTc5ODA0FQIAERgSN0I5N0NCMEY0OUZDMURCQTI5AA==\",\n" +
                "      \"message_status\": \"accepted\"\n" +
                "    }\n" +
                "  ]\n" +
                "}");


        whatsAppResponseService.saveMessage(body, "order_planning", "255747179804", "Jane Jane", "ORD12345");

        given()
                .auth().oauth2(ADMIN_TOKEN)
                .contentType("application/json")
                .get("/itibari-whatsapp/get-all-messages")
                .then().log().all()
                .statusCode(200);
    }

    @Test
    public void testSendRejectsFileUnderWrongFormFieldName() {
        given()
                .auth().oauth2(ADMIN_TOKEN)
                .multiPart("customerPhone", "+255000000000")
                .multiPart("messageBody", "Tazama sasa")
                .multiPart("attachment", "photo.jpg", new byte[]{1, 2, 3}, "image/jpeg")
                .post("/itibari-whatsapp/messages")
                .then().log().all()
                .statusCode(400)
                .body("userMessage", equalTo("Attach the file as a form field named file"));
    }

    @Test
    public void testSendGetIsMethodNotAllowed() {
        given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("customerPhone", "+255111111111")
                .queryParam("messageBody", "test")
                .get("/itibari-whatsapp/messages")
                .then().log().all()
                .statusCode(405)
                .body("userMessage", equalTo("HTTP method GET is not supported for this URL"));
    }

    @Test
    public void testReplyGetWithQueryParamsIsMethodNotAllowed() {
        given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("customerPhone", "+255762385818")
                .queryParam("messageBody", "test")
                .get("/itibari-whatsapp/reply")
                .then().log().all()
                .statusCode(405)
                .body("userMessage", equalTo("HTTP method GET is not supported for this URL"));
    }

    @Test
    public void testReplyPostWithQueryParamsDoesNotRequireJsonBody() {
        given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("customerPhone", "+255000000000")
                .queryParam("messageBody", "test")
                .post("/itibari-whatsapp/reply")
                .then().log().all()
                .statusCode(400)
                .body("userMessage", equalTo("Sorry, message to reply to not found"));
    }

    @Test
    public void testReplyPersistsTextAndAttachmentWithoutCallingMeta() {
        String from = "2558" + String.format("%08d", Math.abs(System.nanoTime() % 100_000_000L));
        String inboundId = "wamid.in." + from;
        String inboundPayload = "{\n" +
                "  \"entry\": [{\n" +
                "    \"changes\": [{\n" +
                "      \"value\": {\n" +
                "        \"messaging_product\": \"whatsapp\",\n" +
                "        \"messages\": [{\n" +
                "          \"from\": \"" + from + "\",\n" +
                "          \"id\": \"" + inboundId + "\",\n" +
                "          \"timestamp\": 1504902988,\n" +
                "          \"type\": \"text\",\n" +
                "          \"text\": { \"body\": \"hello from customer\" }\n" +
                "        }]\n" +
                "      }\n" +
                "    }]\n" +
                "  }]\n" +
                "}";

        given()
                .contentType("application/json")
                .body(inboundPayload)
                .post("/itibari-whatsapp")
                .then().log().all()
                .statusCode(200);

        Person agent = personRepository.findByEmailAndDeletedIsFalse("itibari@gmail.com")
                .orElseThrow(() -> new IllegalStateException("Admin user itibari@gmail.com is required for WhatsApp reply tests"));

        WhatsAppRequest textRequest = WhatsAppRequest.builder()
                .customerPhone(from)
                .messageBody("Asante")
                .build();
        whatsAppResponseService.saveReplyMessage(
                "{\"messages\":[{\"id\":\"wamid.out.text." + from + "\"}]}",
                textRequest,
                Optional.of(agent),
                null);

        WhatsAppRequest fileRequest = WhatsAppRequest.builder()
                .customerPhone(from)
                .messageBody("see attached")
                .build();
        WhatsAppOutboundMedia media = new WhatsAppOutboundMedia(
                "image", "image/jpeg", new byte[]{1, 2, 3}, "photo.jpg");
        whatsAppResponseService.saveReplyMessage(
                "{\"messages\":[{\"id\":\"wamid.out.image." + from + "\"}]}",
                fileRequest,
                Optional.of(agent),
                media);

        given()
                .auth().oauth2(ADMIN_TOKEN)
                .contentType("application/json")
                .queryParam("phoneNumber", from)
                .get("/itibari-whatsapp/get-all-message-items")
                .then().log().all()
                .statusCode(200)
                .body("content.find { it.messageId == 'wamid.out.text." + from + "' }.message", equalTo("Asante"))
                .body("content.find { it.messageId == 'wamid.out.text." + from + "' }.messageType", equalTo("text"))
                .body("content.find { it.messageId == 'wamid.out.image." + from + "' }.message", equalTo("see attached"))
                .body("content.find { it.messageId == 'wamid.out.image." + from + "' }.messageType", equalTo("image"))
                .body("content.find { it.messageId == 'wamid.out.image." + from + "' }.isCxReply", equalTo(true));
    }

}
