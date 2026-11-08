package com.app.itibari.web;

import com.app.itibari.ItibariApplicationTests;
import com.app.itibari.dataTypes.*;
import com.app.itibari.models.entities.Counter;
import com.app.itibari.models.entities.DeliveryAgentLoginSession;
import com.app.itibari.models.entities.DeliveryAgents;
import com.app.itibari.models.request.*;
import com.app.itibari.repository.CounterRepository;
import com.app.itibari.repository.DeliveryAgentLoginSessionRepository;
import com.app.itibari.repository.DeliveryAgentsRepository;
import io.restassured.http.ContentType;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

public class DeliveryAgentsControllerTests extends ItibariApplicationTests {

    private static final UUID TEST_PRODUCT_PUBLIC_ID = UUID.fromString("08ebb084-2633-428f-82b9-e8d98a22ec53");
    private static final UUID TEST_CUSTOMER_PUBLIC_ID = UUID.fromString("afbcaadc-45ff-4f6d-8267-e811b9dfc434");
    private static final UUID TEST_LOCATION_PUBLIC_ID = UUID.fromString("90ad01f1-4a13-41f7-b825-c428b41f35d0");
    private static final UUID TEST_REGION_PUBLIC_ID = UUID.fromString("8c5d4db0-f9a1-47ed-a160-56780e4e4d14");
    private static final UUID TEST_VEHICLE_PUBLIC_ID = UUID.fromString("fa4bf2ee-6665-493a-8dbd-5d56dc8a7d93");
    private static final UUID TEST_DELIVERY_AGENT_PUBLIC_ID = UUID.fromString("af485765-69c5-4dd0-8fa0-560058b85c65");
    private static final UUID TEST_WAREHOUSE_PUBLIC_ID = UUID.fromString("3849b1be-a9dd-4f11-abbf-229ad2780a56");
    private static final String TEST_DELIVERY_AGENT_PHONE = "+254712345678";
    private static final String TEST_DELIVERY_AGENT_PASSWORD = "password123";
    private static final String TEST_ADMIN_USERNAME = "itibari@gmail.com";
    private static final String TEST_ADMIN_PASSWORD = "password";
    private static final int TEST_SALES_ORDER_COUNTER_FLOOR = 200000;

    @Autowired
    private DeliveryAgentsRepository deliveryAgentsRepository;

    @Autowired
    private DeliveryAgentLoginSessionRepository loginSessionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CounterRepository counterRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Before
    public void prepareDeliveryAgentControllerTestData() {
        grantDeliveryAgentPermissionsToAdminUser();
        resetDeliveryFlowFixture();
        ensureSalesOrderCounterIsAboveSeededOrders();
        ensureTestProductStockIsAvailable();
    }

