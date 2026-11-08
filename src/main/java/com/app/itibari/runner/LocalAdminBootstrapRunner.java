package com.app.itibari.runner;

import com.app.itibari.dataTypes.IdentificationType;
import com.app.itibari.dataTypes.ProductStatus;
import com.app.itibari.dataTypes.SupplierStatus;
import com.app.itibari.dataTypes.TaxType;
import com.app.itibari.models.entities.Category;
import com.app.itibari.models.entities.Permission;
import com.app.itibari.models.entities.Person;
import com.app.itibari.models.entities.Product;
import com.app.itibari.models.entities.ProductClass;
import com.app.itibari.models.entities.ProductSubClass;
import com.app.itibari.models.entities.Role;
import com.app.itibari.models.entities.Suppliers;
import com.app.itibari.models.entities.TaxCode;
import com.app.itibari.models.entities.TaxMapping;
import com.app.itibari.repository.CategoryRepository;
import com.app.itibari.repository.PermissionRepository;
import com.app.itibari.repository.PersonRepository;
import com.app.itibari.repository.ProductClassRepository;
import com.app.itibari.repository.ProductRepository;
import com.app.itibari.repository.ProductSubClassRepository;
import com.app.itibari.repository.RoleRepository;
import com.app.itibari.repository.SupplierRepository;
import com.app.itibari.repository.TaxCodeRepository;
import com.app.itibari.repository.TaxMappingRepository;
import com.app.itibari.services.helper.SupplierPortalAccessHelper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Local-only bootstrap: ensures ROLE_ADMIN exists with FE module + API permissions,
 * creates/updates a default local admin user, seeds two demo suppliers with portal logins,
 * and ~10 products per supplier.
 *
 * Active when spring.profiles.active includes {@code local}.
 */
