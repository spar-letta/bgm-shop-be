package com.app.itibari.web;

import com.app.itibari.ItibariApplicationTests;
import com.app.itibari.dataTypes.*;
import com.app.itibari.models.entities.DeliveryAgents;
import com.app.itibari.models.entities.ProductItem;
import com.app.itibari.models.entities.ProductRecon;
import com.app.itibari.models.entities.SalesOrderItems;
import com.app.itibari.models.request.*;
import com.app.itibari.models.response.ProductItemDTO;
import com.app.itibari.repository.DeliveryAgentsRepository;
import com.app.itibari.repository.SalesOrdersRepository;
import com.app.itibari.scheduler.Scheduler;
import com.app.itibari.services.SalesOrdersService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Slf4j
public class SalesOrdersControllerTests extends ItibariApplicationTests {

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private SalesOrdersRepository salesOrdersRepository;

    @Autowired
    private DeliveryAgentsRepository deliveryAgentsRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String MOBILE_PASSWORD = "password123";

    ObjectMapper mapper = new ObjectMapper();
    @Autowired
    private SalesOrdersService salesOrdersService;

    private static final UUID KISSKIDS_PRODUCT_ID = UUID.fromString("7c4a6d52-5da2-47fe-92a8-ea2fce2602af");

    @Before
    public void resetSharedFixtures() {
        clearActiveVolumeDiscountForProduct(KISSKIDS_PRODUCT_ID);
        resetSeededDeliveryAgentAvailability();
    }

    public String testDeliveryAgentLoginViaPassword() {
        String phoneNumber = "+254712345678";
        DeliveryAgents deliveryAgent = deliveryAgentsRepository.findDeliveryAgentsByPhoneNumberAndDeletedIsFalse(phoneNumber)
                .orElseThrow();
        deliveryAgent.setPassword(passwordEncoder.encode(MOBILE_PASSWORD));
        deliveryAgent.setForcePasswordReset(false);
        deliveryAgentsRepository.save(deliveryAgent);

        MobileLoginRequest mobileLoginRequest = new MobileLoginRequest();
        mobileLoginRequest.setPhoneNumber(phoneNumber);
        mobileLoginRequest.setPassword(MOBILE_PASSWORD);
        mobileLoginRequest.setMobileUserType(MobileUserType.DeliveryAgent);
        mobileLoginRequest.setDeviceId("sales-orders-test-device");
        mobileLoginRequest.setDeviceName("Sales Orders Test Device");
        mobileLoginRequest.setPlatform("TEST");

        return given()
                .contentType("application/json")
                .body(mobileLoginRequest)
                .log().all()
                .post("/authentication/mobilePlatform/login")
                .then().log().all()
                .statusCode(200)
                .body("action", equalTo("Logged in"))
                .body("phoneNumber", equalTo(phoneNumber))
                .body("token", notNullValue())
                .body("username", equalTo("Erick Massawe"))
                .body("forcePasswordReset", equalTo(false))
                .extract().path("token");
    }

    @Test
    public void testCreateSalesOrderFails() {

        SalesOrdersRequest request = SalesOrdersRequest.builder().build();
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .post("/salesOrders")
                .then().log().all()
                .statusCode(400);
    }

    @Test
    public void testFetchSalesOrders() {

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("salesOrderNumber", "ORD10001")
                .get("/salesOrders")
                .then().log().all()
                .statusCode(200);
    }

    @Ignore // works alone
    @Test
    public void testCreateOrderWorks() {

        SalesOrderItemsRequest orderItems = SalesOrderItemsRequest
                .builder()
                .productId(UUID.fromString("08ebb084-2633-428f-82b9-e8d98a22ec53"))
                .quantity(5)
                .build();
        SalesOrdersRequest request = SalesOrdersRequest.builder()
                .customerId(UUID.fromString("d7c5bf34-fda1-46e2-b5ee-40db98aed06d"))
                .bookedBy("Itibari Test")
                .sourceChannel(SourceChannelEnum.BACK_OFFICE_APP.name())
                .salesOrderItems(Collections.singleton(orderItems))
                .build();

        String publicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .post("/salesOrders")
                .then().log().all()
                .statusCode(200)
                .extract().path("publicId");


        // transition to planning

        scheduler.runOrderPlanning();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(publicId))
                .then()
                .statusCode(200)
                .body("salesOrderStatus", equalTo(LoadingStatusEnum.PLANNING.name()));


    }

    @Test
//    @Ignore  // works alone
    public void testCreateOrderAndCheckDiscountWorksAllThrough() throws IOException {
        String deliveryAgentToken = testDeliveryAgentLoginViaPassword();
        UUID productId = UUID.fromString("7c4a6d52-5da2-47fe-92a8-ea2fce2602af");
        DiscountCreateRequest discountCreateRequest = new DiscountCreateRequest();
        discountCreateRequest.setDiscountType(DiscountType.VolumeDiscount);
        discountCreateRequest.setProductIds(Collections.singletonList(productId));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(discountCreateRequest)
                .log().all()
                .post("/discounts")
                .then()
                .statusCode(200);

        String publicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("discountType", DiscountType.VolumeDiscount)
                .get("/discounts")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].status", equalTo(DiscountStatus.Incomplete.name()))
                .extract().path("content[0].publicId");

        VolumeRangeCreateRequest volumeRangeCreateRequest = new VolumeRangeCreateRequest();
        volumeRangeCreateRequest.setMinVolume(2);
        volumeRangeCreateRequest.setMaxVolume(10);
        volumeRangeCreateRequest.setDiscountAmount(new BigDecimal(786));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(volumeRangeCreateRequest)
                .log().all()
                .patch("/discounts/{discountPublicId}/addVolumeRange", UUID.fromString(publicId))
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/discounts/{discountPublicId}/volumeRanges", UUID.fromString(publicId))
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("discountType", DiscountType.VolumeDiscount)
                .get("/discounts")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].status", equalTo(DiscountStatus.Pending.name()));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .patch("/discounts/{publicId}/approve", UUID.fromString(publicId))
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("discountType", DiscountType.VolumeDiscount)
                .get("/discounts")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].status", equalTo(DiscountStatus.Active.name()));

        CartDiscountRequest cartDiscountRequest = new CartDiscountRequest();
        CartRequestItem cartRequestItem = new CartRequestItem();
        cartRequestItem.setProductPublicId(productId);
        cartRequestItem.setQuantity(5);

        cartDiscountRequest.setCartRequestItems(Collections.singletonList(cartRequestItem));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(cartDiscountRequest)
                .log().all()
                .post("/salesOrders/cartDiscountCheck")
                .then()
                .statusCode(200);

        SalesOrderItemsRequest orderItems = SalesOrderItemsRequest
                .builder()
                .productId(productId)
                .quantity(5)
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

        log.info("---------check 1----------------");
        String orderItemPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders/{salesOrderPublicId}/items", UUID.fromString(orderPublicId))
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].discount", notNullValue())
                .body("content[0].hasDiscount", equalTo(true))
                .extract().path("content[0].publicId");

//        checkInventoryNegativeTests();

        scheduler.runOrderPlanning();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200)
                .body("salesOrderStatus", equalTo(LoadingStatusEnum.PLANNING.name()));

//        checkInventoryNegativeTests();

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

//        checkInventoryNegativeTests();

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

//        checkInventoryNegativeTests();

        given()
                .contentType("application/json")
                .auth().oauth2(deliveryAgentToken)
                .header("X-App-Version", "1.0.0")
                .queryParam("acceptType", AcceptTypeEnum.AcceptRoute)
                .patch("/deliveryAgents/accept/{routeId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200);

//        checkInventoryNegativeTests();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/logistics/routePlanning/{routePlanningPublicId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .body("routeStatus", equalTo(RouteStatus.ReadyToGo.name()));

        given()
                .contentType("application/json")
                .auth().oauth2(deliveryAgentToken)
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

        String productReconItemId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/warehouse/productRecons/{publicId}/items", UUID.fromString(productReconId))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].publicId");

//        checkInventoryNegativeTests();

        // partial deliver products

        DeliverItemRequest deliverItemRequest = new DeliverItemRequest();
        deliverItemRequest.setSalesOrderPublicId(UUID.fromString(orderPublicId));
        deliverItemRequest.setRoutePublicId(UUID.fromString(routePublicId));

        ItemRequest itemRequest = new ItemRequest();
        itemRequest.setProductItemPublicId(UUID.fromString(orderItemPublicId));
        itemRequest.setProductReconItemPublicId(UUID.fromString(orderItemPublicId));
        itemRequest.setQuantitySold(3);
        deliverItemRequest.setItems(Collections.singletonList(itemRequest));

        given()
                .contentType("application/json")
                .auth().oauth2(deliveryAgentToken)
                .header("X-App-Version", "1.0.0")
                .body(deliverItemRequest)
                .patch("/deliveryAgents/markOrderItemAsDelivered")
                .then()
                .statusCode(200);

        // mark order as delivered

        given()
                .contentType("application/json")
                .auth().oauth2(deliveryAgentToken)
                .header("X-App-Version", "1.0.0")
                .patch("/deliveryAgents/markOrderAsDelivered/{orderPublicId}", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200);

//        checkInventoryNegativeTests();

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
                .auth().oauth2(deliveryAgentToken)
                .header("X-App-Version", "1.0.0")
                .queryParam("routeId", UUID.fromString(routePublicId))
                .queryParam("action", RouteAction.end)
                .patch("/deliveryAgents/routeActions")
                .then()
                .statusCode(200);

        // get recon

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("deliveryId", deliveryId)
                .get("/reconciliation")
                .then()
                .statusCode(200)
                .body("content[0].totalDiscount", notNullValue());

        // return goods

        ProductItemRequest productItemRequest = new ProductItemRequest();
        productItemRequest.setProductItemPublicId(UUID.fromString(productReconItemId));
        productItemRequest.setQuantityReturned(2);

        ProductReconAction productReconAction = new ProductReconAction();
        productReconAction.setProductReconPublicId(UUID.fromString(productReconId));
        productReconAction.setProductItems(Collections.singletonList(productItemRequest));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(productReconAction)
                .patch("/warehouse/productRecons/{publicId}", UUID.fromString(productReconId))
                .then()
                .statusCode(200);

//        checkInventoryNegativeTests();

        //reconcile

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .patch("/warehouse/productRecons/{publicId}/reconcile", UUID.fromString(productReconId))
                .then()
                .statusCode(200);


        // get sales order and check status

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200)
                .body("salesOrderStatus", equalTo(LoadingStatusEnum.DELIVERED.name()));


        // sales order item check

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders/{salesOrderPublicId}/items", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));


        // fetch file for receipt

//        Response response = given()
//                .auth().oauth2(ADMIN_TOKEN)
//                .queryParam("orderType", OrderType.CREDIT)
//                .get("/reports/{salesOrderPublic}/customerInvoiceReceipt", UUID.fromString(orderPublicId))
//                .then()
//                .statusCode(200)
//                .extract().response();
//
//        saveExport(response, "receipt.pdf");
    }

    @Ignore
    @Test
    public void testCreateOrderWithSAFullJourney() {

        UUID productId = UUID.fromString("7c4a6d52-5da2-47fe-92a8-ea2fce2602af");

        SalesOrderItemsRequest orderItems = SalesOrderItemsRequest
                .builder()
                .productId(productId)
                .quantity(5)
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
                .body("salesOrderStatus", equalTo(LoadingStatusEnum.PLANNING.name()));

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

        String productItemPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/logistics/routePlanning/{routePlanningPublicId}/loadingProducts", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].productItemPublicId");

        checkInventoryNegativeTests();

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

        checkInventoryNegativeTests();

        given()
                .contentType("application/json")
                .auth().oauth2(DELIVERY_AGENT_TOKEN)
                .header("X-App-Version", "1.0.0")
                .queryParam("acceptType", AcceptTypeEnum.AcceptRoute)
                .patch("/deliveryAgents/accept/{routeId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200);

        checkInventoryNegativeTests();

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

        String productReconItemId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/warehouse/productRecons/{publicId}/items", UUID.fromString(productReconId))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].publicId");

        checkInventoryNegativeTests();

        // partial deliver products

        DeliverItemRequest deliverItemRequest = new DeliverItemRequest();
        deliverItemRequest.setSalesOrderPublicId(UUID.fromString(orderPublicId));
        deliverItemRequest.setRoutePublicId(UUID.fromString(routePublicId));

        ItemRequest itemRequest = new ItemRequest();
        itemRequest.setProductItemPublicId(UUID.fromString(orderItemPublicId));
        itemRequest.setProductReconItemPublicId(UUID.fromString(orderItemPublicId));
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
                .patch("/deliveryAgents/markOrderAsDelivered/{orderPublicId}", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200);

        checkInventoryNegativeTests();

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
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders/salesOrderAgent")
                .then().log().all()
                .statusCode(200);