    public String fetchDeliveryAgents() {
        return given()
                .contentType("application/json")
                .auth().oauth2(adminToken())
                .get("/deliveryAgents")
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].publicId");
    }

    @Test
    public void testSubmitOrder_fullDelivery_marksOrderDeliveredAndRegistersEvent() {
        DeliveryFlow flow = createStartedDeliveryFlow(1);

        markOrderItemsAsDelivered(flow, 1)
                .then()
                .statusCode(200);

        submitOrderAsDelivered(flow)
                .then()
                .statusCode(200);

        assertOrderDeliveryState(flow.orderPublicId, LoadingStatusEnum.DELIVERED, OrderStatusEnum.Delivered);
        assertOrderDeliveredEventRegistered(flow.orderPublicId);
    }

    @Test
    public void testSubmitOrder_partialDelivery_marksHeaderDeliveredAndBusinessStatusPartial() {
        DeliveryFlow flow = createStartedDeliveryFlow(2);

        markOrderItemsAsDelivered(flow, 1)
                .then()
                .statusCode(200);

        submitOrderAsDelivered(flow)
                .then()
                .statusCode(200);

        assertOrderDeliveryState(flow.orderPublicId, LoadingStatusEnum.DELIVERED, OrderStatusEnum.Partially_Delivered);
        assertOrderDeliveredEventRegistered(flow.orderPublicId);
    }

    @Test
    public void testSubmitOrder_withoutDeliveredItems_stillPersistsDeliveredEventAndPartialBusinessStatus() {
        DeliveryFlow flow = createStartedDeliveryFlow(2);

        submitOrderAsDelivered(flow)
                .then()
                .statusCode(200);

        assertOrderDeliveryState(flow.orderPublicId, LoadingStatusEnum.DELIVERED, OrderStatusEnum.Partially_Delivered);
        assertOrderDeliveredEventRegistered(flow.orderPublicId);
    }

    @Test
    public void testSubmitOrder_rejectsOverDeliveryAndKeepsOrderInTransitWithoutEvent() {
        DeliveryFlow flow = createStartedDeliveryFlow(1);

        markOrderItemsAsDelivered(flow, 2)
                .then()
                .statusCode(400);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(adminToken())
                .get("/salesOrders/{salesOrderPublicId}", flow.orderPublicId)
                .then()
                .statusCode(200)
                .body("salesOrderStatus", equalTo(LoadingStatusEnum.IN_TRANSIT.name()));

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(adminToken())
                .get("/events/{publicId}", flow.orderPublicId)
                .then()
                .statusCode(200)
                .body("content.eventType", not(hasItem("Order Delivered")));
    }

    @Test
    public void testSubmitOrder_rejectsUnknownOrder() {
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(deliveryAgentToken())
                .header("X-App-Version", "1.0.0")
                .patch("/deliveryAgents/markOrderAsDelivered/{orderPublicId}", UUID.randomUUID())
                .then()
                .statusCode(400);
    }

    @Test
    public void testSubmitOrder_cashToCreditConversionRequiresReasonAndDoesNotRegisterDeliveredEvent() {
        DeliveryFlow flow = createStartedDeliveryFlow(1);
        MarkOrderDeliveredRequest request = new MarkOrderDeliveredRequest();
        request.setConvertCashToCredit(true);

        markOrderItemsAsDelivered(flow, 1)
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(deliveryAgentToken())
                .header("X-App-Version", "1.0.0")
                .queryParam("routePublicId", flow.routePublicId)
                .body(request)
                .patch("/deliveryAgents/markOrderAsDelivered/{orderPublicId}", flow.orderPublicId)
                .then()
                .statusCode(400);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(adminToken())
                .get("/salesOrders/{salesOrderPublicId}", flow.orderPublicId)
                .then()
                .statusCode(200)
                .body("salesOrderStatus", equalTo(LoadingStatusEnum.IN_TRANSIT.name()));

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(adminToken())
                .get("/events/{publicId}", flow.orderPublicId)
                .then()
                .statusCode(200)
                .body("content.eventType", not(hasItem("Order Delivered")));
    }

    @Ignore
    @Test
    public void testAmendOrder() {
        String tokenFound = tokenGen();

        UUID productId = UUID.fromString("534cf5d4-f8c3-495c-a813-9a42f338c466");
        UUID reasonId = UUID.fromString("632b6fda-e23c-4a5f-bec0-a0f9e1f0e293");

        SalesOrderItemsRequest orderItems = SalesOrderItemsRequest
                .builder()
                .productId(productId)
                .quantity(5)
                .build();
        SalesOrdersRequest request = SalesOrdersRequest.builder()
                .customerId(UUID.fromString("d7c5bf34-fda1-46e2-b5ee-40db98aed06d"))
                .bookedBy("Itibari Test")
                .sourceChannel(SourceChannelEnum.CUSTOMER_CARE_APP.name())
                .salesOrderItems(Arrays.asList(orderItems).stream().collect(Collectors.toSet()))
                .build();

        String orderPublicId =
                given()
                        .contentType("application/json")
                        .auth().oauth2(tokenFound)
                        .body(request)
                        .post("/salesOrders/customer/placeOrder")
                        .then()
                        .statusCode(200)
                        .extract().path("publicId");

        RoutePlanningRequest routePlanningRequest = new RoutePlanningRequest();
        routePlanningRequest.setLocationPublicId(UUID.fromString("90ad01f1-4a13-41f7-b825-c428b41f35d0"));
        routePlanningRequest.setRegionPublicId(UUID.fromString("8c5d4db0-f9a1-47ed-a160-56780e4e4d14"));
        routePlanningRequest.setVehiclePublicId(UUID.fromString("fa4bf2ee-6665-493a-8dbd-5d56dc8a7d93"));
        routePlanningRequest.setDeliveryAgentPublicId(UUID.fromString("af485765-69c5-4dd0-8fa0-560058b85c65"));
        routePlanningRequest.setWarehousePublicId(UUID.fromString("3849b1be-a9dd-4f11-abbf-229ad2780a56"));
        routePlanningRequest.setSalesOrdersPublicIds(Collections.singletonList(UUID.fromString(orderPublicId)));

        String routePublicId = given()
                .contentType("application/json")
                .auth().oauth2(adminToken())
                .body(routePlanningRequest)
                .post("/logistics/routePlanning")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        String productItemPublicId = given()
                .contentType("application/json")
                .auth().oauth2(adminToken())
                .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200)
                .extract().path("salesOrderItems[0].publicId");

        AmendOrderRequest amendOrderRequest = new AmendOrderRequest();
        amendOrderRequest.setSalesOrderPublicId(UUID.fromString(orderPublicId));
        amendOrderRequest.setRoutePublicId(UUID.fromString(routePublicId));
        amendOrderRequest.setReasonPublicId(reasonId);
        amendOrderRequest.setDescription("description amend order");
        amendOrderRequest.setAmendAction(AmendAction.CancelAndRequestVanSale);

        ItemRequest itemRequest = new ItemRequest();
        itemRequest.setQuantitySold(2);
        itemRequest.setProductItemPublicId(UUID.fromString(productItemPublicId));

        amendOrderRequest.setItems(Arrays.asList(itemRequest));

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken())
                .body(amendOrderRequest)
                .patch("/deliveryAgents/{deliveryAgentPublicId}/amendOrder", UUID.fromString(fetchDeliveryAgents()))
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken())
                .get("/deliveryAgents/orderAmendments")
                .then().log().all()
                .statusCode(200);
    }

    @Ignore
    @Test
    public void testFullyAmendSalesOrder() {
        String tokenFound = tokenGen();

        UUID productId = UUID.fromString("534cf5d4-f8c3-495c-a813-9a42f338c466");
        UUID reasonId = UUID.fromString("632b6fda-e23c-4a5f-bec0-a0f9e1f0e293");

        SalesOrderItemsRequest orderItems = SalesOrderItemsRequest
                .builder()
                .productId(productId)
                .quantity(5)
                .build();
        SalesOrdersRequest request = SalesOrdersRequest.builder()
                .customerId(UUID.fromString("d7c5bf34-fda1-46e2-b5ee-40db98aed06d"))
                .bookedBy("Itibari Test")
                .sourceChannel(SourceChannelEnum.CUSTOMER_CARE_APP.name())
                .salesOrderItems(Arrays.asList(orderItems).stream().collect(Collectors.toSet()))
                .build();

        String orderPublicId =
                given()
                        .contentType("application/json")
                        .auth().oauth2(tokenFound)
                        .body(request)
                        .post("/salesOrders/customer/placeOrder")
                        .then()
                        .statusCode(200)
                        .extract().path("publicId");

        RoutePlanningRequest routePlanningRequest = new RoutePlanningRequest();
        routePlanningRequest.setLocationPublicId(UUID.fromString("90ad01f1-4a13-41f7-b825-c428b41f35d0"));
        routePlanningRequest.setRegionPublicId(UUID.fromString("8c5d4db0-f9a1-47ed-a160-56780e4e4d14"));
        routePlanningRequest.setVehiclePublicId(UUID.fromString("fa4bf2ee-6665-493a-8dbd-5d56dc8a7d93"));
        routePlanningRequest.setDeliveryAgentPublicId(UUID.fromString("af485765-69c5-4dd0-8fa0-560058b85c65"));
        routePlanningRequest.setWarehousePublicId(UUID.fromString("3849b1be-a9dd-4f11-abbf-229ad2780a56"));
        routePlanningRequest.setSalesOrdersPublicIds(Collections.singletonList(UUID.fromString(orderPublicId)));

        String routePublicId = given()
                .contentType("application/json")
                .auth().oauth2(adminToken())
                .body(routePlanningRequest)
                .post("/logistics/routePlanning")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        String productItemPublicId = given()
                .contentType("application/json")
                .auth().oauth2(adminToken())
                .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200)
                .extract().path("salesOrderItems[0].publicId");

        AmendOrderRequest amendOrderRequest = new AmendOrderRequest();
        amendOrderRequest.setSalesOrderPublicId(UUID.fromString(orderPublicId));
        amendOrderRequest.setRoutePublicId(UUID.fromString(routePublicId));
        amendOrderRequest.setReasonPublicId(reasonId);
        amendOrderRequest.setDescription("description amend order");
        amendOrderRequest.setAmendAction(AmendAction.CancelAndRequestVanSale);

        ItemRequest itemRequest = new ItemRequest();
        itemRequest.setQuantitySold(3);
        itemRequest.setProductItemPublicId(UUID.fromString(productItemPublicId));

        amendOrderRequest.setItems(Arrays.asList(itemRequest));
