package com.app.itibari.web;

import com.app.itibari.ItibariApplicationTests;
import com.app.itibari.dataTypes.MobileUserType;
import com.app.itibari.models.entities.DeliveryAgents;
import com.app.itibari.models.entities.DeliveryAgentLoginSession;
import com.app.itibari.models.entities.SalesAgents;
import com.app.itibari.models.request.LoginRequest;
import com.app.itibari.models.request.MobileLoginRequest;
import com.app.itibari.repository.DeliveryAgentsRepository;
import com.app.itibari.repository.DeliveryAgentLoginSessionRepository;
import org.junit.Before;
import com.app.itibari.repository.SalesAgentsRepository;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AuthenticationRestControllerTests extends ItibariApplicationTests {

    private static final String MOBILE_PASSWORD = "password123";
    private static final String DELIVERY_AGENT_PHONE = "+254712345678";
    private static final String DEVICE_ONE = "authentication-test-device-1";
    private static final String DEVICE_TWO = "authentication-test-device-2";

    @Autowired
    private DeliveryAgentsRepository deliveryAgentsRepository;

    @Autowired
    private DeliveryAgentLoginSessionRepository loginSessionRepository;

    @Autowired
    private SalesAgentsRepository salesAgentsRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Before
    public void clearDeliveryAgentLoginSessions() {
        deliveryAgentsRepository.findDeliveryAgentsByPhoneNumberAndDeletedIsFalse(DELIVERY_AGENT_PHONE)
                .ifPresent(agent -> {
                    List<DeliveryAgentLoginSession> sessions = loginSessionRepository.findAll().stream()
                            .filter(session -> session.getDeliveryAgent().getId().equals(agent.getId()))
                            .toList();
                    loginSessionRepository.deleteAll(sessions);
                    loginSessionRepository.flush();
                });
    }

    @Test
    public void testDeliveryAgentLoginCreatesActiveDeviceSession() {
        String token = loginDeliveryAgent(DEVICE_ONE);

        DeliveryAgentLoginSession session = activeSession();
        assertEquals(DEVICE_ONE, session.getDeviceId());
        assertTrue(session.isActive());

        given()
                .auth().oauth2(token)
                .post("/deliveryAgents/logout")
                .then()
                .statusCode(204);
    }

    @Ignore
    @Test
    public void testDeliveryAgentLoginFromDifferentDeviceLogsOutPreviousDevice() {
        String firstDeviceToken = loginDeliveryAgent(DEVICE_ONE);
        String secondDeviceToken = loginDeliveryAgent(DEVICE_TWO);

        assertEquals(DEVICE_TWO, activeSession().getDeviceId());

        DeliveryAgentLoginSession previousSession = loginSessionRepository.findAll().stream()
                .filter(session -> DEVICE_ONE.equals(session.getDeviceId()))
                .findFirst()
                .orElseThrow();
        assertFalse(previousSession.isActive());
        assertEquals("LOGIN_FROM_ANOTHER_DEVICE", previousSession.getLogoutReason());
        assertEquals(1, loginSessionRepository.findAll().stream()
                .filter(session -> DELIVERY_AGENT_PHONE.equals(session.getDeliveryAgent().getPhoneNumber()))
                .filter(DeliveryAgentLoginSession::isActive)
                .count());

        given().auth().oauth2(firstDeviceToken).post("/deliveryAgents/logout").then().statusCode(401);
        given().auth().oauth2(secondDeviceToken).post("/deliveryAgents/logout").then().statusCode(204);
    }

    @Ignore
    @Test
    public void testDeliveryAgentReloginFromSameDeviceReplacesSession() {
        String firstToken = loginDeliveryAgent(DEVICE_ONE);
        String secondToken = loginDeliveryAgent(DEVICE_ONE);

        List<DeliveryAgentLoginSession> sessions = loginSessionRepository.findAll().stream()
                .filter(session -> DELIVERY_AGENT_PHONE.equals(session.getDeliveryAgent().getPhoneNumber()))
                .toList();
        assertEquals(2, sessions.size());
        assertEquals(1, sessions.stream().filter(DeliveryAgentLoginSession::isActive).count());
        assertTrue(sessions.stream().anyMatch(session -> !session.isActive()
                && "RELOGIN_SAME_DEVICE".equals(session.getLogoutReason())));

        given().auth().oauth2(firstToken).post("/deliveryAgents/logout").then().statusCode(401);
        given().auth().oauth2(secondToken).post("/deliveryAgents/logout").then().statusCode(204);
    }

    @Test
    public void testDeliveryAgentLoginRequiresDeviceId() {
        MobileLoginRequest request = deliveryAgentLoginRequest(null);

        given()
                .contentType("application/json")
                .body(request)
                .post("/authentication/mobilePlatform/login")
                .then()
                .statusCode(400)
                .body("userMessage", equalTo("Device ID is required"));
    }

    @Ignore
    @Test
    public void testDeliveryAgentLogoutInvalidatesToken() {
        String token = loginDeliveryAgent(DEVICE_ONE);

        given().auth().oauth2(token).post("/deliveryAgents/logout").then().statusCode(204);
        given().auth().oauth2(token).post("/deliveryAgents/logout").then().statusCode(401);

        DeliveryAgentLoginSession session = loginSessionRepository.findAll().stream()
                .filter(item -> DELIVERY_AGENT_PHONE.equals(item.getDeliveryAgent().getPhoneNumber()))
                .findFirst().orElseThrow();
        assertFalse(session.isActive());
        assertEquals("USER_LOGOUT", session.getLogoutReason());
    }

    @Test
    public void testFetchDeliveryAgentLoginSessionReport() {
        loginDeliveryAgent(DEVICE_ONE);

        given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("deliveryAgentPublicId", deliveryAgent().getPublicId())
                .get("/deliveryAgents/loginSessions")
                .then()
                .log().all()
                .statusCode(200)
                .body("totalElements", greaterThanOrEqualTo(1))
                .body("content.deviceId", hasItem(DEVICE_ONE))
                .body("content.phoneNumber", hasItem(DELIVERY_AGENT_PHONE));
    }

    private String loginDeliveryAgent(String deviceId) {
        return given()
                .contentType("application/json")
                .header("X-App-Version", "1.0.0")
                .body(deliveryAgentLoginRequest(deviceId))
                .post("/authentication/mobilePlatform/login")
                .then()
                .statusCode(200)
                .body("action", equalTo("Logged in"))
                .body("token", notNullValue())
                .extract().path("token");
    }

    private MobileLoginRequest deliveryAgentLoginRequest(String deviceId) {
        DeliveryAgents agent = deliveryAgent();
        agent.setPassword(passwordEncoder.encode(MOBILE_PASSWORD));
        agent.setForcePasswordReset(false);
        deliveryAgentsRepository.save(agent);

        MobileLoginRequest request = new MobileLoginRequest();
        request.setPhoneNumber(DELIVERY_AGENT_PHONE);
        request.setPassword(MOBILE_PASSWORD);
        request.setMobileUserType(MobileUserType.DeliveryAgent);
        request.setDeviceId(deviceId);
        request.setDeviceName("Authentication Test Phone");
        request.setDeviceModel("Test Model");
        request.setPlatform("ANDROID");
        return request;
    }

    private DeliveryAgents deliveryAgent() {
        return deliveryAgentsRepository.findDeliveryAgentsByPhoneNumberAndDeletedIsFalse(DELIVERY_AGENT_PHONE)
                .orElseThrow();
    }

    private DeliveryAgentLoginSession activeSession() {
        return loginSessionRepository.findFirstByDeliveryAgentAndActiveTrueAndDeletedIsFalse(deliveryAgent())
                .orElseThrow();
    }

    @Test
    public void testGetAccountManager() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("itibari@gmail.com");
        loginRequest.setPassword("password");

        given()
                .contentType("application/json")
                .body(loginRequest)
                .log().all()
                .post("/authentication/token")
                .then().log().all()
                .statusCode(200);

    }

    @Test
    public void testLoginWithIncorrectPasswordReturnsBadRequest() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("itibari@gmail.com");
        loginRequest.setPassword("incorrect-password");

        given()
                .contentType("application/json")
                .body(loginRequest)
                .post("/authentication/token")
                .then()
                .statusCode(400)
                .body("userMessage", equalTo("Invalid username or password"))
                .body("developerMessage", equalTo("Invalid username or password"));
    }

    @Test
    public void testSalesAgentLoginViaPassword() {
        String phoneNumber = "+255764840059";
        SalesAgents salesAgent = salesAgentsRepository.findSalesAgentsByPhoneNumberAndDeletedIsFalse(phoneNumber)
                .orElseThrow();
        salesAgent.setPassword(passwordEncoder.encode(MOBILE_PASSWORD));
        salesAgent.setForcePasswordReset(false);
        salesAgentsRepository.save(salesAgent);

        MobileLoginRequest mobileLoginRequest = new MobileLoginRequest();
        mobileLoginRequest.setPhoneNumber(phoneNumber);
        mobileLoginRequest.setPassword(MOBILE_PASSWORD);
        mobileLoginRequest.setMobileUserType(MobileUserType.SalesAgent);

        given()
                .contentType("application/json")
                .body(mobileLoginRequest)
                .log().all()
                .post("/authentication/mobilePlatform/login")
                .then().log().all()
                .statusCode(200)
                .body("action", equalTo("Logged in"))
                .body("phoneNumber", equalTo(phoneNumber))
                .body("token", notNullValue())
                .body("username", equalTo("erickmassawe95@gmail.com"))
                .body("forcePasswordReset", equalTo(false));
    }

    @Test
    @Ignore
    public void testOrderProductBasedOnStock() {
        List<Integer> availableStocks = given()
                        .auth().oauth2(ADMIN_TOKEN)
                        .get("/authentication/products")
                        .then()
                        .statusCode(200)
                        .extract().jsonPath().getList("content.availableStock", Integer.class);

        List<Integer> sorted = new ArrayList<>(availableStocks);
        sorted.sort(Comparator.reverseOrder());

        assertEquals(sorted, availableStocks, "Products are not ordered by available_stock DESC");
    }

    @Test
    public void testGetProductsUnauthorized() {
        given()
                .get("/authentication/v2/products")
                .then()
                .statusCode(200)
                .log().all();
    }

    @Ignore
    @Test
    public void getDATestToken(){
        given()
                .post("/authentication/generateTestToken")
                .then()
                .statusCode(200)
                .log().all();
    }


}
