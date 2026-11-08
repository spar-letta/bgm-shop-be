package com.app.itibari;

import com.app.itibari.configuration.JwtToken;
import com.app.itibari.configuration.UserService;
import com.app.itibari.dataTypes.*;
import com.app.itibari.models.request.*;
import com.app.itibari.scheduler.Scheduler;
import com.github.javafaker.Faker;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;


/**
 * Created by christopher oigo (oigoc60@gmail.com) on May 2025
 */


@Slf4j
@ContextConfiguration(classes = ItibariApplication.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class ItibariApplicationTests {
    public static final Faker faker = new Faker();
    Random random = new Random();

    /**
     * ${random.uuid} in application.yaml can be resolved once per JVM, so classes would share
     * the same H2 mem DB (with DB_CLOSE_DELAY=-1) and pollute each other. Force a unique DB
     * name for every Spring test context.
     */
    @DynamicPropertySource
    static void uniqueInMemoryDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:mem:itibari-test-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
    }

    public static String ADMIN_TOKEN;
    public static String CUSTOMER_TOKEN;
    public static String DELIVERY_AGENT_TOKEN;
    public static String SALES_AGENT_TOKEN;
    public static String SUPPLIER_USER_TOKEN;
    public static String SUPER_ADMIN_TOKEN;
    private static volatile boolean tokensReady;
    private static int tokenContextHash;
    private static final List<String> PREAUTHORIZE_PERMISSIONS = List.of(
            "ACTION_SMS_TEMPLATE", "ADD_DEFINE_WORKFLOW", "APPROVE_DELIVERY_AGENT", "APPROVE_PURCHASE_ORDER",
            "APPROVE_QUARANTINE", "APPROVE_SPECIAL_DISCOUNT", "CREATE_BNPL_NOTES", "CREATE_CALL_STATUS",
            "CREATE_CREDIT_RULE", "CREATE_CUSTOMER_NOTES", "CREATE_DELIVERY_AGENT", "CREATE_DEPARTMENT",
            "CREATE_DISCOUNT", "CREATE_DOCUMENT_RULE", "CREATE_GOODS_RETURN_NOTE", "CREATE_ISSUE_TYPE",
            "CREATE_ORDER_CUT_OFF_TIME", "CREATE_PERMISSION_GROUP", "CREATE_PERSON", "CREATE_PRICE_MANAGEMENT",
            "CREATE_PRODUCT", "CREATE_PRODUCT_ADJUSTMENT", "CREATE_PRODUCT_CATEGORY", "CREATE_PRODUCT_CLASS",
            "CREATE_PURCHASE_ORDER", "CREATE_PURCHASE_ORDER_ITEM", "CREATE_QUARANTINE",
            "CREATE_REGION_WAREHOUSE_MANAGEMENT", "CREATE_REJECTION_CODE", "CREATE_ROLE", "CREATE_ROUTE_PLANNING",
            "CREATE_SMS_TEMPLATE", "CREATE_SUPPLIER", "CREATE_TICKET", "CREATE_VANSALE", "CREATE_VEHICLE",
            "CREATE_WAREHOUSE", "DEFINE_WORKFLOW", "DELETE_CALL_STATUS", "DELETE_CREDIT_RULE",
            "DELETE_CUSTOMER_NOTES", "DELETE_DELIVERY_AGENT", "DELETE_DISCOUNT", "DELETE_DOCUMENT_RULE",
            "DELETE_ORDER_CUT_OFF_TIME", "DELETE_PERMISSION", "DELETE_PERMISSION_GROUP", "DELETE_PERSON",
            "DELETE_PRODUCT", "DELETE_PRODUCT_ADJUSTMENT", "DELETE_PRODUCT_CATEGORY", "DELETE_PRODUCT_CLASS",
            "DELETE_QUARANTINE", "DELETE_REGION_WAREHOUSE_MANAGEMENT", "DELETE_REJECTION_CODE", "DELETE_ROLE",
            "DELETE_ROUTE_PLANNING", "DELETE_SMS_TEMPLATE", "DELETE_SUPPLIER", "DELETE_TYPE", "DELETE_VANSALE",
            "DELETE_VEHICLE", "DELETE_WAREHOUSE", "FINAL_APPROVE_SPECIAL_DISCOUNT", "FINANCE_APPROVE_PURCHASE_ORDER",
            "FINANCE_CREATE_DEBIT_NOTE", "FINANCE_GET_RECONCILIATION", "FINANCE_UPDATE_RECONCILIATION",
            "GET_QUARANTINE", "GET_WAREHOUSE", "GRANT_SUPPLIER_PORTAL_ACCESS", "LOAD_PRODUCTS", "READ_CREDIT_RULE",
            "READ_DOCUMENT_RULE", "READ_REGION_WAREHOUSE_MANAGEMENT", "READ_SUPPLIER_PORTAL", "READ_SUPPLIER_PORTTAL",
            "RECEIVE_GOODS", "RETURN_QUARANTINE_PRODUCT", "SEND_APP_LINK", "SUSPEND_DISCOUNT", "UPDATE_CALL_STATUS",
            "UPDATE_CUSTOMER_CALL_STATUS", "UPDATE_CUSTOMER_NOTES", "UPDATE_DELIVERY_AGENT", "UPDATE_DEPARTMENT",
            "UPDATE_DOCUMENT_RULE", "UPDATE_LEAD_NOTES", "UPDATE_PERMISSION_GROUP", "UPDATE_PERSON", "UPDATE_PRODUCT",
            "UPDATE_PRODUCT_ADJUSTMENT", "UPDATE_PRODUCT_CATEGORY", "UPDATE_PRODUCT_CLASS",
            "UPDATE_PRODUCT_RECONCILIATIONS", "UPDATE_PURCHASE_ORDER", "UPDATE_QUARANTINE", "UPDATE_REJECTION_CODE",
            "UPDATE_ROLE", "UPDATE_ROUTE_PLANNING", "UPDATE_SMS_TEMPLATE", "UPDATE_SUPPLIER", "UPDATE_VANSALE",
            "UPDATE_VEHICLE", "UPLOAD_PRICE_MANAGEMENT", "VIEW_CALL_STATUS", "VIEW_CUSTOMERS_REPORT",
            "VIEW_DELIVERY_AGENT", "VIEW_GOODS_RETURN_NOTE", "VIEW_INVENTORY", "VIEW_LIVE_STOCK_REPORT",
            "VIEW_ORDER_CUT_OFF_TIME", "VIEW_PERMISSION_GROUP", "VIEW_PERSON", "VIEW_PRICE_MANAGEMENT", "VIEW_PRODUCT",
            "VIEW_PRODUCTS_REPORT", "VIEW_PRODUCT_ADJUSTMENT", "VIEW_PRODUCT_CATEGORY", "VIEW_PRODUCT_CLASS",
            "VIEW_PRODUCT_RECONCILIATIONS", "VIEW_PURCHASE_ORDER", "VIEW_PURCHASE_ORDER_REPORT", "VIEW_REJECTION_CODE",
            "VIEW_RETURN_NOTE_REPORT", "VIEW_ROLE", "VIEW_ROUTE_ORDER", "VIEW_ROUTE_PLANNING",
            "VIEW_SALES_ORDERS_REPORT", "VIEW_SUPPLIER", "VIEW_VANSALE", "VIEW_VEHICLE"
    );

    @LocalServerPort
    private int port;

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private JwtToken jwtToken;

    @Autowired
    private UserService userService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    private ApplicationContext applicationContext;

    @Before
    public void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        ensureAuthTokens();
        // Seed cut-off is 01:15–23:59; keep a full-day window so overnight CI/local runs still plan.
        jdbcTemplate.update(
                "update itibari.order_cut_off_time set cut_off_time = '00:00:00', cut_off_time_end = '23:59:59' "
                        + "where deleted = false");
    }

    /**
     * Soft-deletes volume discounts for the product so a later test can create a fresh one
     * without hitting "Product has active discount" or picking a stale Incomplete row.
     */
    protected void clearActiveVolumeDiscountForProduct(UUID productId) {
        jdbcTemplate.update(
                "update itibari.discounts set deleted = true, status = 'Cancelled', suspended = true "
                        + "where product_public_id = ? and type = 'VolumeDiscount' and deleted = false",
                productId.toString());
    }

    /**
     * Frees the seeded delivery agent and closes leftover routes / van-sales that would block
     * route planning or van-sale creation.
     */
    protected void resetSeededDeliveryAgentAvailability() {
        UUID deliveryAgentId = UUID.fromString("af485765-69c5-4dd0-8fa0-560058b85c65");
        jdbcTemplate.update(
                "update itibari.delivery_agents set available = true, loaded = false where public_id = ?",
                deliveryAgentId.toString());
        jdbcTemplate.update(
                "update itibari.route_planning set status = 'TRIP_COMPLETED' "
                        + "where delivery_agent_id = ? and deleted = false "
                        + "and status in ('Loading','CREATED','Loaded','ReadyToGo','IN_TRANSIT')",
                deliveryAgentId.toString());
        jdbcTemplate.update(
                "update itibari.vehicles set available = true where public_id = ?",
                "fa4bf2ee-6665-493a-8dbd-5d56dc8a7d93");
        jdbcTemplate.update(
                "update itibari.van_sales set deleted = true "
                        + "where van_sale_delivery_agent_fk = "
                        + "(select id from itibari.delivery_agents where public_id = ?) "
                        + "and status = 'Pending' and deleted = false",
                deliveryAgentId.toString());
    }

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    /**
     * Password-login for the seeded delivery agent so the JWT includes a persisted session_id.
     */
    protected String loginSeededDeliveryAgentViaPassword() {
        String phoneNumber = "+254712345678";
        String password = "password123";
        jdbcTemplate.update(
                "update itibari.delivery_agents set password = ?, force_password_reset = false "
                        + "where phone_number = ? and deleted = false",
                passwordEncoder.encode(password),
                phoneNumber);

        MobileLoginRequest login = new MobileLoginRequest();
        login.setPhoneNumber(phoneNumber);
        login.setPassword(password);
        login.setMobileUserType(MobileUserType.DeliveryAgent);
        login.setDeviceId("itibari-shared-test-device");
        login.setDeviceName("Itibari Shared Tests");
        login.setPlatform("TEST");

        return given()
                .contentType(ContentType.JSON)
                .body(login)
                .post("/authentication/mobilePlatform/login")
                .then()
                .statusCode(200)
                .extract().path("token");
    }

    private synchronized void ensureAuthTokens() {
        int contextHash = System.identityHashCode(applicationContext);
        if (tokensReady && tokenContextHash == contextHash) {
            return;
        }
        tokenContextHash = contextHash;
        persistPreAuthorizePermissions();
        grantAllPermissionsToAdminRole();
        assignAdminRole("itibari@gmail.com");
        assignAdminRole("e.wangila@itibari.io");
        detachDeliveryAgentRole("itibari@gmail.com");
        detachDeliveryAgentRole("e.wangila@itibari.io");
        bumpCountersAboveSeedData();
        ADMIN_TOKEN = adminStaffToken("itibari@gmail.com");
        SUPER_ADMIN_TOKEN = adminStaffToken("e.wangila@itibari.io");
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            SUPPLIER_USER_TOKEN = supplierStaffToken("supp@email.com");
            CUSTOMER_TOKEN = jwtToken.generateMobilePlatformJwtToken(MobileUserType.Customer, "+255772345345");
            DELIVERY_AGENT_TOKEN = jwtToken.generateMobilePlatformJwtToken(
                    MobileUserType.DeliveryAgent, "+254712345678", UUID.randomUUID());
            SALES_AGENT_TOKEN = jwtToken.generateMobilePlatformJwtToken(MobileUserType.SalesAgent, "+255764840059");
        });
        tokensReady = true;
    }

    private void persistPreAuthorizePermissions() {
        for (String permissionName : PREAUTHORIZE_PERMISSIONS) {
            Integer existing = jdbcTemplate.queryForObject(
                    "select count(*) from itibari.permission where name = ? and deleted = false",
                    Integer.class,
                    permissionName);
            if (existing != null && existing > 0) {
                continue;
            }
            jdbcTemplate.update(
                    "insert into itibari.permission (public_id, deleted, name, supplier_role) values (?, false, ?, false)",
                    UUID.randomUUID(),
                    permissionName);
        }
    }

    private void grantAllPermissionsToAdminRole() {
        Long adminRoleId = jdbcTemplate.queryForObject(
                "select id from itibari.roles where name = ? and deleted = false",
                Long.class,
                "ROLE_ADMIN");
        jdbcTemplate.update(
                "insert into itibari.role_permissions (role_id, permission_id) " +
                        "select ?, p.id from itibari.permission p where p.deleted = false " +
                        "and not exists (select 1 from itibari.role_permissions rp " +
                        "where rp.role_id = ? and rp.permission_id = p.id)",
                adminRoleId,
                adminRoleId);
    }

    private void assignAdminRole(String email) {
        jdbcTemplate.update(
                "insert into itibari.person_roles (person_id, role_id) " +
                        "select p.id, r.id from itibari.person p, itibari.roles r " +
                        "where p.email = ? and p.deleted = false " +
                        "and r.name = 'ROLE_ADMIN' and r.deleted = false " +
                        "and not exists (select 1 from itibari.person_roles pr " +
                        "where pr.person_id = p.id and pr.role_id = r.id)",
                email);
    }

    private void detachDeliveryAgentRole(String email) {
        jdbcTemplate.update(
                "delete from itibari.person_roles where person_id = (" +
                        "select id from itibari.person where email = ? and deleted = false) " +
                        "and role_id = (select id from itibari.roles where name = 'ROLE_DELIVERY_AGENT' and deleted = false)",
                email);
    }

    private String adminStaffToken(String email) {
        List<String> authorities = new ArrayList<>();
        authorities.add("ROLE_ADMIN");
        authorities.addAll(PREAUTHORIZE_PERMISSIONS);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                email,
                "n/a",
                authorities.stream().map(SimpleGrantedAuthority::new).toList());
        return jwtToken.generateJwtToken(authentication, authorities);
    }

    private String supplierStaffToken(String email) {
        UserDetails userDetails = userService.loadUserByUsername(email);
        List<String> authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toCollection(ArrayList::new));
        if (!authorities.contains("READ_SUPPLIER_PORTAL")) {
            authorities.add("READ_SUPPLIER_PORTAL");
        }
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                email, "n/a", authorities.stream().map(SimpleGrantedAuthority::new).toList());
        return jwtToken.generateJwtToken(authentication, authorities);
    }

    private void bumpCountersAboveSeedData() {
        // Seeded sales_orders already use ORD100001; the default counter starts at 100000.
        bumpCounter("SALES_ORDER", 200000);
    }

    private void bumpCounter(String type, int floor) {
        Integer existing = jdbcTemplate.queryForObject(
                "select count(*) from itibari.counter where counter_type = ?",
                Integer.class,
                type);
        if (existing == null || existing == 0) {
            jdbcTemplate.update(
                    "insert into itibari.counter (counter_type, counter_current) values (?, ?)",
                    type,
                    floor);
            return;
        }
        jdbcTemplate.update(
                "update itibari.counter set counter_current = ? " +
                        "where counter_type = ? and (counter_current is null or counter_current < ?)",
                floor,
                type,
                floor);
    }

    public void saveExport(Response response, String downloadFileName) throws IOException {
        Assert.assertEquals(200, response.getStatusCode());

        String downloadFolder = "src/test/resources/downloads";
        File outputPath = new File(downloadFolder);

        // create the folder structure if it does not exist
        outputPath.mkdirs();

        // For the purpose of the test, if the file already exists then I will delete it

        File checkDownloaded = new File(outputPath.getPath(), downloadFileName);
        if (checkDownloaded.exists()) {
            checkDownloaded.delete();
        }
        File outputFile = new File(outputPath.getPath(), downloadFileName);
        if (outputFile.exists()) {
            outputFile.delete();
        }

        // I might choose to use the mime type of the file to control the file extension
        // here I am just outputting it to the console to demonstrate how to get the type
        System.out.println("Downloaded an " + response.getHeader("Content-Type"));

        // get the contents of the file
        byte[] fileContents = response.getBody().asByteArray();

        // output contents to file
        OutputStream outStream = null;

        try {
            outStream = new FileOutputStream(outputFile);
            outStream.write(fileContents);
        } catch (Exception e) {
            System.out.println("Error writing file " + outputFile.getAbsolutePath());
        } finally {
            if (outStream != null) {
                outStream.close();
            }
        }
    }

    private static final UUID DEFAULT_TAX_CODE_PUBLIC_ID =
            UUID.fromString("a6f320b2-ad75-42a7-859e-253505242736");
    private static final Set<UUID> SEEDED_TAX_CODE_PUBLIC_IDS = Set.of(
            DEFAULT_TAX_CODE_PUBLIC_ID,
            UUID.fromString("d6f320b2-ad75-42a7-859e-253505242732"));

    private UUID resolveTaxCodePublicId(UUID taxCodePublicId) {
        return taxCodePublicId != null && SEEDED_TAX_CODE_PUBLIC_IDS.contains(taxCodePublicId)
                ? taxCodePublicId
                : DEFAULT_TAX_CODE_PUBLIC_ID;
    }

    public String productPayload(String description, UUID taxCodePublicId) throws IOException {

        ProductBrandRequest productBrandRequest = new ProductBrandRequest(UUID.randomUUID().toString());

        String productBrandPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(productBrandRequest).log().all()
                .post("/settings/product-brands")
                .then().log().all()
                .statusCode(200)
                .extract().path("publicId");

        String productName = UUID.randomUUID().toString().substring(0, 10);
        UUID taxCode = resolveTaxCodePublicId(taxCodePublicId);
        return String.format("{\n" +
                "    \"productName\": \"%s\",\n" +
                "    \"categoryPublicId\": \"5bb272d7-cb94-4ba3-8e7e-dc9955718942\",\n" +
                "    \"classPublicId\": \"dc222e53-3136-419f-b149-fc72c19d02b2\",\n" +
                "    \"subClassPublicId\": \"52ee0c90-0fcf-44fd-90ef-eab036e5fafd\",\n" +
                "    \"brand\": \"BRAND\",\n" +
                "    \"productBrandPublicId\": \"%s\",\n" +
                "    \"taxCodePublicId\": \"%s\",\n" +
                "    \"buyingPrice\": 1,\n" +
                "    \"sellPrice\": 500,\n" +
                "    \"quantity\": 1,\n" +
                "    \"customTaxAddition\": 1,\n" +
                "    \"weight\": 1,\n" +
                "    \"length\": 10,\n" +
                "    \"width\": 10,\n" +
                "    \"height\": 10,\n" +
                "    \"unitOfMeasurement\": \"PRO_UNIT\",\n" +
                "    \"barcode\": \"%s\",\n" +
                "    \"packageSize\": \"PRO_PACKAGE_SIZE\",\n" +
                "    \"margin\": 1,\n" +
                "    \"supplierPublicIds\": [\n" +
                "        \"e94a7afb-9560-4498-a789-e8197a9e12fd\"\n" +
                "    ],\n" +
                "    \"bulkPurchaseUnit\": 3,\n" +
                "    \"bulkPurchaseUnitName\": \"PRO_BULK_UNIT\",\n" +
                "    \"sellingUnit\": 4,\n" +
                "    \"description\": \"%s\"\n" +
                "}", productName, productBrandPublicId, taxCode, UUID.randomUUID(), description);
    }

    public String supplierProductPayload(String description, UUID taxCodePublicId, String productName) throws IOException {
        UUID taxCode = resolveTaxCodePublicId(taxCodePublicId);
        return String.format("{\n" +
                "    \"productName\": \"%s\",\n" +
                "    \"categoryPublicId\": \"5bb272d7-cb94-4ba3-8e7e-dc9955718942\",\n" +
                "    \"classPublicId\": \"dc222e53-3136-419f-b149-fc72c19d02b2\",\n" +
                "    \"subClassPublicId\": \"52ee0c90-0fcf-44fd-90ef-eab036e5fafd\",\n" +
                "    \"brand\": \"BRAND\",\n" +
                "    \"taxMappingPublicId\": \"c265bc4d-271c-447a-8133-5cdc986200d5\",\n" +
                "    \"taxCodePublicId\": \"%s\",\n" +
                "    \"buyingPrice\": 1,\n" +
                "    \"sellPrice\": 500,\n" +
                "    \"quantity\": 1,\n" +
                "    \"customTaxAddition\": 1,\n" +
                "    \"weight\": 1,\n" +
                "    \"length\": 10,\n" +
                "    \"width\": 10,\n" +
                "    \"height\": 10,\n" +
                "    \"unitOfMeasurement\": \"PRO_UNIT\",\n" +
                "    \"barcode\": \"%s\",\n" +
                "    \"packageSize\": \"PRO_PACKAGE_SIZE\",\n" +
                "    \"margin\": 1,\n" +
                "    \"supplierPublicIds\": [\n" +
                "        \"e94a7afb-9560-4498-a789-e8197a9e12fd\"\n" +
                "    ],\n" +
                "    \"bulkPurchaseUnit\": 3,\n" +
                "    \"bulkPurchaseUnitName\": \"PRO_BULK_UNIT\",\n" +
                "    \"sellingUnit\": 4,\n" +
                "    \"description\": \"%s\",\n" +
                "    \"supplierTargetRequestDtos\":[\n" +
                "  {\n" +
                "    \"supplierPublicId\": \"e94a7afb-9560-4498-a789-e8197a9e12fd\",\n" +
                "    \"targetValue\": 100\n" +
                "  },\n" +
                "  {\n" +
                "    \"supplierPublicId\": \"32cd5a2d-7d67-4725-9ab8-a1e3b682bda6\",\n" +
                "    \"targetValue\": 250\n" +
                "  }\n" +
                "]\n" +
                "}\n", productName, taxCode, UUID.randomUUID(), description);
    }

    public String createCustomerPayload(String name, String firstName, String lastName, String phone,
                                        String sourceChannel, String organizationRegionId
            , String organizationLocationId, Boolean selfRegistered, Boolean saleAgentRegistered, CustomerType customerType) {

        String email = String.format("%s@gmail", phone);
        BigInteger number = new BigInteger(33, random);

        return String.format(
                " {\n" +
                        "   \"name\":\"%s\",\n" +
                        "   \"firstName\":\"%s\",\n" +
                        "   \"lastName\":\"%s\",\n" +
                        "   \"email\":\"%s\",\n" +
                        "   \"nationalId\":\"%s\",\n" +
                        "   \"phoneNumber\":\"%s\",\n" +
                        "   \"sourceChannel\":\"%s\",\n" +
                        "   \"latitude\": -1.2918418829728668,\n" +
                        "   \"longitude\": 36.825836934149265,\n" +
                        "   \"address\":\"p.o box 12-nai\",\n" +
                        "   \"landmark\":\"Redhill - Nairobi\",\n" +
                        "   \"tin\":\"123452345\",\n" +
                        "   \"customerType\":\"%s\",\n" +
                        "   \"organizationRegionId\":\"%s\",\n" +
                        "   \"organizationLocationId\":\"%s\",\n" +
                        "   \"selfRegistered\":%s,\n" +
                        "   \"saleAgentRegistered\":%s,\n" +
                        "   \"storeName\":\"%s\"\n" +
                        " }",
                name,
                firstName,
                lastName,
                email,
                number,
                phone,
                sourceChannel,
                customerType,
                organizationRegionId,
                organizationLocationId,
                selfRegistered,
                saleAgentRegistered,
                firstName
        );

    }

    public String createCustomerUpdatePayload(String name, String firstName, String lastName, String phone,
                                              String sourceChannel, String organizationRegionId
            , String organizationLocationId, Boolean selfRegistered, Boolean saleAgentRegistered) {

        String email = String.format("%s@gmail", phone);
        BigInteger number = new BigInteger(33, random);

        return String.format(
                " {\n" +
                        "   \"name\":\"%s\",\n" +
                        "   \"firstName\":\"%s\",\n" +
                        "   \"lastName\":\"%s\",\n" +
                        "   \"email\":\"%s\",\n" +
                        "   \"nationalId\":\"%s\",\n" +
                        "   \"phoneNumber\":\"%s\",\n" +
                        "   \"sourceChannel\":\"%s\",\n" +
                        "   \"latitude\": -1.2918418829728668,\n" +
                        "   \"longitude\": 36.825836934149265,\n" +
                        "   \"address\":\"p.o box 12-nai\",\n" +
                        "   \"landmark\":\"Redhill - Nairobi\",\n" +
                        "   \"tin\":\"123452345\",\n" +
//                        "   \"customerType\":\"%s\",\n" +
                        "   \"organizationRegionId\":\"%s\",\n" +
                        "   \"organizationLocationId\":\"%s\",\n" +
                        "   \"selfRegistered\":%s,\n" +
                        "   \"saleAgentRegistered\":%s,\n" +
                        "   \"storeName\":\"%s\"\n" +
                        " }",
                name,
                firstName,
                lastName,
                email,
                number,
                phone,
                sourceChannel,
//                customerType,
                organizationRegionId,
                organizationLocationId,
                selfRegistered,
                saleAgentRegistered,
                firstName
        );
    }

    public List<String> testCreateCustomerSelfRegistered() {
        Set<String> customerPayloadSet = new HashSet<>();

        String customerOne = createCustomerPayload("Customer One", "one", "two", "0706541343",
                SourceChannelEnum.CUSTOMER_CARE_APP.name(),
                "ae9df8ce-6ad0-4829-8082-95fb4c491e95"
                , "0e0b9c9c-3dfd-43c5-8be7-60a28e1ef2a0", Boolean.TRUE, Boolean.FALSE, CustomerType.REGULAR);

        String customerTwo = createCustomerPayload("Customer Two", "one", "two", "0705555555",
                SourceChannelEnum.CUSTOMER_CARE_APP.name(),
                "ae9df8ce-6ad0-4829-8082-95fb4c491e95"
                , "0e0b9c9c-3dfd-43c5-8be7-60a28e1ef2a0", Boolean.TRUE, Boolean.FALSE, CustomerType.REGULAR);

        customerPayloadSet.add(customerOne);
        customerPayloadSet.add(customerTwo);

        for (String payload : customerPayloadSet) {
            given()
                    .contentType(ContentType.JSON)
                    .body(payload)
                    .post("/authentication/mobilePlatform/customerRegister")
                    .then()
                    .statusCode(200);
        }

        given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("phoneNumber", "0706541343")
                .get("/customers")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1));

        given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("phoneNumber", "0705555555")
                .get("/customers")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1));

        List<String> customerPublicIds = given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("organizationRegionId", "ae9df8ce-6ad0-4829-8082-95fb4c491e95")
                .queryParam("organizationLocationId", "0e0b9c9c-3dfd-43c5-8be7-60a28e1ef2a0")
                .queryParam("organizationLocationId", "0e0b9c9c-3dfd-43c5-8be7-60a28e1ef2a0")
                .get("/customers")
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(2))
//                .body("content.phoneNumber", containsInAnyOrder("0706541343", "0705555555", "0708543212"))
                .body("content.phoneNumber", containsInAnyOrder("0706541343", "0705555555"))
                .extract().path("content.publicId");

        return customerPublicIds;
    }

    public String tokenGen() {
        String customerOne = createCustomerPayload("Customer One", "one", "two", "0706541341",
                SourceChannelEnum.CUSTOMER_CARE_APP.name(),
                "ae9df8ce-6ad0-4829-8082-95fb4c491e95"
                , "0e0b9c9c-3dfd-43c5-8be7-60a28e1ef2a0", Boolean.TRUE, Boolean.FALSE, CustomerType.REGULAR);

        return given()
                .contentType(ContentType.JSON)
                .body(customerOne)
                .post("/authentication/mobilePlatform/customerRegister")
                .then().log().all()
                .statusCode(200)
                .extract().path("token");
    }

    public PayloadCarrierDto runPurchaseOrder(UUID wareHousePublicId, UUID productPublicId, UUID supplierPublicId,
                                              UUID productIds) {
        PurchaseOrderRequest purchaseOrderRequest = new PurchaseOrderRequest();
        purchaseOrderRequest.setOrderDate(LocalDate.now());
        purchaseOrderRequest.setSupplierId(supplierPublicId);
        purchaseOrderRequest.setWarehouseId(wareHousePublicId);
        purchaseOrderRequest.setProformaInvoiceNumber("23458");
        purchaseOrderRequest.setRebate(false);

        PurchaseOrderItemRequest purchaseOrderItemRequest = new PurchaseOrderItemRequest();
        purchaseOrderItemRequest.setProductPublicId(productPublicId);
        purchaseOrderItemRequest.setQuantity(500);
        purchaseOrderItemRequest.setUnitPrice(new BigDecimal(300));

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
        purchaseOrderItemReceiveRequest.setQuantityReceived(500);

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
                .then()
                .statusCode(200)
                .body("content[0].createdBy", notNullValue())
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
                .then()
                .statusCode(200)
                .body("productItemList[0].productName", equalToIgnoringCase("Zingida Box"))
                .body("productItemList[0].productSellingPrice", equalTo(300.0F))
                .body("productItemList[0].productPublicId", equalToIgnoringCase(String.valueOf(productPublicId)))
                .body("productItemList[0].quantityReceived", equalTo(500))
                .body("productItemList[0].quantityRequested", equalTo(500))
                .extract().path("productItemList[0].publicId");
        return PayloadCarrierDto.builder().purchaseOrderNumber(purchaseOrderNumber)
                .shipmentReceivedNotePublicId(shipmentReceivedNotePublicId)
                .itemReceivedPublicId(itemReceivedPublicId)
                .build();
    }

    public void runPromoGoods(UUID shipmentReceivedNotePublicId, UUID productId, String purchaseOrderNumber) {
        PromoRequestNoteRequest promoNoteRequest = new PromoRequestNoteRequest();
        promoNoteRequest.setReturnNotePublicId(shipmentReceivedNotePublicId);
        promoNoteRequest.setPurchaseOrderNumber(purchaseOrderNumber);

        PromoItemRequest promoItemRequest = new PromoItemRequest();
        promoItemRequest.setProductPublicId(productId);
        promoItemRequest.setQuantityReceived(70);

        promoNoteRequest.setItems(Arrays.asList(promoItemRequest));

        given()
                .auth().oauth2(ADMIN_TOKEN)
                .contentType(ContentType.JSON)
                .body(promoNoteRequest)
                .patch("/goodsReturnNote/promo/receive")
                .then()
                .statusCode(200);
    }

    public void runGoodsReturns(UUID shipmentReceivedNotePublicId, UUID itemReceivedPublicId) {
        GoodsReturnNoteRequest goodsReturnNoteRequest = new GoodsReturnNoteRequest();
        goodsReturnNoteRequest.setGoodsReturnDate(LocalDate.now());
        goodsReturnNoteRequest.setReturnNotePublicId(shipmentReceivedNotePublicId);

        GoodsNoteItemRequest goodsNoteItemRequest1 = new GoodsNoteItemRequest();
        goodsNoteItemRequest1.setItemPublicId(itemReceivedPublicId);
        goodsNoteItemRequest1.setQuantity(10);
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
                .body("content[0].goodsReturnNoteNumber", equalToIgnoringCase("GRTN100000"))
                .body("content[0].replacementRaised", equalTo(true))
                .body("content[0].replacementPurchaseOrder", notNullValue())
                .extract().path("content[0].publicId");

        given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/goodsReturnNote/returnedGoodsNotes/{goodsReturnedNotePublicId}/items", UUID.fromString(returnedNotePublicId))
                .then()
                .statusCode(200)
                .body("content[0].quantityReturned", equalTo(10))
                .body("content[0].rejectionCode", equalToIgnoringCase("Shop Closed"));

        String publicId = given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/goodsReturnNote/returnedGoodsNotes")
                .then()
                .statusCode(200)
                .extract().path("content[0].replacementPurchaseOrder");


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}/items", UUID.fromString(publicId))
                .then()
                .statusCode(200);
    }

    public void runOrderPlaceBySA(UUID productPublicId, UUID inventoryPublicId, UUID wareHousePublicId
            , UUID inventoryProductPublicId, UUID productId2) throws IOException {
        UUID customerPublicId = UUID.fromString("afbcaadc-45ff-4f6d-8267-e811b9dfc434");

        String orderPublicId;
        String routePublicId;
        String orderItemPublicId;
        String orderItemPublicId_one;
        String deliveryId;
        String productItemPublicId;

        SalesOrderItemsRequest orderItems = SalesOrderItemsRequest
                .builder()
                .productId(productPublicId)
                .quantity(500)
                .build();
        SalesOrderItemsRequest orderItems2 = SalesOrderItemsRequest
                .builder()
                .productId(productId2)
                .quantity(245)
                .build();
        Set<SalesOrderItemsRequest> salesOrderItemsRequestSet = new HashSet<>();
        salesOrderItemsRequestSet.add(orderItems);
        salesOrderItemsRequestSet.add(orderItems2);
        SalesOrdersRequest request = SalesOrdersRequest.builder()
                .customerId(customerPublicId)
                .bookedBy("Itibari Test")
                .sourceChannel(SourceChannelEnum.BACK_OFFICE_APP.name())
                .salesOrderItems(salesOrderItemsRequestSet)
                .build();

        orderPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .post("/salesOrders")
                .then()
                .statusCode(200)
                .extract().path("publicId");


        List<String> orderItemPublicIds = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders/{salesOrderPublicId}/items", UUID.fromString(orderPublicId))
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content.publicId");

        orderItemPublicId_one = orderItemPublicIds.get(0);
        orderItemPublicId = orderItemPublicIds.get(1);

        scheduler.runOrderPlanning();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200);

        RoutePlanningRequest routePlanningRequest = new RoutePlanningRequest();
        routePlanningRequest.setLocationPublicId(UUID.fromString("90ad01f1-4a13-41f7-b825-c428b41f35d0"));
        routePlanningRequest.setRegionPublicId(UUID.fromString("8c5d4db0-f9a1-47ed-a160-56780e4e4d14"));
        routePlanningRequest.setVehiclePublicId(UUID.fromString("fa4bf2ee-6665-493a-8dbd-5d56dc8a7d93"));
        routePlanningRequest.setDeliveryAgentPublicId(UUID.fromString("af485765-69c5-4dd0-8fa0-560058b85c65"));
        routePlanningRequest.setWarehousePublicId(wareHousePublicId);
        routePlanningRequest.setSalesOrdersPublicIds(Collections.singletonList(UUID.fromString(orderPublicId)));

        routePublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(routePlanningRequest)
                .post("/logistics/routePlanning")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        deliveryId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/logistics/routePlanning/{routePlanningPublicId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .extract().path("routeId");

        List<String> productItemPublicIds = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/logistics/routePlanning/{routePlanningPublicId}/loadingProducts", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content.productItemPublicId");

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
        loadingDTO.setProductItemPublicId(UUID.fromString(productItemPublicIds.get(0)));

        LoadingDTO loadingDTO_2 = new LoadingDTO();
        loadingDTO_2.setLoadingType(LoadingType.OrderSale);
        loadingDTO_2.setProductItemPublicId(UUID.fromString(productItemPublicIds.get(1)));


        loadingRequest.setLoadingDTOList(Arrays.asList(loadingDTO, loadingDTO_2));

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

        String productReconItemId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/warehouse/productRecons/{publicId}/items", UUID.fromString(productReconId))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].publicId");


        log.info("----------checker 2------");
        DeliverItemRequest deliverItemRequest = new DeliverItemRequest();
        deliverItemRequest.setSalesOrderPublicId(UUID.fromString(orderPublicId));
        deliverItemRequest.setRoutePublicId(UUID.fromString(routePublicId));

        ItemRequest itemRequest = new ItemRequest();
        itemRequest.setProductItemPublicId(UUID.fromString(orderItemPublicId));
        itemRequest.setProductReconItemPublicId(UUID.fromString(orderItemPublicId));
        itemRequest.setQuantitySold(495);

        ItemRequest itemRequest_one = new ItemRequest();
        itemRequest_one.setProductItemPublicId(UUID.fromString(orderItemPublicId_one));
        itemRequest_one.setProductReconItemPublicId(UUID.fromString(orderItemPublicId_one));
        itemRequest_one.setQuantitySold(245);
        deliverItemRequest.setItems(Arrays.asList(itemRequest, itemRequest_one));

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

        log.info("-----------booo------------");
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders/{salesOrderPublicId}/items", UUID.fromString(orderPublicId))
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content.quantity", containsInAnyOrder(245, 500));

        log.info("-----------booo------a------");
        String recoPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("deliveryId", deliveryId)
                .get("/warehouse/productRecons")
                .then().log().all()
                .statusCode(200)
                .extract().path("content[0].publicId");

        String recoItemPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("deliveryId", deliveryId)
                .get("/warehouse/productRecons/{publicId}/items", UUID.fromString(recoPublicId))
                .then().log().all()
                .statusCode(200)
                .extract().path("content[1].publicId");

        ProductReconAction productReconAction = new ProductReconAction();
        productReconAction.setProductReconPublicId(UUID.fromString(recoPublicId));

        ProductItemRequest productItemRequest = new ProductItemRequest();
        productItemRequest.setQuantityReturned(5);
        productItemRequest.setProductItemPublicId(UUID.fromString(recoItemPublicId));
        productItemRequest.setQuantityMissing(0);
        productItemRequest.setMissingRejectionPublicId(UUID.fromString("d88a0e04-23b7-4145-93ba-48b31efacf8d"));
        productReconAction.setProductItems(Collections.singletonList(productItemRequest));

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(productReconAction)
                .patch("/warehouse/productRecons/{publicId}", UUID.fromString(recoPublicId))
                .then()
                .statusCode(200);

        String stagedItemPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("searchParam", "Erick")