@Slf4j
@Order(1)
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalAdminBootstrapRunner implements CommandLineRunner {

    private static final String ADMIN_ROLE = "ROLE_ADMIN";
    private static final String PERMISSIONS_RESOURCE = "classpath:local/admin-permissions.txt";
    private static final String SUPPLIER_PORTAL_PASSWORD = "Supplier@123";
    private static final String DEMO_CATEGORY = "Local Demo Building";
    private static final String DEMO_CLASS = "Local Demo Hardware";
    private static final String DEMO_SUB_CLASS = "Local Demo General";
    private static final String DEMO_TAX_CODE = "LOCAL-VAT-18";
    private static final String DEMO_TAX_NAME = "Local VAT 18%";
    private static final int PRODUCTS_PER_SUPPLIER = 10;

    private static final String[] ALPHA_PRODUCTS = {
            "Alpha Portland Cement 50kg",
            "Alpha Binding Wire 1.5mm",
            "Alpha Roofing Nail 3inch",
            "Alpha Emulsion Paint 20L",
            "Alpha PVC Pipe 1inch",
            "Alpha Timber Plank 2x4",
            "Alpha Iron Sheet Gauge 30",
            "Alpha Aggregate Mix 1T",
            "Alpha Sand Fine 1T",
            "Alpha Concrete Block 6inch"
    };

    private static final String[] BETA_PRODUCTS = {
            "Beta DAP Fertilizer 50kg",
            "Beta Urea Fertilizer 50kg",
            "Beta Maize Seed Hybrid",
            "Beta Herbicide Roundup 1L",
            "Beta Knapsack Sprayer 16L",
            "Beta Chicken Feed Mash 50kg",
            "Beta Animal Salt Block",
            "Beta Irrigation Hose 50m",
            "Beta Gumboots Pair",
            "Beta Wheelbarrow Steel"
    };

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PersonRepository personRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductClassRepository productClassRepository;
    private final ProductSubClassRepository productSubClassRepository;
    private final TaxCodeRepository taxCodeRepository;
    private final TaxMappingRepository taxMappingRepository;
    private final SupplierPortalAccessHelper supplierPortalAccessHelper;
    private final PasswordEncoder passwordEncoder;
    private final ResourceLoader resourceLoader;

    @Value("${itibari.local-admin.enabled:true}")
    private boolean enabled;

    @Value("${itibari.local-admin.email:admin@itibari.local}")
    private String adminEmail;

    @Value("${itibari.local-admin.password:Admin@123}")
    private String adminPassword;

    @Value("${itibari.local-admin.first-name:Local}")
    private String firstName;

    @Value("${itibari.local-admin.last-name:Admin}")
    private String lastName;

    @Value("${itibari.local-admin.phone:+255700000001}")
    private String phone;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (!enabled) {
            log.info("Local admin bootstrap skipped (itibari.local-admin.enabled=false)");
            return;
        }

        Set<String> permissionNames = loadPermissionNames();
        if (permissionNames.isEmpty()) {
            log.warn("Local admin bootstrap: no permissions found in {}", PERMISSIONS_RESOURCE);
            return;
        }

        Role adminRole = ensureAdminRole();
        int[] sync = syncPermissionsOnRole(adminRole, permissionNames);
        Person admin = ensureAdminUser(adminRole);
        List<Suppliers> demoSuppliers = ensureDemoSuppliers(admin);
        DemoCatalog catalog = ensureDemoCatalog(admin);
        int productsCreated = 0;
        productsCreated += ensureDemoProducts(admin, catalog, demoSuppliers.get(0), "A", ALPHA_PRODUCTS);
        productsCreated += ensureDemoProducts(admin, catalog, demoSuppliers.get(1), "B", BETA_PRODUCTS);

        log.info(
                "Local admin bootstrap complete: email={}, role={}, permissionsOnRole={}, newlyLinked={}, revoked={}, passwordReset=true, productsCreated={}",
                admin.getEmail(),
                ADMIN_ROLE,
                adminRole.getPermissions().size(),
                sync[0],
                sync[1],
                productsCreated
        );
        log.info("Local admin credentials: {} / {}", adminEmail, adminPassword);
        for (Suppliers supplier : demoSuppliers) {
            log.info(
                    "Local demo supplier: name={}, email={}, publicId={}, portalPassword={}",
                    supplier.getSupplierName(),
                    supplier.getEmail(),
                    supplier.getPublicId(),
                    SUPPLIER_PORTAL_PASSWORD
            );
        }
    }

    private Set<String> loadPermissionNames() throws Exception {
        Set<String> names = new LinkedHashSet<>();
        Resource resource = resourceLoader.getResource(PERMISSIONS_RESOURCE);
        if (!resource.exists()) {
            return names;
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String name = line.trim();
                if (!StringUtils.hasText(name) || name.startsWith("#")) {
                    continue;
                }
                names.add(name);
            }
        }
        return names;
    }

    private Role ensureAdminRole() {
        Optional<Role> existing = roleRepository.findByRoleNameAndDeletedIsFalse(ADMIN_ROLE);
        if (existing.isPresent()) {
            return existing.get();
        }
        Role role = new Role();
        role.setRoleName(ADMIN_ROLE);
        role.setSupplierRole(false);
        Role saved = roleRepository.save(role);
        log.info("Created role {}", ADMIN_ROLE);
        return saved;
    }

    /**
     * Adds missing IMS permissions and removes Supplier Portal / BNPL (and any other) extras
     * so local admin sidebar matches the core Fedha/IMS menu.
     *
     * @return int[]{attachedCount, revokedCount}
     */
    private int[] syncPermissionsOnRole(Role adminRole, Set<String> permissionNames) {
        Set<String> desired = permissionNames.stream()
                .map(String::toUpperCase)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<Permission> toAttach = new ArrayList<>();
        for (String name : permissionNames) {
            Permission permission = permissionRepository.findByNameIgnoreCaseAndDeletedIsFalse(name)
                    .orElseGet(() -> {
                        Permission created = new Permission();
                        created.setName(name);
                        created.setSupplierRole(false);
                        Permission saved = permissionRepository.save(created);
                        log.debug("Created permission {}", name);
                        return saved;
                    });
            boolean alreadyOnRole = adminRole.getPermissions().stream()
                    .anyMatch(p -> p.getName() != null && p.getName().equalsIgnoreCase(name));
            if (!alreadyOnRole) {
                toAttach.add(permission);
            }
        }

        List<Permission> toRevoke = adminRole.getPermissions().stream()
                .filter(p -> p.getName() == null || !desired.contains(p.getName().toUpperCase()))
                .toList();

        for (Permission permission : toAttach) {
            adminRole.addPermission(permission);
        }
        for (Permission permission : toRevoke) {
            adminRole.removePermission(permission);
            log.info("Revoked permission {} from {}", permission.getName(), ADMIN_ROLE);
        }
        if (!toAttach.isEmpty() || !toRevoke.isEmpty()) {
            roleRepository.save(adminRole);
        }
        return new int[]{toAttach.size(), toRevoke.size()};
    }

    private Person ensureAdminUser(Role adminRole) {
        Optional<Person> existing = personRepository.findByEmailAndDeletedIsFalse(adminEmail);
        Person person;
        if (existing.isEmpty()) {
            person = new Person();
            person.setEmail(adminEmail);
            person.setFirstName(firstName);
            person.setLastName(lastName);
            person.setPhoneNumber(phone);
            person.setHasSupplierAccount(false);
            person.setMainSupplier(false);
            person.setFinanceServiceOfficer(false);
            person.setAcceptedTerms(true);
            log.info("Creating local admin user {}", adminEmail);
        } else {
            person = existing.get();
            person.setFirstName(firstName);
            person.setLastName(lastName);
            person.setPhoneNumber(phone);
            log.info("Updating local admin user {}", adminEmail);
        }
        person.setPassword(passwordEncoder.encode(adminPassword));
        person.addRole(adminRole);
        return personRepository.save(person);
    }

    private List<Suppliers> ensureDemoSuppliers(Person admin) {
        List<Suppliers> seeded = new ArrayList<>(2);
        seeded.add(ensureDemoSupplier(
                admin,
                "Local Alpha Supplies",
                "supplier-a@itibari.local",
                "+255700000101",
                "Alpha Contact",
                "TIN-LOCAL-A001"));
        seeded.add(ensureDemoSupplier(
                admin,
                "Local Beta Supplies",
                "supplier-b@itibari.local",
                "+255700000102",
                "Beta Contact",
                "TIN-LOCAL-B002"));
        return seeded;
    }

    private Suppliers ensureDemoSupplier(Person admin,
                                         String supplierName,
                                         String email,
                                         String phoneNumber,
                                         String pointOfContact,
                                         String tinNumber) {
        Suppliers supplier = supplierRepository.findBySupplierNameIgnoreCaseAndDeletedIsFalse(supplierName)
                .orElseGet(() -> {
                    Suppliers created = new Suppliers();
                    created.setSupplierName(supplierName);
                    created.setSupplierAddress("Bungoma Local Demo Yard");
                    created.setSupplierPointOfContact(pointOfContact);
                    created.setPhoneNumber(phoneNumber);
                    created.setEmail(email);
                    created.setLeadDays(7);
                    created.setCreditTerms(30);
                    created.setStatus(SupplierStatus.ACTIVE);
                    created.setTinNumber(tinNumber);
                    created.setCreditLimit(new BigDecimal("5000000"));
                    created.setPostalAddress("P.O. Box 1, Bungoma");
                    created.setBankName("Local Demo Bank");
                    created.setBankAccountName(supplierName);
                    created.setBankAccountNumber("000000" + phoneNumber.substring(phoneNumber.length() - 4));
                    created.setPortalAccessGranted(false);
                    created.setCreatedBy(admin);
                    created.setCreationTime(LocalDateTime.now());
                    Suppliers saved = supplierRepository.save(created);
                    log.info("Created local demo supplier {}", supplierName);
                    return saved;
                });

        ensureDemoSupplierPortalUser(admin, supplier);
        return supplier;
    }

    /**
     * Creates (or refreshes) the main supplier-portal login without going through SMS password delivery.
     */
    private void ensureDemoSupplierPortalUser(Person admin, Suppliers supplier) {
        String supplierPublicId = supplier.getPublicId().toString();
        Optional<Person> existingPortalUser = personRepository.findByEmailAndDeletedIsFalse(supplier.getEmail());

        if (existingPortalUser.isEmpty()) {
            // Builds role + permission group via the same helper used by procurement portal grant.
            supplierPortalAccessHelper.grantMainSupplierPortalAccess(supplier, admin);
            existingPortalUser = personRepository.findByEmailAndDeletedIsFalse(supplier.getEmail());
        } else if (!Boolean.TRUE.equals(supplier.getPortalAccessGranted())) {
            supplier.setPortalAccessGranted(true);
            supplierRepository.save(supplier);
        }

        if (existingPortalUser.isEmpty()) {
            log.warn("Local demo supplier portal user missing after grant for {}", supplier.getEmail());
            return;
        }

        Person portalUser = existingPortalUser.get();
        portalUser.setHasSupplierAccount(true);
        portalUser.setMainSupplier(true);
        portalUser.setSupplierPublicId(supplierPublicId);
        portalUser.setPhoneNumber(supplier.getPhoneNumber());
        portalUser.setIdentificationType(IdentificationType.TIN);
        portalUser.setIdentificationNumber(supplier.getTinNumber());
        portalUser.setAcceptedTerms(true);
        portalUser.setPassword(passwordEncoder.encode(SUPPLIER_PORTAL_PASSWORD));
        personRepository.save(portalUser);

        supplierPortalAccessHelper.ensureSupplierPortalRolePermissions(supplierPublicId);
    }

    private DemoCatalog ensureDemoCatalog(Person admin) {
        Category category = categoryRepository.findByNameIgnoreCaseAndDeletedIsFalse(DEMO_CATEGORY)
                .orElseGet(() -> {
                    Category created = new Category();
                    created.setName(DEMO_CATEGORY);
                    created.setDescription("Local bootstrap product category");
                    created.setCreatedBy(admin);
                    created.setCreationTime(LocalDateTime.now());
                    return categoryRepository.save(created);
                });

        ProductClass productClass = productClassRepository.findByNameIgnoreCaseAndDeletedIsFalse(DEMO_CLASS)
                .orElseGet(() -> {
                    ProductClass created = new ProductClass();
                    created.setName(DEMO_CLASS);
                    created.setDescription("Local bootstrap product class");
                    created.setCategory(category);
                    created.setCreatedBy(admin);
                    created.setCreationTime(LocalDateTime.now());
                    return productClassRepository.save(created);
                });

        ProductSubClass productSubClass = productSubClassRepository
                .findByProductClassAndNameIgnoreCaseAndDeletedIsFalse(productClass, DEMO_SUB_CLASS)
                .orElseGet(() -> {
                    ProductSubClass created = new ProductSubClass();
                    created.setName(DEMO_SUB_CLASS);
                    created.setProductClass(productClass);
                    created.setCreatedBy(admin);
                    created.setCreationTime(LocalDateTime.now());
                    return productSubClassRepository.save(created);
                });

        TaxCode taxCode = taxCodeRepository.findByCodeNameIgnoreCase(DEMO_TAX_CODE)
                .orElseGet(() -> {
                    TaxCode created = new TaxCode();
                    created.setCode(1);
                    created.setCodeName(DEMO_TAX_CODE);
                    created.setCodeValue(new BigDecimal("0.1800"));
                    created.setCreatedBy(admin);
                    created.setCreationTime(LocalDateTime.now());
                    return taxCodeRepository.save(created);
                });

        TaxMapping taxMapping = taxMappingRepository.findByTaxNameIgnoreCaseAndDeletedIsFalse(DEMO_TAX_NAME)
                .orElseGet(() -> {
                    TaxMapping created = new TaxMapping();
                    created.setTaxCode("A");
                    created.setTaxName(DEMO_TAX_NAME);
                    created.setTaxRate(new BigDecimal("18.00"));
                    created.setTaxType(TaxType.Percentage);
                    created.setCreatedBy(admin);
                    created.setCreationTime(LocalDateTime.now());
                    return taxMappingRepository.save(created);
                });

        return new DemoCatalog(category, productClass, productSubClass, taxCode, taxMapping);
    }

    private int ensureDemoProducts(Person admin,
                                   DemoCatalog catalog,
                                   Suppliers supplier,
                                   String skuPrefix,
                                   String[] productNames) {
        int createdCount = 0;
        int limit = Math.min(PRODUCTS_PER_SUPPLIER, productNames.length);
        for (int i = 0; i < limit; i++) {
            String productName = productNames[i];
            String sku = String.format("LOCAL-%s-%02d", skuPrefix, i + 1);
            Optional<Product> existing = productRepository.findByProductNameIgnoreCaseAndDeletedIsFalse(productName);
            if (existing.isEmpty()) {
                existing = productRepository.findProductBySkuIgnoreCaseAndDeletedIsFalse(sku);
            }
            if (existing.isPresent()) {
                Product product = existing.get();
                product.addSupplier(supplier);
                applyLocalProductTaxAmounts(product, catalog.taxCode());
                productRepository.save(product);
                continue;
            }

            BigDecimal buying = BigDecimal.valueOf(5_000L + (i * 1_250L)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal selling = buying.multiply(new BigDecimal("1.25")).setScale(2, RoundingMode.HALF_UP);
            BigDecimal weightTonnes = BigDecimal.valueOf(0.025 + (i * 0.005)).setScale(3, RoundingMode.HALF_UP);

            Product product = new Product();
            product.setProductName(productName);
            product.setSku(sku);
            product.setBarcode("BC-" + sku);
            product.setCategory(catalog.category());
            product.setProductClass(catalog.productClass());
            product.setProductSubClass(catalog.productSubClass());
            product.setTaxCode(catalog.taxCode());
            product.setTaxMapping(catalog.taxMapping());
            product.setBrand(skuPrefix.equals("A") ? "Alpha Local" : "Beta Local");
            product.setBuyingPrice(buying);
            product.setSellPrice(selling);
            applyLocalProductTaxAmounts(product, catalog.taxCode());
            product.setMargin(new BigDecimal("25.00"));
            product.setWeight(weightTonnes);
            product.setLength(20 + i);
            product.setWidth(10 + i);
            product.setHeight(5 + i);
            product.setUnitOfMeasure("PCS");
            product.setPackageSize("1");
            product.setBulkPurchaseUnit(1);
            product.setBulkPurchaseUnitName("Unit");
            product.setSellingUnit(1);
            product.setSellingUnitName("Unit");
            product.setAvailableStock(100L + (i * 10L));
            product.setProductStatus(ProductStatus.InStock);
            product.setDescription("Local demo product for " + supplier.getSupplierName());
            product.setCreatedBy(admin);
            product.setCreationTime(LocalDateTime.now());
            product.addSupplier(supplier);
            productRepository.save(product);
            createdCount++;
        }
        log.info(
                "Local demo products for {}: ensured={}, newlyCreated={}",
                supplier.getSupplierName(),
                limit,
                createdCount
        );
        return createdCount;
    }

    private void applyLocalProductTaxAmounts(Product product, TaxCode taxCode) {
        BigDecimal selling = product.getSellPrice() != null ? product.getSellPrice() : BigDecimal.ZERO;
        BigDecimal buying = product.getBuyingPrice() != null ? product.getBuyingPrice() : BigDecimal.ZERO;
        BigDecimal taxRate = taxCode != null && taxCode.getCodeValue() != null
                ? taxCode.getCodeValue()
                : BigDecimal.ZERO;

        if (taxRate.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal sellingBeforeTax = selling.divide(taxRate.add(BigDecimal.ONE), 2, RoundingMode.HALF_UP);
            BigDecimal buyingBeforeTax = buying.divide(taxRate.add(BigDecimal.ONE), 2, RoundingMode.HALF_UP);
            product.setSellingPriceBeforeTax(sellingBeforeTax);
            product.setTaxAmount(selling.subtract(sellingBeforeTax).setScale(2, RoundingMode.HALF_UP));
            product.setBuyingPriceBeforeTax(buyingBeforeTax);
            product.setBuyingTaxAmount(buying.subtract(buyingBeforeTax).setScale(2, RoundingMode.HALF_UP));
        } else {
            product.setSellingPriceBeforeTax(selling);
            product.setTaxAmount(BigDecimal.ZERO);
            product.setBuyingPriceBeforeTax(buying);
            product.setBuyingTaxAmount(BigDecimal.ZERO);
        }

        if (taxCode != null) {
            product.setTaxCode(taxCode);
        }
    }

    private record DemoCatalog(
            Category category,
            ProductClass productClass,
            ProductSubClass productSubClass,
            TaxCode taxCode,
            TaxMapping taxMapping
    ) {
    }
}
