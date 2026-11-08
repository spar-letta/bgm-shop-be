package com.app.itibari.web;

import com.app.itibari.ItibariApplicationTests;
import com.app.itibari.dataTypes.*;
import com.app.itibari.models.request.*;
import com.app.itibari.scheduler.Scheduler;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Slf4j
public class SalesAgentsControllerTests extends ItibariApplicationTests {

    @Autowired
    private Scheduler scheduler;

    @Test
    public void testWorks() {

        SalesAgentsRequest salesAgentsRequest = SalesAgentsRequest.builder()
                .name("Agent One")
                .email("agentEmail@gmail.com")
                .phoneNumber("+255777777778")
                .organizationRegionId(UUID.fromString("8c5d4db0-f9a1-47ed-a160-56780e4e4d14"))
                .salesAgentManagerPersonId(UUID.fromString("a3397780-f434-4fa2-8041-32ade6ecb425"))
                .identificationType(IdentificationType.NATIONAL_ID)
                .locationPublicIds(Collections.singletonList(UUID.fromString("92a00f88-9e01-4c0d-8e1c-1f675e181f91")))
                .identificationNumber(UUID.randomUUID().toString())
                .build();

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(salesAgentsRequest)
                .post("/salesAgents")
                .then()
                .statusCode(200)
                .body("salesAgentManager.fullName", equalToIgnoringCase("Manager Kimambo"));

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesAgents")
                .then().log().all()
                .statusCode(200);

    }

