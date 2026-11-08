package com.app.itibari.web;

import com.app.itibari.ItibariApplicationTests;
import com.app.itibari.dataTypes.*;
import com.app.itibari.models.request.*;
import io.restassured.http.ContentType;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Slf4j
public class ProcurementRestControllerTests extends ItibariApplicationTests {
    @Test
    public void testFetchSuppliers() {
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/suppliers")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));
    }

    @Test
    public void fetchPurchaseOrders() {
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrders")
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(2));
    }

    @Test
    public void fetchPurchaseOrderItemsTest() {
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}/items", UUID.fromString("97bd0646-4cca-49c4-961a-021243fd3e0e"))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));
    }

    @Test
    @Ignore  // return note data commentated out in data.sql for now
    public void testReplacementPurchaseOrderCreate() {

        String publicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .post("/procurement/purchaseOrder/{goodReturnNotePublicId}/replacement", UUID.fromString("066c26b4-05ff-4c6f-b97c-dc8292ed5f70"))
                .then().log().all()
                .statusCode(200)
                .extract().path("publicId");


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}/items", UUID.fromString(publicId))
                .then().log().all()
                .statusCode(200)
                .body("content.size()", equalTo(2));
    }

    @Ignore
    @Test
    public void testReplacementPurchaseOrderFromReceiveToReplacement() {
        // receive, then return, then raise a replacement
        PurchaseOrderItemReceiveRequest itemRequest = new PurchaseOrderItemReceiveRequest();
        itemRequest.setQuantityReceived(1);
        itemRequest.setOrderItemPublicId(UUID.fromString("57eafdea-2a30-4119-8580-8043b245e95f"));
        PurchaseOrderReceiveRequest request = new PurchaseOrderReceiveRequest();
        request.setDeliveryNoteNumber("ITIBARI-09039");
        request.setPurchaseOrderItems(Collections.singletonList(itemRequest));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .patch("/procurement/purchaseOrder/{purchaseOrderPublicId}/receiveGoods", UUID.fromString("a3495a45-9326-4806-8aa7-d7d9a7b34d67"))
                .then().log().all()
                .statusCode(200);


        String publicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/goodsReturnNote/receive")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].publicId");


        String receiveItemPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/goodsReturnNote/receive/{goodsReturnNotePublicId}/items", UUID.fromString(publicId))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].publicId");


        GoodsNoteItemRequest item = new GoodsNoteItemRequest();
        item.setItemPublicId(UUID.fromString(receiveItemPublicId));
        item.setQuantity(1);
        item.setRejectionCodePublicId(UUID.fromString("d88a0e04-23b7-4145-93ba-48b31efacf8d"));

        GoodsReturnNoteRequest returnRequest = new GoodsReturnNoteRequest();
        returnRequest.setReturnNotePublicId(UUID.fromString(publicId));
        returnRequest.setGoodsReturnDate(LocalDate.now());
        returnRequest.setGoodsNoteItemRequests(Collections.singletonList(item));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(returnRequest)
                .post("/goodsReturnNote")
                .then()
                .statusCode(200);


        String notePublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/goodsReturnNote")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].publicId");


        String poPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .post("/procurement/purchaseOrder/{goodReturnNotePublicId}/replacement", UUID.fromString(notePublicId))
                .then().log().all()
                .statusCode(200)
                .extract().path("publicId");


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}/items", UUID.fromString(poPublicId))
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));
    }


    @Test
    public void testPurchaseOrderPartialReceive() {

        checkInventoryNegativeTests();

        PurchaseOrderItemReceiveRequest itemRequest = new PurchaseOrderItemReceiveRequest();
        itemRequest.setQuantityReceived(50);
        itemRequest.setOrderItemPublicId(UUID.fromString("2818b47e-f1d4-4f57-842f-a45b9b6ab819"));


        PurchaseOrderItemReceiveRequest itemRequest1 = new PurchaseOrderItemReceiveRequest();
        itemRequest1.setQuantityReceived(280);
        itemRequest1.setOrderItemPublicId(UUID.fromString("80a6daa6-0b5a-4c43-9ea3-81fdb897329d"));

        PurchaseOrderItemReceiveRequest itemRequest2 = new PurchaseOrderItemReceiveRequest();
        itemRequest2.setQuantityReceived(385);
        itemRequest2.setOrderItemPublicId(UUID.fromString("57eafdea-2a30-4119-8580-8043b245e95f"));


        PurchaseOrderReceiveRequest request = new PurchaseOrderReceiveRequest();
        request.setDeliveryNoteNumber("ITIBARI-09039");
        request.setPurchaseOrderItems(java.util.Arrays.asList(itemRequest, itemRequest1, itemRequest2));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .patch("/procurement/purchaseOrder/{purchaseOrderPublicId}/receiveGoods", UUID.fromString("97bd0646-4cca-49c4-961a-021243fd3e0e"))
                .then().log().all()
                .statusCode(200);

        checkInventoryNegativeTests();

        String publicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/goodsReturnNote/receive")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].publicId");


        String receiveItemPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/goodsReturnNote/receive/{goodsReturnNotePublicId}/items", UUID.fromString(publicId))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].publicId");


        given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString("97bd0646-4cca-49c4-961a-021243fd3e0e"))
                .then().log().all()
                .statusCode(200)
                .body("status", equalTo(PurchaseOrderStatus.PARTIAL_RECEIVED.name()));


        given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}/items", UUID.fromString("97bd0646-4cca-49c4-961a-021243fd3e0e"))
                .then().log().all()
                .statusCode(200);


        itemRequest = new PurchaseOrderItemReceiveRequest();
        itemRequest.setQuantityReceived(150);
        itemRequest.setOrderItemPublicId(UUID.fromString("2818b47e-f1d4-4f57-842f-a45b9b6ab819"));


        itemRequest1 = new PurchaseOrderItemReceiveRequest();
        itemRequest1.setQuantityReceived(480);
        itemRequest1.setOrderItemPublicId(UUID.fromString("80a6daa6-0b5a-4c43-9ea3-81fdb897329d"));

        itemRequest2 = new PurchaseOrderItemReceiveRequest();
        itemRequest2.setQuantityReceived(535);
        itemRequest2.setOrderItemPublicId(UUID.fromString("57eafdea-2a30-4119-8580-8043b245e95f"));


        request = new PurchaseOrderReceiveRequest();
        request.setDeliveryNoteNumber("ITIBARI-09039");
        request.setPurchaseOrderItems(java.util.Arrays.asList(itemRequest, itemRequest1, itemRequest2));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .patch("/procurement/purchaseOrder/{purchaseOrderPublicId}/receiveGoods", UUID.fromString("97bd0646-4cca-49c4-961a-021243fd3e0e"))
                .then().log().all()
                .statusCode(400);


        checkInventoryNegativeTests();


    }

    private void checkInventoryNegativeTests() {

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].availableStock", greaterThanOrEqualTo(0));
    }

    @Test
    public void testCancelPurchaseOrderAfterPaymentWorks() throws IOException {
        String payload = productPayload("String description", UUID.randomUUID());
        String productPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(payload)
                .post("/products")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        PurchaseOrderRequest purchaseOrderRequest = new PurchaseOrderRequest();
        purchaseOrderRequest.setOrderDate(LocalDate.now());
        purchaseOrderRequest.setSupplierId(UUID.fromString("0bacd072-df86-4d05-bf86-27a1d38cacfb"));
        purchaseOrderRequest.setWarehouseId(UUID.fromString("3849b1be-a9dd-4f11-abbf-229ad2780a56"));
        purchaseOrderRequest.setProformaInvoiceNumber("23458");
        purchaseOrderRequest.setRebate(false);

        PurchaseOrderItemRequest purchaseOrderItemRequest = new PurchaseOrderItemRequest();
        purchaseOrderItemRequest.setProductPublicId(UUID.fromString(productPublicId));
        purchaseOrderItemRequest.setQuantity(4);
        purchaseOrderItemRequest.setUnitPrice(new BigDecimal(10));

        purchaseOrderRequest.setPurchaseOrderItems(List.of(purchaseOrderItemRequest));
        purchaseOrderRequest.setComment("This is a test purchase order");


        String POPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(purchaseOrderRequest)
                .post("/procurement/purchaseOrder")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then().log().all()
                .statusCode(200)
                .body("status", equalToIgnoringCase("PENDING"))
                .body("purchaseOrderItems[0].quantity", equalTo(purchaseOrderItemRequest.getQuantity()));


        PurchaseOrderUpdateRequest purchaseOrderUpdateRequest = new PurchaseOrderUpdateRequest();
        purchaseOrderUpdateRequest.setPurchaseOrderAction(PurchaseOrderAction.PROCESS);
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(purchaseOrderUpdateRequest)
                .patch("/procurement/purchaseOrder/{purchaseOrderPublicId}/actions", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("SUBMITTED"));

        PurchaseOrderApprovalRequest purchaseOrderApprovalRequest = new PurchaseOrderApprovalRequest();
        purchaseOrderApprovalRequest.setActionType(ActionType.APPROVE);
        purchaseOrderApprovalRequest.setComment("This is a test purchase order");
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(purchaseOrderApprovalRequest)
                .patch("/procurement/purchaseOrder/{purchaseOrderPublicId}/adminApprove", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("AMIN_APPROVED"));

        purchaseOrderApprovalRequest = new PurchaseOrderApprovalRequest();
        purchaseOrderApprovalRequest.setActionType(ActionType.APPROVE);
        purchaseOrderApprovalRequest.setAmountPaid(new BigDecimal(10));
        purchaseOrderApprovalRequest.setPaymentMethod(PaymentMethod.PrePaid);
        purchaseOrderApprovalRequest.setComment("This is a test purchase order");
        purchaseOrderApprovalRequest.setPaymentReference("UUUID20");
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(purchaseOrderApprovalRequest)
                .patch("/financial/purchaseOrder/{purchaseOrderPublicId}/approve", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("ACCOUNTANT_APPROVED"));

        String PONumber = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .extract().path("purchaseOrderNumber");

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("purchaseOrder", PONumber)
                .get("/financial/remittance")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1));

        purchaseOrderUpdateRequest.setPurchaseOrderAction(PurchaseOrderAction.CANCEL);
        purchaseOrderUpdateRequest.setReasonCodePublicId(UUID.fromString("632b6fda-e23c-4a5f-bec0-a0f9e1f0e293"));
        cancelPurchaseOrder(purchaseOrderUpdateRequest, POPublicId);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("purchaseOrder", PONumber)
                .get("/financial/remittance")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1));

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("PENDING_CANCEL"));

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("statuses", List.of("PENDING_CANCEL"))
                .get("/procurement/purchaseOrders")
                .then().log().all()
                .statusCode(200);

        purchaseOrderUpdateRequest.setPurchaseOrderAction(PurchaseOrderAction.CANCELLATION_DECLINED);
        purchaseOrderUpdateRequest.setCancelReason("Approved cancel test reason");
        cancelPurchaseOrder(purchaseOrderUpdateRequest, POPublicId);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then().log().all()
                .statusCode(200)
                .body("status", equalToIgnoringCase("ACCOUNTANT_APPROVED"));
    }

    @Test
    public void testCancelPurchaseOrderBeforePaymentWorks() throws IOException {
        String payload = productPayload("String description", UUID.randomUUID());
        String productPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(payload)
                .post("/products")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        PurchaseOrderRequest purchaseOrderRequest = new PurchaseOrderRequest();
        purchaseOrderRequest.setOrderDate(LocalDate.now());
        purchaseOrderRequest.setSupplierId(UUID.fromString("0bacd072-df86-4d05-bf86-27a1d38cacfb"));
        purchaseOrderRequest.setWarehouseId(UUID.fromString("3849b1be-a9dd-4f11-abbf-229ad2780a56"));
        purchaseOrderRequest.setProformaInvoiceNumber("23458");
        purchaseOrderRequest.setRebate(false);

        PurchaseOrderItemRequest purchaseOrderItemRequest = new PurchaseOrderItemRequest();
        purchaseOrderItemRequest.setProductPublicId(UUID.fromString(productPublicId));
        purchaseOrderItemRequest.setQuantity(4);
        purchaseOrderItemRequest.setUnitPrice(new BigDecimal(10));

        purchaseOrderRequest.setPurchaseOrderItems(List.of(purchaseOrderItemRequest));
        purchaseOrderRequest.setComment("This is a test purchase order");


        String POPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(purchaseOrderRequest)
                .post("/procurement/purchaseOrder")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then().log().all()
                .statusCode(200)
                .body("status", equalToIgnoringCase("PENDING"))
                .body("purchaseOrderItems[0].quantity", equalTo(purchaseOrderItemRequest.getQuantity()));


        PurchaseOrderUpdateRequest purchaseOrderUpdateRequest = new PurchaseOrderUpdateRequest();
        purchaseOrderUpdateRequest.setPurchaseOrderAction(PurchaseOrderAction.PROCESS);
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(purchaseOrderUpdateRequest)
                .patch("/procurement/purchaseOrder/{purchaseOrderPublicId}/actions", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("SUBMITTED"));

        PurchaseOrderApprovalRequest purchaseOrderApprovalRequest = new PurchaseOrderApprovalRequest();
        purchaseOrderApprovalRequest.setActionType(ActionType.APPROVE);
        purchaseOrderApprovalRequest.setComment("This is a test purchase order");
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(purchaseOrderApprovalRequest)
                .patch("/procurement/purchaseOrder/{purchaseOrderPublicId}/adminApprove", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("AMIN_APPROVED"));

        purchaseOrderUpdateRequest.setPurchaseOrderAction(PurchaseOrderAction.CANCEL);
        purchaseOrderUpdateRequest.setReasonCodePublicId(UUID.fromString("632b6fda-e23c-4a5f-bec0-a0f9e1f0e293"));
        cancelPurchaseOrder(purchaseOrderUpdateRequest, POPublicId);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then().log().all()
                .statusCode(200)
                .body("status", equalToIgnoringCase("CANCELLED"));
    }

    @Ignore
    @Test
    public void testRaiseCreditNotesJourney() {
        UUID purchaseOrderPublicId = UUID.fromString("a3495a45-9326-4806-8aa7-d7d9a7b34d67");

        CreditNoteRequest request = new CreditNoteRequest();
        request.setCreditNoteAmount(new BigDecimal(100));
        request.setDescription("Test credit note");
        request.setPurchaseOrderPublicId(purchaseOrderPublicId);
        request.setCreditNoteDate(LocalDate.now());
        request.setAction(CreditNoteOrderAction.CLOSE_PURCHASE_ORDER);

        String publicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .log().all()
                .post("/procurement/purchaseOrder/{purchaseOrderPublicId}/creditNote", purchaseOrderPublicId)
                .then().log().all()
                .statusCode(200)
                .body("note", equalTo(request.getDescription()))
                .body("status", equalTo(CreditNoteStatus.PENDING.name()))
                .body("purchaseOrderPublicId", equalTo(request.getPurchaseOrderPublicId().toString()))
                .body("actionApplied", equalTo(request.getAction().name()))
                .extract().path("publicId");


        // approve
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("action", CreditNoteApproveAction.APPROVE)
                .queryParam("comment", "This is a test approval")
                .patch("/procurement/creditNote/{creditNotePublicId}/approve", UUID.fromString(publicId))
                .then()
                .statusCode(200);


        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("fromDate", "2026-03-16")
                .queryParam("toDate", "2026-03-18")
                .queryParam("statuses", List.of("PENDING", "APPROVED"))
                .queryParam("tagged", false)
                .get("/procurement/creditNotes")
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].status", equalTo(CreditNoteStatus.APPROVED.name()))
                .body("content[0].publicId", equalTo(publicId));
    }

    @Ignore
    @Test
    public void testCancel_purchaseOrder_When_StatusDispatch(){
        UUID productPublicId = UUID.fromString("581c5c73-afc0-4b89-bafb-e160011f2715");
        UUID wareHousePublicId = UUID.fromString("3849b1be-a9dd-4f11-abbf-229ad2780a56");

        PurchaseOrderRequest purchaseOrderRequest = new PurchaseOrderRequest();
        purchaseOrderRequest.setOrderDate(LocalDate.now());
        purchaseOrderRequest.setSupplierId(UUID.fromString("0bacd072-df86-4d05-bf86-27a1d38cacfb"));
        purchaseOrderRequest.setWarehouseId(wareHousePublicId);
        purchaseOrderRequest.setProformaInvoiceNumber("23458");
        purchaseOrderRequest.setRebate(false);

        PurchaseOrderItemRequest purchaseOrderItemRequest = new PurchaseOrderItemRequest();
        purchaseOrderItemRequest.setProductPublicId(productPublicId);
        purchaseOrderItemRequest.setQuantity(1500);
        purchaseOrderItemRequest.setUnitPrice(new BigDecimal(10));

        purchaseOrderRequest.setPurchaseOrderItems(List.of(purchaseOrderItemRequest));
        purchaseOrderRequest.setComment("This is a test purchase order");


        String POPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(purchaseOrderRequest)
                .post("/procurement/purchaseOrder")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("purchaseOrderItems[0].quantity", equalTo(purchaseOrderItemRequest.getQuantity()));

        PurchaseOrderUpdateRequest purchaseOrderUpdateRequest = new PurchaseOrderUpdateRequest();
        purchaseOrderUpdateRequest.setPurchaseOrderAction(PurchaseOrderAction.PROCESS);
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(purchaseOrderUpdateRequest)
                .patch("/procurement/purchaseOrder/{purchaseOrderPublicId}/actions", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("SUBMITTED"));

        PurchaseOrderApprovalRequest purchaseOrderApprovalRequest = new PurchaseOrderApprovalRequest();
        purchaseOrderApprovalRequest.setActionType(ActionType.APPROVE);
        purchaseOrderApprovalRequest.setComment("This is a test purchase order");
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(purchaseOrderApprovalRequest)
                .patch("/procurement/purchaseOrder/{purchaseOrderPublicId}/adminApprove", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("AMIN_APPROVED"));

        purchaseOrderApprovalRequest = new PurchaseOrderApprovalRequest();
        purchaseOrderApprovalRequest.setActionType(ActionType.APPROVE);
        purchaseOrderApprovalRequest.setAmountPaid(new BigDecimal(10));
        purchaseOrderApprovalRequest.setPaymentMethod(PaymentMethod.PrePaid);
        purchaseOrderApprovalRequest.setComment("This is a test purchase order");
        purchaseOrderApprovalRequest.setPaymentReference("UUUID20");
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(purchaseOrderApprovalRequest)
                .patch("/financial/purchaseOrder/{purchaseOrderPublicId}/approve", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("ACCOUNTANT_APPROVED"));

        purchaseOrderUpdateRequest = new PurchaseOrderUpdateRequest();
        purchaseOrderUpdateRequest.setPurchaseOrderAction(PurchaseOrderAction.DISPATCH);
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(purchaseOrderUpdateRequest)
                .patch("/procurement/purchaseOrder/{purchaseOrderPublicId}/actions", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("DISPATCHED"))
                .extract().path("purchaseOrderNumber");

        purchaseOrderUpdateRequest.setPurchaseOrderAction(PurchaseOrderAction.CANCEL);
        purchaseOrderUpdateRequest.setReasonCodePublicId(UUID.fromString("632b6fda-e23c-4a5f-bec0-a0f9e1f0e293"));
        cancelPurchaseOrder(purchaseOrderUpdateRequest, POPublicId);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then().log().all()
                .statusCode(200)
                .body("status", equalToIgnoringCase("PENDING_CANCEL"));

        purchaseOrderUpdateRequest.setPurchaseOrderAction(PurchaseOrderAction.CANCELLATION_DECLINED);
        purchaseOrderUpdateRequest.setReasonCodePublicId(UUID.fromString("632b6fda-e23c-4a5f-bec0-a0f9e1f0e293"));
        cancelPurchaseOrder(purchaseOrderUpdateRequest, POPublicId);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then().log().all()
                .statusCode(200)
                .body("status", equalToIgnoringCase("DISPATCHED"));
    }

    @Test
    public void fetchReturnGoodsTestWorks() throws IOException {

        UUID productPublicId = UUID.fromString("581c5c73-afc0-4b89-bafb-e160011f2715");
        UUID wareHousePublicId = UUID.fromString("3849b1be-a9dd-4f11-abbf-229ad2780a56");

        PurchaseOrderRequest purchaseOrderRequest = new PurchaseOrderRequest();
        purchaseOrderRequest.setOrderDate(LocalDate.now());
        purchaseOrderRequest.setSupplierId(UUID.fromString("0bacd072-df86-4d05-bf86-27a1d38cacfb"));
        purchaseOrderRequest.setWarehouseId(wareHousePublicId);
        purchaseOrderRequest.setProformaInvoiceNumber("75000");
        purchaseOrderRequest.setRebate(false);

        PurchaseOrderItemRequest purchaseOrderItemRequest = new PurchaseOrderItemRequest();
        purchaseOrderItemRequest.setProductPublicId(productPublicId);
        purchaseOrderItemRequest.setQuantity(50);
        purchaseOrderItemRequest.setUnitPrice(new BigDecimal(200));

        purchaseOrderRequest.setPurchaseOrderItems(List.of(purchaseOrderItemRequest));
        purchaseOrderRequest.setComment("This is a test purchase order");


        String POPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(purchaseOrderRequest)
                .post("/procurement/purchaseOrder")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("purchaseOrderItems[0].quantity", equalTo(purchaseOrderItemRequest.getQuantity()));

        PurchaseOrderUpdateRequest purchaseOrderUpdateRequest = new PurchaseOrderUpdateRequest();
        purchaseOrderUpdateRequest.setPurchaseOrderAction(PurchaseOrderAction.PROCESS);
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(purchaseOrderUpdateRequest)
                .patch("/procurement/purchaseOrder/{purchaseOrderPublicId}/actions", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("SUBMITTED"));

        PurchaseOrderApprovalRequest purchaseOrderApprovalRequest = new PurchaseOrderApprovalRequest();
        purchaseOrderApprovalRequest.setActionType(ActionType.APPROVE);
        purchaseOrderApprovalRequest.setComment("This is a test purchase order");
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(purchaseOrderApprovalRequest)
                .patch("/procurement/purchaseOrder/{purchaseOrderPublicId}/adminApprove", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("AMIN_APPROVED"));

        purchaseOrderApprovalRequest = new PurchaseOrderApprovalRequest();
        purchaseOrderApprovalRequest.setActionType(ActionType.APPROVE);
        purchaseOrderApprovalRequest.setAmountPaid(new BigDecimal(10000));
        purchaseOrderApprovalRequest.setPaymentMethod(PaymentMethod.PrePaid);
        purchaseOrderApprovalRequest.setComment("This is a test purchase order");
        purchaseOrderApprovalRequest.setPaymentReference("UUUID20");
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(purchaseOrderApprovalRequest)
                .patch("/financial/purchaseOrder/{purchaseOrderPublicId}/approve", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("ACCOUNTANT_APPROVED"));

        purchaseOrderUpdateRequest = new PurchaseOrderUpdateRequest();
        purchaseOrderUpdateRequest.setPurchaseOrderAction(PurchaseOrderAction.DISPATCH);
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(purchaseOrderUpdateRequest)
                .patch("/procurement/purchaseOrder/{purchaseOrderPublicId}/actions", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        String purchaseOrderNumber = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("DISPATCHED"))
                .extract().path("purchaseOrderNumber");

        String purchaseOrderItemPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("searchParam", purchaseOrderNumber)
                .queryParam("statuses", Arrays.asList(PurchaseOrderStatus.PARTIAL_RECEIVED, PurchaseOrderStatus.DISPATCHED))
                .get("/procurement/purchaseOrders")
                .then()
                .statusCode(200)
                .extract().path("content[0].purchaseOrderItems[0].publicId");

        PurchaseOrderReceiveRequest purchaseOrderReceiveRequest = new PurchaseOrderReceiveRequest();
        purchaseOrderReceiveRequest.setDeliveryNoteNumber("12345");

        PurchaseOrderItemReceiveRequest purchaseOrderItemReceiveRequest = new PurchaseOrderItemReceiveRequest();
        purchaseOrderItemReceiveRequest.setOrderItemPublicId(UUID.fromString(purchaseOrderItemPublicId));
        purchaseOrderItemReceiveRequest.setQuantityReceived(50);

        purchaseOrderReceiveRequest.setPurchaseOrderItems(Arrays.asList(purchaseOrderItemReceiveRequest));
        given()
                .auth().oauth2(ADMIN_TOKEN)
                .contentType(ContentType.JSON)
                .body(purchaseOrderReceiveRequest)
                .patch("/procurement/{purchaseOrderPublicId}/receiveShipment", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        String shipmentReceivedNotePublicId = given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/goodsReturnNote/shipmentReceived")
                .then().log().all()
                .statusCode(200)
                .body("content[0].noteDate", notNullValue())
                .body("content[0].noteNumber", notNullValue())
                .body("content[0].supplier.supplierName", notNullValue())
                .body("content[0].status", equalToIgnoringCase("APPROVED"))
                .body("content[0].purchaseOrderNumber", equalToIgnoringCase(purchaseOrderNumber))
                .body("content[0].deliveryNoteNumber", notNullValue())
                .body("content[0].purchaseOrderStatus", equalToIgnoringCase("RECEIVED"))
                .extract().path("content[0].publicId");

        String itemReceivedPublicId = given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/goodsReturnNote/shipmentReceived/{goodsReceivedNotePublicId}/items", UUID.fromString(shipmentReceivedNotePublicId))
                .then().log().all()
                .statusCode(200)
                .body("productItemList[0].productName", equalToIgnoringCase("Biscuite 50G x 200"))
                .body("productItemList[0].productCode", equalToIgnoringCase("SKU100022"))
                .body("productItemList[0].productSellingPrice", equalTo(200.0F))
                .body("productItemList[0].productPublicId", equalToIgnoringCase(String.valueOf(productPublicId)))
                .body("productItemList[0].quantityReceived", equalTo(50))
                .body("productItemList[0].quantityRequested", equalTo(50))
                .extract().path("productItemList[0].publicId");

        GoodsReturnNoteRequest goodsReturnNoteRequest = new GoodsReturnNoteRequest();
        goodsReturnNoteRequest.setGoodsReturnDate(LocalDate.now());
        goodsReturnNoteRequest.setReturnNotePublicId(UUID.fromString(shipmentReceivedNotePublicId));

        GoodsNoteItemRequest goodsNoteItemRequest1 = new GoodsNoteItemRequest();
        goodsNoteItemRequest1.setItemPublicId(UUID.fromString(itemReceivedPublicId));
        goodsNoteItemRequest1.setQuantity(2);
        goodsNoteItemRequest1.setRejectionCodePublicId(UUID.fromString("632b6fda-e23c-4a5f-bec0-a0f9e1f0e293"));

        goodsReturnNoteRequest.setGoodsNoteItemRequests(List.of(goodsNoteItemRequest1));

        given()
                .auth().oauth2(ADMIN_TOKEN)
                .contentType(ContentType.JSON)
                .body(goodsReturnNoteRequest)
                .post("/goodsReturnNote/createGoodsReturnNotes")
                .then()
                .statusCode(200);

        String returnedNotePublicId = given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/goodsReturnNote/returnedGoodsNotes")
                .then()
                .statusCode(200)
                .body("content[0].replacementRaised", equalTo(true))
                .body("content[0].replacementPurchaseOrder", notNullValue())
                .extract().path("content[0].publicId");

        given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/goodsReturnNote/returnedGoodsNotes/{goodsReturnedNotePublicId}/items", returnedNotePublicId)
                .then().log().all()
                .statusCode(200)
                .extract().path("content[0].publicId");

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .post("/procurement/purchaseOrder/{goodReturnNotePublicId}/replacement", returnedNotePublicId)
                .then()
                .statusCode(400)
                .body("userMessage", equalToIgnoringCase("Replacement purchase order already raised for this goods return note"));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrders")
                .then().log().all()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", POPublicId)
                .then()
                .statusCode(200);
    }

    @Ignore
    @Test
    public void testRaiseRefundRequestOnDispatchedPO_Works() throws IOException {
        String payload = productPayload("Refund request test product", UUID.randomUUID());
        String productPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(payload)
                .post("/products")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        PurchaseOrderRequest purchaseOrderRequest = new PurchaseOrderRequest();
        purchaseOrderRequest.setOrderDate(LocalDate.now());
        purchaseOrderRequest.setSupplierId(UUID.fromString("0bacd072-df86-4d05-bf86-27a1d38cacfb"));
        purchaseOrderRequest.setWarehouseId(UUID.fromString("3849b1be-a9dd-4f11-abbf-229ad2780a56"));
        purchaseOrderRequest.setProformaInvoiceNumber("REFUND-TEST-001");
        purchaseOrderRequest.setRebate(false);

        PurchaseOrderItemRequest purchaseOrderItemRequest = new PurchaseOrderItemRequest();
        purchaseOrderItemRequest.setProductPublicId(UUID.fromString(productPublicId));
        purchaseOrderItemRequest.setQuantity(10);
        purchaseOrderItemRequest.setUnitPrice(new BigDecimal(500));

        purchaseOrderRequest.setPurchaseOrderItems(List.of(purchaseOrderItemRequest));
        purchaseOrderRequest.setComment("Refund request test purchase order");

        String POPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(purchaseOrderRequest)
                .post("/procurement/purchaseOrder")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        PurchaseOrderUpdateRequest purchaseOrderUpdateRequest = new PurchaseOrderUpdateRequest();
        purchaseOrderUpdateRequest.setPurchaseOrderAction(PurchaseOrderAction.PROCESS);
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(purchaseOrderUpdateRequest)
                .patch("/procurement/purchaseOrder/{purchaseOrderPublicId}/actions", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        PurchaseOrderApprovalRequest adminApproval = new PurchaseOrderApprovalRequest();
        adminApproval.setActionType(ActionType.APPROVE);
        adminApproval.setComment("Admin approval for refund test");
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(adminApproval)
                .patch("/procurement/purchaseOrder/{purchaseOrderPublicId}/adminApprove", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        PurchaseOrderApprovalRequest financeApproval = new PurchaseOrderApprovalRequest();
        financeApproval.setActionType(ActionType.APPROVE);
        financeApproval.setAmountPaid(new BigDecimal(5000));
        financeApproval.setPaymentMethod(PaymentMethod.PrePaid);
        financeApproval.setComment("Finance approval for refund test");
        financeApproval.setPaymentReference("REF-REFUND-001");
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(financeApproval)
                .patch("/financial/purchaseOrder/{purchaseOrderPublicId}/approve", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        purchaseOrderUpdateRequest = new PurchaseOrderUpdateRequest();
        purchaseOrderUpdateRequest.setPurchaseOrderAction(PurchaseOrderAction.DISPATCH);
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(purchaseOrderUpdateRequest)
                .patch("/procurement/purchaseOrder/{purchaseOrderPublicId}/actions", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("DISPATCHED"));

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .patch("/procurement/purchaseOrder/{purchaseOrderPublicId}/refund", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("REFUND_REQUESTED"));
    }

    private void cancelPurchaseOrder(PurchaseOrderUpdateRequest purchaseOrderUpdateRequest, String POPublicId) {
        purchaseOrderUpdateRequest.setCancelReason("bad data");

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .body(purchaseOrderUpdateRequest).log().all()
                .patch("/procurement/purchaseOrder/{purchaseOrderPublicId}/actions", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);
    }

    @Test
    public void testRevert_purchaseOrder_When_StatusDispatch(){
        final String accessToken = SUPER_ADMIN_TOKEN;
        UUID productPublicId = UUID.fromString("581c5c73-afc0-4b89-bafb-e160011f2715");
        UUID wareHousePublicId = UUID.fromString("3849b1be-a9dd-4f11-abbf-229ad2780a56");

        PurchaseOrderRequest purchaseOrderRequest = new PurchaseOrderRequest();
        purchaseOrderRequest.setOrderDate(LocalDate.now());
        purchaseOrderRequest.setSupplierId(UUID.fromString("0bacd072-df86-4d05-bf86-27a1d38cacfb"));
        purchaseOrderRequest.setWarehouseId(wareHousePublicId);
        purchaseOrderRequest.setProformaInvoiceNumber("23458");
        purchaseOrderRequest.setRebate(false);

        PurchaseOrderItemRequest purchaseOrderItemRequest = new PurchaseOrderItemRequest();
        purchaseOrderItemRequest.setProductPublicId(productPublicId);
        purchaseOrderItemRequest.setQuantity(1500);
        purchaseOrderItemRequest.setUnitPrice(new BigDecimal(10));

        purchaseOrderRequest.setPurchaseOrderItems(List.of(purchaseOrderItemRequest));
        purchaseOrderRequest.setComment("This is a test purchase order");

        String POPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(accessToken)
                .body(purchaseOrderRequest)
                .post("/procurement/purchaseOrder")
                .then().log().all()
                .statusCode(200)
                .extract().path("publicId");

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(accessToken)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then().log().all()
                .statusCode(200)
                .body("status", equalToIgnoringCase("PENDING"))
                .body("publicId", notNullValue())
                .body("purchaseOrderNumber", equalTo("PO-100000"))
                .body("purchaseOrderDate", equalTo(LocalDate.now().toString()))
                .body("notes", equalTo("This is a test purchase order"))
                .body("supplier.publicId", equalTo("0bacd072-df86-4d05-bf86-27a1d38cacfb"))
                .body("supplier.supplierName", equalTo("Africa Harmony Industry and Trade Limited_1"))
                .body("purchaseOrderItems[0].publicId", notNullValue())
                .body("purchaseOrderItems[0].quantity", equalTo(1500))
                .body("purchaseOrderItems[0].unitPrice", equalTo(10.0F))
                .body("purchaseOrderItems[0].product.productName", equalTo("Biscuite 50G x 200"))
                .body("purchaseOrderItems[0].product.category.name", equalTo("Fats & Oils"))
                .body("purchaseOrderItems[0].totalAmount", equalTo(15000.0000f))
                .body("purchaseOrderItems[0].taxAmount", equalTo(2295.00f))
                .body("purchaseOrderItems[0].amountAfterTax", equalTo(12705.0000f))
                .body("proformaInvoiceNumber", equalTo("23458"))
                .body("warehouseId", equalTo("3849b1be-a9dd-4f11-abbf-229ad2780a56"))
                .body("warehouseName", equalTo("Dar North"))
                .body("rebate", equalTo(false))
                .body("replacementPurchaseOrder", equalTo(false))
                .body("balance", equalTo(17295.0000f))
                .body("payableAmount", equalTo(17295.0000f))
                .body("totalAmountAfterTax", equalTo(12705.0000f))
                .body("totalTaxAmount", equalTo(2295.0000f))
                .body("resolved", equalTo(true))
                .body("supplierName", equalTo("Africa Harmony Industry and Trade Limited_1"))
                .body("totalAmountReceived", equalTo(0.0000f));

        PurchaseOrderUpdateRequest purchaseOrderUpdateRequest = new PurchaseOrderUpdateRequest();
        purchaseOrderUpdateRequest.setPurchaseOrderAction(PurchaseOrderAction.PROCESS);
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(accessToken)
                .body(purchaseOrderUpdateRequest)
                .patch("/procurement/purchaseOrder/{purchaseOrderPublicId}/actions", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(accessToken)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("SUBMITTED"));

        PurchaseOrderApprovalRequest purchaseOrderApprovalRequest = new PurchaseOrderApprovalRequest();
        purchaseOrderApprovalRequest.setActionType(ActionType.APPROVE);
        purchaseOrderApprovalRequest.setComment("This is a test purchase order");
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(accessToken)
                .body(purchaseOrderApprovalRequest)
                .patch("/procurement/purchaseOrder/{purchaseOrderPublicId}/adminApprove", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(accessToken)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("AMIN_APPROVED"));

        purchaseOrderApprovalRequest = new PurchaseOrderApprovalRequest();
        purchaseOrderApprovalRequest.setActionType(ActionType.APPROVE);
        purchaseOrderApprovalRequest.setAmountPaid(new BigDecimal(10));
        purchaseOrderApprovalRequest.setPaymentMethod(PaymentMethod.PrePaid);
        purchaseOrderApprovalRequest.setComment("This is a test purchase order");
        purchaseOrderApprovalRequest.setPaymentReference("UUUID20");
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(accessToken)
                .body(purchaseOrderApprovalRequest)
                .patch("/financial/purchaseOrder/{purchaseOrderPublicId}/approve", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(accessToken)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("ACCOUNTANT_APPROVED"));

        purchaseOrderUpdateRequest = new PurchaseOrderUpdateRequest();
        purchaseOrderUpdateRequest.setPurchaseOrderAction(PurchaseOrderAction.DISPATCH);
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(accessToken)
                .body(purchaseOrderUpdateRequest)
                .patch("/procurement/purchaseOrder/{purchaseOrderPublicId}/actions", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(accessToken)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("DISPATCHED"))
                .extract().path("purchaseOrderNumber");

        purchaseOrderUpdateRequest.setPurchaseOrderAction(PurchaseOrderAction.REVERT);
        purchaseOrderUpdateRequest.setReasonCodePublicId(UUID.fromString("632b6fda-e23c-4a5f-bec0-a0f9e1f0e293"));
        cancelPurchaseOrder(purchaseOrderUpdateRequest, POPublicId);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(accessToken)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then().log().all()
                .statusCode(200)
                .body("status", equalToIgnoringCase("PENDING"))
                .body("publicId", notNullValue())
                .body("purchaseOrderNumber", equalTo("PO-100000"))
                .body("purchaseOrderDate", equalTo(LocalDate.now().toString()))
                .body("notes", equalTo("This is a test purchase order"))
                .body("supplier.publicId", equalTo("0bacd072-df86-4d05-bf86-27a1d38cacfb"))
                .body("supplier.supplierName", equalTo("Africa Harmony Industry and Trade Limited_1"))
                .body("purchaseOrderItems[0].publicId", notNullValue())
                .body("purchaseOrderItems[0].quantity", equalTo(1500))
                .body("purchaseOrderItems[0].unitPrice", equalTo(10.0F))
                .body("purchaseOrderItems[0].product.productName", equalTo("Biscuite 50G x 200"))
                .body("purchaseOrderItems[0].product.category.name", equalTo("Fats & Oils"))
                .body("purchaseOrderItems[0].totalAmount", equalTo(15000.0000f))
                .body("purchaseOrderItems[0].taxAmount", equalTo(2295.00f))
                .body("purchaseOrderItems[0].amountAfterTax", equalTo(12705.0000f))
                .body("proformaInvoiceNumber", equalTo("23458"))
                .body("warehouseId", equalTo("3849b1be-a9dd-4f11-abbf-229ad2780a56"))
                .body("warehouseName", equalTo("Dar North"))
                .body("rebate", equalTo(false))
                .body("replacementPurchaseOrder", equalTo(false))
                .body("payableAmount", equalTo(17295.0000f))
                .body("totalAmountAfterTax", equalTo(12705.0000f))
                .body("totalTaxAmount", equalTo(2295.0000f))
                .body("resolved", equalTo(true))
                .body("supplierName", equalTo("Africa Harmony Industry and Trade Limited_1"))
                .body("totalAmountReceived", equalTo(0.0000f));
    }
}
