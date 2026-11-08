package com.app.itibari.web;

import com.app.itibari.ItibariApplicationTests;
import com.app.itibari.dataTypes.*;
import com.app.itibari.models.entities.DeliveryAgents;
import com.app.itibari.models.entities.ProductItem;
import com.app.itibari.models.entities.SalesOrderItems;
import com.app.itibari.models.request.*;
import com.app.itibari.repository.DeliveryAgentsRepository;
import com.app.itibari.scheduler.Scheduler;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Slf4j
public class DiscountRestControllerTests extends ItibariApplicationTests {

    @Autowired
    private Scheduler scheduler;

    private static final String MOBILE_PASSWORD = "password123";

    @Autowired
    private DeliveryAgentsRepository deliveryAgentsRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Before
    public void resetSharedFixtures() {
        resetSeededDeliveryAgentAvailability();
        jdbcTemplate.update(
                "update itibari.products set available_stock = 100, reserved_stock = 0 "
                        + "where public_id in ("
                        + "'534cf5d4-f8c3-495c-a813-9a42f338c466',"
                        + "'581c5c73-afc0-4b89-bafb-e160011f2715',"
                        + "'7c4a6d52-5da2-47fe-92a8-ea2fce2602af')");
        jdbcTemplate.update(
                "update itibari.inventory_products ip set available_stock = 100, reserved_stock = 0 "
                        + "where ip.product_fk in ("
                        + "select p.id from itibari.products p where p.public_id in ("
                        + "'534cf5d4-f8c3-495c-a813-9a42f338c466',"
                        + "'581c5c73-afc0-4b89-bafb-e160011f2715',"
                        + "'7c4a6d52-5da2-47fe-92a8-ea2fce2602af'))");
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
    public void testCreateDiscountFails() {

        DiscountCreateRequest request = new DiscountCreateRequest();
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .post("/discounts")
                .then().log().all()
                .statusCode(400);
    }

    @Ignore
    @Test
    public void testDiscountSearches() {
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


        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        String startDate = LocalDate.parse("2025-11-12", formatter)
                .minusDays(2)
                .format(formatter);

        String endDate = LocalDate.parse("2025-11-13", formatter)
                .format(formatter);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("discountType", DiscountType.VolumeDiscount)
                .queryParam("status", DiscountStatus.Active)
                .queryParam("startDate", startDate)
                .queryParam("endDate", endDate)
                .get("/discounts")
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));

    }

    @Test
    @Ignore
    public void testCreateDiscountSuccess() {
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
                .then().log().all()
                .statusCode(200)
                .body("content.size()", equalTo(1));
    }

    @Ignore // works alone
    @Test
    public void testCreateDiscountWorks() {

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
                .body("content[0].discount", notNullValue())
                .body("content[0].hasDiscount", equalTo(true))
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
                .auth().oauth2(DELIVERY_AGENT_TOKEN)
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


        checkInventoryNegativeTests();


        //reconcile


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .patch("/warehouse/productRecons/{publicId}/reconcile", UUID.fromString(productReconId))
                .then()
                .statusCode(200);


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/warehouse/productRecons/{publicId}/items", UUID.fromString(productReconId))
                .then().log().all()
                .statusCode(200);


        // ANOTHER ORDER


        orderItems = SalesOrderItemsRequest
                .builder()
                .productId(productId)
                .quantity(2)
                .build();
        request = SalesOrdersRequest.builder()
                .customerId(UUID.fromString("d7c5bf34-fda1-46e2-b5ee-40db98aed06d"))
                .bookedBy("Itibari Test")
                .sourceChannel(SourceChannelEnum.BACK_OFFICE_APP.name())
                .salesOrderItems(Collections.singleton(orderItems))
                .build();

        String orderPublicId2 = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .post("/salesOrders")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        String orderItemPublicId2 = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders/{salesOrderPublicId}/items", UUID.fromString(orderPublicId2))
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].discount", notNullValue())
                .body("content[0].hasDiscount", equalTo(true))
                .extract().path("content[0].publicId");

        checkInventoryNegativeTests();

        scheduler.runOrderPlanning();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId2))
                .then()
                .statusCode(200)
                .body("salesOrderStatus", equalTo(LoadingStatusEnum.PLANNING.name()));


        checkInventoryNegativeTests();

        routePlanningRequest = new RoutePlanningRequest();
        routePlanningRequest.setLocationPublicId(UUID.fromString("90ad01f1-4a13-41f7-b825-c428b41f35d0"));
        routePlanningRequest.setRegionPublicId(UUID.fromString("8c5d4db0-f9a1-47ed-a160-56780e4e4d14"));
        routePlanningRequest.setVehiclePublicId(UUID.fromString("fa4bf2ee-6665-493a-8dbd-5d56dc8a7d93"));
        routePlanningRequest.setDeliveryAgentPublicId(UUID.fromString("af485765-69c5-4dd0-8fa0-560058b85c65"));
        routePlanningRequest.setWarehousePublicId(UUID.fromString("3849b1be-a9dd-4f11-abbf-229ad2780a56"));
        routePlanningRequest.setSalesOrdersPublicIds(Collections.singletonList(UUID.fromString(orderPublicId2)));


        String routePublicId2 = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(routePlanningRequest)
                .post("/logistics/routePlanning")
                .then()
                .statusCode(200)
                .extract().path("publicId");


        String productItemPublicId2 = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/logistics/routePlanning/{routePlanningPublicId}/loadingProducts", UUID.fromString(routePublicId2))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].productItemPublicId");


        checkInventoryNegativeTests();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/logistics/routePlanning/{routePlanningPublicId}", UUID.fromString(routePublicId2))
                .then()
                .statusCode(200)
                .body("routeStatus", equalTo(RouteStatus.CREATED.name()));

        loadingRequest = new LoadingRequest();
        loadingDTO = new LoadingDTO();
        loadingDTO.setLoadingType(LoadingType.OrderSale);
        loadingDTO.setProductItemPublicId(UUID.fromString(productItemPublicId2));
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
                .get("/logistics/routePlanning/{routePlanningPublicId}", UUID.fromString(routePublicId2))
                .then()
                .statusCode(200)
                .body("routeStatus", equalTo(RouteStatus.Loaded.name()));


        checkInventoryNegativeTests();

        given()
                .contentType("application/json")
                .auth().oauth2(DELIVERY_AGENT_TOKEN)
                .header("X-App-Version", "1.0.0")
                .queryParam("acceptType", AcceptTypeEnum.AcceptRoute)
                .patch("/deliveryAgents/accept/{routeId}", UUID.fromString(routePublicId2))
                .then()
                .statusCode(200);

        checkInventoryNegativeTests();


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/logistics/routePlanning/{routePlanningPublicId}", UUID.fromString(routePublicId2))
                .then()
                .statusCode(200)
                .body("routeStatus", equalTo(RouteStatus.ReadyToGo.name()));

        given()
                .contentType("application/json")
                .auth().oauth2(DELIVERY_AGENT_TOKEN)
                .header("X-App-Version", "1.0.0")
                .queryParam("routeId", UUID.fromString(routePublicId2))
                .queryParam("action", RouteAction.Start)
                .patch("/deliveryAgents/routeActions")
                .then()
                .statusCode(200);

        log.info("================================started delivery agent route action============================");


        String productReconId2 = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/warehouse/productRecons")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].publicId");


        String productReconItemId2 = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/warehouse/productRecons/{publicId}/items", UUID.fromString(productReconId2))
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].publicId");


        checkInventoryNegativeTests();


        // partial deliver products

        deliverItemRequest = new DeliverItemRequest();
        deliverItemRequest.setSalesOrderPublicId(UUID.fromString(orderPublicId2));
        deliverItemRequest.setRoutePublicId(UUID.fromString(routePublicId2));

        itemRequest = new ItemRequest();
        itemRequest.setProductItemPublicId(UUID.fromString(orderItemPublicId2));
        itemRequest.setProductReconItemPublicId(UUID.fromString(orderItemPublicId2));
        itemRequest.setQuantitySold(1);
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
                .patch("/deliveryAgents/markOrderAsDelivered/{orderPublicId}", UUID.fromString(orderPublicId2))
                .then()
                .statusCode(200);

        checkInventoryNegativeTests();


        // fetch order check

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId2))
                .then()
                .statusCode(200)
                .body("salesOrderStatus", equalTo(LoadingStatusEnum.DELIVERED.name()));

        given()
                .contentType("application/json")
                .auth().oauth2(DELIVERY_AGENT_TOKEN)
                .header("X-App-Version", "1.0.0")
                .queryParam("routeId", UUID.fromString(routePublicId2))
                .queryParam("action", RouteAction.end)
                .patch("/deliveryAgents/routeActions")
                .then()
                .statusCode(200);

        // return goods

        productItemRequest = new ProductItemRequest();
        productItemRequest.setProductItemPublicId(UUID.fromString(productReconItemId2));
        productItemRequest.setQuantityReturned(1);

        productReconAction = new ProductReconAction();
        productReconAction.setProductReconPublicId(UUID.fromString(productReconId2));
        productReconAction.setProductItems(Collections.singletonList(productItemRequest));


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(productReconAction)
                .patch("/warehouse/productRecons/{publicId}", UUID.fromString(productReconId2))
                .then()
                .statusCode(200);


        checkInventoryNegativeTests();


        orderItems = SalesOrderItemsRequest
                .builder()
                .productId(productId)
                .quantity(2)
                .build();
        request = SalesOrdersRequest.builder()
                .customerId(UUID.fromString("d7c5bf34-fda1-46e2-b5ee-40db98aed06d"))
                .bookedBy("Itibari Test")
                .sourceChannel(SourceChannelEnum.BACK_OFFICE_APP.name())
                .salesOrderItems(Collections.singleton(orderItems))
                .build();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .post("/salesOrders")
                .then().log().all()
                .statusCode(400);


        orderItems = SalesOrderItemsRequest
                .builder()
                .productId(productId)
                .quantity(1)
                .build();
        request = SalesOrdersRequest.builder()
                .customerId(UUID.fromString("d7c5bf34-fda1-46e2-b5ee-40db98aed06d"))
                .bookedBy("Itibari Test")
                .sourceChannel(SourceChannelEnum.BACK_OFFICE_APP.name())
                .salesOrderItems(Collections.singleton(orderItems))
                .build();


        String order3 = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .post("/salesOrders")
                .then()
                .statusCode(200)
                .extract().path("publicId");


        String orderItemPublicId3 = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders/{salesOrderPublicId}/items", UUID.fromString(order3))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].publicId");

        checkInventoryNegativeTests();

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
    @Ignore // works alone
    public void testCustomerCreateOrder() {
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
                .then().log().all()
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
                .then().log().all()
                .statusCode(200)
                .body("content.size()", equalTo(1));


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("discountType", DiscountType.VolumeDiscount)
                .get("/discounts")
                .then().log().all()
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
                .body(cartDiscountRequest)
                .log().all()
                .post("/salesOrders/cartDiscountCheck")
                .then().log().all()
                .statusCode(200);


        SalesOrderItemsRequest orderItems = SalesOrderItemsRequest
                .builder()
                .productId(productId)
                .quantity(5)
                .build();
        SalesOrdersRequest request = SalesOrdersRequest.builder()
                .customerId(UUID.fromString("d7c5bf34-fda1-46e2-b5ee-40db98aed06d"))
                .bookedBy("Itibari Test")
                .sourceChannel(SourceChannelEnum.CUSTOMER_CARE_APP.name())
                .salesOrderItems(Collections.singleton(orderItems))
                .build();

        String orderPublicId = given()
                .contentType("application/json")
                .auth().oauth2(CUSTOMER_TOKEN)
                .body(request)
                .post("/salesOrders/customer/placeOrder")
                .then().log().all()
                .statusCode(200)
                .body("totalDiscount", equalTo(3930.00f))
                .extract().path("publicId");


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .patch("/salesOrders/{salesOrderPublicId}/recalculateDiscount", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200);


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                .then().log().all()
                .body("totalDiscount", equalTo(3930.00f))
                .statusCode(200);
    }

    @Test