    @Test
    public void tesFetchSalesAgents() {
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesAgents")
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));
    }

    @Test
    @Ignore
    public void testSalesAgentCreateOrderWorks() {
        SalesOrderItemsRequest orderItems = SalesOrderItemsRequest
                .builder()
                .productId(UUID.fromString("08ebb084-2633-428f-82b9-e8d98a22ec53"))
                .quantity(5)
                .build();
        SalesOrdersRequest request = SalesOrdersRequest.builder()
                .customerId(UUID.fromString("d7c5bf34-fda1-46e2-b5ee-40db98aed06d"))
                .bookedBy("Itibari Test")
                .sourceChannel(SourceChannelEnum.SALES_AGENTS_APP.name())
                .salesOrderItems(Collections.singleton(orderItems))
                .bookedBySalesAgent(true)
                .salesAgentPublicId(UUID.fromString("e388bdc8-5de4-41b3-91f5-35ef26c51200"))
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(SALES_AGENT_TOKEN)
                .header("X-App-Version", "2.0.0")
                .body(request)
                .post("/salesOrders/customer/placeOrder")
                .then().log().all()
                .statusCode(200)
                .body("bookedBySalesAgent", equalTo(true))
                .body("salesAgentPublicId", equalTo("e388bdc8-5de4-41b3-91f5-35ef26c51200"));
    }

    @Ignore
    @Test
    public void testMapSalesAgentToCustomer() throws IOException {
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


        List<String> customersIds = testCreateCustomerSelfRegistered();
        Set<UUID> collectedIds = customersIds.stream().map(id -> UUID.fromString(id)).collect(Collectors.toSet());

        AgentAction agentAction = new AgentAction(AgentAction.ActionType.Map_Customer, collectedIds,
                null);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(agentAction).log().all()
                .patch("/salesAgents/{salesAgentPublicId}/actions", UUID.fromString(salesAgentPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("servedBy", UUID.fromString(salesAgentPublicId))
                .get("/customers")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(2));

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesAgents/{salesAgentPublicId}", UUID.fromString(salesAgentPublicId))
                .then().log().all()
                .statusCode(200);

        String salesAgentPublicId2 = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("organisationRegionPublicId", UUID.fromString("ae9df8ce-6ad0-4829-8082-95fb4c491e95"))
                .queryParam("organisationLocationPublicId", UUID.fromString("0e0b9c9c-3dfd-43c5-8be7-60a28e1ef2a0"))
                .get("/salesAgents")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .extract().path("content[0].publicId");

        Response response = given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/reports/salesAgentWithCustomers")
                .then()
                .statusCode(200)
                .extract().response();

        saveExport(response, "SalesAgent_Customers.xlsx");


        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("servedBy", UUID.fromString(salesAgentPublicId2))
                .get("/customers")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(0));


        agentAction = new AgentAction(AgentAction.ActionType.De_Activate_Agent, Collections.EMPTY_SET,
                UUID.fromString(salesAgentPublicId2));

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(agentAction).log().all()
                .patch("/salesAgents/{salesAgentPublicId}/actions", UUID.fromString(salesAgentPublicId))
                .then()
                .statusCode(200);

        // confirm salesAgentPublicId agent hana customers
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("servedBy", UUID.fromString(salesAgentPublicId))
                .get("/customers")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(0));

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesAgents/{salesAgentPublicId}", salesAgentPublicId)
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("servedBy", UUID.fromString(salesAgentPublicId2))
                .get("/customers")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(2));
    }

    @Test
    public void testMapSalesAgentToCustomer3() {
        String salesAgentPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
//                .queryParam("organisationRegionPublicId", UUID.fromString("ae9df8ce-6ad0-4829-8082-95fb4c491e95"))
                .queryParam("organisationLocationPublicId", UUID.fromString("8946aa9d-396a-41f6-881e-8819386ec1b3"))
                .get("/salesAgents")
                .then().log().all()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .extract().path("content[0].publicId");
    }

    @Test
    @Ignore  // ignoring because it sends real sms
    public void testRaiseADiscountRequest() {

        UUID customerId = UUID.fromString("d7c5bf34-fda1-46e2-b5ee-40db98aed06d");
        UUID productId = UUID.fromString("08ebb084-2633-428f-82b9-e8d98a22ec53");

        SpecialRequest request = new SpecialRequest();
        RequestItemDTO item = new RequestItemDTO();
        item.setProductPublicId(productId);
        item.setProductName("Test Product");
        item.setSku("TESTSKU001");
        item.setQuantity(10);
        item.setUnitPrice(new BigDecimal("100.00"));
        item.setTotalDiscountRequest(new BigDecimal("15.00"));
        request.setItems(Collections.singletonList(item));

        request.setCustomerId(customerId);


        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(SALES_AGENT_TOKEN)
                .header("X-App-Version", "2.0.0")
                .body(request)
                .patch("/salesOrders/requestSpecialDiscount")
                .then()
                .statusCode(200);


        // fetch discounts approval

        String discountPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/discounts/discountApprovals")
                .then()
                .statusCode(200)
                .body("content.find { it.requestedBy == 'Erick Massawe' && it.customer == 'Eliamani Kimambo' }.approvalStatus", equalTo(DiscountApprovalStatus.Pending.name()))
                .body("content.find { it.requestedBy == 'Erick Massawe' && it.customer == 'Eliamani Kimambo' }.requestItems[0].productName", equalTo("Test Product"))
                .body("content.find { it.requestedBy == 'Erick Massawe' && it.customer == 'Eliamani Kimambo' }.requestItems[0].quantity", equalTo(10))
                .body("content.find { it.requestedBy == 'Erick Massawe' && it.customer == 'Eliamani Kimambo' }.requestItems[0].unitPrice", equalTo(100.00f))
                .body("content.find { it.requestedBy == 'Erick Massawe' && it.customer == 'Eliamani Kimambo' }.requestItems[0].discountRequest", equalTo(15.00f))
                .body("content.find { it.requestedBy == 'Erick Massawe' && it.customer == 'Eliamani Kimambo' }.totalDiscountRequested", equalTo(150.00f))
                .extract().path("content.find { it.requestedBy == 'Erick Massawe' && it.customer == 'Eliamani Kimambo' }.publicId");


        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("comment", "First approval by admin")
                .queryParam("approve", true)
                .post("/discounts/{publicId}/firstApproval", UUID.fromString(discountPublicId))
                .then()
                .statusCode(200);


        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/discounts/discountApprovals")
                .then()
                .statusCode(200)
                .body("content.find { it.requestedBy == 'Erick Massawe' && it.customer == 'Eliamani Kimambo' }.approvalStatus", equalTo(DiscountApprovalStatus.FirstLevelApproved.name()));


        // final approval


        ApprovalRequest approvalRequest = new ApprovalRequest();

        approvalRequest.setAction(ApprovalRequest.ApprovalAction.APPROVE);
        approvalRequest.setComment("Final approval by admin");
        approvalRequest.setHasSpecialDiscount(true);
        approvalRequest.setAmountType(AmountType.Fixed);
        approvalRequest.setAmount(new BigDecimal("15.00"));


        ApprovedProductItems approvedProductItems = new ApprovedProductItems();
        approvedProductItems.setProductId(productId.toString());
        approvedProductItems.setApproved(true);
        approvedProductItems.setApprovedAmount(new BigDecimal("15.00"));

        approvalRequest.setProductItems(Collections.singletonList(approvedProductItems));
        approvalRequest.setStartDate(LocalDate.now());
        approvalRequest.setEndDate(LocalDate.now().plusMonths(1));


        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(approvalRequest)
                .post("/discounts/{publicId}/finalApproval", UUID.fromString(discountPublicId))
                .then()
                .statusCode(200);


        // fetch approved discount

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("discountType", DiscountType.SpecialDiscount)
                .get("/discounts")
                .then()
                .statusCode(200)
                .body("content.find { it.customer == 'Eliamani Kimambo' }.status", equalTo(DiscountStatus.Active.name()))
                .body("content.find { it.customer == 'Eliamani Kimambo' }.amount", equalTo(150.00f))
                .body("content.find { it.customer == 'Eliamani Kimambo' }.specialItems[0].productName", equalTo("Test Product"))
                .body("content.find { it.customer == 'Eliamani Kimambo' }.specialItems[0].quantityRequested", equalTo(10))
                .body("content.find { it.customer == 'Eliamani Kimambo' }.specialItems[0].amountApprovedPerQuantity", equalTo(15.00f));


        // fetch discount approval to get otp


        /**
         *  here for otp to work,and view on it in discountApproval entity and makes sure to remove otp_used check in
         *  DiscountServiceImpl->finalApproval method  for testing purpose only
         */
        String otpCode = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/discounts/discountApprovals")
                .then()
                .statusCode(200)
                .body("content.find { it.requestedBy == 'Erick Massawe' && it.customer == 'Eliamani Kimambo' }.approvalStatus", equalTo(DiscountApprovalStatus.Approved.name()))
                .extract().path("content.find { it.requestedBy == 'Erick Massawe' && it.customer == 'Eliamani Kimambo' }.otp");

        String agent = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/discounts/discountApprovals")
                .then().log().all()
                .statusCode(200)
                .body("content.find { it.requestedBy == 'Erick Massawe' && it.customer == 'Eliamani Kimambo' }.approvalStatus", equalTo(DiscountApprovalStatus.Approved.name()))
                .extract().path("content.find { it.requestedBy == 'Erick Massawe' && it.customer == 'Eliamani Kimambo' }.requestedByPublicId");


        //  we will create sales order to check if discount is applied

        SalesOrderItemsRequest orderItems = SalesOrderItemsRequest
                .builder()
                .productId(productId)
                .quantity(5)
                .build();
        SalesOrdersRequest salesOrdersRequest = SalesOrdersRequest.builder()
                .customerId(customerId)
                .bookedBy("Itibari Test")
                .sourceChannel(SourceChannelEnum.SALES_AGENTS_APP.name())
                .salesOrderItems(Collections.singleton(orderItems))
                .bookedBySalesAgent(true)
                .otp(otpCode)
                .salesAgentPublicId(UUID.fromString(agent))
                .build();

        String orderPublicId = given()
                .contentType("application/json")
                .auth().oauth2(SALES_AGENT_TOKEN)
                .header("X-App-Version", "2.0.0")
                .body(salesOrdersRequest)
                .post("/salesOrders/customer/placeOrder")
                .then()
                .statusCode(200)
                .extract().path("publicId");


        // fetch order

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders/{publicId}", UUID.fromString(orderPublicId))
                .then().log().all()
                .statusCode(200)
                .body("customer.publicId", equalTo("d7c5bf34-fda1-46e2-b5ee-40db98aed06d"))
                .body("salesOrderItems.size()", is(1))
                .body("totalAmount", equalTo(265000.00f))
                .body("totalDiscount", equalTo(75.00f)) // 15 * 5  = 75
                .body("totalAmountPayable", equalTo(264925.00f));  // 265000 - 75 = 264925



        String orderItemPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders/{salesOrderPublicId}/items", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].quantity", equalTo(5))
                .extract().path("content[0].publicId");


        //recalculate order disc
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .patch("/salesOrders/recalculateDiscounts/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200);


        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders/{publicId}", UUID.fromString(orderPublicId))
                .then().log().all()
                .statusCode(200)
                .body("customer.publicId", equalTo("d7c5bf34-fda1-46e2-b5ee-40db98aed06d"))
                .body("salesOrderItems.size()", is(1))
                .body("totalAmount", equalTo(265000.00f))
                .body("totalDiscount", equalTo(75.00f)) // 15 * 5  = 75
                .body("totalAmountPayable", equalTo(264925.00f));  // 265000 - 75 = 264925

        scheduler.runOrderPlanning();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200)
                .body("salesOrderStatus", equalTo(LoadingStatusEnum.PLANNING.name()));



        RoutePlanningRequest routePlanningRequest = new RoutePlanningRequest();
        routePlanningRequest.setLocationPublicId(UUID.fromString("90ad01f1-4a13-41f7-b825-c428b41f35d0"));
        routePlanningRequest.setRegionPublicId(UUID.fromString("8c5d4db0-f9a1-47ed-a160-56780e4e4d14"));
        routePlanningRequest.setVehiclePublicId(UUID.fromString("fa4bf2ee-6665-493a-8dbd-5d56dc8a7d93"));
        routePlanningRequest.setDeliveryAgentPublicId(UUID.fromString("af485765-69c5-4dd0-8fa0-560058b85c65"));
        routePlanningRequest.setWarehousePublicId(UUID.fromString("3849b1be-a9dd-4f11-abbf-229ad2780a56"));
        routePlanningRequest.setSalesOrdersPublicIds(Collections.singletonList(UUID.fromString(orderPublicId)));


        String routePublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(routePlanningRequest)
                .post("/logistics/routePlanning")
                .then()
                .statusCode(200)
                .extract().path("publicId");


        String deliveryId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/logistics/routePlanning/{routePlanningPublicId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .extract().path("routeId");

        String productItemPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/logistics/routePlanning/{routePlanningPublicId}/loadingProducts", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].productItemPublicId");



        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/logistics/routePlanning/{routePlanningPublicId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .body("routeStatus", equalTo(RouteStatus.CREATED.name()));

        LoadingRequest loadingRequest = new LoadingRequest();
        LoadingDTO loadingDTO = new LoadingDTO();
        loadingDTO.setLoadingType(LoadingType.OrderSale);
        loadingDTO.setProductItemPublicId(UUID.fromString(productItemPublicId));
        loadingRequest.setLoadingDTOList(Collections.singletonList(loadingDTO));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(loadingRequest)
                .post("warehouse/agent/{agentPublicId}/loading", UUID.fromString("af485765-69c5-4dd0-8fa0-560058b85c65"))
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/logistics/routePlanning/{routePlanningPublicId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .body("routeStatus", equalTo(RouteStatus.Loaded.name()));



        given()
                .contentType("application/json")
                .auth().oauth2(DELIVERY_AGENT_TOKEN)
                .header("X-App-Version", "1.0.0")
                .queryParam("acceptType", AcceptTypeEnum.AcceptRoute)
                .patch("/deliveryAgents/accept/{routeId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200);



        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/logistics/routePlanning/{routePlanningPublicId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .body("routeStatus", equalTo(RouteStatus.ReadyToGo.name()));

        given()
                .contentType("application/json")
                .auth().oauth2(DELIVERY_AGENT_TOKEN)
                .header("X-App-Version", "1.0.0")
                .queryParam("routeId", UUID.fromString(routePublicId))
                .queryParam("action", RouteAction.Start)
                .patch("/deliveryAgents/routeActions")
                .then()
                .statusCode(200);


        String productReconId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/warehouse/productRecons")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].publicId");


        String productReconItemId =   given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/warehouse/productRecons/{publicId}/items", UUID.fromString(productReconId))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].publicId");


        // partial deliver products

        DeliverItemRequest deliverItemRequest = new DeliverItemRequest();
        deliverItemRequest.setSalesOrderPublicId(UUID.fromString(orderPublicId));
        deliverItemRequest.setRoutePublicId(UUID.fromString(routePublicId));

        ItemRequest itemRequest = new ItemRequest();
        itemRequest.setProductItemPublicId(UUID.fromString(orderItemPublicId));
        itemRequest.setProductReconItemPublicId( UUID.fromString(orderItemPublicId));
        itemRequest.setQuantitySold(3);
        deliverItemRequest.setItems(Collections.singletonList(itemRequest));


        given()
                .contentType("application/json")
                .auth().oauth2(DELIVERY_AGENT_TOKEN)
                .header("X-App-Version", "1.0.0")
                .body(deliverItemRequest)
                .patch("/deliveryAgents/markOrderItemAsDelivered")
                .then()
                .statusCode(200);

        // mark order as delivered

        given()
                .contentType("application/json")
                .auth().oauth2(DELIVERY_AGENT_TOKEN)
                .header("X-App-Version", "1.0.0")
                .patch("/deliveryAgents/markOrderAsDelivered/{orderPublicId}",UUID.fromString(orderPublicId))
                .then()
                .statusCode(200);


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("productPublicId",productId)
                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
                .then()
                .statusCode(200)
                .body("content[0].availableStock", equalTo(45))
                .body("content[0].reservedStock", equalTo(0))
                .body("content[0].stockValue", equalTo(1620000F))
                .body("content[0].totalStock", equalTo(45));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("productPublicId", productId)
                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].product.availableStock", equalTo(10));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/products/{publicId}", productId)
                .then()
                .statusCode(200)
                .body("availableStock", equalTo(10));

        // fetch order check

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200)
                .body("salesOrderStatus", equalTo(LoadingStatusEnum.DELIVERED.name()));

        given()
                .contentType("application/json")
                .auth().oauth2(DELIVERY_AGENT_TOKEN)
                .header("X-App-Version", "1.0.0")
                .queryParam("routeId", UUID.fromString(routePublicId))
                .queryParam("action", RouteAction.end)
                .patch("/deliveryAgents/routeActions")
                .then()
                .statusCode(200);

        // fetch stock movement records

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/warehouse/stockMovements")
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));
    }

    @Ignore
    @Test
    public void testCreate_Products_SalesAgent(){
            UUID productId = UUID.fromString("7c4a6d52-5da2-47fe-92a8-ea2fce2602af");

            SalesOrderItemsRequest orderItems = SalesOrderItemsRequest
                    .builder()
                    .productId(productId)
                    .quantity(3)
                    .build();
            SalesOrdersRequest request = SalesOrdersRequest.builder()
                    .customerId(UUID.fromString("d7c5bf34-fda1-46e2-b5ee-40db98aed06d"))
                    .bookedBy("Itibari Test")
                    .sourceChannel(SourceChannelEnum.BACK_OFFICE_APP.name())
                    .salesOrderItems(Collections.singleton(orderItems))
                    .build();

            String orderPublicId = given()
                    .contentType("application/json")
                    .auth().oauth2(ADMIN_TOKEN)
                    .body(request)
                    .post("/salesOrders")
                    .then()
                    .statusCode(200)
                    .extract().path("publicId");

            String orderItemPublicId = given()
                    .contentType("application/json")
                    .auth().oauth2(ADMIN_TOKEN)
                    .get("/salesOrders/{salesOrderPublicId}/items", UUID.fromString(orderPublicId))
                    .then().log().all()
                    .statusCode(200)
                    .body("content.size()", greaterThanOrEqualTo(1))
                    .extract().path("content[0].publicId");

            checkInventoryNegativeTests();

            scheduler.runOrderPlanning();

            given()
                    .contentType("application/json")
                    .auth().oauth2(ADMIN_TOKEN)
                    .body(request)
                    .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                    .then()
                    .statusCode(200)
                    .body("salesOrderStatus", equalTo(LoadingStatusEnum.ORDER_PLACED.name()));

            checkInventoryNegativeTests();

            RoutePlanningRequest routePlanningRequest = new RoutePlanningRequest();
            routePlanningRequest.setLocationPublicId(UUID.fromString("90ad01f1-4a13-41f7-b825-c428b41f35d0"));
            routePlanningRequest.setRegionPublicId(UUID.fromString("8c5d4db0-f9a1-47ed-a160-56780e4e4d14"));
            routePlanningRequest.setVehiclePublicId(UUID.fromString("fa4bf2ee-6665-493a-8dbd-5d56dc8a7d93"));
            routePlanningRequest.setDeliveryAgentPublicId(UUID.fromString("af485765-69c5-4dd0-8fa0-560058b85c65"));
            routePlanningRequest.setWarehousePublicId(UUID.fromString("3849b1be-a9dd-4f11-abbf-229ad2780a56"));
            routePlanningRequest.setSalesOrdersPublicIds(Collections.singletonList(UUID.fromString(orderPublicId)));

            String routePublicId = given()
                    .contentType("application/json")
                    .auth().oauth2(ADMIN_TOKEN)
                    .body(routePlanningRequest)
                    .post("/logistics/routePlanning")
                    .then()
                    .statusCode(200)
                    .extract().path("publicId");

            String deliveryId = given()
                    .contentType("application/json")
                    .auth().oauth2(ADMIN_TOKEN)
                    .get("/logistics/routePlanning/{routePlanningPublicId}", UUID.fromString(routePublicId))
                    .then()
                    .statusCode(200)
                    .extract().path("routeId");
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders")
                .then().log().all()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(SALES_AGENT_TOKEN)
                .get("/salesAgents/salesAgentOderSummary")
                .then().log().all()
                .statusCode(200);
    }

    @Test
    public void test_getSalesAgentMinifiedData_ok(){
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("searchParam", "fire")
                .get("/salesAgents/minifiedData")
                .then().log().all()
                .statusCode(200);
    }