//                .queryParam("startDate", LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
//                .queryParam("endDate", LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .get("/warehouse/stagedForApproval")
                .then().log().all()
                .statusCode(200)
                .extract().path("content[0].publicId");

        Response response = given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/reports/warehouse/stagedForApproval")
                .then().log().all()
                .statusCode(200)
                .extract().response();

//        saveExport(response, "Staging_Report.xlsx");

        log.info("--------checker 2");
        String itemPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/warehouse/stagedForApproval/{publicId}/items", UUID.fromString(stagedItemPublicId))
                .then().log().all()
                .statusCode(200)
                .body("content[0].quantityReturned", equalTo(5))
                .extract().path("content[0].publicId");

        StagingActionRequest stagingActionRequest = StagingActionRequest.builder()
                .actionType(StagingActionRequest.ActionType.Edit_Item)
                .quantity(0)
                .comment("approved")
                .build();
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(stagingActionRequest).log().all()
                .queryParam("itemPublicId", UUID.fromString(itemPublicId))
                .patch("/warehouse/stagedForApproval/{publicId}", UUID.fromString(stagedItemPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/warehouse/stagedForApproval/{publicId}/items", UUID.fromString(stagedItemPublicId))
                .then().log().all()
                .statusCode(200)
                .body("content[0].quantityReturned", equalTo(0));

        stagingActionRequest = StagingActionRequest.builder().actionType(StagingActionRequest.ActionType.Approve).comment("approved").build();
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(stagingActionRequest).log().all()
                .patch("/warehouse/stagedForApproval/{publicId}", UUID.fromString(stagedItemPublicId))
                .then()
                .statusCode(200);
    }

    public void testCreateQuarantineProductWorks(UUID productId, UUID inventoryProductId, UUID mainWarehouseId, UUID inventoryProductPublicId) throws IOException {
        UUID quarantineWarehouseId = UUID.fromString("6bd6caf4-f11b-4eba-bc9d-419d4668d182");
        UUID reasonId = UUID.fromString("632b6fda-e23c-4a5f-bec0-a0f9e1f0e293");

        QuarantineRequest quarantineRequest = new QuarantineRequest();
        quarantineRequest.setMainWarehousePublicId(mainWarehouseId);
        quarantineRequest.setQuarantineWarehousePublicId(quarantineWarehouseId);

        QuarantineProductRequest quarantineProductRequest = new QuarantineProductRequest();
        quarantineProductRequest.setInventoryProductPublicId(inventoryProductPublicId);
        quarantineProductRequest.setQuantity(4);
        quarantineProductRequest.setReasonPublicId(reasonId);
        quarantineRequest.setQuarantineProducts(Arrays.asList(quarantineProductRequest));

        String quarantinePublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(quarantineRequest)
                .post("/quarantine")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(quarantineRequest)
                .queryParam("action", QuarantineAction.Approve)
                .patch("/quarantine/{publicId}/actions", UUID.fromString(quarantinePublicId))
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/quarantine/{publicId}", UUID.fromString(quarantinePublicId))
                .then()
                .statusCode(200)
                .body("transferType", equalToIgnoringCase("TO_QUARANTINE_WAREHOUSE"))
                .body("quarantineStatus", equalToIgnoringCase("Approved"));

        String quarantineProductId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/quarantine/{publicId}/quarantineProducts", UUID.fromString(quarantinePublicId))
                .then()
                .statusCode(200)
                .body("content[0].quantity", equalTo(4))
                .body("content[0].productName", equalToIgnoringCase("Zingida Box"))
                .body("content[0].reasonDescription", equalToIgnoringCase("Shop Closed"))
                .extract().path("content[0].publicId");

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/inventory/{id}/products", inventoryProductId)
                .then()
                .statusCode(200)
                .body("content[0].product.availableStock", equalTo(557))
                .body("content[0].availableStock", equalTo(557))
                .body("content[0].reservedStock", equalTo(0))
                .body("content[0].onHoldStock", equalTo(0))
                .body("content[0].quarantinedStock", equalTo(4))
                .body("content[0].totalStock", equalTo(561))
                .body("content[0].closingStock", equalTo(500));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("quantity", 2)
                .pathParam("publicId", UUID.fromString(quarantinePublicId))
                .pathParam("quarantineProductId", quarantineProductId)
                .post("/quarantine/{publicId}/returnProduct/{quarantineProductId}")
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/quarantine/{publicId}/quarantineProducts", UUID.fromString(quarantinePublicId))
                .then()
                .statusCode(200)
                .body("content[0].quantity", equalTo(2));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("productPublicId", productId)
                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("runDate", LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_DATE))
                .patch("/warehouse/runStaticStock")
                .then()
                .statusCode(200);

        String liveDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("warehouseId", mainWarehouseId)
                .queryParam("liveDate", liveDate)
                .queryParam("searchParam", "biscu")
                .get("/reports/liveStockReport")
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/warehouse/productStock")
                .then().log().all()
                .statusCode(200);

        String wareHousePublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("warehousePublicId", UUID.fromString("3849b1be-a9dd-4f11-abbf-229ad2780a56"))
                .get("/warehouse/productStockRapper")
                .then().log().all()
                .statusCode(200)
                .extract().path("content[0].wareHousePublicId");

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("searchParam", "Biscuite")
                .get("/warehouse/productStock")
                .then()
                .statusCode(200);

        Response responseResult = given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/warehouse/downloadTemplate")
                .andReturn();

        byte[] fileContents = responseResult.getBody().asByteArray();

        saveExport(responseResult, "product_stock_template.xlsx");

        int available_stock = 4;
        int reserved_stock = 5;
        int quarantine_stock = 6;
        int total_stock = 7;
        int closing_stock = 8;

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(fileContents);

        XSSFWorkbook workbook = new XSSFWorkbook(byteArrayInputStream);
        Sheet sheet = workbook.getSheetAt(0);

        Row rowIndex1 = sheet.getRow(1);

        rowIndex1.createCell(available_stock).setCellValue("100");
        rowIndex1.createCell(reserved_stock).setCellValue("20");
        rowIndex1.createCell(quarantine_stock).setCellValue("10");
        rowIndex1.createCell(total_stock).setCellValue("100");
        rowIndex1.createCell(closing_stock).setCellValue("110");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        bos.close();

        try (OutputStream outputStream = Files.newOutputStream(Paths.get("src/test/resources/downloads" +
                "/Updated_Upload_Product_Stock.xlsx"))) {
            bos.writeTo(outputStream);
        }

        Response updatedResponse = given()
                .auth().oauth2(ADMIN_TOKEN)
                .multiPart("file", "product_stock_template.xlsx", bos.toByteArray())
                .post("/warehouse/productStockBulkUpload")
                .andReturn();
//        saveExport(updatedResponse, "Updated_Upload_Product_Stock.xlsx");

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/warehouse/productStock")
                .then().log().all()
                .statusCode(200);

        Response response = given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("warehouseId", mainWarehouseId)
                .queryParam("liveDate", liveDate)
                .queryParam("searchParam", "SKU100021")
                .get("/reports/staticReportExport")
                .then()
                .statusCode(200)
                .extract().response();

//        saveExport(response, "static_stock_report.xlsx");

        given()
                .get("/authentication/v2/products")
                .then()
                .statusCode(200);
//                .log().all();
    }


    public void runAnOrderViaSA(UUID orderPublicId, boolean isPartiallyFulfilled) {

        RoutePlanningRequest routePlanningRequest = new RoutePlanningRequest();
        routePlanningRequest.setLocationPublicId(UUID.fromString("90ad01f1-4a13-41f7-b825-c428b41f35d0"));
        routePlanningRequest.setRegionPublicId(UUID.fromString("8c5d4db0-f9a1-47ed-a160-56780e4e4d14"));
        routePlanningRequest.setVehiclePublicId(UUID.fromString("fa4bf2ee-6665-493a-8dbd-5d56dc8a7d93"));
        routePlanningRequest.setDeliveryAgentPublicId(UUID.fromString("af485765-69c5-4dd0-8fa0-560058b85c65"));
        routePlanningRequest.setWarehousePublicId(UUID.fromString("3849b1be-a9dd-4f11-abbf-229ad2780a56"));
        routePlanningRequest.setSalesOrdersPublicIds(Collections.singletonList(orderPublicId));


        String orderItemPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders/{salesOrderPublicId}/items", orderPublicId)
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].discount", notNullValue())
                .body("content[0].hasDiscount", equalTo(true))
                .extract().path("content[0].publicId");

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

        String productReconItemId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/warehouse/productRecons/{publicId}/items", UUID.fromString(productReconId))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].publicId");


        DeliverItemRequest deliverItemRequest = new DeliverItemRequest();
        deliverItemRequest.setSalesOrderPublicId(orderPublicId);
        deliverItemRequest.setRoutePublicId(UUID.fromString(routePublicId));

        ItemRequest itemRequest = new ItemRequest();
        itemRequest.setProductItemPublicId(UUID.fromString(orderItemPublicId));
        itemRequest.setProductReconItemPublicId(UUID.fromString(orderItemPublicId));
        itemRequest.setQuantitySold(isPartiallyFulfilled ? 1 : 2);
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
                .patch("/deliveryAgents/markOrderAsDelivered/{orderPublicId}", orderPublicId)
                .then()
                .statusCode(200);

    }

    public void runCreditItemPayments(String customerPublicId, String amount, String receipt) {

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("amount", amount)
                .queryParam("receipts", Collections.singletonList(receipt))
                .patch("/bnpl/{customerPublicId}/pay", UUID.fromString(customerPublicId))
                .then()
                .statusCode(200);
    }

    public String runTicketWorkflow() {
        String payload = String.format("{\n" +
                "  \"id\": \"123\",\n" +
                "  \"statuses\": [\n" +
                "    \"NEW_TICKET\",\n" +
                "    \"VALIDATED\",\n" +
                "    \"ASSIGNED_TO_LOGISTICS\",\n" +
                "    \"IN_TRANSIT\",\n" +
                "    \"DELIVERED\",\n" +
                "    \"CLOSED\"\n" +
                "  ],\n" +
                "  \"transitions\": [\n" +
                "    {\"name\":\"Validate Ticket\",\"from\":\"NEW_TICKET\",\"to\":\"VALIDATED\"},\n" +
                "    {\"name\":\"Assign to Logistics\",\"from\":\"VALIDATED\",\"to\":\"ASSIGNED_TO_LOGISTICS\"},\n" +
                "    {\"name\":\"Ship Order\",\"from\":\"ASSIGNED_TO_LOGISTICS\",\"to\":\"IN_TRANSIT\"},\n" +
                "    {\"name\":\"Confirm Delivery\",\"from\":\"IN_TRANSIT\",\"to\":\"DELIVERED\"},\n" +
                "    {\"name\":\"Close Ticket\",\"from\":\"DELIVERED\",\"to\":\"CLOSED\"}\n" +
                "  ]\n" +
                "}\n");

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(payload).log().all()
                .post("/workflowEngine/define")
                .then().log().all()
                .statusCode(200);

        String idYaWorkflowPublicId = given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/workflowEngine/define")
                .then().log().all()
                .statusCode(200)
                .extract().path("content[0].publicId");

        return idYaWorkflowPublicId;
    }


    public String createChildCustomer(UUID parentCustomerPublicId){
        String phoneNumber = "07" + String.valueOf(System.currentTimeMillis()).substring(3, 11);

        String body = String.format("""
                {
                    "name": "Child Customer-oo Test",
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
                    "organizationRegionId": "8c5d4db0-f9a1-47ed-a160-56780e4e4d14",
                    "organizationLocationId": "90ad01f1-4a13-41f7-b825-c428b41f35d0",
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

        return given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(body)
                .log().all()
                .post("/customers")
                .then().log().all()
                .statusCode(200)
                .body("publicId", notNullValue())
                .extract().path("publicId");
    }

    // place order,create route,load,accept,deliver


    public String createOrder_createRoute_load_daAccept_deliver(UUID childCustomerPublicId){
        String deliveryAgentToken = loginSeededDeliveryAgentViaPassword();

        SalesOrderItemsRequest orderItems = SalesOrderItemsRequest
                .builder()
                .productId(UUID.fromString("08ebb084-2633-428f-82b9-e8d98a22ec53"))
                .quantity(1)
                .build();
        SalesOrdersRequest request = SalesOrdersRequest.builder()
                .customerId(childCustomerPublicId)
                .bookedBy("Parent Customer")
                .sourceChannel(SourceChannelEnum.CUSTOMER_CARE_APP.name())
                .salesOrderItems(Collections.singleton(orderItems))
                .buyOnCredit(true)
                .orderBookedByParent(true)
                .build();

        String orderPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .log().all()
                .post("/salesOrders")
                .then().log().all()
                .statusCode(200)
                .extract().path("publicId");

        String orderItemPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders/{salesOrderPublicId}/items", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].publicId");

        // transition to planning

        scheduler.runOrderPlanning();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200)
                .body("salesOrderStatus", equalTo(LoadingStatusEnum.PLANNING.name()));


        // create route

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


        //  deliver product

        DeliverItemRequest deliverItemRequest = new DeliverItemRequest();
        deliverItemRequest.setSalesOrderPublicId(UUID.fromString(orderPublicId));
        deliverItemRequest.setRoutePublicId(UUID.fromString(routePublicId));

        ItemRequest itemRequest = new ItemRequest();
        itemRequest.setProductItemPublicId(UUID.fromString(orderItemPublicId));
        itemRequest.setProductReconItemPublicId(UUID.fromString(orderItemPublicId));
        itemRequest.setQuantitySold(1);
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
                .then()
                .statusCode(200)
                .body("salesOrderStatus", equalTo(LoadingStatusEnum.DELIVERED.name()));


        return orderPublicId;
    }
}