//
//        given()
//                .contentType("application/json")
//                .auth().oauth2(DELIVERY_AGENT_TOKEN)
//                .header("X-App-Version", "1.0.0")
//                .queryParam("routeId", UUID.fromString(routePublicId))
//                .queryParam("action", RouteAction.end)
//                .patch("/deliveryAgents/routeActions")
//                .then()
//                .statusCode(200);
//
//        // get recon
//
//        given()
//                .contentType("application/json")
//                .auth().oauth2(ADMIN_TOKEN)
//                .queryParam("deliveryId", deliveryId)
//                .get("/reconciliation")
//                .then()
//                .statusCode(200)
//                .body("content[0].totalDiscount", notNullValue());
//
//        // return goods
//
//        ProductItemRequest productItemRequest = new ProductItemRequest();
//        productItemRequest.setProductItemPublicId(UUID.fromString(productReconItemId));
//        productItemRequest.setQuantityReturned(2);
//
//        ProductReconAction productReconAction = new ProductReconAction();
//        productReconAction.setProductReconPublicId(UUID.fromString(productReconId));
//        productReconAction.setProductItems(Collections.singletonList(productItemRequest));
//
//        given()
//                .contentType("application/json")
//                .auth().oauth2(ADMIN_TOKEN)
//                .body(productReconAction)
//                .patch("/warehouse/productRecons/{publicId}", UUID.fromString(productReconId))
//                .then()
//                .statusCode(200);
//
//        checkInventoryNegativeTests();
//
//        //reconcile
//
//        given()
//                .contentType("application/json")
//                .auth().oauth2(ADMIN_TOKEN)
//                .patch("/warehouse/productRecons/{publicId}/reconcile", UUID.fromString(productReconId))
//                .then()
//                .statusCode(200);
//
//
//        // get sales order and check status
//
//        given()
//                .contentType("application/json")
//                .auth().oauth2(ADMIN_TOKEN)
//                .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
//                .then()
//                .statusCode(200)
//                .body("salesOrderStatus", equalTo(LoadingStatusEnum.DELIVERED.name()));
//
//
//        // sales order item check
//
//        given()
//                .contentType("application/json")
//                .auth().oauth2(ADMIN_TOKEN)
//                .get("/salesOrders/{salesOrderPublicId}/items", UUID.fromString(orderPublicId))
//                .then().log().all()
//                .statusCode(200)
//                .body("content.size()", greaterThanOrEqualTo(1));

    }

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


    @Test
    @Ignore
    public void testCancelOrder() {

        SalesOrderItemsRequest orderItems = SalesOrderItemsRequest
                .builder()
                .productId(UUID.fromString("08ebb084-2633-428f-82b9-e8d98a22ec53"))
                .quantity(5)
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
                .then().log().all()
                .statusCode(200)
                .extract().path("publicId");

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .patch("/salesOrders/{salesOrderPublicId}/cancel", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200);


        // get sales order and check status

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200)
                .body("salesOrderStatus", equalTo(LoadingStatusEnum.CANCELLED.name()))
                .body("paymentStatus", equalTo(PaymentStatusEnum.NOT_PAID.name()));


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("isCustomerInitiated", Boolean.TRUE)
                .patch("/salesOrders/{salesOrderPublicId}/cancel", UUID.fromString(orderPublicId))
                .then()
                .statusCode(400);
    }

    @Test
    @Ignore
    public void testPriceChangesOnAlreadyCreatedOrder() {

        SalesOrderItemsRequest orderItems = SalesOrderItemsRequest
                .builder()
                .productId(UUID.fromString("08ebb084-2633-428f-82b9-e8d98a22ec53"))
                .quantity(5)
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
                .then().log().all()
                .statusCode(200)
                .extract().path("publicId");


        // change product selling price

        PriceManagementRequestItem item = new PriceManagementRequestItem();
        item.setProductId(UUID.fromString("08ebb084-2633-428f-82b9-e8d98a22ec53"));
        item.setPrice(BigDecimal.valueOf(50000.00));
        item.setSku("SKU100020");
        item.setProductName("Bwana Sukari Brown Sugar 5KG x 4");

        PriceManagementRequest priceRequest = new PriceManagementRequest();
        priceRequest.setItems(Collections.singletonList(item));


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(priceRequest)
                .post("/priceManagement")
                .then().log().all()
                .statusCode(200);


        // fetch product and check new price is reflected on order
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/products/{productPublicId}", UUID.fromString("08ebb084-2633-428f-82b9-e8d98a22ec53"))
                .then().log().all()
                .statusCode(200);


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                .then().log().all()
                .statusCode(200);


        // do price change on product
    }

    @Ignore
    @Test
    public void testCustomerEditSalesOrder() {
        String tokenFound = tokenGen();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("productPublicId", UUID.fromString("534cf5d4-f8c3-495c-a813-9a42f338c466"))
                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
                .then()
                .statusCode(200)
                .body("content[0].availableStock", equalTo(50))
                .body("content[0].reservedStock", equalTo(0))
                .body("content[0].totalStock", equalTo(50));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].availableStock", greaterThanOrEqualTo(0));

        UUID productId = UUID.fromString("534cf5d4-f8c3-495c-a813-9a42f338c466");
        UUID productId2 = UUID.fromString("08ebb084-2633-428f-82b9-e8d98a22ec53");

        SalesOrderItemsRequest orderItems = SalesOrderItemsRequest
                .builder()
                .productId(productId)
                .quantity(3)
                .build();
        SalesOrderItemsRequest orderItems2 = SalesOrderItemsRequest
                .builder()
                .productId(productId2)
                .quantity(2)
                .build();
        SalesOrdersRequest request = SalesOrdersRequest.builder()
                .customerId(UUID.fromString("d7c5bf34-fda1-46e2-b5ee-40db98aed06d"))
                .bookedBy("Itibari Test")
                .sourceChannel(SourceChannelEnum.CUSTOMER_CARE_APP.name())
                .salesOrderItems(Arrays.asList(orderItems, orderItems2).stream().collect(Collectors.toSet()))
                .build();

        int expectedQuantity = orderItems.getQuantity();
        double expectedPricePerUnit = 36000.00;
        double expectedTotalAmount = expectedQuantity * expectedPricePerUnit;

        String orderPublicId =
                given()
                        .contentType("application/json")
                        .auth().oauth2(tokenFound)
                        .body(request)
                        .post("/salesOrders/customer/placeOrder")
                        .then().log().all()
                        .statusCode(200)
//                .body("salesOrderItems[0].quantity", equalTo(orderItems.getQuantity()))
//                .body("salesOrderItems[0].totalAmount", equalTo((float)expectedTotalAmount))
                        .extract().path("publicId");


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("productPublicId", UUID.fromString("534cf5d4-f8c3-495c-a813-9a42f338c466"))
                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
                .then().log().all()
                .statusCode(200)
                .body("content[0].availableStock", equalTo(47))
                .body("content[0].reservedStock", equalTo(3))
                .body("content[0].totalStock", equalTo(50));
//

        SalesOrderItemsRequest previousOrderItem = SalesOrderItemsRequest
                .builder()
                .productId(productId)
                .quantity(8)
                .build();
        Set<SalesOrderItemsRequest> salesOrderItemsRequests = new HashSet<>();
        salesOrderItemsRequests.add(previousOrderItem);

        request = SalesOrdersRequest.builder()
                .customerId(UUID.fromString("d7c5bf34-fda1-46e2-b5ee-40db98aed06d"))
                .bookedBy("Chris Test")
                .sourceChannel(SourceChannelEnum.CUSTOMER_CARE_APP.name())
                .salesOrderItems(salesOrderItemsRequests)
                .build();

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(tokenFound)
                .queryParam("isCustomerInitiated", Boolean.TRUE)
                .body(request).log().all()
                .patch("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200);

        int expectedQuantity2 = previousOrderItem.getQuantity();
        expectedTotalAmount = expectedQuantity2 * expectedPricePerUnit;

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                .then().log().all()
                .statusCode(200)
                .body("salesOrderItems[0].quantity", equalTo(previousOrderItem.getQuantity()))
                .body("salesOrderItems[0].totalAmount", equalTo((float) expectedTotalAmount));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("productPublicId", UUID.fromString("534cf5d4-f8c3-495c-a813-9a42f338c466"))
                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
                .then().log().all()
                .statusCode(200)
                .body("content[0].availableStock", equalTo(42))
                .body("content[0].reservedStock", equalTo(8))
                .body("content[0].totalStock", equalTo(50));

    }

    @Test
    public void testCreatePrintOutFromOrderWithSAFullJourney() throws IOException {

        UUID productId = UUID.fromString("7c4a6d52-5da2-47fe-92a8-ea2fce2602af");

        SalesOrderItemsRequest orderItems = SalesOrderItemsRequest
                .builder()
                .productId(productId)
                .quantity(5)
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

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders")
                .then().log().all()
                .statusCode(200);

        Response response = given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("exportType", StatementExportType.EXCEL)
                .get("/reports/salesOrdersReport")
                .then()
                .statusCode(200)
                .extract().response();

//        saveExport(response, "order_print_out.xlsx");
//        saveExport(response, "order_print_out.PDF");
    }

    @Test
    @Ignore
    public void testSalesAgentEditSalesOrder() {

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("productPublicId", UUID.fromString("534cf5d4-f8c3-495c-a813-9a42f338c466"))
                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
                .then()
                .statusCode(200)
                .body("content[0].availableStock", equalTo(42))
                .body("content[0].reservedStock", equalTo(8))
                .body("content[0].totalStock", equalTo(50));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].availableStock", greaterThanOrEqualTo(0));

        UUID productId = UUID.fromString("534cf5d4-f8c3-495c-a813-9a42f338c466");
        UUID productId2 = UUID.fromString("08ebb084-2633-428f-82b9-e8d98a22ec53");

        SalesOrderItemsRequest orderItems = SalesOrderItemsRequest
                .builder()
                .productId(productId)
                .quantity(3)
                .build();
        SalesOrderItemsRequest orderItems2 = SalesOrderItemsRequest
                .builder()
                .productId(productId2)
                .quantity(2)
                .build();
        SalesOrdersRequest request = SalesOrdersRequest.builder()
                .customerId(UUID.fromString("d7c5bf34-fda1-46e2-b5ee-40db98aed06d"))
                .bookedBy("Itibari Test")
                .sourceChannel(SourceChannelEnum.SALES_AGENTS_APP.name())
                .salesOrderItems(Arrays.asList(orderItems, orderItems2).stream().collect(Collectors.toSet()))
                .build();

        int expectedQuantity = orderItems.getQuantity();
        double expectedPricePerUnit = 36000.00;
        double expectedTotalAmount = expectedQuantity * expectedPricePerUnit;

        String orderPublicId =
                given()
                        .contentType("application/json")
                        .auth().oauth2(SALES_AGENT_TOKEN)
                        .header("X-App-Version", "2.0.0")
                        .body(request)
                        .post("/salesOrders/customer/placeOrder")
                        .then().log().all()
                        .statusCode(200)
//                .body("salesOrderItems[0].quantity", equalTo(orderItems.getQuantity()))
//                .body("salesOrderItems[0].totalAmount", equalTo((float)expectedTotalAmount))
                        .extract().path("publicId");


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("productPublicId", UUID.fromString("534cf5d4-f8c3-495c-a813-9a42f338c466"))
                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
                .then().log().all()
                .statusCode(200)
                .body("content[0].availableStock", equalTo(47))
                .body("content[0].reservedStock", equalTo(3))
                .body("content[0].totalStock", equalTo(50));