//    @Test
//    public void testSupplier(){
//
//        given()
//                .contentType("application/json")
//                .auth().oauth2(SUPPLIER_USER_TOKEN)
//                .get("/reports/purchaseReport")
//                .then().log().all()
//                .statusCode(200);
//            String productItemPublicId = given()
//                    .contentType("application/json")
//                    .auth().oauth2(ADMIN_TOKEN)
//                    .get("/logistics/routePlanning/{routePlanningPublicId}/loadingProducts", UUID.fromString(routePublicId))
//                    .then()
//                    .statusCode(200)
//                    .body("content.size()", greaterThanOrEqualTo(1))
//                    .extract().path("content[0].productItemPublicId");
//
//            checkInventoryNegativeTests();
//
//            given()
//                    .contentType("application/json")
//                    .auth().oauth2(ADMIN_TOKEN)
//                    .get("/logistics/routePlanning/{routePlanningPublicId}", UUID.fromString(routePublicId))
//                    .then()
//                    .statusCode(200)
//                    .body("routeStatus", equalTo(RouteStatus.CREATED.name()));
//
//            LoadingRequest loadingRequest = new LoadingRequest();
//            LoadingDTO loadingDTO = new LoadingDTO();
//            loadingDTO.setLoadingType(LoadingType.OrderSale);
//            loadingDTO.setProductItemPublicId(UUID.fromString(productItemPublicId));
//            loadingRequest.setLoadingDTOList(Collections.singletonList(loadingDTO));
//
//            given()
//                    .contentType("application/json")
//                    .auth().oauth2(ADMIN_TOKEN)
//                    .body(loadingRequest)
//                    .post("warehouse/agent/{agentPublicId}/loading", UUID.fromString("af485765-69c5-4dd0-8fa0-560058b85c65"))
//                    .then()
//                    .statusCode(200);
//
//            given()
//                    .contentType("application/json")
//                    .auth().oauth2(ADMIN_TOKEN)
//                    .get("/logistics/routePlanning/{routePlanningPublicId}", UUID.fromString(routePublicId))
//                    .then()
//                    .statusCode(200)
//                    .body("routeStatus", equalTo(RouteStatus.Loaded.name()));
//
//            checkInventoryNegativeTests();
//
//            given()
//                    .contentType("application/json")
//                    .auth().oauth2(DELIVERY_AGENT_TOKEN)
//                    .header("X-App-Version", "1.0.0")
//                    .queryParam("acceptType", AcceptTypeEnum.AcceptRoute)
//                    .patch("/deliveryAgents/accept/{routeId}", UUID.fromString(routePublicId))
//                    .then()
//                    .statusCode(200);
//
//            checkInventoryNegativeTests();
//
//            given()
//                    .contentType("application/json")
//                    .auth().oauth2(ADMIN_TOKEN)
//                    .get("/logistics/routePlanning/{routePlanningPublicId}", UUID.fromString(routePublicId))
//                    .then()
//                    .statusCode(200)
//                    .body("routeStatus", equalTo(RouteStatus.ReadyToGo.name()));
//
//            given()
//                    .contentType("application/json")
//                    .auth().oauth2(DELIVERY_AGENT_TOKEN)
//                    .header("X-App-Version", "1.0.0")
//                    .queryParam("routeId", UUID.fromString(routePublicId))
//                    .queryParam("action", RouteAction.Start)
//                    .patch("/deliveryAgents/routeActions")
//                    .then()
//                    .statusCode(200);
//
//            String productReconId = given()
//                    .contentType("application/json")
//                    .auth().oauth2(ADMIN_TOKEN)
//                    .get("/warehouse/productRecons")
//                    .then()
//                    .statusCode(200)
//                    .body("content.size()", greaterThanOrEqualTo(1))
//                    .extract().path("content[0].publicId");
//
//            String productReconItemId = given()
//                    .contentType("application/json")
//                    .auth().oauth2(ADMIN_TOKEN)
//                    .get("/warehouse/productRecons/{publicId}/items", UUID.fromString(productReconId))
//                    .then()
//                    .statusCode(200)
//                    .body("content.size()", greaterThanOrEqualTo(1))
//                    .extract().path("content[0].publicId");
//
//            checkInventoryNegativeTests();
//
//            // partial deliver products
//
//            DeliverItemRequest deliverItemRequest = new DeliverItemRequest();
//            deliverItemRequest.setSalesOrderPublicId(UUID.fromString(orderPublicId));
//            deliverItemRequest.setRoutePublicId(UUID.fromString(routePublicId));
//
//            ItemRequest itemRequest = new ItemRequest();
//            itemRequest.setProductItemPublicId(UUID.fromString(orderItemPublicId));
//            itemRequest.setProductReconItemPublicId(UUID.fromString(orderItemPublicId));
//            itemRequest.setQuantitySold(2);
//            deliverItemRequest.setItems(Collections.singletonList(itemRequest));
//
//            given()
//                    .contentType("application/json")
//                    .auth().oauth2(DELIVERY_AGENT_TOKEN)
//                    .header("X-App-Version", "1.0.0")
//                    .body(deliverItemRequest)
//                    .patch("/deliveryAgents/markOrderItemAsDelivered")
//                    .then()
//                    .statusCode(200);
//
//            // mark order as delivered
//
//            given()
//                    .contentType("application/json")
//                    .auth().oauth2(DELIVERY_AGENT_TOKEN)
//                    .header("X-App-Version", "1.0.0")
//                    .patch("/deliveryAgents/markOrderAsDelivered/{orderPublicId}", UUID.fromString(orderPublicId))
//                    .then()
//                    .statusCode(200);
//
//            checkInventoryNegativeTests();
//
//            // fetch order check
//
//            given()
//                    .contentType("application/json")
//                    .auth().oauth2(ADMIN_TOKEN)
//                    .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
//                    .then().log().all()
//                    .statusCode(200)
//                    .body("salesOrderStatus", equalTo(LoadingStatusEnum.DELIVERED.name()));
//    }
//
//    @Test
//    public void testCreate_Products_SalesAgent_2(){
//        given()
//                .contentType("application/json")
//                .auth().oauth2(ADMIN_TOKEN)
//                .get("/salesOrders")
//                .then().log().all()
//                .statusCode(200);
////                .body("salesOrderStatus", equalTo(LoadingStatusEnum.DELIVERED.name()));
//    }
//
    private void checkInventoryNegativeTests() {

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].availableStock", greaterThanOrEqualTo(0));
    }
}