//    @Ignore  //works alone
    public void testCreateStaging_returnedGoods() throws Exception {
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
                .body("content[0].availableStock", greaterThanOrEqualTo(0))
                .body("content[0].reservedStock", greaterThanOrEqualTo(0))
                .body("content[0].totalStock", greaterThanOrEqualTo(0));

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .queryParam("productPublicId", productId)
                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].product.availableStock", greaterThanOrEqualTo(0));

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .get("/products/{publicId}", productId)
                .then()
                .statusCode(200)
                .body("availableStock", greaterThanOrEqualTo(0));

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

        VanSaleRequest vanSaleRequest = new VanSaleRequest();
        vanSaleRequest.setDeliveryAgentPublicId(UUID.fromString("af485765-69c5-4dd0-8fa0-560058b85c65"));

        ProductItemRequest productItemRequest = new ProductItemRequest();
        productItemRequest.setProductItemPublicId(productId);
        productItemRequest.setQuantity(2);

        vanSaleRequest.setProductItemRequestList(List.of(productItemRequest));

//        given()
//                .contentType("application/json")
//                .auth().oauth2(SUPER_ADMIN_TOKEN)
//                .body(vanSaleRequest)
//                .post("/vanSales")
//                .then()
//                .statusCode(200);

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
        itemRequest2.setQuantitySold(3);
        itemRequest2.setProductItemPublicId(UUID.fromString(orderItemPublicId));

        amendOrderRequest.setItems(Arrays.asList(itemRequest2));

        given()
                .contentType("application/json")
                .auth().oauth2(deliveryAgentToken)
                .body(amendOrderRequest).log().all()
                .patch("/deliveryAgents/{deliveryAgentPublicId}/amendOrder", deliveryAgentId)
                .then()
                .statusCode(200);

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

        // partial deliver products

        DeliverItemRequest deliverItemRequest = new DeliverItemRequest();
        deliverItemRequest.setSalesOrderPublicId(UUID.fromString(orderPublicId));
        deliverItemRequest.setRoutePublicId(UUID.fromString(routePublicId));

        ItemRequest itemRequest = new ItemRequest();
        itemRequest.setProductItemPublicId(UUID.fromString(orderItemPublicId));
        itemRequest.setQuantitySold(3);

        ItemRequest item1 = new ItemRequest();
        item1.setProductItemPublicId(UUID.fromString(orderItemPublicId2));
        item1.setQuantitySold(0);
        deliverItemRequest.setItems(Arrays.asList(itemRequest, item1));


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
                .auth().oauth2(SUPER_ADMIN_TOKEN)
                .queryParam("deliveryId", deliveryId)
                .get("/warehouse/agent/{agentPublicId}/items", deliveryAgentId)
                .then().log().all()
                .statusCode(200);

        log.info("--1------check items--1-");
        given()
                .contentType("application/json")
                .auth().oauth2(deliveryAgentToken)
                .get("/vanSales")
                .then().log().all()
                .statusCode(200);

        DeliverVanSaleItemRequest deliverVanSaleItemRequest = new DeliverVanSaleItemRequest();
        deliverVanSaleItemRequest.setRoutePublicId(UUID.fromString(routePublicId));
        String uniqueVanSalePhone = "07" + String.valueOf(System.currentTimeMillis()).substring(3, 11);
        CustomersRequest customersRequest = CustomersRequest.builder()
                .phoneNumber(uniqueVanSalePhone)
                .firstName("sellah sella")
                .organizationLocationId(UUID.fromString("90ad01f1-4a13-41f7-b825-c428b41f35d0"))
                .organizationRegionId(UUID.fromString("8c5d4db0-f9a1-47ed-a160-56780e4e4d14"))
                .build();

        ItemRequest itemRequest1 = new ItemRequest();
        itemRequest1.setProductItemPublicId(UUID.fromString(productReconItemId));
        itemRequest1.setQuantitySold(2);

        ItemRequest itemRequest12 = new ItemRequest();
        itemRequest12.setProductItemPublicId(UUID.fromString(productReconItemId2));
        itemRequest12.setQuantitySold(6);

        deliverVanSaleItemRequest.setCustomersRequest(customersRequest);
        deliverVanSaleItemRequest.setItems(List.of(itemRequest1, itemRequest12));

        given()
                .contentType("application/json")
                .auth().oauth2(deliveryAgentToken)
                .header("X-App-Version", "1.0.0")
                .body(deliverVanSaleItemRequest)
                .patch("/deliveryAgents/markVanSaleItemAsSold")
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

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .queryParam("productPublicId", productId)
                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
                .then()
                .statusCode(200);