//

        SalesOrderItemsRequest previousOrderItem = SalesOrderItemsRequest
                .builder()
                .productId(productId)
                .quantity(8)
                .build();
        Set<SalesOrderItemsRequest> salesOrderItemsRequests = new HashSet<>();
        salesOrderItemsRequests.add(previousOrderItem);

        request = SalesOrdersRequest.builder()
                .customerId(UUID.fromString("d7c5bf34-fda1-46e2-b5ee-40db98aed06d"))
                .bookedBy("Chris Test")
                .sourceChannel(SourceChannelEnum.CUSTOMER_CARE_APP.name())
                .salesOrderItems(salesOrderItemsRequests)
                .build();

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(SALES_AGENT_TOKEN)
                .queryParam("isSalesAgentInitiated", Boolean.TRUE)
                .body(request).log().all()
                .patch("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200);

        int expectedQuantity2 = previousOrderItem.getQuantity();
        expectedTotalAmount = expectedQuantity2 * expectedPricePerUnit;

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                .then().log().all()
                .statusCode(200)
                .body("salesOrderItems[0].quantity", equalTo(previousOrderItem.getQuantity()))
                .body("salesOrderItems[0].totalAmount", equalTo((float) expectedTotalAmount));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("productPublicId", UUID.fromString("534cf5d4-f8c3-495c-a813-9a42f338c466"))
                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
                .then().log().all()
                .statusCode(200)
                .body("content[0].availableStock", equalTo(42))
                .body("content[0].reservedStock", equalTo(8))
                .body("content[0].totalStock", equalTo(50));

    }


    // update sales order test

    @Test
    public void testUpdateSalesOrder() {

        UUID productId = UUID.fromString("7c4a6d52-5da2-47fe-92a8-ea2fce2602af");

        SalesOrderItemsRequest orderItems = SalesOrderItemsRequest
                .builder()
                .productId(productId)
                .quantity(5)
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

        // update sales order

        SalesOrderItemsRequest request1 = SalesOrderItemsRequest
                .builder()
                .productId(productId)
                .quantity(3)
                .build();

        SalesOrdersRequest updateRequest = SalesOrdersRequest.builder()
                .customerId(UUID.fromString("d7c5bf34-fda1-46e2-b5ee-40db98aed06d"))
                .bookedBy("Itibari Test Updated")
                .sourceChannel(SourceChannelEnum.BACK_OFFICE_APP.name())
                .salesOrderItems(Collections.singleton(request1))
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(updateRequest)
                .patch("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200);


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
    public void testCreateOrderJourney() throws IOException, InterruptedException {
        String deliveryAgentToken = testDeliveryAgentLoginViaPassword();
        UUID productId = UUID.fromString("7c4a6d52-5da2-47fe-92a8-ea2fce2602af");

        SalesOrderItemsRequest orderItems = SalesOrderItemsRequest
                .builder()
                .productId(productId)
                .quantity(5)
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

        scheduler.runOrderPlanning();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200)
                .body("salesOrderStatus", equalTo(LoadingStatusEnum.PLANNING.name()));

        salesOrdersService.notifyCustomerWithSmsOrderInPlanningAndTransit();

        try {
            Thread.sleep(1000);
        } catch (Exception e) {
        }


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
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("deliveryId", deliveryId)
                .get("/warehouse/agent/{agentPublicId}/items", UUID.fromString("af485765-69c5-4dd0-8fa0-560058b85c65"))
                .then().log().all()
                .statusCode(200);

        Response response = given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("exportType", StatementExportType.PDF)
                .get("/reports/issuanceReport/{routePublicId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .extract().response();

        saveExport(response, "issuance_items.pdf");


        given()
                .contentType("application/json")
                .auth().oauth2(deliveryAgentToken)
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
                .auth().oauth2(deliveryAgentToken)
                .header("X-App-Version", "1.0.0")
                .queryParam("routeId", UUID.fromString(routePublicId))
                .queryParam("action", RouteAction.Start)
                .patch("/deliveryAgents/routeActions")
                .then()
                .statusCode(200);

        salesOrdersService.notifyCustomerWithSmsOrderInPlanningAndTransit();

        try {
            Thread.sleep(1000);
        } catch (Exception e) {
        }

        String productReconId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/warehouse/productRecons")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].publicId");

        String productReconItemId = given()
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
        itemRequest.setProductReconItemPublicId(UUID.fromString(orderItemPublicId));
        itemRequest.setQuantitySold(3);
        deliverItemRequest.setItems(Collections.singletonList(itemRequest));

        given()
                .contentType("application/json")
                .auth().oauth2(deliveryAgentToken)
                .header("X-App-Version", "1.0.0")
                .body(deliverItemRequest)
                .patch("/deliveryAgents/markOrderItemAsDelivered")
                .then()
                .statusCode(200);

        // mark order as delivered

        given()
                .contentType("application/json")
                .auth().oauth2(deliveryAgentToken)
                .header("X-App-Version", "1.0.0")
                .patch("/deliveryAgents/markOrderAsDelivered/{orderPublicId}", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200);

        // fetch order check

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                .then().log().all()
                .statusCode(200)
                .body("salesOrderStatus", equalTo(LoadingStatusEnum.DELIVERED.name()));
    }

    @Ignore
    @Test
    public void testProductPerformanceByDateRange() throws IOException {

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders")
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("startDate", LocalDate.now().minusDays(15).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .queryParam("endDate", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .queryParam("locationId", UUID.fromString("90ad01f1-4a13-41f7-b825-c428b41f35d0"))
                .queryParam("productId", UUID.fromString("581c5c73-afc0-4b89-bafb-e160011f2715"))
                .get("/reports/productPerformanceDateRangeByZone")
                .then().log().all()
                .statusCode(200)
                .body("size()", equalTo(1));
    }

    //    @Ignore
    @Test
    public void testProductPerformanceByZone() throws IOException {

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders")
                .then().log().all()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("startDate", LocalDate.now().minusDays(28).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .queryParam("endDate", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .queryParam("regionId", UUID.fromString("8c5d4db0-f9a1-47ed-a160-56780e4e4d14"))
                .queryParam("productId", UUID.fromString("7c4a6d52-5da2-47fe-92a8-ea2fce2602af"))
                .get("/reports/productPerformance")
                .then().log().all()
                .statusCode(200);
//                .body("size()", equalTo(1));
    }

    @Ignore
    @Test
    public void testOrderValuePerZone() throws IOException {

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders")
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(SUPPLIER_USER_TOKEN)
                .queryParam("startDate", LocalDate.now().minusDays(28).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .queryParam("endDate", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
//                .queryParam("regionId", UUID.fromString("8c5d4db0-f9a1-47ed-a160-56780e4e4d14"))
//                .queryParam("productId", UUID.fromString("581c5c73-afc0-4b89-bafb-e160011f2715"))
                .get("/reports/orderValuePerZone")
                .then().log().all()
                .statusCode(200);

    }

    @Ignore
    @Test
    public void testSupplierTotalOrders() throws IOException {

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders")
                .then().log().all()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(SUPPLIER_USER_TOKEN)
                .queryParam("startDate", LocalDate.now().minusDays(30).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .queryParam("endDate", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .queryParam("regionId", UUID.fromString("8c5d4db0-f9a1-47ed-a160-56780e4e4d14"))
                .get("/reports/supplierTotalOrders")
                .then().log().all()
                .statusCode(200);
    }

    @Ignore
    @Test
    public void testSumQuantitySoldOrders() throws IOException {

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders")
                .then().log().all()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(SUPPLIER_USER_TOKEN)
                .queryParam("startDate", LocalDate.now().minusDays(15).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .queryParam("endDate", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .queryParam("regionId", UUID.fromString("8c5d4db0-f9a1-47ed-a160-56780e4e4d14"))
                .get("/reports/supplierTotalQuantitySold")
                .then().log().all()
                .statusCode(200);
    }

    @Ignore
    @Test
    public void testSumSupplierActiveCustomers() throws IOException {

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders")
                .then().log().all()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(SUPPLIER_USER_TOKEN)
                .queryParam("startDate", LocalDate.now().minusDays(15).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .queryParam("endDate", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .queryParam("regionId", UUID.fromString("8c5d4db0-f9a1-47ed-a160-56780e4e4d14"))
                .get("/reports/totalActiveCustomers")
                .then().log().all()
                .statusCode(200);
    }

//    @Ignore
    @Test
    public void testEnforce_Completion_of_All_Deliveries_or_Amendments_Before_Delivery_Agent_Can_End_Trip() throws IOException {
        String deliveryAgentToken = testDeliveryAgentLoginViaPassword();
        String adminToken = SUPER_ADMIN_TOKEN;

        UUID productId = UUID.fromString("7c4a6d52-5da2-47fe-92a8-ea2fce2602af");
        DiscountCreateRequest discountCreateRequest = new DiscountCreateRequest();
        discountCreateRequest.setDiscountType(DiscountType.VolumeDiscount);
        discountCreateRequest.setProductIds(Collections.singletonList(productId));
        UUID reasonId = UUID.fromString("632b6fda-e23c-4a5f-bec0-a0f9e1f0e293");
        UUID deliveryAgentId = UUID.fromString("af485765-69c5-4dd0-8fa0-560058b85c65");

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .body(discountCreateRequest)
                .log().all()
                .post("/discounts")
                .then()
                .statusCode(200);

        String publicId = given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .queryParam("discountType", DiscountType.VolumeDiscount)
                .get("/discounts")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].status", equalTo(DiscountStatus.Incomplete.name()))
                .extract().path("content[0].publicId");

        VolumeRangeCreateRequest volumeRangeCreateRequest = new VolumeRangeCreateRequest();
        volumeRangeCreateRequest.setMinVolume(2);
        volumeRangeCreateRequest.setMaxVolume(10);
        volumeRangeCreateRequest.setDiscountAmount(new BigDecimal(786));

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .body(volumeRangeCreateRequest)
                .log().all()
                .patch("/discounts/{discountPublicId}/addVolumeRange", UUID.fromString(publicId))
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .get("/discounts/{discountPublicId}/volumeRanges", UUID.fromString(publicId))
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1));

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .queryParam("discountType", DiscountType.VolumeDiscount)
                .get("/discounts")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].status", equalTo(DiscountStatus.Pending.name()));

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .patch("/discounts/{publicId}/approve", UUID.fromString(publicId))
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .queryParam("discountType", DiscountType.VolumeDiscount)
                .get("/discounts")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].status", equalTo(DiscountStatus.Active.name()));

        CartDiscountRequest cartDiscountRequest = new CartDiscountRequest();
        CartRequestItem cartRequestItem = new CartRequestItem();
        cartRequestItem.setProductPublicId(productId);
        cartRequestItem.setQuantity(5);

        cartDiscountRequest.setCartRequestItems(Collections.singletonList(cartRequestItem));

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .body(cartDiscountRequest)
                .log().all()
                .post("/salesOrders/cartDiscountCheck")
                .then()
                .statusCode(200);

        SalesOrderItemsRequest orderItems = SalesOrderItemsRequest
                .builder()
                .productId(productId)
                .quantity(5)
                .build();
        SalesOrdersRequest request = SalesOrdersRequest.builder()
                .customerId(UUID.fromString("d7c5bf34-fda1-46e2-b5ee-40db98aed06d"))
                .bookedBy("Itibari Test")
                .sourceChannel(SourceChannelEnum.BACK_OFFICE_APP.name())
                .salesOrderItems(Collections.singleton(orderItems))
                .build();

        String orderPublicId = given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .body(request)
                .post("/salesOrders")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        String orderItemPublicId = given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .get("/salesOrders/{salesOrderPublicId}/items", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].discount", notNullValue())
                .body("content[0].hasDiscount", equalTo(true))
                .extract().path("content[0].publicId");

        scheduler.runOrderPlanning();

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .body(request)
                .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200)
                .body("salesOrderStatus", equalTo(LoadingStatusEnum.PLANNING.name()));


        RoutePlanningRequest routePlanningRequest = new RoutePlanningRequest();
        routePlanningRequest.setLocationPublicId(UUID.fromString("90ad01f1-4a13-41f7-b825-c428b41f35d0"));
        routePlanningRequest.setRegionPublicId(UUID.fromString("8c5d4db0-f9a1-47ed-a160-56780e4e4d14"));
        routePlanningRequest.setVehiclePublicId(UUID.fromString("fa4bf2ee-6665-493a-8dbd-5d56dc8a7d93"));
        routePlanningRequest.setDeliveryAgentPublicId(deliveryAgentId);
        routePlanningRequest.setWarehousePublicId(UUID.fromString("3849b1be-a9dd-4f11-abbf-229ad2780a56"));
        routePlanningRequest.setSalesOrdersPublicIds(Collections.singletonList(UUID.fromString(orderPublicId)));


        String routePublicId = given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .body(routePlanningRequest)
                .post("/logistics/routePlanning")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        String deliveryId = given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .get("/logistics/routePlanning/{routePlanningPublicId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .extract().path("routeId");

        String productItemPublicId = given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .get("/logistics/routePlanning/{routePlanningPublicId}/loadingProducts", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].productItemPublicId");

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
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
                .auth().oauth2(adminToken)
                .body(loadingRequest)
                .post("warehouse/agent/{agentPublicId}/loading", UUID.fromString("af485765-69c5-4dd0-8fa0-560058b85c65"))
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .get("/logistics/routePlanning/{routePlanningPublicId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .body("routeStatus", equalTo(RouteStatus.Loaded.name()));


        given()
                .contentType("application/json")
                .auth().oauth2(deliveryAgentToken)
                .header("X-App-Version", "1.0.0")
                .queryParam("acceptType", AcceptTypeEnum.AcceptRoute)
                .patch("/deliveryAgents/accept/{routeId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200);


        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .get("/logistics/routePlanning/{routePlanningPublicId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .body("routeStatus", equalTo(RouteStatus.ReadyToGo.name()));

        given()
                .contentType("application/json")
                .auth().oauth2(deliveryAgentToken)
                .header("X-App-Version", "1.0.0")
                .queryParam("routeId", UUID.fromString(routePublicId))
                .queryParam("action", RouteAction.Start)
                .patch("/deliveryAgents/routeActions")
                .then()
                .statusCode(200);

        String productReconId = given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .get("/warehouse/productRecons")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].publicId");

        String productReconItemId = given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
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
        itemRequest.setProductReconItemPublicId(UUID.fromString(orderItemPublicId));
        itemRequest.setQuantitySold(3);
        deliverItemRequest.setItems(Collections.singletonList(itemRequest));

        given()
                .contentType("application/json")
                .auth().oauth2(deliveryAgentToken)
                .header("X-App-Version", "1.0.0")
                .body(deliverItemRequest)
                .patch("/deliveryAgents/markOrderItemAsDelivered")
                .then()
                .statusCode(200);

        // mark order as delivered

        given()
                .contentType("application/json")
                .auth().oauth2(deliveryAgentToken)
                .header("X-App-Version", "1.0.0")
                .patch("/deliveryAgents/markOrderAsDelivered/{orderPublicId}", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200);

        // fetch order check

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200)
                .body("salesOrderStatus", equalTo(LoadingStatusEnum.DELIVERED.name()));

        //----------amendment
        AmendOrderRequest amendOrderRequest = new AmendOrderRequest();
        amendOrderRequest.setSalesOrderPublicId(UUID.fromString(orderPublicId));
        amendOrderRequest.setRoutePublicId(UUID.fromString(routePublicId));
        amendOrderRequest.setReasonPublicId(reasonId);
        amendOrderRequest.setDescription("description amend order");
        amendOrderRequest.setAmendAction(AmendAction.CancelAndRequestVanSale);

        ItemRequest itemRequest1 = new ItemRequest();
        itemRequest1.setQuantitySold(3);
        itemRequest1.setProductItemPublicId(UUID.fromString(productItemPublicId));

        amendOrderRequest.setItems(Arrays.asList(itemRequest1));

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .body(amendOrderRequest)
                .patch("/deliveryAgents/{deliveryAgentPublicId}/amendOrder", deliveryAgentId)
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .get("/deliveryAgents/orderAmendments")
                .then().log().all()
                .statusCode(200)
                .body("content[0].status", equalToIgnoringCase("Pending"));

        Response response = given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("exportType", StatementExportType.EXCEL)
                .get("/reports/get-order-amendments-export")
                .then()
                .statusCode(200)
                .extract().response();

        saveExport(response, "order_amendment.xlsx");

        //--------end amendment

        given()
                .contentType("application/json")
                .auth().oauth2(deliveryAgentToken)
                .header("X-App-Version", "1.0.0")
                .queryParam("routeId", UUID.fromString(routePublicId))
                .queryParam("action", RouteAction.end)
                .patch("/deliveryAgents/routeActions")
                .then()
                .statusCode(400);
    }

    @Test
    public void testCancelOrder2() {
        final String deliveryAgentToken = testDeliveryAgentLoginViaPassword();
        final String adminToken = SUPER_ADMIN_TOKEN;

        UUID productId = UUID.fromString("7c4a6d52-5da2-47fe-92a8-ea2fce2602af");
        DiscountCreateRequest discountCreateRequest = new DiscountCreateRequest();
        discountCreateRequest.setDiscountType(DiscountType.VolumeDiscount);
        discountCreateRequest.setProductIds(Collections.singletonList(productId));
        UUID reasonId = UUID.fromString("632b6fda-e23c-4a5f-bec0-a0f9e1f0e293");
        UUID deliveryAgentId = UUID.fromString("af485765-69c5-4dd0-8fa0-560058b85c65");

        SalesOrderItemsRequest orderItems = SalesOrderItemsRequest
                .builder()
                .productId(productId)
                .quantity(5)
                .build();
        SalesOrdersRequest request = SalesOrdersRequest.builder()
                .customerId(UUID.fromString("d7c5bf34-fda1-46e2-b5ee-40db98aed06d"))
                .bookedBy("Itibari Test")
                .sourceChannel(SourceChannelEnum.BACK_OFFICE_APP.name())
                .salesOrderItems(Collections.singleton(orderItems))
                .build();

        String orderPublicId = given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .body(request)
                .post("/salesOrders")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        String orderItemPublicId = given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .get("/salesOrders/{salesOrderPublicId}/items", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200)
                .extract().path("content[0].publicId");

        scheduler.runOrderPlanning();

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .body(request)
                .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200)
                .body("salesOrderStatus", equalTo(LoadingStatusEnum.PLANNING.name()));


        RoutePlanningRequest routePlanningRequest = new RoutePlanningRequest();
        routePlanningRequest.setLocationPublicId(UUID.fromString("90ad01f1-4a13-41f7-b825-c428b41f35d0"));
        routePlanningRequest.setRegionPublicId(UUID.fromString("8c5d4db0-f9a1-47ed-a160-56780e4e4d14"));
        routePlanningRequest.setVehiclePublicId(UUID.fromString("fa4bf2ee-6665-493a-8dbd-5d56dc8a7d93"));
        routePlanningRequest.setDeliveryAgentPublicId(deliveryAgentId);
        routePlanningRequest.setWarehousePublicId(UUID.fromString("3849b1be-a9dd-4f11-abbf-229ad2780a56"));
        routePlanningRequest.setSalesOrdersPublicIds(Collections.singletonList(UUID.fromString(orderPublicId)));


        String routePublicId = given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .body(routePlanningRequest)
                .post("/logistics/routePlanning")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        String deliveryId = given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .get("/logistics/routePlanning/{routePlanningPublicId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .extract().path("routeId");

        String productItemPublicId = given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .get("/logistics/routePlanning/{routePlanningPublicId}/loadingProducts", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].productItemPublicId");


        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
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
                .auth().oauth2(adminToken)
                .body(loadingRequest)
                .post("warehouse/agent/{agentPublicId}/loading", UUID.fromString("af485765-69c5-4dd0-8fa0-560058b85c65"))
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .get("/logistics/routePlanning/{routePlanningPublicId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .body("routeStatus", equalTo(RouteStatus.Loaded.name()));

        given()
                .contentType("application/json")
                .auth().oauth2(deliveryAgentToken)
                .header("X-App-Version", "1.0.0")
                .queryParam("acceptType", AcceptTypeEnum.AcceptRoute)
                .patch("/deliveryAgents/accept/{routeId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200);


        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .get("/logistics/routePlanning/{routePlanningPublicId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .body("routeStatus", equalTo(RouteStatus.ReadyToGo.name()));

        given()
                .contentType("application/json")
                .auth().oauth2(deliveryAgentToken)
                .header("X-App-Version", "1.0.0")
                .queryParam("routeId", UUID.fromString(routePublicId))
                .queryParam("action", RouteAction.Start)
                .patch("/deliveryAgents/routeActions")
                .then()
                .statusCode(200);

        log.info("--------check items--e-");
        given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .queryParam("deliveryId", deliveryId)
                .get("/warehouse/agent/{agentPublicId}/items", deliveryAgentId)
                .then().log().all()
                .statusCode(200);

        log.info("-----check order number---");
        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .get("/logistics/delivery-operations")
                .then().log().all()
                .statusCode(200);

        CancelReasonRequest cancelReasonRequest =
                CancelReasonRequest.builder().validReason("Order cancelled due to fraud issue.").build();
        given()
                .contentType("application/json")
                .auth().oauth2(deliveryAgentToken)
                .header("X-App-Version", "1.0.0")
                .body(cancelReasonRequest).log().all()
                .patch("/deliveryAgents/{salesOrderPublicId}/cancelOrder", orderPublicId)
                .then()
                .statusCode(200);

        log.info("--------check amend---");
        String orderAmendmentPublicId = given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .get("/deliveryAgents/orderAmendments")
                .then().log().all()
                .statusCode(200)
                .body("content[0].status", equalToIgnoringCase("Pending"))
                .extract().path("content[0].publicId");

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .body(request)
                .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200)
                .body("awaitingApproval", equalTo(true))
                .body("orderAmendStatus", equalTo(OrderAmendStatus.Pending.name()));

        log.info("----tt----check items---");
        given()
                .contentType("application/json")
                .auth().oauth2(deliveryAgentToken)
                .get("/vanSales")
                .then().log().all()
                .statusCode(200);

        UUID rejectionCodePublicId = UUID.fromString("632b6fda-e23c-4a5f-bec0-a0f9e1f0e293");
        AmendRequest amendRequest = new AmendRequest();
        amendRequest.setActionType(ActionType.APPROVE);
        amendRequest.setComment("Approved failed delivery");
        amendRequest.setReasonCodePublicId(rejectionCodePublicId);

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .body(amendRequest)
                .patch("/deliveryAgents/amendOrder/{orderAmendmentId}", UUID.fromString(orderAmendmentPublicId))
                .then()
                .statusCode(200);

        log.info("--------check items---");
        given()
                .contentType("application/json")
                .auth().oauth2(deliveryAgentToken)
                .get("/vanSales")
                .then().log().all()
                .statusCode(200);


        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .body(request)
                .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                .then().log().all()
                .statusCode(200)
                .body("awaitingApproval", equalTo(false))
                .body("orderAmendStatus", equalTo(OrderAmendStatus.Approved.name()))
                .body("salesOrderStatus", equalTo(LoadingStatusEnum.FAILED_DELIVERY.name()))
                .body("deliveryStatus", equalTo(DeliveryStatus.Failed.name()))
                .body("status", equalTo(OrderStatusEnum.Failed.name()));

        given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .queryParam("deliveryId", deliveryId)
                .get("/warehouse/agent/{agentPublicId}/items", deliveryAgentId)
                .then().log().all()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .get("/logistics/issuances")
                .then().log().all()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .get("/logistics/delivery-operations")
                .then().log().all()
                .statusCode(200);
    }

    @Test
    public void testCreateOrderWithSAFullJourneyAndCancel() {

        UUID productId = UUID.fromString("7c4a6d52-5da2-47fe-92a8-ea2fce2602af");

        SalesOrderItemsRequest orderItems = SalesOrderItemsRequest
                .builder()
                .productId(productId)
                .quantity(2)
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
                .then().log().all()
                .statusCode(200)
                .body("salesOrderStatus", equalTo(LoadingStatusEnum.PLANNING.name()));
    }

    @Test
    public void testCreateProduct() throws IOException {

        UUID productId = UUID.fromString("7c4a6d52-5da2-47fe-92a8-ea2fce2602af");

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/products/{productId}", productId)
                .then().log().all()
                .statusCode(200);

        SalesOrderItemsRequest orderItems = SalesOrderItemsRequest
                .builder()
                .productId(productId)
                .quantity(2)
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
                .then().log().all()
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

        Response response = given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/reports/{salesOrderPublic}/invoiceReceipt", orderPublicId)
                .then()
                .statusCode(200)
                .extract().response();

//        saveExport(response, "invoice_receipt.pdf");
    }

    @Ignore
    @Test
    public void test_fetchCustomers_mappedToSupplier() throws IOException {

        UUID productId = UUID.fromString("7c4a6d52-5da2-47fe-92a8-ea2fce2602af");

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/products/{productId}", productId)
                .then().log().all()
                .statusCode(200);

        SalesOrderItemsRequest orderItems = SalesOrderItemsRequest
                .builder()
                .productId(productId)
                .quantity(2)
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
                .then().log().all()
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

        log.info("----------geg");
        given()
                .contentType("application/json")
                .auth().oauth2(SUPPLIER_USER_TOKEN)
                .queryParam("searchParam", "Eliamani")
                .queryParam("regionId", UUID.fromString("8c5d4db0-f9a1-47ed-a160-56780e4e4d14"))
                .queryParam("locationId", UUID.fromString("90ad01f1-4a13-41f7-b825-c428b41f35d0"))
                .get("/customers/supplierCustomerOnSalesOrder")
                .then().log().all()
                .statusCode(200);

        Response response = given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("startDate", "2025-04-05")
                .queryParam("endDate", "2025-04-28")
                .queryParam("regionId", UUID.fromString("59fb9c9f-cac4-49fe-9ba3-74e9026fe1c7"))
                .queryParam("exportType", StatementExportType.EXCEL)
                .get("/reports/supplierCustomersReport")
                .then()
                .statusCode(200)
                .extract().response();

//        saveExport(response, "kastoma_wa_mpeanaji_2_report.xlsx");
//        saveExport(response, "kastoma_wa_mpeanaji_report.pdf");
    }

    @Ignore
    @Test
    public void testSupplierTotalRevenue() throws IOException {

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders")
                .then().log().all()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(SUPPLIER_USER_TOKEN)
                .queryParam("startDate", LocalDate.now().minusDays(24).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .queryParam("endDate", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
//                .queryParam("regionId", UUID.fromString("8c5d4db0-f9a1-47ed-a160-56780e4e4d14"))
                .get("/reports/supplierRevenueReport")
                .then().log().all()
                .statusCode(200);
    }

    // Works But change the order counter number in Counter.class
//    @Ignore
    @Test
    public void testCreateSalesOrder_with_product_taxCode_with_Discount_Receipt() throws IOException {

        final String token = SUPER_ADMIN_TOKEN;
        String deliveryAgentToken = testDeliveryAgentLoginViaPassword();

        UUID productId = UUID.fromString("7c4a6d52-5da2-47fe-92a8-ea2fce2602af");
        UUID productId_2 = UUID.fromString("08ebb084-2633-428f-82b9-e8d98a22ec53");
        UUID productId_3 = UUID.fromString("6c09cc16-7262-423f-8715-50295dc2b8d6");
        DiscountCreateRequest discountCreateRequest = new DiscountCreateRequest();
        discountCreateRequest.setDiscountType(DiscountType.VolumeDiscount);
        discountCreateRequest.setProductIds(Arrays.asList(productId));

        given()
                .contentType("application/json")
                .auth().oauth2(token)
                .body(discountCreateRequest)
                .log().all()
                .post("/discounts")
                .then()
                .statusCode(200);

        String publicId = given()
                .contentType("application/json")
                .auth().oauth2(token)
                .queryParam("discountType", DiscountType.VolumeDiscount)
                .get("/discounts")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].status", equalTo(DiscountStatus.Incomplete.name()))
                .extract().path("content[0].publicId");

        VolumeRangeCreateRequest volumeRangeCreateRequest = new VolumeRangeCreateRequest();
        volumeRangeCreateRequest.setMinVolume(5);
        volumeRangeCreateRequest.setMaxVolume(10);
        volumeRangeCreateRequest.setDiscountAmount(new BigDecimal(10));

        given()
                .contentType("application/json")
                .auth().oauth2(token)
                .body(volumeRangeCreateRequest)
                .log().all()
                .patch("/discounts/{discountPublicId}/addVolumeRange", UUID.fromString(publicId))
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(token)
                .get("/discounts/{discountPublicId}/volumeRanges", UUID.fromString(publicId))
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1));

        given()
                .contentType("application/json")
                .auth().oauth2(token)
                .queryParam("discountType", DiscountType.VolumeDiscount)
                .get("/discounts")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].status", equalTo(DiscountStatus.Pending.name()));

        given()
                .contentType("application/json")
                .auth().oauth2(token)
                .patch("/discounts/{publicId}/approve", UUID.fromString(publicId))
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(token)
                .queryParam("discountType", DiscountType.VolumeDiscount)
                .get("/discounts")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].status", equalTo(DiscountStatus.Active.name()));

        CartDiscountRequest cartDiscountRequest = new CartDiscountRequest();
        CartRequestItem cartRequestItem = new CartRequestItem();
        cartRequestItem.setProductPublicId(productId);
        cartRequestItem.setQuantity(5);

        cartDiscountRequest.setCartRequestItems(Collections.singletonList(cartRequestItem));

        given()
                .contentType("application/json")
                .auth().oauth2(token)
                .body(cartDiscountRequest)
                .log().all()
                .post("/salesOrders/cartDiscountCheck")
                .then()
                .statusCode(200);

        SalesOrderItemsRequest orderItems = SalesOrderItemsRequest
                .builder()
                .productId(productId)
                .quantity(10)
                .build();
        SalesOrderItemsRequest orderItems_2 = SalesOrderItemsRequest
                .builder()
                .productId(productId_2)
                .quantity(2)
                .build();
        SalesOrderItemsRequest orderItems_3 = SalesOrderItemsRequest
                .builder()
                .productId(productId_3)
                .quantity(1)
                .build();
        SalesOrdersRequest request = SalesOrdersRequest.builder()
                .customerId(UUID.fromString("d7c5bf34-fda1-46e2-b5ee-40db98aed06d"))
                .bookedBy("Itibari Test")
                .sourceChannel(SourceChannelEnum.BACK_OFFICE_APP.name())
                .salesOrderItems(Set.of(orderItems, orderItems_2, orderItems_3))
                .build();

        String orderPublicId = given()
                .contentType("application/json")
                .auth().oauth2(token)
                .body(request)
                .post("/salesOrders")
                .then().log().all()
                .statusCode(200)
                .extract().path("publicId");

        List<SalesOrderItems> items = given()
                .contentType("application/json")
                .auth().oauth2(token)
                .get("/salesOrders/{salesOrderPublicId}/items", UUID.fromString(orderPublicId))
                .then().log().all()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()
                .getList("content", SalesOrderItems.class);

        UUID orderItemPublicId_1 = items.stream()
                .filter(i -> i.getProduct().getPublicId().equals(productId))
                .map(SalesOrderItems::getPublicId)
                .findFirst()
                .orElse(null);

        UUID orderItemPublicId_2 = items.stream()
                .filter(i -> i.getProduct().getPublicId().equals(productId_2))
                .map(SalesOrderItems::getPublicId)
                .findFirst()
                .orElse(null);

        UUID orderItemPublicId_3 = items.stream()
                .filter(i -> i.getProduct().getPublicId().equals(productId_3))
                .map(SalesOrderItems::getPublicId)
                .findFirst()
                .orElse(null);


//        checkInventoryNegativeTests();

        scheduler.runOrderPlanning();

        given()
                .contentType("application/json")
                .auth().oauth2(token)
                .body(request)
                .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                .then().log().all()
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
                .auth().oauth2(token)
                .body(routePlanningRequest)
                .post("/logistics/routePlanning")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        String deliveryId = given()
                .contentType("application/json")
                .auth().oauth2(token)
                .get("/logistics/routePlanning/{routePlanningPublicId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .extract().path("routeId");

//        String productItemPublicId = given()
        List<ProductItemDTO> productItems = given()
                .contentType("application/json")
                .auth().oauth2(token)
                .get("/logistics/routePlanning/{routePlanningPublicId}/loadingProducts", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()
                .getList("content", ProductItemDTO.class);

//                .extract().path("content[0].productItemPublicId");

        UUID productItemPublicId = productItems.get(0).getProductItemPublicId();
        UUID productItemPublicId_2 = productItems.get(1).getProductItemPublicId();
        UUID productItemPublicId_3 = productItems.get(2).getProductItemPublicId();

//        checkInventoryNegativeTests();

        given()
                .contentType("application/json")
                .auth().oauth2(token)
                .get("/logistics/routePlanning/{routePlanningPublicId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .body("routeStatus", equalTo(RouteStatus.CREATED.name()));

        LoadingRequest loadingRequest = new LoadingRequest();
        LoadingDTO loadingDTO = new LoadingDTO();
        loadingDTO.setLoadingType(LoadingType.OrderSale);
        loadingDTO.setProductItemPublicId(productItemPublicId);

        LoadingDTO loadingDTO_2 = new LoadingDTO();
        loadingDTO_2.setLoadingType(LoadingType.OrderSale);
        loadingDTO_2.setProductItemPublicId(productItemPublicId_2);

        LoadingDTO loadingDTO_3 = new LoadingDTO();
        loadingDTO_3.setLoadingType(LoadingType.OrderSale);
        loadingDTO_3.setProductItemPublicId(productItemPublicId_3);

        loadingRequest.setLoadingDTOList(List.of(loadingDTO, loadingDTO_2, loadingDTO_3));

        given()
                .contentType("application/json")
                .auth().oauth2(token)
                .body(loadingRequest)
                .post("warehouse/agent/{agentPublicId}/loading", UUID.fromString("af485765-69c5-4dd0-8fa0-560058b85c65"))
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(token)
                .get("/logistics/routePlanning/{routePlanningPublicId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .body("routeStatus", equalTo(RouteStatus.Loaded.name()));

//        checkInventoryNegativeTests();

        given()
                .contentType("application/json")
                .auth().oauth2(deliveryAgentToken)
                .header("X-App-Version", "1.0.0")
                .queryParam("acceptType", AcceptTypeEnum.AcceptRoute)
                .patch("/deliveryAgents/accept/{routeId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200);

//        checkInventoryNegativeTests();

        given()
                .contentType("application/json")
                .auth().oauth2(token)
                .get("/logistics/routePlanning/{routePlanningPublicId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .body("routeStatus", equalTo(RouteStatus.ReadyToGo.name()));

        given()
                .contentType("application/json")
                .auth().oauth2(deliveryAgentToken)
                .header("X-App-Version", "1.0.0")
                .queryParam("routeId", UUID.fromString(routePublicId))
                .queryParam("action", RouteAction.Start)
                .patch("/deliveryAgents/routeActions")
                .then()
                .statusCode(200);

//        List<ProductRecon> productReconId = given()
        List<ProductRecon> productRecons = given()
                .contentType("application/json")
                .auth().oauth2(token)
                .get("/warehouse/productRecons")
                .then().log().all()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()
                .getList("content", ProductRecon.class);

        UUID publicId1 = productRecons.get(0).getPublicId();

        String productReconItemId = given()
                .contentType("application/json")
                .auth().oauth2(token)
                .get("/warehouse/productRecons/{publicId}/items", publicId1)
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].publicId");

//        checkInventoryNegativeTests();

        DeliverItemRequest deliverItemRequest = new DeliverItemRequest();
        deliverItemRequest.setSalesOrderPublicId(UUID.fromString(orderPublicId));
        deliverItemRequest.setRoutePublicId(UUID.fromString(routePublicId));

        ItemRequest itemRequest = new ItemRequest();
        itemRequest.setProductItemPublicId(orderItemPublicId_1);
        itemRequest.setProductReconItemPublicId(UUID.fromString(productReconItemId));
        itemRequest.setQuantitySold(8);

        ItemRequest itemRequest_2 = new ItemRequest();
        itemRequest_2.setProductItemPublicId(orderItemPublicId_2);
        itemRequest_2.setProductReconItemPublicId(orderItemPublicId_2);
        itemRequest_2.setQuantitySold(2);

        ItemRequest itemRequest_3 = new ItemRequest();
        itemRequest_3.setProductItemPublicId(orderItemPublicId_3);
        itemRequest_3.setProductReconItemPublicId(orderItemPublicId_3);
        itemRequest_3.setQuantitySold(1);

        deliverItemRequest.setItems(List.of(itemRequest, itemRequest_2, itemRequest_3));

        given()
                .contentType("application/json")
                .auth().oauth2(deliveryAgentToken)
                .header("X-App-Version", "1.0.0")
                .body(deliverItemRequest)
                .patch("/deliveryAgents/markOrderItemAsDelivered")
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(deliveryAgentToken)
                .header("X-App-Version", "1.0.0")
                .patch("/deliveryAgents/markOrderAsDelivered/{orderPublicId}", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200);

//        checkInventoryNegativeTests();

        given()
                .contentType("application/json")
                .auth().oauth2(token)
                .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                .then().log().all()
                .statusCode(200)
                .body("salesOrderStatus", equalTo(LoadingStatusEnum.DELIVERED.name()));

        given()
                .contentType("application/json")
                .auth().oauth2(deliveryAgentToken)
                .header("X-App-Version", "1.0.0")
                .queryParam("routeId", UUID.fromString(routePublicId))
                .queryParam("action", RouteAction.end)
                .patch("/deliveryAgents/routeActions")
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(token)
                .queryParam("deliveryId", deliveryId)
                .get("/reconciliation")
                .then()
                .statusCode(200)
                .body("content[0].totalDiscount", notNullValue());

        given()
                .contentType("application/json")
                .auth().oauth2(token)
                .get("/salesOrders/{salesOrderPublicId}/items", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));

        given()
                .contentType("application/json")
                .auth().oauth2(token)
                .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                .then().log().all()
                .statusCode(200)
                .body("salesOrderStatus", equalTo(LoadingStatusEnum.DELIVERED.name()));
//
//        given()
//                .contentType("application/json")
//                .auth().oauth2(token)
//                .get("/warehouse/productReconSummary/{deliveryAgentPublicId}", UUID.fromString("af485765-69c5-4dd0-8fa0-560058b85c65"))
//                .then().log().all()
//                .statusCode(200);



//        Response response = given()
//                .auth().oauth2(token)
//                .queryParam("orderType", OrderType.CASH)
//                .get("/reports/{salesOrderPublic}/customerInvoiceReceipt", UUID.fromString(orderPublicId))
//                .then()
//                .statusCode(200)
//                .extract().response();
//
//        saveExport(response, "new_receipt.pdf");
    }

    @Test
    public void testDiscountOnUpdateOrder() {

        UUID productId = UUID.fromString("7c4a6d52-5da2-47fe-92a8-ea2fce2602af");
        DiscountCreateRequest discountCreateRequest = new DiscountCreateRequest();
        discountCreateRequest.setDiscountType(DiscountType.VolumeDiscount);
        discountCreateRequest.setProductIds(Collections.singletonList(productId));

        UUID customerPublicId = UUID.fromString("d7c5bf34-fda1-46e2-b5ee-40db98aed06d");

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(discountCreateRequest)
                .log().all()
                .post("/discounts")
                .then()
                .statusCode(200);

        String publicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("discountType", DiscountType.VolumeDiscount)
                .get("/discounts")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].status", equalTo(DiscountStatus.Incomplete.name()))
                .extract().path("content[0].publicId");

        VolumeRangeCreateRequest volumeRangeCreateRequest = new VolumeRangeCreateRequest();
        volumeRangeCreateRequest.setMinVolume(2);
        volumeRangeCreateRequest.setMaxVolume(10);
        volumeRangeCreateRequest.setDiscountAmount(new BigDecimal(100));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(volumeRangeCreateRequest)
                .log().all()
                .patch("/discounts/{discountPublicId}/addVolumeRange", UUID.fromString(publicId))
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/discounts/{discountPublicId}/volumeRanges", UUID.fromString(publicId))
                .then().log().all()
                .statusCode(200)
                .body("content.size()", equalTo(1));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("discountType", DiscountType.VolumeDiscount)
                .get("/discounts")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].status", equalTo(DiscountStatus.Pending.name()));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .patch("/discounts/{publicId}/approve", UUID.fromString(publicId))
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("discountType", DiscountType.VolumeDiscount)
                .get("/discounts")
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].status", equalTo(DiscountStatus.Active.name()));

        CartDiscountRequest cartDiscountRequest = new CartDiscountRequest();
        CartRequestItem cartRequestItem = new CartRequestItem();
        cartRequestItem.setProductPublicId(productId);
        cartRequestItem.setQuantity(5);

        cartDiscountRequest.setCartRequestItems(Collections.singletonList(cartRequestItem));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(cartDiscountRequest).log().all()
                .post("/salesOrders/cartDiscountCheck")
                .then()
                .statusCode(200);

        SalesOrderItemsRequest orderItems = SalesOrderItemsRequest
                .builder()
                .productId(productId)
                .quantity(5)
                .build();

        SalesOrdersRequest request = SalesOrdersRequest.builder()
                .customerId(customerPublicId)
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
                .body("content[0].discount", notNullValue())
                .body("content[0].hasDiscount", equalTo(true))
                .extract().path("content[0].publicId");

        checkInventoryNegativeTests();

//        scheduler.runOrderPlanning();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                .then().log().all()
                .statusCode(200)
                .body("salesOrderStatus", equalTo(LoadingStatusEnum.ORDER_PLACED.name()));


        SalesOrderItemsRequest request1 = SalesOrderItemsRequest
                .builder()
                .productId(productId)
                .quantity(3)
                .build();

        SalesOrdersRequest updateRequest = SalesOrdersRequest.builder()
                .customerId(customerPublicId)
                .bookedBy("Itibari Test Updated")
                .sourceChannel(SourceChannelEnum.BACK_OFFICE_APP.name())
                .salesOrderItems(Collections.singleton(request1))
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(updateRequest)
                .patch("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                .then().log().all()
                .statusCode(200);
    }

    @Ignore
    @Test
    public void testPartiallyDeliverOrderWithDiscount() {

        UUID productId = UUID.fromString("7c4a6d52-5da2-47fe-92a8-ea2fce2602af");
        DiscountCreateRequest discountCreateRequest = new DiscountCreateRequest();
        discountCreateRequest.setDiscountType(DiscountType.VolumeDiscount);
        discountCreateRequest.setProductIds(Collections.singletonList(productId));

        UUID customerPublicId = UUID.fromString("d7c5bf34-fda1-46e2-b5ee-40db98aed06d");

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(discountCreateRequest)
                .log().all()
                .post("/discounts")
                .then()
                .statusCode(200);

        String publicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("discountType", DiscountType.VolumeDiscount)
                .get("/discounts")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].status", equalTo(DiscountStatus.Incomplete.name()))
                .extract().path("content[0].publicId");

        VolumeRangeCreateRequest volumeRangeCreateRequest = new VolumeRangeCreateRequest();
        volumeRangeCreateRequest.setMinVolume(2);
        volumeRangeCreateRequest.setMaxVolume(10);
        volumeRangeCreateRequest.setDiscountAmount(new BigDecimal(100));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(volumeRangeCreateRequest)
                .log().all()
                .patch("/discounts/{discountPublicId}/addVolumeRange", UUID.fromString(publicId))
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/discounts/{discountPublicId}/volumeRanges", UUID.fromString(publicId))
                .then().log().all()
                .statusCode(200)
                .body("content.size()", equalTo(1));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("discountType", DiscountType.VolumeDiscount)
                .get("/discounts")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].status", equalTo(DiscountStatus.Pending.name()));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .patch("/discounts/{publicId}/approve", UUID.fromString(publicId))
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("discountType", DiscountType.VolumeDiscount)
                .get("/discounts")
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].status", equalTo(DiscountStatus.Active.name()));

        CartDiscountRequest cartDiscountRequest = new CartDiscountRequest();
        CartRequestItem cartRequestItem = new CartRequestItem();
        cartRequestItem.setProductPublicId(productId);
        cartRequestItem.setQuantity(5);

        cartDiscountRequest.setCartRequestItems(Collections.singletonList(cartRequestItem));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(cartDiscountRequest).log().all()
                .post("/salesOrders/cartDiscountCheck")
                .then()
                .statusCode(200);

        SalesOrderItemsRequest orderItems = SalesOrderItemsRequest
                .builder()
                .productId(productId)
                .quantity(5)
                .build();

        SalesOrdersRequest request = SalesOrdersRequest.builder()
                .customerId(customerPublicId)
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
                .body("content[0].discount", notNullValue())
                .body("content[0].hasDiscount", equalTo(true))
                .extract().path("content[0].publicId");

        checkInventoryNegativeTests();

        scheduler.runOrderPlanning();

        log.info("---------check 1 -- 1");
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                .then().log().all()
                .statusCode(200)
                .body("salesOrderStatus", equalTo(LoadingStatusEnum.ORDER_PLACED.name()));

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

        checkInventoryNegativeTests();

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

        checkInventoryNegativeTests();

        given()
                .contentType("application/json")
                .auth().oauth2(DELIVERY_AGENT_TOKEN)
                .header("X-App-Version", "1.0.0")
                .queryParam("acceptType", AcceptTypeEnum.AcceptRoute)
                .patch("/deliveryAgents/accept/{routeId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200);

        checkInventoryNegativeTests();

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

        String productReconItemId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/warehouse/productRecons/{publicId}/items", UUID.fromString(productReconId))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].publicId");

        checkInventoryNegativeTests();

        DeliverItemRequest deliverItemRequest = new DeliverItemRequest();
        deliverItemRequest.setSalesOrderPublicId(UUID.fromString(orderPublicId));
        deliverItemRequest.setRoutePublicId(UUID.fromString(routePublicId));

        ItemRequest itemRequest = new ItemRequest();
        itemRequest.setProductItemPublicId(UUID.fromString(orderItemPublicId));
        itemRequest.setProductReconItemPublicId(UUID.fromString(orderItemPublicId));
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

        given()
                .contentType("application/json")
                .auth().oauth2(DELIVERY_AGENT_TOKEN)
                .header("X-App-Version", "1.0.0")
                .patch("/deliveryAgents/markOrderAsDelivered/{orderPublicId}", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200);

        checkInventoryNegativeTests();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                .then().log().all()
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

        log.info("---------check 2 -- 2");
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/reports/{customerPublicId}/customerMostBoughtProductReport", customerPublicId)
                .then().log().all()
                .statusCode(200);
    }

    @Test
    public void testGetOrderStats() {
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders/salesOrderStats")
                .then().log().all()
                .statusCode(200)
                .body("totalOrderValue", notNullValue())
                .body("totalOrderValue", notNullValue());
    }

    @Test
    @Ignore
    public void testResendTraInvoice() {
        UUID orderPublicId = UUID.fromString("5b127535-2e8b-43b9-854b-72d3159609d7");
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .patch("/salesOrders/{salesOrderPublicId}/resendTraInvoice", orderPublicId)
                .then()
                .statusCode(200);

    }

    @Test
    public void testGetSlimOrderItems() {
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders/{salesOrderPublicId}/items/slim", UUID.fromString("5b127535-2e8b-43b9-854b-72d3159609d7"))
                .then().log().all()
                .statusCode(200);
    }

    @Test
    public void testOrderTransition() {
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .patch("/salesOrders/{publicId}/transitionOrders", UUID.fromString("5b127535-2e8b-43b9-854b-72d3159609d7"))
                .then().log().all()
                .statusCode(400);
    }

    //    @Ignore
    @Test
    public void testCreateOrderAndDoAmendment() throws IOException, InterruptedException {
        String deliveryAgentToken = testDeliveryAgentLoginViaPassword();
        UUID productId = UUID.fromString("7c4a6d52-5da2-47fe-92a8-ea2fce2602af");
        UUID rejectionCodePublicId = UUID.fromString("632b6fda-e23c-4a5f-bec0-a0f9e1f0e293");

        SalesOrderItemsRequest orderItems = SalesOrderItemsRequest
                .builder()
                .productId(productId)
                .quantity(5)
                .build();
        SalesOrdersRequest request = SalesOrdersRequest.builder()
                .customerId(UUID.fromString("d7c5bf34-fda1-46e2-b5ee-40db98aed06d"))
                .bookedBy("Itibari Test")
                .sourceChannel(SourceChannelEnum.BACK_OFFICE_APP.name())
                .salesOrderItems(Collections.singleton(orderItems))
                .build();

        String orderPublicId = given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .body(request)
                .post("/salesOrders")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        log.info("------check-----10---");
        String orderItemPublicId = given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .get("/salesOrders/{salesOrderPublicId}/items", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].quantity", equalTo(5))
                .body("content[0].quantityDelivered", equalTo(0))
                .extract().path("content[0].publicId");

        scheduler.runOrderPlanning();

        given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .body(request)
                .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200)
                .body("salesOrderStatus", equalTo(LoadingStatusEnum.PLANNING.name()));

        try {
            Thread.sleep(1000);
        } catch (Exception e) {
        }

        RoutePlanningRequest routePlanningRequest = new RoutePlanningRequest();
        routePlanningRequest.setLocationPublicId(UUID.fromString("90ad01f1-4a13-41f7-b825-c428b41f35d0"));
        routePlanningRequest.setRegionPublicId(UUID.fromString("8c5d4db0-f9a1-47ed-a160-56780e4e4d14"));
        routePlanningRequest.setVehiclePublicId(UUID.fromString("fa4bf2ee-6665-493a-8dbd-5d56dc8a7d93"));
        routePlanningRequest.setDeliveryAgentPublicId(UUID.fromString("af485765-69c5-4dd0-8fa0-560058b85c65"));
        routePlanningRequest.setWarehousePublicId(UUID.fromString("3849b1be-a9dd-4f11-abbf-229ad2780a56"));
        routePlanningRequest.setSalesOrdersPublicIds(Collections.singletonList(UUID.fromString(orderPublicId)));

        String routePublicId = given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .body(routePlanningRequest)
                .post("/logistics/routePlanning")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        String deliveryId = given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .get("/logistics/routePlanning/{routePlanningPublicId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .extract().path("routeId");  //RT-100029

        log.info("---------check---17--");
        String productItemPublicId = given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .get("/logistics/routePlanning/{routePlanningPublicId}/loadingProducts", UUID.fromString(routePublicId))
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].productItemPublicId");

        given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .get("/logistics/routePlanning/{routePlanningPublicId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .body("routeStatus", equalTo(RouteStatus.CREATED.name()));


        VanSaleRequest vanSaleRequest = new VanSaleRequest();
        vanSaleRequest.setDeliveryAgentPublicId(UUID.fromString("af485765-69c5-4dd0-8fa0-560058b85c65"));

        ProductItemRequest productItemRequest = new ProductItemRequest();
        productItemRequest.setProductItemPublicId(productId);
        productItemRequest.setQuantity(2);

        vanSaleRequest.setProductItemRequestList(List.of(productItemRequest));

        given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .body(vanSaleRequest)
                .post("/vanSales")
                .then()
                .statusCode(200);

        LoadingRequest loadingRequest = new LoadingRequest();
        LoadingDTO loadingDTO = new LoadingDTO();
        loadingDTO.setLoadingType(LoadingType.OrderSale);
        loadingDTO.setProductItemPublicId(UUID.fromString(productItemPublicId));
        loadingRequest.setLoadingDTOList(Collections.singletonList(loadingDTO));

        given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .body(loadingRequest)
                .post("warehouse/agent/{agentPublicId}/loading", UUID.fromString("af485765-69c5-4dd0-8fa0-560058b85c65"))
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .get("/logistics/routePlanning/{routePlanningPublicId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .body("routeStatus", equalTo(RouteStatus.Loaded.name()));

        log.info("------check-----11---");
        given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .queryParam("deliveryId", deliveryId)
                .get("/warehouse/agent/{agentPublicId}/items", UUID.fromString("af485765-69c5-4dd0-8fa0-560058b85c65"))
                .then().log().all()
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content.quantityPicked", hasItem(greaterThanOrEqualTo(1)))
                .statusCode(200);

        Response response = given()
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .queryParam("exportType", StatementExportType.PDF)
                .get("/reports/issuanceReport/{routePublicId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .extract().response();

        saveExport(response, "issuance_items.pdf");


        given()
                .contentType("application/json")
                .auth().oauth2(deliveryAgentToken)
                .header("X-App-Version", "1.0.0")
                .queryParam("acceptType", AcceptTypeEnum.AcceptRoute)
                .patch("/deliveryAgents/accept/{routeId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .get("/logistics/routePlanning/{routePlanningPublicId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .body("routeStatus", equalTo(RouteStatus.ReadyToGo.name()));

        given()
                .contentType("application/json")
                .auth().oauth2(deliveryAgentToken)
                .header("X-App-Version", "1.0.0")
                .queryParam("routeId", UUID.fromString(routePublicId))
                .queryParam("action", RouteAction.Start)
                .patch("/deliveryAgents/routeActions")
                .then()
                .statusCode(200);

        try {
            Thread.sleep(1000);
        } catch (Exception e) {
        }

        String productReconId = given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .get("/warehouse/productRecons")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].publicId");

        String productReconItemId = given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
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
        itemRequest.setProductReconItemPublicId(UUID.fromString(orderItemPublicId));
        itemRequest.setQuantitySold(5);
        deliverItemRequest.setItems(Collections.singletonList(itemRequest));

        given()
                .contentType("application/json")
                .auth().oauth2(deliveryAgentToken)
                .header("X-App-Version", "1.0.0")
                .body(deliverItemRequest)
                .patch("/deliveryAgents/markOrderItemAsDelivered")
                .then()
                .statusCode(200);

        // mark order as delivered

        given()
                .contentType("application/json")
                .auth().oauth2(deliveryAgentToken)
                .header("X-App-Version", "1.0.0")
                .patch("/deliveryAgents/markOrderAsDelivered/{orderPublicId}", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200);

        // fetch order check

        String salesOrderNumber = given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                .then().log().all()
                .statusCode(200)
                .body("salesOrderStatus", equalTo(LoadingStatusEnum.DELIVERED.name()))
                .extract().path("salesOrderNumber");

        String salesItemPublicId = given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .get("/salesOrders/{salesOrderPublicId}/items", UUID.fromString(orderPublicId))
                .then().log().all()
                .statusCode(200)
                .extract().path("content[0].publicId");

        DeliverVanSaleItemRequest deliverVanSaleItemRequest = new DeliverVanSaleItemRequest();
        deliverVanSaleItemRequest.setRoutePublicId(UUID.fromString(routePublicId));
        CustomersRequest customersRequest = CustomersRequest.builder()
                .phoneNumber("07" + String.valueOf(System.currentTimeMillis()).substring(3, 11))
                .firstName("sellah sella")
                .organizationLocationId(UUID.fromString("90ad01f1-4a13-41f7-b825-c428b41f35d0"))
                .organizationRegionId(UUID.fromString("8c5d4db0-f9a1-47ed-a160-56780e4e4d14"))
                .build();

        ItemRequest itemRequest1 = new ItemRequest();
        itemRequest1.setProductItemPublicId(UUID.fromString(productReconItemId));
        itemRequest1.setQuantitySold(1);
        deliverVanSaleItemRequest.setCustomersRequest(customersRequest);
        deliverVanSaleItemRequest.setItems(List.of(itemRequest1));

        given()
                .contentType("application/json")
                .auth().oauth2(deliveryAgentToken)
                .header("X-App-Version", "1.0.0")
                .body(deliverVanSaleItemRequest)
                .patch("/deliveryAgents/markVanSaleItemAsSold")
                .then()
                .statusCode(200);



        given()
                .contentType("application/json")
                .auth().oauth2(deliveryAgentToken)
                .header("X-App-Version", "1.0.0")
                .queryParam("routeId", UUID.fromString(routePublicId))
                .queryParam("action", RouteAction.end)
                .patch("/deliveryAgents/routeActions")
                .then()
                .statusCode(200);

        // get recon

        given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .queryParam("deliveryId", deliveryId)
                .get("/reconciliation")
                .then().log().all()
                .statusCode(200);

        // return goods

        productItemRequest = new ProductItemRequest();
        productItemRequest.setProductItemPublicId(UUID.fromString(productReconItemId));
        productItemRequest.setQuantityReturned(1);

        ProductReconAction productReconAction = new ProductReconAction();
        productReconAction.setProductReconPublicId(UUID.fromString(productReconId));
        productReconAction.setProductItems(Collections.singletonList(productItemRequest));

        given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .body(productReconAction)
                .patch("/warehouse/productRecons/{publicId}", UUID.fromString(productReconId))
                .then()
                .statusCode(200);

        //reconcile

        given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .patch("/warehouse/productRecons/{publicId}/reconcile", UUID.fromString(productReconId))
                .then()
                .statusCode(200);

        /////////----------
        String productReconPublicId = given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .get("/warehouse/productRecons")
                .then().log().all()
                .statusCode(200)
                .extract().path("content[0].publicId");

        given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .get("/warehouse/productRecons/{publicId}/items", UUID.fromString(productReconPublicId))
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].productName", equalToIgnoringCase("KissKids Baby Pants M-32 x 6"))
                .body("content[0].productCode", equalToIgnoringCase("SKU100006"))
                .body("content[0].productSellingPrice", equalTo(500.0F))
                .body("content[0].quantityReturned", equalTo(1))
                .body("content[0].quantityPicked", equalTo(7))
                .body("content[0].quantitySold", equalTo(6))
                .body("content[0].totalAmountPicked", equalTo(3500.0F))
                .body("content[0].totalAmountSold", equalTo(3000.0F));

        given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .get("/reconciliation/creditRecons")
                .then().log().all()
                .statusCode(200);

        AmendOrderItemRequest.Item itemPayload = AmendOrderItemRequest.Item.builder()
                .salesOrderItemPublicId(UUID.fromString(salesItemPublicId))
                .returnedQuantity(2)
                .reasonCodePublicId(rejectionCodePublicId)
                .build();
        AmendOrderItemRequest amendOrderItemRequest = AmendOrderItemRequest.builder()
                .itemList(Collections.singletonList(itemPayload))
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .queryParam("orderNumber", salesOrderNumber)
                .get("/salesOrders/get-sales-items")
                .then().log().all()
                .statusCode(200);

//        given()
//                .contentType("application/json")
//                .auth().oauth2(SUPER_ADMIN_TOKEN)
//                .body(amendOrderItemRequest).log().all()
//                .post("/salesOrders/{salesOrderPublicId}/amend", UUID.fromString(orderPublicId))
//                .then().log().all()
//                .statusCode(200);
//
//        String customerReturnPublicId = given()
//                .contentType("application/json")
//                .auth().oauth2(SUPER_ADMIN_TOKEN)
//                .get("/salesOrders/get-customer-returns")
//                .then().log().all()
//                .statusCode(200)
//                .extract().path("content[0].publicId");
//
//
//        Response response_1 = given()
//                .auth().oauth2(SUPER_ADMIN_TOKEN)
//                .queryParam("exportType", StatementExportType.EXCEL)
//                .get("/reports/get-customer-return-export")
//                .then()
//                .statusCode(200)
//                .extract().response();

//        saveExport(response_1, "customer_return.xlsx");

//        given()
//                .contentType("application/json")
//                .auth().oauth2(SUPER_ADMIN_TOKEN)
//                .get("/salesOrders/get-customer-returns/{returnPublicId}/items", UUID.fromString(customerReturnPublicId))
//                .then().log().all()
//                .statusCode(200);
//
//        given()
//                .contentType("application/json")
//                .auth().oauth2(SUPER_ADMIN_TOKEN)
//                .queryParam("returnApprovalType", ReturnApprovalType.Approve)
//                .post("/salesOrders/{customerReturnPublicId}/approveAmendment", UUID.fromString(customerReturnPublicId))
//                .then()
//                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .get("/warehouse/productRecons/{publicId}/items", UUID.fromString(productReconPublicId))
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].productName", equalToIgnoringCase("KissKids Baby Pants M-32 x 6"))
                .body("content[0].productCode", equalToIgnoringCase("SKU100006"))
                .body("content[0].productSellingPrice", equalTo(500.0F))
                .body("content[0].quantityReturned", equalTo(1))
                .body("content[0].quantityPicked", equalTo(7))
                .body("content[0].quantitySold", equalTo(6))
                .body("content[0].totalAmountPicked", equalTo(3500.0F))
                .body("content[0].totalAmountSold", equalTo(3000.0F));

        log.info("--------------------cddd");
//        given()
//                .contentType("application/json")
//                .auth().oauth2(SUPER_ADMIN_TOKEN)
//                .get("/deliveryAgents/orderAmendments")
//                .then().log().all()
//                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .queryParam("deliveryId", deliveryId)
                .get("/warehouse/agent/{agentPublicId}/items", UUID.fromString("af485765-69c5-4dd0-8fa0-560058b85c65"))
                .then().log().all()
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content.quantityPicked", hasItem(greaterThanOrEqualTo(1)))
                .statusCode(200);

    }

    @Test
    public void testGetBNPLOrderSummary(){
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("salesOrderPublicId",UUID.fromString("ef2f4703-b478-4ca6-98d1-bc3aa88e1c3f"))
                .get("/salesOrders/bnplOrderSummary")
                .then().log().all()
                .statusCode(400);
    }

    @Test
//    @Ignore  //works alone
    public void testCreateOrderAndDeliverOneAndLeaveOne() throws Exception {
        final String deliveryAgentToken = testDeliveryAgentLoginViaPassword();
        final String adminToken = SUPER_ADMIN_TOKEN;
        UUID productId = UUID.fromString("534cf5d4-f8c3-495c-a813-9a42f338c466");
        UUID productId2 = UUID.fromString("581c5c73-afc0-4b89-bafb-e160011f2715");
        UUID reasonId = UUID.fromString("632b6fda-e23c-4a5f-bec0-a0f9e1f0e293");
        UUID deliveryAgentId = UUID.fromString("af485765-69c5-4dd0-8fa0-560058b85c65");

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .queryParam("productPublicId", productId)
                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
                .then()
                .statusCode(200)
                .body("content[0].availableStock", equalTo(50))
                .body("content[0].reservedStock", equalTo(0))
                .body("content[0].stockValue", equalTo(1620044.0F))
                .body("content[0].totalStock", equalTo(50));

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .queryParam("productPublicId", productId)
                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].product.availableStock", equalTo(15));

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .get("/products/{publicId}", productId)
                .then()
                .statusCode(200)
                .body("availableStock", equalTo(15));

        SalesOrderItemsRequest orderItems = SalesOrderItemsRequest
                .builder()
                .productId(productId)
                .quantity(5)
                .build();
        SalesOrderItemsRequest orderItems2 = SalesOrderItemsRequest
                .builder()
                .productId(productId2)
                .quantity(10)
                .build();
        Set<SalesOrderItemsRequest> salesOrderItemsRequests = new HashSet<>();
        salesOrderItemsRequests.add(orderItems);
        salesOrderItemsRequests.add(orderItems2);
        SalesOrdersRequest request = SalesOrdersRequest.builder()
                .customerId(UUID.fromString("d7c5bf34-fda1-46e2-b5ee-40db98aed06d"))
                .bookedBy("Itibari Test")
                .sourceChannel(SourceChannelEnum.BACK_OFFICE_APP.name())
                .salesOrderItems(salesOrderItemsRequests)
                .build();

        String orderPublicId = given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .body(request)
                .post("/salesOrders")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        log.info("--------items-------------");
        List<SalesOrderItems> items = given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .get("/salesOrders/{salesOrderPublicId}/items", UUID.fromString(orderPublicId))
                .then().statusCode(200)
                .extract().jsonPath().getList("content", SalesOrderItems.class);

        Map<UUID, String> publicIdSkuMap = items.stream().collect(Collectors.toMap(SalesOrderItems::getPublicId,
                item -> item.getProduct().getSku()));

        String orderItemPublicId = null;
        String orderItemPublicId2 = null;

        for (Map.Entry<UUID, String> entry : publicIdSkuMap.entrySet()) {
            UUID publicId = entry.getKey();
            String sku = entry.getValue();
            if (sku.equals("SKU100021")) {
                orderItemPublicId = publicId.toString();
            }
            if (sku.equals("SKU100022")) {
                orderItemPublicId2 = publicId.toString();
            }
        }

        scheduler.runOrderPlanning();

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
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
                .auth().oauth2(adminToken)
                .body(routePlanningRequest)
                .post("/logistics/routePlanning")
                .then()
                .statusCode(200)
                .extract().path("publicId");


        String deliveryId = given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .get("/logistics/routePlanning/{routePlanningPublicId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .extract().path("routeId");

        List<String> productItems = given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .get("/logistics/routePlanning/{routePlanningPublicId}/loadingProducts", UUID.fromString(routePublicId))
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().jsonPath().getList("content.productItemPublicId");


        String productItemPublicId = productItems.get(0).toString();
        String productItemPublicId2 = productItems.get(1).toString();

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .get("/logistics/routePlanning/{routePlanningPublicId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .body("routeStatus", equalTo(RouteStatus.CREATED.name()));

        LoadingRequest loadingRequest = new LoadingRequest();
        LoadingDTO loadingDTO = new LoadingDTO();
        loadingDTO.setLoadingType(LoadingType.OrderSale);
        loadingDTO.setProductItemPublicId(UUID.fromString(productItemPublicId));

        LoadingDTO loadingDTO1 = new LoadingDTO();
        loadingDTO1.setLoadingType(LoadingType.OrderSale);
        loadingDTO1.setProductItemPublicId(UUID.fromString(productItemPublicId2));


        loadingRequest.setLoadingDTOList(Arrays.asList(loadingDTO, loadingDTO1));

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .body(loadingRequest)
                .post("warehouse/agent/{agentPublicId}/loading", UUID.fromString("af485765-69c5-4dd0-8fa0-560058b85c65"))
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .get("/logistics/routePlanning/{routePlanningPublicId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .body("routeStatus", equalTo(RouteStatus.Loaded.name()));


        given()
                .contentType("application/json")
                .auth().oauth2(deliveryAgentToken)
                .header("X-App-Version", "1.0.0")
                .queryParam("acceptType", AcceptTypeEnum.AcceptRoute)
                .patch("/deliveryAgents/accept/{routeId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .get("/logistics/routePlanning/{routePlanningPublicId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .body("routeStatus", equalTo(RouteStatus.ReadyToGo.name()));

        given()
                .contentType("application/json")
                .auth().oauth2(deliveryAgentToken)
                .header("X-App-Version", "1.0.0")
                .queryParam("routeId", UUID.fromString(routePublicId))
                .queryParam("action", RouteAction.Start)
                .patch("/deliveryAgents/routeActions")
                .then()
                .statusCode(200);


        String productReconId = given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .get("/warehouse/productRecons")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].publicId");


        List<ProductItem> productReconItem = given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .get("/warehouse/productRecons/{publicId}/items", UUID.fromString(productReconId))
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().jsonPath().getList("content", ProductItem.class);

        Map<UUID, String> productReconItemIdSkuMap = productReconItem.stream()
                .collect(Collectors.toMap(
                        ProductItem::getPublicId,
                        item -> item.getProductCode()
                ));

        String productReconItemId = null;
        String productReconItemId2 = null;

        for (Map.Entry<UUID, String> entry : productReconItemIdSkuMap.entrySet()) {
            UUID publicId = entry.getKey();
            String sku = entry.getValue();
            if (sku.equals("SKU100021")) {
                productReconItemId = publicId.toString();
            }
            if (sku.equals("SKU100022")) {
                productReconItemId2 = publicId.toString();
            }
        }


        given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .queryParam("deliveryId", deliveryId)
                .get("/warehouse/agent/{agentPublicId}/items", deliveryAgentId)
                .then().log().all()
                .statusCode(200);

        AmendOrderRequest amendOrderRequest = new AmendOrderRequest();
        amendOrderRequest.setSalesOrderPublicId(UUID.fromString(orderPublicId));
        amendOrderRequest.setRoutePublicId(UUID.fromString(routePublicId));
        amendOrderRequest.setReasonPublicId(reasonId);
        amendOrderRequest.setDescription("description amend order");
        amendOrderRequest.setAmendAction(AmendAction.CancelAndRequestVanSale);

        ItemRequest itemRequest2 = new ItemRequest();
        itemRequest2.setQuantitySold(0);
        itemRequest2.setProductItemPublicId(UUID.fromString(orderItemPublicId));

        ItemRequest itemRequest3 = new ItemRequest();
        itemRequest3.setQuantitySold(0);
        itemRequest3.setProductItemPublicId(UUID.fromString(orderItemPublicId2));

        amendOrderRequest.setItems(new ArrayList<>());
//        amendOrderRequest.setItems(Arrays.asList(itemRequest2, itemRequest3));

        given()
                .contentType("application/json")
                .auth().oauth2(deliveryAgentToken)
                .body(amendOrderRequest).log().all()
                .patch("/deliveryAgents/{deliveryAgentPublicId}/amendOrder", deliveryAgentId)
                .then()
                .statusCode(200);

        log.info("---check amends------1---");
        String orderAmendmentPublicId = given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .get("/deliveryAgents/orderAmendments")
                .then().log().all()
                .statusCode(200)
                .body("content[0].status", equalToIgnoringCase("Pending"))
                .extract().path("content[0].publicId");

        UUID rejectionCodePublicId = UUID.fromString("632b6fda-e23c-4a5f-bec0-a0f9e1f0e293");
        AmendRequest amendRequest = new AmendRequest();
        amendRequest.setActionType(ActionType.APPROVE);
        amendRequest.setComment("Approved failed delivery");
        amendRequest.setReasonCodePublicId(rejectionCodePublicId);

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .body(amendRequest)
                .patch("/deliveryAgents/amendOrder/{orderAmendmentId}", UUID.fromString(orderAmendmentPublicId))
                .then()
                .statusCode(200);

        DeliverItemRequest deliverItemRequest = new DeliverItemRequest();
        deliverItemRequest.setSalesOrderPublicId(UUID.fromString(orderPublicId));
        deliverItemRequest.setRoutePublicId(UUID.fromString(routePublicId));

        ItemRequest itemRequest = new ItemRequest();
        itemRequest.setProductItemPublicId(UUID.fromString(orderItemPublicId));
        itemRequest.setQuantitySold(0);

        ItemRequest item1 = new ItemRequest();
        item1.setProductItemPublicId(UUID.fromString(orderItemPublicId2));
        item1.setQuantitySold(0);
        deliverItemRequest.setItems(Arrays.asList(itemRequest, item1));


//        given()
//                .contentType("application/json")
//                .auth().oauth2(deliveryAgentToken)
//                .header("X-App-Version", "1.0.0")
//                .body(deliverItemRequest)
//                .patch("/deliveryAgents/markOrderItemAsDelivered")
//                .then()
//                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .queryParam("deliveryId", deliveryId)
                .get("/warehouse/agent/{agentPublicId}/items", deliveryAgentId)
                .then().log().all()
                .statusCode(200);

        log.info("--1------check items--1q-");
        given()
                .contentType("application/json")
                .auth().oauth2(deliveryAgentToken)
                .get("/vanSales")
                .then().log().all()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(deliveryAgentToken)
                .header("X-App-Version", "1.0.0")
                .patch("/deliveryAgents/markOrderAsDelivered/{orderPublicId}", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200);

        log.info("--1------check items--2q-");
        given()
                .contentType("application/json")
                .auth().oauth2(deliveryAgentToken)
                .get("/vanSales")
                .then().log().all()
                .statusCode(200);

        // fetch order check

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200)
                .body("salesOrderStatus", equalTo(LoadingStatusEnum.FAILED_DELIVERY.name()));


        given()
                .contentType("application/json")
                .auth().oauth2(deliveryAgentToken)
                .header("X-App-Version", "1.0.0")
                .queryParam("routeId", UUID.fromString(routePublicId))
                .queryParam("action", RouteAction.end)
                .patch("/deliveryAgents/routeActions")
                .then()
                .statusCode(200);


        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .get("/logistics/issuances")
                .then().log().all()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .get("/logistics/delivery-operations")
                .then().log().all()
                .statusCode(200);
    }


}