//        amendOrderRequest.setItems(Collections.EMPTY_LIST);

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken())
                .body(amendOrderRequest)
                .patch("/deliveryAgents/{deliveryAgentPublicId}/amendOrder", UUID.fromString(fetchDeliveryAgents()))
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(adminToken())
                .queryParam("searchParam", "RT-100000")
                .get("/deliveryAgents/orderAmendments")
                .then().log().all()
                .statusCode(200);
    }

    @Test
    public void testGetLocationApprovals(){
        given()
                .contentType("application/json")
                .auth().oauth2(adminToken())
                .get("/deliveryAgents/locationApprovals")
                .then().log().all()
                .statusCode(200);
    }

    @Test
    public void test_list_DeliveryAgents_ok() {
        given()
                .contentType("application/json")
                .auth().oauth2(adminToken())
                .queryParam("searchParam", "Dennis")
                .get("/deliveryAgents/minifiedData")
                .then().log().all()
                .statusCode(200);
    }

    private DeliveryFlow createStartedDeliveryFlow(int orderedQuantity) {
        SalesOrderItemsRequest orderItems = SalesOrderItemsRequest
                .builder()
                .productId(TEST_PRODUCT_PUBLIC_ID)
                .quantity(orderedQuantity)
                .build();
        SalesOrdersRequest request = SalesOrdersRequest.builder()
                .customerId(TEST_CUSTOMER_PUBLIC_ID)
                .bookedBy("Itibari Delivery Submit Test")
                .sourceChannel(SourceChannelEnum.BACK_OFFICE_APP.name())
                .salesOrderItems(new HashSet<>(Collections.singletonList(orderItems)))
                .build();

        UUID orderPublicId = UUID.fromString(given()
                .contentType(ContentType.JSON)
                .auth().oauth2(adminToken())
                .body(request)
                .post("/salesOrders")
                .then()
                .statusCode(200)
                .extract().path("publicId"));

        UUID orderItemPublicId = UUID.fromString(given()
                .contentType(ContentType.JSON)
                .auth().oauth2(adminToken())
                .get("/salesOrders/{salesOrderPublicId}/items", orderPublicId)
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].publicId"));

        given()
                .contentType(ContentType.JSON)
                .post("/scheduledTasks/updateOrdersPlannings")
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(adminToken())
                .get("/salesOrders/{salesOrderPublicId}", orderPublicId)
                .then()
                .statusCode(200)
                .body("salesOrderStatus", equalTo(LoadingStatusEnum.PLANNING.name()));

        RoutePlanningRequest routePlanningRequest = new RoutePlanningRequest();
        routePlanningRequest.setLocationPublicId(TEST_LOCATION_PUBLIC_ID);
        routePlanningRequest.setRegionPublicId(TEST_REGION_PUBLIC_ID);
        routePlanningRequest.setVehiclePublicId(TEST_VEHICLE_PUBLIC_ID);
        routePlanningRequest.setDeliveryAgentPublicId(TEST_DELIVERY_AGENT_PUBLIC_ID);
        routePlanningRequest.setWarehousePublicId(TEST_WAREHOUSE_PUBLIC_ID);
        routePlanningRequest.setSalesOrdersPublicIds(Collections.singletonList(orderPublicId));

        UUID routePublicId = UUID.fromString(given()
                .contentType(ContentType.JSON)
                .auth().oauth2(adminToken())
                .body(routePlanningRequest)
                .post("/logistics/routePlanning")
                .then()
                .statusCode(200)
                .extract().path("publicId"));

        UUID productItemPublicId = UUID.fromString(given()
                .contentType(ContentType.JSON)
                .auth().oauth2(adminToken())
                .get("/logistics/routePlanning/{routePlanningPublicId}/loadingProducts", routePublicId)
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].productItemPublicId"));

        LoadingDTO loadingDTO = new LoadingDTO();
        loadingDTO.setLoadingType(LoadingType.OrderSale);
        loadingDTO.setProductItemPublicId(productItemPublicId);

        LoadingRequest loadingRequest = new LoadingRequest();
        loadingRequest.setLoadingDTOList(Collections.singletonList(loadingDTO));

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(adminToken())
                .body(loadingRequest)
                .post("warehouse/agent/{agentPublicId}/loading", TEST_DELIVERY_AGENT_PUBLIC_ID)
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(deliveryAgentToken())
                .header("X-App-Version", "1.0.0")
                .queryParam("acceptType", AcceptTypeEnum.AcceptRoute)
                .patch("/deliveryAgents/accept/{routeId}", routePublicId)
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(deliveryAgentToken())
                .header("X-App-Version", "1.0.0")
                .queryParam("routeId", routePublicId)
                .queryParam("action", RouteAction.Start)
                .patch("/deliveryAgents/routeActions")
                .then()
                .statusCode(200);

        return new DeliveryFlow(orderPublicId, routePublicId, orderItemPublicId);
    }

    private io.restassured.response.Response markOrderItemsAsDelivered(DeliveryFlow flow, int deliveredQuantity) {
        ItemRequest itemRequest = new ItemRequest();
        itemRequest.setProductItemPublicId(flow.orderItemPublicId);
        itemRequest.setProductReconItemPublicId(flow.orderItemPublicId);
        itemRequest.setQuantitySold(deliveredQuantity);

        DeliverItemRequest deliverItemRequest = new DeliverItemRequest();
        deliverItemRequest.setSalesOrderPublicId(flow.orderPublicId);
        deliverItemRequest.setRoutePublicId(flow.routePublicId);
        deliverItemRequest.setItems(Collections.singletonList(itemRequest));

        return given()
                .contentType(ContentType.JSON)
                .auth().oauth2(deliveryAgentToken())
                .header("X-App-Version", "1.0.0")
                .body(deliverItemRequest)
                .patch("/deliveryAgents/markOrderItemAsDelivered")
                .then()
                .extract().response();
    }

    private io.restassured.response.Response submitOrderAsDelivered(DeliveryFlow flow) {
        return given()
                .contentType(ContentType.JSON)
                .auth().oauth2(deliveryAgentToken())
                .header("X-App-Version", "1.0.0")
                .queryParam("routePublicId", flow.routePublicId)
                .patch("/deliveryAgents/markOrderAsDelivered/{orderPublicId}", flow.orderPublicId)
                .then()
                .extract().response();
    }

    private String adminToken() {
        grantDeliveryAgentPermissionsToAdminUser();

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(TEST_ADMIN_USERNAME);
        loginRequest.setPassword(TEST_ADMIN_PASSWORD);

        return given()
                .contentType(ContentType.JSON)
                .body(loginRequest)
                .post("/authentication/token")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .extract().path("token");
    }

    private void grantDeliveryAgentPermissionsToAdminUser() {
        Long adminId = jdbcTemplate.queryForObject(
                "select id from itibari.person where email = ? and deleted = false",
                Long.class,
                TEST_ADMIN_USERNAME);
        Long adminRoleId = jdbcTemplate.queryForObject(
                "select id from itibari.roles where name = ? and deleted = false",
                Long.class,
                "ROLE_ADMIN");
        Long deliveryAgentRoleId = jdbcTemplate.queryForObject(
                "select id from itibari.roles where name = ? and deleted = false",
                Long.class,
                "ROLE_DELIVERY_AGENT");

        jdbcTemplate.update(
                "delete from itibari.person_roles where person_id = ? and role_id = ?",
                adminId,
                deliveryAgentRoleId);

        List<Long> deliveryAgentPermissionIds = jdbcTemplate.queryForList(
                "select permission_id from itibari.role_permissions where role_id = ?",
                Long.class,
                deliveryAgentRoleId);
        List<Long> logisticsPermissionIds = List.of(ensurePermissionExists("CREATE_ROUTE_PLANNING"));

        List<Long> adminPermissionIds = new java.util.ArrayList<>();
        adminPermissionIds.addAll(deliveryAgentPermissionIds);
        adminPermissionIds.addAll(logisticsPermissionIds);

        for (Long permissionId : adminPermissionIds) {
            Integer assignedCount = jdbcTemplate.queryForObject(
                    "select count(*) from itibari.role_permissions where role_id = ? and permission_id = ?",
                    Integer.class,
                    adminRoleId,
                    permissionId);

            if (assignedCount == null || assignedCount == 0) {
                jdbcTemplate.update(
                        "insert into itibari.role_permissions (role_id, permission_id) values (?, ?)",
                        adminRoleId,
                        permissionId);
            }
        }
    }

    private Long ensurePermissionExists(String permissionName) {
        List<Long> permissionIds = jdbcTemplate.queryForList(
                "select id from itibari.permission where name = ? and deleted = false",
                Long.class,
                permissionName);

        if (!permissionIds.isEmpty()) {
            return permissionIds.get(0);
        }

        jdbcTemplate.update(
                "insert into itibari.permission (public_id, deleted, name, supplier_role) values (?, false, ?, false)",
                UUID.randomUUID(),
                permissionName);

        return jdbcTemplate.queryForObject(
                "select id from itibari.permission where name = ? and deleted = false",
                Long.class,
                permissionName);
    }

    private void ensureSalesOrderCounterIsAboveSeededOrders() {
        Counter counter = counterRepository.findByCounterType(CounterType.SALES_ORDER)
                .orElseGet(() -> new Counter(CounterType.SALES_ORDER));

        if (counter.getCurrent() == null || counter.getCurrent() < TEST_SALES_ORDER_COUNTER_FLOOR) {
            counter.setCurrent(TEST_SALES_ORDER_COUNTER_FLOOR);
            counterRepository.saveAndFlush(counter);
        }
    }

    private void resetDeliveryFlowFixture() {
        jdbcTemplate.update("""
                        update itibari.route_planning
                        set deleted = true
                        where delivery_agent_id = ? or vehicle_id = ?
                        """,
                TEST_DELIVERY_AGENT_PUBLIC_ID.toString(),
                TEST_VEHICLE_PUBLIC_ID.toString());
        jdbcTemplate.update("""
                        update itibari.delivery_agents
                        set available = true, loaded = false
                        where public_id = ?
                        """,
                TEST_DELIVERY_AGENT_PUBLIC_ID);
        jdbcTemplate.update("""
                        update itibari.vehicles
                        set available = true
                        where public_id = ?
                        """,
                TEST_VEHICLE_PUBLIC_ID);
    }

    private void ensureTestProductStockIsAvailable() {
        jdbcTemplate.update("""
                        update itibari.live_stock_value
                        set deleted = true
                        where warehouse_public_id = ?
                          and inventory_item_public_id in (
                              select cast(ip.public_id as varchar)
                              from itibari.inventory_products ip
                              join itibari.products p on p.id = ip.product_fk
                              where p.public_id = ?
                          )
                        """,
                TEST_WAREHOUSE_PUBLIC_ID.toString(),
                TEST_PRODUCT_PUBLIC_ID);
        jdbcTemplate.update("""
                        update itibari.products
                        set available_stock = 100, reserved_stock = 0
                        where public_id = ?
                        """,
                TEST_PRODUCT_PUBLIC_ID);
        jdbcTemplate.update("""
                        update itibari.inventory_products
                        set available_stock = 100, reserved_stock = 0
                        where product_fk = (select id from itibari.products where public_id = ?)
                        """,
                TEST_PRODUCT_PUBLIC_ID);
        jdbcTemplate.update("""
                        update itibari.product_stocks
                        set available_stock = 100, reserved_stock = 0, total_stock = 100
                        where product_public_id = ?
                        """,
                TEST_PRODUCT_PUBLIC_ID);
    }

    private String deliveryAgentToken() {
        DeliveryAgents deliveryAgent = deliveryAgentsRepository
                .findDeliveryAgentsByPhoneNumberAndDeletedIsFalse(TEST_DELIVERY_AGENT_PHONE)
                .orElseThrow();

        List<DeliveryAgentLoginSession> sessions = loginSessionRepository.findAll().stream()
                .filter(session -> session.getDeliveryAgent().getId().equals(deliveryAgent.getId()))
                .toList();
        loginSessionRepository.deleteAll(sessions);
        loginSessionRepository.flush();

        deliveryAgent.setPassword(passwordEncoder.encode(TEST_DELIVERY_AGENT_PASSWORD));
        deliveryAgent.setForcePasswordReset(false);
        deliveryAgentsRepository.saveAndFlush(deliveryAgent);

        MobileLoginRequest mobileLoginRequest = new MobileLoginRequest();
        mobileLoginRequest.setPhoneNumber(TEST_DELIVERY_AGENT_PHONE);
        mobileLoginRequest.setPassword(TEST_DELIVERY_AGENT_PASSWORD);
        mobileLoginRequest.setMobileUserType(MobileUserType.DeliveryAgent);
        mobileLoginRequest.setDeviceId("delivery-agent-submit-order-test-device");
        mobileLoginRequest.setDeviceName("Delivery Agent Submit Order Test Device");
        mobileLoginRequest.setPlatform("TEST");

        return given()
                .contentType(ContentType.JSON)
                .body(mobileLoginRequest)
                .post("/authentication/mobilePlatform/login")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .extract().path("token");
    }

    private void assertOrderDeliveryState(UUID orderPublicId,
                                          LoadingStatusEnum loadingStatus,
                                          OrderStatusEnum orderStatus) {
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(adminToken())
                .get("/salesOrders/{salesOrderPublicId}", orderPublicId)
                .then()
                .statusCode(200)
                .body("salesOrderStatus", equalTo(loadingStatus.name()))
                .body("status", equalTo(orderStatus.name()))
                .body("deliveryDate", notNullValue());
    }

    private void assertOrderDeliveredEventRegistered(UUID orderPublicId) {
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(adminToken())
                .get("/events/{publicId}", orderPublicId)
                .then()
                .statusCode(200)
                .body("content.eventType", hasItem("Order Delivered"))
                .body("content.eventAction", hasItem("Order Delivered"));
    }

    private static class DeliveryFlow {
        private final UUID orderPublicId;
        private final UUID routePublicId;
        private final UUID orderItemPublicId;

        private DeliveryFlow(UUID orderPublicId, UUID routePublicId, UUID orderItemPublicId) {
            this.orderPublicId = orderPublicId;
            this.routePublicId = routePublicId;
            this.orderItemPublicId = orderItemPublicId;
        }
    }
}