//                .body("content[0].availableStock", equalTo(43))
//                .body("content[0].reservedStock", equalTo(0))
//                .body("content[0].stockValue", equalTo(1393237.9F))
//                .body("content[0].totalStock", equalTo(43));

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .queryParam("productPublicId", productId)
                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));
//                .body("content[0].product.availableStock", equalTo(8));

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .get("/products/{publicId}", productId)
                .then()
                .statusCode(200);
//                .body("availableStock", equalTo(8));

        // fetch order check

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
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

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .get("/salesOrders/{salesOrderPublicId}/items", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
//                .body("content[0].quantity", equalTo(5))
                .extract().path("content[0].publicId");

        String recoPublicId = given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .queryParam("deliveryId", deliveryId)
                .get("/warehouse/productRecons")
                .then().log().all()
                .statusCode(200)
                .extract().path("content[0].publicId");

        List<ProductItem> recoItemPublicIds = given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .queryParam("deliveryId", deliveryId)
                .get("/warehouse/productRecons/{publicId}/items", UUID.fromString(recoPublicId))
                .then().log().all()
                .statusCode(200)
                .extract().jsonPath().getList("content", ProductItem.class);

        Map<UUID, String> itemList = recoItemPublicIds.stream()
                .collect(Collectors.toMap(
                        ProductItem::getPublicId,
                        item -> item.getProductCode()
                ));

        String recoItemPublicId = null;
        String recoItemPublicId2 = null;

        for (Map.Entry<UUID, String> entry : itemList.entrySet()) {
            UUID publicId = entry.getKey();
            String sku = entry.getValue();
            if (sku.equals("SKU100021")) {
                recoItemPublicId = publicId.toString();
            }
            if (sku.equals("SKU100022")) {
                recoItemPublicId2 = publicId.toString();
            }
        }


        ProductReconAction productReconAction = new ProductReconAction();
        productReconAction.setProductReconPublicId(UUID.fromString(recoPublicId));

//        productItemRequest = new ProductItemRequest();
//        productItemRequest.setQuantityReturned(1);
//        productItemRequest.setProductItemPublicId(UUID.fromString(recoItemPublicId));

        ProductItemRequest productItemRequest1 = new ProductItemRequest();
        productItemRequest1.setQuantityReturned(4);
        productItemRequest1.setProductItemPublicId(UUID.fromString(recoItemPublicId2));
//        productItemRequest.setQuantityMissing(1);
//        productItemRequest.setMissingRejectionPublicId(UUID.fromString("d88a0e04-23b7-4145-93ba-48b31efacf8d"));
        productReconAction.setProductItems(Arrays.asList(productItemRequest1));

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(adminToken)
                .body(productReconAction)
                .patch("/warehouse/productRecons/{publicId}", UUID.fromString(recoPublicId))
                .then()
                .statusCode(200);

        String stagedItemPublicId = given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .queryParam("searchParam", "Erick")
//                .queryParam("startDate", LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
//                .queryParam("endDate", LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .get("/warehouse/stagedForApproval")
                .then().log().all()
                .statusCode(200)
                .extract().path("content[0].publicId");

        Response response = given()
                .auth().oauth2(adminToken)
                .get("/reports/warehouse/stagedForApproval")
                .then()
                .statusCode(200)
                .extract().response();

//        saveExport(response, "Staging_Report.xlsx");

        String itemPublicId = given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .get("/warehouse/stagedForApproval/{publicId}/items", UUID.fromString(stagedItemPublicId))
                .then().log().all()
                .statusCode(200)
//                .body("content[0].quantityReturned", equalTo(2))
                .extract().path("content[0].publicId");


        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .queryParam("productPublicId", productId)
                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
                .then()
                .statusCode(200);
//                .body("content[0].availableStock", equalTo(43))
//                .body("content[0].reservedStock", equalTo(0))
//                .body("content[0].totalStock", equalTo(43));

        // Edit item quantity
        StagingActionRequest stagingActionRequest = StagingActionRequest.builder()
                .actionType(StagingActionRequest.ActionType.Edit_Item)
                .quantity(2)  //  5  sold 3 return 1 return 1   .... return ... edit to 2..meaning 1 missing bado iko
                .comment("approved")
                .build();
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(adminToken)
                .body(stagingActionRequest).log().all()
                .queryParam("itemPublicId", UUID.fromString(itemPublicId))
                .patch("/warehouse/stagedForApproval/{publicId}", UUID.fromString(stagedItemPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .get("/warehouse/stagedForApproval/{publicId}/items", UUID.fromString(stagedItemPublicId))
                .then()
                .statusCode(200)
                .body("content[0].quantityReturned", equalTo(2));

        stagingActionRequest = StagingActionRequest.builder().actionType(StagingActionRequest.ActionType.Approve).comment("approved").build();
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(adminToken)
                .body(stagingActionRequest).log().all()
                .patch("/warehouse/stagedForApproval/{publicId}", UUID.fromString(stagedItemPublicId))
                .then()
                .statusCode(200);

        // ONE ITEM RETURNED ONLY
        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .queryParam("productPublicId", productId)
                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
                .then()
                .statusCode(200);
//                .body("content[0].availableStock", equalTo(45))
//                .body("content[0].reservedStock", equalTo(0))
//                .body("content[0].totalStock", equalTo(45));

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .queryParam("productPublicId", productId)
                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));
//                .body("content[0].product.availableStock", equalTo(10));

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .get("/products/{publicId}", productId)
                .then()
                .statusCode(200);
//                .body("availableStock", equalTo(10));

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .queryParam("deliveryId", deliveryId)
                .get("/warehouse/productRecons")
                .then()
                .statusCode(200)
                .body("content[0].status", equalToIgnoringCase("AwaitingApproval"));

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .queryParam("deliveryId", deliveryId)
                .get("/warehouse/productRecons/{publicId}/items", UUID.fromString(recoPublicId))
                .then()
                .statusCode(200)
                .extract().path("content[0].publicId");

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
//    @Ignore  //works alone
    public void testCancelOrderWith2Products() throws Exception {
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
                .body("content[0].availableStock", greaterThanOrEqualTo(0))
                .body("content[0].reservedStock", greaterThanOrEqualTo(0))
                .body("content[0].totalStock", greaterThanOrEqualTo(0));

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .queryParam("productPublicId", productId)
                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].product.availableStock", greaterThanOrEqualTo(0));

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .get("/products/{publicId}", productId)
                .then()
                .statusCode(200)
                .body("availableStock", greaterThanOrEqualTo(0));

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

        log.info("---qw-----check items---");
        given()
                .contentType("application/json")
                .auth().oauth2(deliveryAgentToken)
                .get("/vanSales")
                .then().log().all()
                .statusCode(200);

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
                .body("content[0].availableStock", greaterThanOrEqualTo(0))
                .body("content[0].reservedStock", greaterThanOrEqualTo(0))
                .body("content[0].totalStock", greaterThanOrEqualTo(0));

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .queryParam("productPublicId", productId)
                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].product.availableStock", greaterThanOrEqualTo(0));

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken)
                .get("/products/{publicId}", productId)
                .then()
                .statusCode(200)
                .body("availableStock", greaterThanOrEqualTo(0));

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
        itemRequest2.setQuantitySold(3);
        itemRequest2.setProductItemPublicId(UUID.fromString(orderItemPublicId));

        amendOrderRequest.setItems(Arrays.asList(itemRequest2));

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

        // partial deliver products

        DeliverItemRequest deliverItemRequest = new DeliverItemRequest();
        deliverItemRequest.setSalesOrderPublicId(UUID.fromString(orderPublicId));
        deliverItemRequest.setRoutePublicId(UUID.fromString(routePublicId));

        ItemRequest itemRequest = new ItemRequest();
        itemRequest.setProductItemPublicId(UUID.fromString(orderItemPublicId));
        itemRequest.setQuantitySold(3);

        ItemRequest item1 = new ItemRequest();
        item1.setProductItemPublicId(UUID.fromString(orderItemPublicId2));
        item1.setQuantitySold(0);
        deliverItemRequest.setItems(Arrays.asList(itemRequest, item1));


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
