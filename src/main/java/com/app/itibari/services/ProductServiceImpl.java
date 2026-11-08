package com.app.itibari.services;

import com.app.itibari.dataTypes.*;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.*;
import com.app.itibari.models.response.ProductPageable;
import com.app.itibari.models.response.ProductResponse;
import com.app.itibari.models.response.ProductTransformerDTO;
import com.app.itibari.models.response.ProductViewResponse;
import com.app.itibari.repository.*;
import com.app.itibari.services.helper.LiveStockHelper;
import com.app.itibari.services.helper.ProductImageResolver;
import com.app.itibari.services.helper.StockUpdateHelper;
import com.app.itibari.services.helper.SupplierPortalAccessHelper;
import com.app.itibari.transformer.ProductTransformer;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.helpers.Validators;
import com.app.itibari.utils.specification.ProductAdjustmentSpecification;
import com.app.itibari.utils.specification.ProductSpecification;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.common.aliasing.qual.Unique;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private static final Set<String> NON_TAXABLE_CODES = Set.of("Zero Rate", "Exempt");
    private static final int SCALE = 2;
    private final InventoryProductsRepository inventoryProductsRepository;
    private final CategoryRepository categoryRepository;
    private final PersonRepository personRepository;
    private final ProductClassRepository productClassRepository;
    private final ProductSubClassRepository productSubClassRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final FinancialService financialService;
    private final CounterService counterService;
    private final ProductAdjustmentRepository productAdjustmentRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryRepository inventoryRepository;
    private final EventsRepository eventsRepository;
    private final LiveStockHelper liveStockHelper;
    private final SupplierPortalAccessHelper supplierPortalAccessHelper;
    private final StockUpdateHelper stockUpdateHelper;
    @Lazy
    private final ProductTransformer productTransformer;
    private final TaxCodeRepository taxCodeRepository;
    private final ProductStockRepository productStockRepository;
    private final SupplierTargetRepository supplierTargetRepository;
    private final ProductBrandRepository productBrandRepository;
    private final ProductImageResolver productImageResolver;
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Category createCategory(CategoryRequest categoryRequest, String loggedInUser) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(loggedInUser);
        Optional<Category> categoryOptional = categoryRepository.findByNameIgnoreCaseAndDeletedIsFalse(categoryRequest.getCategoryName());

        if (categoryOptional.isPresent()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Category with name " + categoryRequest.getCategoryName() + " already exists")
                    .userMessage("Sorry. Category with name " + categoryRequest.getCategoryName() + " already exists");
        }

        Category category = new Category();
        category.setName(categoryRequest.getCategoryName());
        category.setDescription(categoryRequest.getCategoryDescription());
        optionalPerson.ifPresent(category::setCreatedBy);
        category.setCreationTime(LocalDateTime.now());

        Category saved = categoryRepository.save(category);
        productImageResolver.enrichCategory(saved);
        return saved;
    }

    @Override
    public void updateCategory(UUID publicId, CategoryRequest categoryRequest, String name) {

        Optional<Category> categoryOptional = categoryRepository.findByPublicIdAndDeletedIsFalse(publicId);
        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        if (categoryOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Category with public id " + publicId + " not found")
                    .userMessage("Sorry. Category with public id " + publicId + " not found");
        }

        Category category = categoryOptional.get();
        category.setName(categoryRequest.getCategoryName());
        category.setDescription(categoryRequest.getCategoryDescription());
        optionalPerson.ifPresent(category::setUpdatedBy);
        category.setUpdateDate(LocalDateTime.now());

        categoryRepository.save(category);
    }

    @Override
    public Category getCategory(UUID publicId) {

        Optional<Category> categoryOptional = categoryRepository.findByPublicIdAndDeletedIsFalse(publicId);

        if (categoryOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Category with public id " + publicId + " not found")
                    .userMessage("Sorry. Category with public id " + publicId + " not found");
        }

        Category category = categoryOptional.get();
        productImageResolver.enrichCategory(category);
        return category;
    }

    @Override
    public Page<Category> getCategories(String searchParam, Pageable pageable) {

        Page<Category> page;
        if (searchParam != null && searchParam.length() > 3) {
            page = categoryRepository.findByNameContainingIgnoreCaseAndDeletedIsFalse(searchParam, pageable);
        } else {
            page = categoryRepository.findByDeletedIsFalse(pageable);
        }
        productImageResolver.enrichCategories(page.getContent());
        return page;
    }

    @Override
    public void deleteCategory(UUID publicId, String name) {

        Optional<Category> categoryOptional = categoryRepository.findByPublicIdAndDeletedIsFalse(publicId);
        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        if (categoryOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Category with public id " + publicId + " not found")
                    .userMessage("Sorry. Category with public id " + publicId + " not found");
        }

        Category category = categoryOptional.get();
        optionalPerson.ifPresent(category::setUpdatedBy);
        category.setUpdateDate(LocalDateTime.now());
        category.setDeleted(true);

        categoryRepository.save(category);
    }

    /*
    product classs
     */

    @Override
    public ProductClass createClass(ProductClassRequest productClassRequest, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);
        Optional<ProductClass> productClassOptional = productClassRepository.findByNameIgnoreCaseAndDeletedIsFalse(productClassRequest.getName());
        Optional<Category> optionalCategory = categoryRepository.findByPublicIdAndDeletedIsFalse(productClassRequest.getCategoryPublicId());

        if (optionalCategory.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Category with public id " + productClassRequest.getCategoryPublicId() + " not found")
                    .userMessage("Sorry. Category with public id " + productClassRequest.getCategoryPublicId() + " not found");
        }

        if (productClassOptional.isPresent()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product class with name " + productClassRequest.getName() + " already exists")
                    .userMessage("Sorry. Product class with name " + productClassRequest.getDescription() + " already exists");
        }

        ProductClass productClass = new ProductClass();
        productClass.setName(productClassRequest.getName());
        productClass.setDescription(productClassRequest.getDescription());
        optionalPerson.ifPresent(productClass::setCreatedBy);
        optionalCategory.ifPresent(productClass::setCategory);
        productClass.setCreationTime(LocalDateTime.now());

        return productClassRepository.save(productClass);
    }

    @Override
    public void updateClass(UUID publicId, ProductClassRequest productClassRequest, String name) {

        Optional<ProductClass> productClassOptional = productClassRepository.findByPublicIdAndDeletedIsFalse(publicId);
        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);
        Optional<Category> optionalCategory = categoryRepository.findByPublicIdAndDeletedIsFalse(productClassRequest.getCategoryPublicId());

        if (optionalCategory.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Category with public id " + productClassRequest.getCategoryPublicId() + " not found")
                    .userMessage("Sorry. Category with public id " + productClassRequest.getCategoryPublicId() + " not found");
        }

        if (productClassOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product class with public id " + publicId + " not found")
                    .userMessage("Sorry. Product class with public id " + publicId + " not found");
        }

        ProductClass productClass = productClassOptional.get();
        productClass.setName(productClassRequest.getName());
        productClass.setDescription(productClassRequest.getDescription());
        optionalPerson.ifPresent(productClass::setUpdatedBy);
        optionalCategory.ifPresent(productClass::setCategory);
        productClass.setUpdateDate(LocalDateTime.now());

        productClassRepository.save(productClass);
    }

    @Override
    public ProductClass getProductClass(UUID publicId) {

        return productClassRepository
                .findByPublicIdAndDeletedIsFalse(publicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Product class with public id " + publicId + " not found")
                        .userMessage("Sorry. Product class with public id " + publicId + " not found"));
    }

    @Override
    public Page<ProductClass> getProductClasses(String searchParam, UUID categoryPublicId, Pageable pageable) {

        if (searchParam != null && searchParam.length() > 3) {
            return productClassRepository.findByNameContainingIgnoreCaseAndDeletedIsFalse(searchParam, pageable);
        } else if (categoryPublicId != null) {
            Optional<Category> optionalCategory = categoryRepository.findByPublicIdAndDeletedIsFalse(categoryPublicId);
            if (optionalCategory.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Category with public id " + categoryPublicId + " not found")
                        .userMessage("Sorry. Category with public id " + categoryPublicId + " not found");
            }
            return productClassRepository.findByCategoryAndDeletedIsFalse(optionalCategory.get(), pageable);
        }
        return productClassRepository.findByDeletedIsFalse(pageable);
    }

    @Override
    public void deleteClass(UUID publicId, String name) {

        Optional<ProductClass> productClassOptional = productClassRepository.findByPublicIdAndDeletedIsFalse(publicId);
        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        if (productClassOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product class with public id " + publicId + " not found")
                    .userMessage("Sorry. Product class with public id " + publicId + " not found");
        }

        ProductClass productClass = productClassOptional.get();
        optionalPerson.ifPresent(productClass::setUpdatedBy);
        productClass.setUpdateDate(LocalDateTime.now());
        productClass.setDeleted(true);

        productClassRepository.save(productClass);
    }

    /*
    product sub_class
     */

    @Override
    public ProductSubClass createSubClass(UUID publicId, ProductSubClassRequest productSubClassRequest, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);
        Optional<ProductClass> productClassOptional = productClassRepository.findByPublicIdAndDeletedIsFalse(publicId);
        Optional<ProductSubClass> productSubClassOptional = productSubClassRepository.findByNameIgnoreCaseAndDeletedIsFalse(
                productSubClassRequest.getName());

        if (productSubClassOptional.isPresent()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product sub class with name " + productSubClassRequest.getName() + " already exists")
                    .userMessage("Sorry. Product sub class with name " + productSubClassRequest.getDescription() + " already exists");
        }

        if (productClassOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product class with public id " + publicId + " not found")
                    .userMessage("Sorry. Product class with public id " + publicId + " not found");
        }

        ProductSubClass productSubClass = new ProductSubClass();
        productSubClass.setName(productSubClassRequest.getName());
        productClassOptional.ifPresent(productSubClass::setProductClass);
        productSubClass.setCreationTime(LocalDateTime.now());
        optionalPerson.ifPresent(productSubClass::setCreatedBy);

        return productSubClassRepository.save(productSubClass);
    }

    @Override
    public void updateSubClass(UUID publicId, UUID subClassPublicId, ProductSubClassRequest productSubClassRequest, String name) {

        ProductClass productClass = productClassRepository
                .findByPublicIdAndDeletedIsFalse(publicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Product class with public id " + publicId + " not found")
                        .userMessage("Sorry. Product class with public id " + publicId + " not found"));

        Optional<ProductSubClass> productSubClassOptional = productSubClassRepository.findByPublicIdAndProductClassAndDeletedIsFalse(
                subClassPublicId,
                productClass);
        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        if (productSubClassOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product sub class with public id " + subClassPublicId + " not found")
                    .userMessage("Sorry. Product sub class with public id " + subClassPublicId + " not found");
        }

        ProductSubClass productSubClass = productSubClassOptional.get();
        productSubClass.setName(productSubClassRequest.getName());
        optionalPerson.ifPresent(productSubClass::setUpdatedBy);
        productSubClass.setUpdateDate(LocalDateTime.now());

        productSubClassRepository.save(productSubClass);
    }

    @Override
    public ProductSubClass getSubClass(UUID subClassPublicId) {

        return productSubClassRepository
                .findByPublicIdAndDeletedIsFalse(subClassPublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Product sub class with public id " + subClassPublicId + " not found")
                        .userMessage("Sorry. Product sub class with public id " + subClassPublicId + " not found"));
    }

    @Override
    public Page<ProductSubClass> getSubClasses(String searchParam, UUID classPublicId, Pageable pageable) {

        if (searchParam != null && searchParam.length() > 3) {
            return productSubClassRepository.findByNameContainingIgnoreCaseAndDeletedIsFalse(searchParam, pageable);
        } else if (classPublicId != null) {
            Optional<ProductClass> productClassOptional = productClassRepository.findByPublicIdAndDeletedIsFalse(classPublicId);
            if (productClassOptional.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Product class with public id " + classPublicId + " not found")
                        .userMessage("Sorry. Product class with public id " + classPublicId + " not found");
            }
            return productSubClassRepository.findByProductClassAndDeletedIsFalse(productClassOptional.get(), pageable);
        }
        return productSubClassRepository.findByDeletedIsFalse(pageable);
    }

    @Override
    public void deleteSubClass(UUID subClassPublicId, String name) {

        Optional<ProductSubClass> productSubClassOptional = productSubClassRepository.findByPublicIdAndDeletedIsFalse(subClassPublicId);
        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        if (productSubClassOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product sub class with public id " + subClassPublicId + " not found")
                    .userMessage("Sorry. Product sub class with public id " + subClassPublicId + " not found");
        }

        ProductSubClass productSubClass = productSubClassOptional.get();
        optionalPerson.ifPresent(productSubClass::setUpdatedBy);
        productSubClass.setUpdateDate(LocalDateTime.now());
        productSubClass.setDeleted(true);

        productSubClassRepository.save(productSubClass);
    }

    /*
    product
     */
    @Override
    public Product createProduct(ProductRequest productRequest, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        Optional<Product> optionalProduct = productRepository.findByProductNameIgnoreCaseAndDeletedIsFalse(productRequest.getProductName());

        if (Validators.anyEqualNull(productRequest.getLength(),
                productRequest.getWidth(),
                productRequest.getHeight(),
                productRequest.getSupplierPublicIds())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product dimensions cannot be null")
                    .userMessage("Sorry. Product dimensions cannot be null");
        }

        if (productRequest.getSupplierPublicIds().isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product must have at least one supplier")
                    .userMessage("Sorry. Product must have at least one supplier");
        }

        if (optionalProduct.isPresent()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product with name " + productRequest.getProductName() + " already exists")
                    .userMessage("Sorry. Product with name " + productRequest.getProductName() + " already exists");
        }

        Category category = categoryRepository
                .findByPublicIdAndDeletedIsFalse(productRequest.getCategoryPublicId())
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Category with public id " + productRequest.getCategoryPublicId() + " not found")
                        .userMessage("Sorry. Category with public id " + productRequest.getCategoryPublicId() + " not found"));

        List<Suppliers> suppliers = new ArrayList<>();
        productRequest.getSupplierPublicIds().forEach(supplierPublicId -> {
            Suppliers supplier = supplierRepository
                    .findByPublicIdAndDeletedIsFalse(supplierPublicId)
                    .orElseThrow(() -> new BadRequestRestApiException()
                            .developerMessage("Supplier with public id " + supplierPublicId + " not found")
                            .userMessage("Sorry. Supplier not found"));
            suppliers.add(supplier);
        });

        ProductClass productClass = getProductClass(productRequest.getClassPublicId());

        ProductSubClass productSubClass = getSubClass(productRequest.getSubClassPublicId());

        TaxMapping taxMapping = financialService.getTaxMappingByTaxId(productRequest.getTaxMappingPublicId());

        TaxCode taxCode = taxCodeRepository.findByPublicId(productRequest.getTaxCodePublicId()).orElse(null);
        if(taxCode == null){
            throw new BadRequestRestApiException()
                    .developerMessage("Tax code not found")
                    .userMessage("Sorry. Tax code not found");
        }

        Product product = new Product();
        product.setProductName(productRequest.getProductName());
        product.setCategory(category);
        product.setBarcode(productRequest.getBarcode());
        product.setBuyingPrice(productRequest.getBuyingPrice());
        product.setSellPrice(productRequest.getSellPrice());
        product.setTaxMapping(taxMapping);
        product.setCustomTaxAddition(productRequest.getCustomTaxAddition());
        product.setProductClass(productClass);
        product.setUnitOfMeasure(productRequest.getUnitOfMeasurement());
        product.setProductSubClass(productSubClass);
        product.setBrand(productRequest.getBrand());
        product.setWeight(productRequest.getWeight());
        product.setLength(productRequest.getLength());
        product.setWidth(productRequest.getWidth());
        product.setHeight(productRequest.getHeight());
        product.setMargin(productRequest.getMargin());
        product.setPackageSize(productRequest.getPackageSize());
        product.setCreationTime(LocalDateTime.now());
        optionalPerson.ifPresent(product::setCreatedBy);
        suppliers.forEach(product::addSupplier);
        product.setBulkPurchaseUnit(productRequest.getBulkPurchaseUnit());
        product.setBulkPurchaseUnitName(productRequest.getBulkPurchaseUnitName());
        product.setSellingUnit(productRequest.getSellingUnit());
        product.setDescription(productRequest.getDescription());
        product.setTaxCode(taxCode);
        product.setSellingUnitName(productRequest.getSellingUnitName());
        product.setNotification(productRequest.isNotification());
        product.setCreationTimeYaNotification(LocalDateTime.now());

        product.setProductStatus(ProductStatus.InStock);
        product.setSku("SKU" + counterService.getNextCounter(CounterType.PRODUCTS));

        if (productRequest.getProductBrandPublicId() != null) {
            ProductBrand productBrand = productBrandRepository.findByPublicIdAndDeletedIsFalse(productRequest.getProductBrandPublicId()).orElse(null);
            if (productBrand == null) {
                throw new BadRequestRestApiException()
                        .developerMessage("Product brand not found")
                        .userMessage("Sorry. Product brand not found");
            }

            product.setProductBrand(productBrand);
        }

        Product savedProduct = productRepository.save(product);
        calculateAmountWithTax(savedProduct);
        runAddProductToAllWarehouses(product);

        if (!productRequest.getSupplierTargetRequestDtos().isEmpty()) {
            runAddSupplierTarget(product.getPublicId(), product.getProductName(),
                    productRequest.getSupplierTargetRequestDtos(), savedProduct);
        }


        return product;
    }

    private void runAddSupplierTarget(UUID productPublicId, String productName,
                                      List<SupplierTargetRequestDto> supplierTargetRequestDtos, Product savedProduct) {

        supplierTargetRequestDtos.forEach(dto -> {
            Suppliers supplier = supplierRepository
                    .findByPublicIdAndDeletedIsFalse(dto.supplierPublicId())
                    .orElseThrow(() -> new BadRequestRestApiException()
                            .developerMessage("Supplier with id " + dto.supplierPublicId() + " not found")
                            .userMessage("Sorry. Supplier not found"));

            SupplierTarget supplierTarget = new SupplierTarget();
            supplierTarget.setTargetValue(dto.targetValue());
            supplierTarget.setProductName(productName);
            supplierTarget.setProductPublicId(productPublicId);
            supplierTarget.setSupplierName(supplier.getSupplierName());
            supplierTarget.setSupplierPublicId(supplier.getPublicId());
            supplierTarget.setProduct(savedProduct);

            supplierTargetRepository.save(supplierTarget);
        });
    }


    @Transactional
    @Override
    public void calculateAmountWithTaxV2(Product savedProduct) {
        if (savedProduct == null) {
            log.warn("Product cannot be found, skipping tax calculation.");
            return;
        }

        BigDecimal sellPrice = savedProduct.getSellPrice();
        if (sellPrice == null) {
            log.warn("Sell price cannot be null for product {}", savedProduct.getPublicId());
            return;
        }

        BigDecimal amountBeforeTax;
        BigDecimal taxAmount;

        if (savedProduct.getTaxCode() != null) {
            if (NON_TAXABLE_CODES.contains(savedProduct.getTaxCode().getCodeName())) {

                log.info("============exampted+++++++++++++{}", savedProduct.getSku());

                amountBeforeTax = sellPrice;
                taxAmount = BigDecimal.ZERO;

            } else {

                log.info("============taxed+++++++++++++{} {}", savedProduct.getTaxCode().getCodeValue(), savedProduct.getSku());
                BigDecimal taxRate = savedProduct.getTaxCode() != null
                        ? savedProduct.getTaxCode().getCodeValue()
                        : BigDecimal.ZERO;

                BigDecimal taxValue = taxRate.add(BigDecimal.ONE);
                amountBeforeTax = sellPrice.divide(taxValue, 4, RoundingMode.HALF_UP);
                taxAmount = sellPrice.divide(taxValue, 4, RoundingMode.HALF_UP).multiply(taxRate).setScale(4, RoundingMode.HALF_UP);
            }
            savedProduct.setSellingPriceBeforeTax(amountBeforeTax);
            savedProduct.setTaxAmount(taxAmount);

            log.info("==============Product {}: =======sellPrice={}, ======taxRate={}, ====beforeTax={}, taxAmount={}================",
                    savedProduct.getPublicId(), sellPrice,
                    savedProduct.getTaxCode() != null ? savedProduct.getTaxCode().getCodeValue() : BigDecimal.ZERO,
                    amountBeforeTax, taxAmount);

            productRepository.save(savedProduct);
        }


    }


    @Override
    public void updateProduct(UUID publicId, ProductRequest productRequest, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);
        Optional<Product> optionalProduct = productRepository.findByPublicIdAndDeletedIsFalse(publicId);

        if (optionalProduct.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product with public id " + publicId + " not found")
                    .userMessage("Sorry. Product with public id " + publicId + " not found");
        }

        Product product = optionalProduct.get();

        if (Validators.anyEqualNull(productRequest.getLength(),
                productRequest.getWidth(),
                productRequest.getHeight(),
                productRequest.getSupplierPublicIds())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product dimensions cannot be null")
                    .userMessage("Sorry. Product dimensions cannot be null");
        }

        if (productRequest.getSupplierPublicIds().isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product must have at least one supplier")
                    .userMessage("Sorry. Product must have at least one supplier");
        }

        Category category = categoryRepository
                .findByPublicIdAndDeletedIsFalse(productRequest.getCategoryPublicId())
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Category with public id " + productRequest.getCategoryPublicId() + " not found")
                        .userMessage("Sorry. Category with public id " + productRequest.getCategoryPublicId() + " not found"));

        ProductClass productClass = getProductClass(productRequest.getClassPublicId());

        ProductSubClass productSubClass = getSubClass(productRequest.getSubClassPublicId());

        List<Suppliers> suppliers = new ArrayList<>();
        productRequest.getSupplierPublicIds().forEach(supplierPublicId -> {
            Suppliers supplier = supplierRepository
                    .findByPublicIdAndDeletedIsFalse(supplierPublicId)
                    .orElseThrow(() -> new BadRequestRestApiException()
                            .developerMessage("Supplier with public id " + supplierPublicId + " not found")
                            .userMessage("Sorry. Supplier with public id " + supplierPublicId + " not found"));
            suppliers.add(supplier);
        });

        TaxMapping taxMapping = financialService.getTaxMappingByTaxId(productRequest.getTaxMappingPublicId());

        TaxCode taxCode = taxCodeRepository.findByPublicId(productRequest.getTaxCodePublicId()).orElse(null);
        if(taxCode == null){
            throw new BadRequestRestApiException()
                    .developerMessage("Tax code not found")
                    .userMessage("Sorry. Tax code not found");
        }

        product.setProductName(productRequest.getProductName());
        product.setCategory(category);
        product.setBarcode(productRequest.getBarcode());
        product.setBuyingPrice(productRequest.getBuyingPrice());
        product.setSellPrice(productRequest.getSellPrice());
        product.setTaxMapping(taxMapping);
        product.setCustomTaxAddition(productRequest.getCustomTaxAddition());
        product.setProductClass(productClass);
        product.setProductSubClass(productSubClass);
        product.setBrand(productRequest.getBrand());
        product.setWeight(productRequest.getWeight());
        product.setLength(productRequest.getLength());
        product.setWidth(productRequest.getWidth());
        product.setHeight(productRequest.getHeight());
        product.setUnitOfMeasure(productRequest.getUnitOfMeasurement());
        product.setMargin(productRequest.getMargin());
        product.setPackageSize(productRequest.getPackageSize());
        optionalPerson.ifPresent(product::setUpdatedBy);
        product.setUpdateDate(LocalDateTime.now());
        suppliers.forEach(product::addSupplier);
        product.setBulkPurchaseUnit(productRequest.getBulkPurchaseUnit());
        product.setBulkPurchaseUnitName(productRequest.getBulkPurchaseUnitName());
        product.setSellingUnit(productRequest.getSellingUnit());
        product.setDescription(productRequest.getDescription());
        product.setTaxCode(taxCode);
        product.setSellingUnitName(productRequest.getSellingUnitName());
        product.setNotification(productRequest.isNotification());

        if (productRequest.getProductBrandPublicId() != null) {
            ProductBrand productBrand = productBrandRepository.findByPublicIdAndDeletedIsFalse(productRequest.getProductBrandPublicId()).orElse(null);
            if (productBrand == null) {
                throw new BadRequestRestApiException()
                        .developerMessage("Product brand not found")
                        .userMessage("Sorry. Product brand not found");
            }

            product.setProductBrand(productBrand);
        }

        Product savedProduct = productRepository.save(product);
        calculateAmountWithTax(savedProduct);

        if (!productRequest.getSupplierTargetRequestDtos().isEmpty()) {
            runAddSupplierTargetUpdate(product.getPublicId(), product.getProductName(),
                    productRequest.getSupplierTargetRequestDtos(), savedProduct);
        }
    }

    private void runAddSupplierTargetUpdate(UUID productPublicId, String productName,
                                            List<SupplierTargetRequestDto> supplierTargetRequestDtos, Product savedProduct) {

        supplierTargetRequestDtos.forEach(dto -> {
            Suppliers supplier = supplierRepository
                    .findByPublicIdAndDeletedIsFalse(dto.supplierPublicId())
                    .orElseThrow(() -> new BadRequestRestApiException()
                            .developerMessage("Supplier with id " + dto.supplierPublicId() + " not found")
                            .userMessage("Sorry. Supplier not found"));

            SupplierTarget supplierTarget = supplierTargetRepository
                    .findBySupplierPublicIdAndProductPublicId(supplier.getPublicId(), productPublicId)
                    .orElse(new SupplierTarget());

            supplierTarget.setTargetValue(dto.targetValue());
            supplierTarget.setProductName(productName);
            supplierTarget.setProductPublicId(productPublicId);
            supplierTarget.setSupplierName(supplier.getSupplierName());
            supplierTarget.setSupplierPublicId(supplier.getPublicId());
            supplierTarget.setProduct(savedProduct);

            supplierTargetRepository.save(supplierTarget);
        });
    }

    @Override
    public Product getProduct(UUID publicId) {

        Product product = productRepository
                .findByPublicIdAndDeletedIsFalse(publicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Product with public id " + publicId + " not found")
                        .userMessage("Sorry. Product with public id " + publicId + " not found"));
        productImageResolver.enrich(product);
        return product;
    }

    @Override
    public Page<Product> getProducts(String searchParam,
                                     String productName,
                                     ProductStatus status,
                                     UUID supplier,
                                     UUID category,
                                     UUID classPublicId,
                                     UUID subClassPublicId,
                                     Boolean isActive,
                                     Boolean hasStock,
                                     UUID brandPublicId,
                                     Pageable pageable) {

        Specification<Product> spec = Specification
                .where(ProductSpecification.isNotDeleted())
                .and(ProductSpecification.hasSupplier(supplier))
                .and(ProductSpecification.hasCategory(category))
                .and(ProductSpecification.hasStatus(status))
                .and(ProductSpecification.hasProductClass(classPublicId))
                .and(ProductSpecification.hasProductSubClass(subClassPublicId))
                .and(ProductSpecification.hasProductNameLike(productName))
                .and(ProductSpecification.isActive(isActive))
                .and(ProductSpecification.hasStock(hasStock))
                .and(ProductSpecification.hasProductBrand(brandPublicId))
                .and(ProductSpecification.searchParamMatches(searchParam));

        Page<Product> page = productRepository.findAll(spec, pageable);
        productImageResolver.enrich(page.getContent());
        return page;
    }

    @Override
    public Page<Product> getProductsBySupplier(UUID supplierPublicId, String searchParam, Pageable pageable) {

        Optional<Suppliers> supplierOptional = supplierRepository.findByPublicIdAndDeletedIsFalse(supplierPublicId);

        if (supplierOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Supplier with public id " + supplierPublicId + " not found")
                    .userMessage("Sorry. Supplier with public id " + supplierPublicId + " not found");
        }
        Suppliers supplier = supplierOptional.get();

        Page<Product> page;
        if (searchParam != null && searchParam.trim().length() >= 3) {
            page = productRepository.findByProductNameAndSuppliersAndDeletedIsFalse(searchParam, supplier, pageable);
        } else {
            page = productRepository.findBySuppliersAndDeletedIsFalse(Collections.singletonList(supplier), pageable);
        }
        productImageResolver.enrich(page.getContent());
        return page;
    }

    @Override
    public void deleteProduct(UUID publicId, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);
        Optional<Product> optionalProduct = productRepository.findByPublicIdAndDeletedIsFalse(publicId);

        if (optionalProduct.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product with public id " + publicId + " not found")
                    .userMessage("Sorry. Product with public id " + publicId + " not found");
        }

        Product product = optionalProduct.get();

        optionalPerson.ifPresent(product::setUpdatedBy);
        product.setUpdateDate(LocalDateTime.now());
        product.setDeleted(true);

        productRepository.save(product);
    }

    @Override
    public List<ProductViewResponse> getProductViewPerWarehouse(UUID productPublicId) {

        Optional<Product> optionalProduct = productRepository.findByPublicIdAndDeletedIsFalse(productPublicId);

        if (optionalProduct.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product with public id " + productPublicId + " not found")
                    .userMessage("Sorry. Product with public id " + productPublicId + " not found");
        }

        Product product = optionalProduct.get();

        List<InventoryProducts> inventoryProducts = inventoryProductsRepository.findAllByProductAndDeletedIsFalse(product);

        List<ProductViewResponse> productViewResponses = new ArrayList<>();

        if (inventoryProducts.isEmpty()) {
            return productViewResponses;
        }

        inventoryProducts.forEach(inventoryProduct -> {
            if (inventoryProduct.getInventory() != null && inventoryProduct.getInventory().getWarehouse() != null) {
                ProductViewResponse productViewResponse = new ProductViewResponse();
                productViewResponse.setWarehouseName(inventoryProduct.getInventory().getWarehouse().getWarehouseName());
                productViewResponse.setWarehousePublicId(inventoryProduct.getInventory().getWarehouse().getPublicId());
                productViewResponse.setProduct(inventoryProduct);
                productViewResponses.add(productViewResponse);
            }
        });

        return productViewResponses;
    }

    @Override
    public ProductResponse getAvailabilityResponse(UUID productId) {

        Optional<Product> optionalProduct = productRepository.findByPublicIdAndDeletedIsFalse(productId);

        if (optionalProduct.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product with public id " + productId + " not found")
                    .userMessage("Sorry. Product with public id " + productId + " not found");
        }

        Product product = optionalProduct.get();

        ProductResponse productResponse = new ProductResponse();

        if (product.getAvailableStock() != null && product.getAvailableStock() > 0) {
            productResponse.setSuccess(true);
        } else {
            productResponse.setSuccess(false);
            productResponse.setMessage(String.format("Sorry. Product %s has no available stock", product.getProductName()));
        }

        return productResponse;
    }

    @Override
    public ProductAdjustment createProductAdjustment(UUID productPublicId, ProductAdjustmentRequest productAdjustmentRequest, String name) {
        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);
        Optional<Product> optionalProduct = productRepository.findByPublicIdAndDeletedIsFalse(productPublicId);

        if (productAdjustmentRequest.getWarehousePublicId() == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Warehouse public id cannot be null")
                    .userMessage("Sorry. Warehouse public id cannot be null");
        }

        if (optionalProduct.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product with public id " + productPublicId + " not found")
                    .userMessage("Sorry. Product with public not found");
        }

        Product product = optionalProduct.get();

        Optional<Warehouse> optionalWarehouse = warehouseRepository.findByPublicIdAndDeletedIsFalse(productAdjustmentRequest.getWarehousePublicId());

        if (optionalWarehouse.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Warehouse with public id " + productAdjustmentRequest.getWarehousePublicId() + " not found")
                    .userMessage("Sorry. Warehouse selected not found");
        }

        Warehouse warehouse = optionalWarehouse.get();

        Optional<Inventory> optionalInventory = inventoryRepository.findByWarehouseAndDeletedIsFalse(warehouse);
        if (optionalInventory.isEmpty()) {

            throw new BadRequestRestApiException()
                    .developerMessage("Inventory for warehouse with public id " + productAdjustmentRequest.getWarehousePublicId() + " not found")
                    .userMessage("Sorry. Inventory for selected warehouse not found");
        }

        Inventory inventory = optionalInventory.get();

        if (!inventoryProductsRepository.existsByProductAndInventory(product, inventory)) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product with public id " + productPublicId + " not found in the selected warehouse")
                    .userMessage("Sorry. Product selected not found in the selected warehouse");
        }

        if (productAdjustmentRequest.getQuantity() <= 0) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product adjustment quantity must be greater than zero")
                    .userMessage("Sorry. Product adjustment quantity must be greater than zero");
        }

        if (productAdjustmentRequest.getAdjustmentType() == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product adjustment type cannot be null")
                    .userMessage("Sorry. Product adjustment type cannot be null");
        }

        ProductAdjustment productAdjustment = new ProductAdjustment();
        productAdjustment.setProductSku(product.getSku());
        productAdjustment.setProductName(product.getProductName());
        productAdjustment.setProductPublicId(String.valueOf(product.getPublicId()));
        productAdjustment.setAdjustmentType(productAdjustmentRequest.getAdjustmentType());
        productAdjustment.setQuantity(productAdjustmentRequest.getQuantity());
        productAdjustment.setReason(productAdjustmentRequest.getReason());
        productAdjustment.setCreatedBy(optionalPerson.orElse(null));
        productAdjustment.setCreationTime(LocalDateTime.now());
        productAdjustment.setStatus(AdjustmentStatus.Pending);
        productAdjustment.setWarehousePublicId(warehouse.getPublicId().toString());
        productAdjustment.setWarehouseName(warehouse.getWarehouseName());

        return productAdjustmentRepository.save(productAdjustment);
    }

    @Override
    public Page<ProductAdjustment> getProductAdjustments(String searchParam,
                                                         AdjustmentStatus status,
                                                         AdjustmentType adjustmentType,
                                                         String warehousePublicId,
                                                         Pageable pageable) {

        Specification<ProductAdjustment> spec = Specification
                .where(ProductAdjustmentSpecification.isNotDeleted())
                .and(ProductAdjustmentSpecification.hasStatus(status))
                .and(ProductAdjustmentSpecification.hasAdjustmentType(adjustmentType))
                .and(ProductAdjustmentSpecification.hasWarehousePublicId(warehousePublicId))
                .and(ProductAdjustmentSpecification.searchParamMatches(searchParam));
        return productAdjustmentRepository.findAll(spec, pageable);
    }

    @Override
    public void approveProductAdjustment(UUID publicId, AdjustmentApprover approver, ProductAdjustmentAction action, String name) {

        Optional<ProductAdjustment> productAdjustmentOptional = productAdjustmentRepository.findByPublicIdAndDeletedIsFalse(publicId);
        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        if (productAdjustmentOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product adjustment with public id " + publicId + " not found")
                    .userMessage("Sorry. Product adjustment not found");
        }

        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Person with name " + name + " not found")
                    .userMessage("Sorry. Person not found");
        }
        Person person = optionalPerson.get();

        ProductAdjustment productAdjustment = productAdjustmentOptional.get();

        Warehouse warehouse = warehouseRepository
                .findByPublicIdAndDeletedIsFalse(UUID.fromString(productAdjustment.getWarehousePublicId()))
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Warehouse with public id " + productAdjustment.getWarehousePublicId() + " not found")
                        .userMessage("Sorry. Warehouse on product adjustment not found"));

        Product product = productRepository
                .findByPublicIdAndDeletedIsFalse(UUID.fromString(productAdjustment.getProductPublicId()))
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Product with public id " + productAdjustment.getProductPublicId() + " not found")
                        .userMessage("Sorry. Product on product adjustment not found"));

        if (approver == AdjustmentApprover.WarehouseTeamLead && productAdjustment.getStatus() != AdjustmentStatus.Pending) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product adjustment with public id " + publicId + " is not in pending status for warehouse manager approval")
                    .userMessage("Sorry. Product adjustment not in pending status for warehouse team lead approval");
        }

        if (approver == AdjustmentApprover.WarehouseManager && productAdjustment.getStatus() != AdjustmentStatus.WarehouseTeamLeadApproved) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product adjustment with public id " + publicId + " is not in warehouse team lead approved status for warehouse manager approval")
                    .userMessage("Sorry. Product adjustment not in warehouse team lead approved status for warehouse manager approval");
        }

        if (approver == AdjustmentApprover.FinanceManager && productAdjustment.getStatus() != AdjustmentStatus.WarehouseManagerApproved) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product adjustment with public id " + publicId + " is not in warehouse manager approved status for financial team lead approval")
                    .userMessage("Sorry. Product adjustment not in warehouse manager approved status for financial team lead approval");
        }

        if (action == ProductAdjustmentAction.Reject) {
            productAdjustment.setStatus(AdjustmentStatus.Rejected);
            optionalPerson.ifPresent(productAdjustment::setUpdatedBy);
            productAdjustment.setUpdateDate(LocalDateTime.now());
        } else if (action == ProductAdjustmentAction.Approve) {

            if (approver == AdjustmentApprover.WarehouseTeamLead) {
                productAdjustment.setStatus(AdjustmentStatus.WarehouseTeamLeadApproved);
            } else if (approver == AdjustmentApprover.WarehouseManager) {
                productAdjustment.setStatus(AdjustmentStatus.WarehouseManagerApproved);
            } else if (approver == AdjustmentApprover.FinanceManager) {
                productAdjustment.setStatus(AdjustmentStatus.Approved);
            } else {
                throw new BadRequestRestApiException()
                        .developerMessage("Invalid approver type")
                        .userMessage("Sorry. Invalid approver type");
            }
            optionalPerson.ifPresent(productAdjustment::setUpdatedBy);
            productAdjustment.setUpdateDate(LocalDateTime.now());

            // Update product stock based on an adjustment type

            if (approver == AdjustmentApprover.FinanceManager) {

                stockUpdateHelper.updateProductAdjustments(product,
                        warehouse,
                        productAdjustment.getAdjustmentType(),
                        productAdjustment.getQuantity(), person.getFullName());

                updateLiveStock(warehouse.getPublicId(), productAdjustment.getQuantity(),
                        productAdjustment.getAdjustmentType().name(), product.getPublicId().toString());
            }
        } else {
            throw new BadRequestRestApiException()
                    .developerMessage("Invalid action for product adjustment")
                    .userMessage("Sorry. Invalid action for product adjustment");
        }

        ProductAdjustment saved = productAdjustmentRepository.save(productAdjustment);

        String event = "Product adjustment " + action.name();
        String description = "Product adjustment for product " + product.getProductName() + " has been " + action.name().toLowerCase();

        eventsRepository.save(new Events(saved.getPublicId().toString(),
                event,
                LocalDateTime.now(),
                description,
                action.name(),
                optionalPerson.orElse(null)));
    }

    @Override
    public void deleteProductAdjustment(UUID publicId, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);
        Optional<ProductAdjustment> productAdjustmentOptional = productAdjustmentRepository.findByPublicIdAndDeletedIsFalse(publicId);

        if (productAdjustmentOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product adjustment with public id " + publicId + " not found")
                    .userMessage("Sorry. Product adjustment not found");
        }

        ProductAdjustment productAdjustment = productAdjustmentOptional.get();
        optionalPerson.ifPresent(productAdjustment::setUpdatedBy);
        productAdjustment.setUpdateDate(LocalDateTime.now());
        productAdjustment.setDeleted(true);
        productAdjustmentRepository.save(productAdjustment);
    }

    @Override
    public void updateProductStock(LocalDate dateNow) {
        log.info("Starting live stock opening-stock rollover for {}", dateNow);
        int processed = 0;
        int skipped = 0;
        int failed = 0;

        List<Inventory> inventories = inventoryRepository.findAllByDeletedIsFalse();
        for (Inventory inventory : inventories) {
            Warehouse warehouse = inventory.getWarehouse();
            if (warehouse == null || Boolean.TRUE.equals(warehouse.getDeleted())) {
                skipped++;
                log.warn("Skipping inventory {} — warehouse missing or deleted", inventory.getPublicId());
                continue;
            }

            List<InventoryProducts> inventoryProducts = inventoryProductsRepository
                    .findByInventoryAndDeletedIsFalse(inventory);
            for (InventoryProducts inventoryProduct : inventoryProducts) {
                Product product = inventoryProduct.getProduct();
                if (product == null || Boolean.TRUE.equals(product.getDeleted())) {
                    skipped++;
                    log.warn("Skipping deleted or missing product on inventory {} warehouse {}",
                            inventory.getPublicId(), warehouse.getPublicId());
                    continue;
                }
                try {
                    liveStockHelper.updateLiveStockValue(
                            warehouse.getPublicId(),
                            product.getPublicId(),
                            0,
                            ProductUpdateType.OPENING_STOCK,
                            dateNow);
                    processed++;
                } catch (Exception e) {
                    failed++;
                    log.error("Live stock opening failed for product {} ({}) warehouse {}: {}",
                            product.getPublicId(), product.getProductName(), warehouse.getPublicId(), e.getMessage(), e);
                }
            }
        }

        log.info("Live stock opening-stock rollover finished for {}. processed={}, skipped={}, failed={}",
                dateNow, processed, skipped, failed);
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }


    @Override
    public void grantSupplierPortalAccess(UUID supplierPublicId, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);
        Optional<Suppliers> supplierOptional = supplierRepository.findByPublicIdAndDeletedIsFalse(supplierPublicId);

        if (supplierOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Supplier with public id " + supplierPublicId + " not found")
                    .userMessage("Sorry. Supplier not found");
        }

        Suppliers supplier = supplierOptional.get();
        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException().developerMessage("User not found").userMessage("Sorry. User not found");
        }

        if (supplier.getPortalAccessGranted() != null && supplier.getPortalAccessGranted()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Supplier with public id " + supplierPublicId + " already has portal access")
                    .userMessage("Sorry. Supplier already has portal access");
        }

        supplierPortalAccessHelper.grantMainSupplierPortalAccess(supplier, optionalPerson.get());

        eventsRepository.save(new Events(supplier.getPublicId().toString(),
                "Supplier portal access granted",
                LocalDateTime.now(),
                "Supplier portal access granted",
                "Supplier portal access granted",
                optionalPerson.get()));
    }

    @Override
    public void markProductAsBestSeller(UUID publicId, boolean isBestSeller, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException().developerMessage("User not found").userMessage("Sorry. User not found");
        }

        Optional<Product> productOptional = productRepository.findByPublicIdAndDeletedIsFalse(publicId);

        if (productOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product with public id " + publicId + " not found")
                    .userMessage("Sorry. Product not found");
        }

        var product = productOptional.get();
        var person = optionalPerson.get();

        product.setProductBestSelling(isBestSeller);
        product.setUpdatedBy(person);
        product.setUpdateDate(LocalDateTime.now());

        productRepository.save(product);
    }

    @Override
    public void markProductAsInactive(UUID publicId, boolean isInactive, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException().developerMessage("User not found").userMessage("Sorry. User not found");
        }

        Optional<Product> productOptional = productRepository.findByPublicIdAndDeletedIsFalse(publicId);

        if (productOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product with public id " + publicId + " not found")
                    .userMessage("Sorry. Product not found");
        }

        var product = productOptional.get();
        var person = optionalPerson.get();

        product.setInActive(isInactive);
        product.setUpdatedBy(person);
        product.setUpdateDate(LocalDateTime.now());

        productRepository.save(product);
    }

    @Override
    public ProductPageable getProductsMobile(String searchParam, String productName, ProductStatus status,
                                             UUID supplier, UUID category, UUID classPublicId,
                                             UUID subClassPublicId, Boolean aTrue, Boolean aTrue1,
                                             UUID brandPublicId, Pageable pageable) {

        ZoneId nairobiZone = ZoneId.of("Africa/Nairobi");
        LocalDate nairobiDate = LocalDate.now(nairobiZone);

        Page<Product> filteredPage = getProducts(searchParam, productName, status, supplier,
                category, classPublicId, subClassPublicId,
                aTrue, aTrue1, brandPublicId, pageable);

        filteredPage.getContent().forEach(product -> {
            ProductStock latestStock = productStockRepository
                    .findTopByProductPublicIdAndRecordTodayOrderByRecordTodayDesc(product.getPublicId(), nairobiDate);
            product.applyStock(latestStock);
        });

        List<Product> productInstock = filteredPage.getContent()
                .stream()
                .filter(product -> product.getAvailableStock() != null && product.getAvailableStock() > 0)
                .sorted(Comparator.comparing(Product::getProductName))
                .toList();

        List<Product> productOutOfStock = filteredPage.getContent()
                .stream()
                .filter(product -> product.getAvailableStock() == null || product.getAvailableStock() <= 0)
                .sorted(Comparator.comparing(Product::getProductName))
                .toList();

        List<Product> sortedProducts = new ArrayList<>();
        sortedProducts.addAll(productInstock);
        sortedProducts.addAll(productOutOfStock);

        List<ProductTransformerDTO> dtos = sortedProducts.stream()
                .map(productTransformer::convert)
                .toList();

        ProductPageable result = new ProductPageable();
        result.setContent(dtos);
        result.setPageNumber(filteredPage.getNumber());
        result.setTotalElements(filteredPage.getTotalElements());
        result.setTotalPages(filteredPage.getTotalPages());
        result.setFirst(filteredPage.isFirst());
        result.setLast(filteredPage.isLast());
        return result;
    }

    @Override
    public void updateTaxCodeProduct(String name) {
        for (Product product : productRepository.findByDeletedFalseAndTaxCodeIsNotNull()) {
            calculateAmountWithTax(product);
        }
    }

    @Override
    @Transactional
    public void runProductStock(Principal principal) {
        ZoneId nairobiZone = ZoneId.of("Africa/Nairobi");
        LocalDate nairobiDate = LocalDate.now(nairobiZone);

        List<Warehouse> allWarehouses = warehouseRepository.findAllByDeletedIsFalse();
        if (allWarehouses.isEmpty()) return;

        for (Warehouse warehouse : allWarehouses) {
            Inventory inventory = inventoryRepository.findByWarehouseAndDeletedIsFalse(warehouse).orElse(null);
            if (inventory == null) continue;

            List<InventoryProducts> inventoryProducts = inventory.getInventoryProducts();
            if (inventoryProducts.isEmpty()) continue;

            for (InventoryProducts inventoryProduct : inventoryProducts) {
                Optional<ProductStock> productStock = productStockRepository.findByProductPublicIdAndInventoryPublicIdAndRecordTodayAndWarehouseAndDeletedFalse(inventoryProduct.getProduct().getPublicId(),
                        inventoryProduct.getInventory().getPublicId(), nairobiDate, warehouse);
                if (!productStock.isPresent()) {
                    createProductStock(
                            inventoryProduct.getProduct().getProductName(),
                            inventoryProduct.getProduct().getPublicId(),
                            inventoryProduct.getProduct().getSellPrice(),
                            inventoryProduct.getProduct().getSku(),
                            inventory.getPublicId(),
                            nairobiDate,
                            warehouse,
                            inventoryProduct.getClosingStock(),
                            inventoryProduct.getAvailableStock(),
                            inventoryProduct.getReservedStock(),
                            inventoryProduct.getQuarantinedStock()
                    );
                }
            }
        }
    }

    @Override
    @Transactional
    public void runProductStockByNextDay(Principal principal) {
        ZoneId nairobiZone = ZoneId.of("Africa/Nairobi");
        LocalDate nairobiDate = LocalDate.now(nairobiZone);

        Inventory inventory = null;
        Warehouse warehouse = warehouseRepository.findById(1L).orElse(null);
        if (warehouse != null){
            inventory = inventoryRepository.findByWarehouseAndDeletedIsFalse(warehouse).orElse(null);
        }

       if (inventory != null){
           loadRecordBasedOnProducts(inventory, warehouse, nairobiDate);
       }else{
           List<ProductStock> productStockList =
                   productStockRepository.findAllByRecordTodayAndDeletedFalse(nairobiDate.minusDays(1));

           productStockList.forEach(item -> {
               try {
                   createProductStock(
                           item.getProductName(),
                           item.getProductPublicId(),
                           item.getSellPrice(),
                           item.getProductSku(),
                           item.getInventoryPublicId(),
                           nairobiDate,
                           item.getWarehouse(),
                           item.getClosingStock(),
                           item.getAvailableStock(),
                           item.getReservedStock(),
                           item.getQuarantinedStock()
                   );
               } catch (Exception e) {
                   log.error("Next-day product stock failed for product {}: {}",
                           item.getProductPublicId(), e.getMessage(), e);
               }
           });
       }
    }

    private void loadRecordBasedOnProducts(Inventory inventory, Warehouse warehouse, LocalDate nairobiDate) {

        for (InventoryProducts inventoryProducts : inventoryProductsRepository.findByInventoryAndDeletedIsFalse(inventory)) {
            Product product = inventoryProducts.getProduct();
            if (product == null || Boolean.TRUE.equals(product.getDeleted())) {
                log.warn("Skipping deleted or missing product while creating next-day product stock for warehouse {}",
                        warehouse.getPublicId());
                continue;
            }
            try {
                LocalDate previousDay = nairobiDate.minusDays(1);
                ProductStock productStock = productStockRepository.findByProductAndRecordTodayAndDeletedFalse(product, previousDay).orElse(null);

                if (productStock != null) {
                    createProductStockRecord(productStock, nairobiDate);
                } else {
                    createProductStockRecord2(inventoryProducts, nairobiDate, warehouse);
                }
            } catch (Exception e) {
                log.error("Next-day product stock failed for product {} warehouse {}: {}",
                        product.getPublicId(), warehouse.getPublicId(), e.getMessage(), e);
            }
        }
    }

    private void createProductStockRecord2(InventoryProducts inventoryProducts, LocalDate nairobiDate, Warehouse warehouse) {
        createProductStock(
                inventoryProducts.getProduct().getProductName(),
                inventoryProducts.getProduct().getPublicId(),
                inventoryProducts.getProduct().getSellPrice(),
                inventoryProducts.getProduct().getSku(),
                inventoryProducts.getInventory().getPublicId(),
                nairobiDate,
                warehouse,
                inventoryProducts.getClosingStock(),
                inventoryProducts.getProduct().getAvailableStock(),
                inventoryProducts.getProduct().getReservedStock(),
                inventoryProducts.getProduct().getQuarantineStock()
        );
    }

    private void createProductStockRecord(ProductStock item, LocalDate nairobiDate) {
        createProductStock(
                item.getProductName(),
                item.getProductPublicId(),
                item.getSellPrice(),
                item.getProductSku(),
                item.getInventoryPublicId(),
                nairobiDate,
                item.getWarehouse(),
                item.getClosingStock(),
                item.getAvailableStock(),
                item.getReservedStock(),
                item.getQuarantinedStock()
        );
    }

    @Transactional
    @Override
    public void updateProductStockManualRun(LocalDate runDate) {
        log.info("-------run manually static stock");
        try {
            List<Inventory> inventories = inventoryRepository.findAll();
            for (Inventory inventory : inventories) {
                Warehouse warehouse = inventory.getWarehouse();
                if (warehouse != null) {
                    List<InventoryProducts> inventoryProducts = inventoryProductsRepository
                            .findAllByInventoryAndDeletedIsFalse(inventory, Pageable.unpaged())
                            .getContent();
                    for (InventoryProducts inventoryProduct : inventoryProducts) {
                        Product product = inventoryProduct.getProduct();
                        if (product != null) {
                            liveStockHelper.updateLiveStockValueManualRun(warehouse.getPublicId(),
                                    product.getPublicId(),
                                    runDate);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.info("Error updating product stock: {}", e.getMessage(), e);
        }
    }

    @Override
    public List<SupplierTarget> getSupplierTarget(UUID productPublicId, UUID supplierPublicId,
                                                  Principal principal, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<SupplierTarget> cq = cb.createQuery(SupplierTarget.class);
        Root<SupplierTarget> root = cq.from(SupplierTarget.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("deleted"), Boolean.FALSE));

        if (productPublicId != null) {
            predicates.add(cb.equal(root.get("productPublicId"), productPublicId));
        }

        if (supplierPublicId != null) {
            predicates.add(cb.equal(root.get("supplierPublicId"), supplierPublicId));
        }

        cq.where(predicates.toArray(new Predicate[0]))
                .orderBy(cb.desc(root.get("id")));

        TypedQuery<SupplierTarget> query = entityManager.createQuery(cq);
        query.setMaxResults(pageable.getPageSize());
        query.setFirstResult((int) pageable.getOffset());

        return query.getResultList();
    }

    @Transactional
    public void runAddProductToAllWarehouses(Product product) {
        List<Warehouse> warehouseList =
                warehouseRepository.findAllByWarehouseType(WarehouseType.MAIN);

        for (Warehouse warehouse : warehouseList) {
            inventoryRepository.findByWarehouseAndDeletedIsFalse(warehouse).ifPresent(inventory -> {
                InventoryProducts inventoryProduct = getOrCreateInventoryProduct(product, inventory);
                if (!inventory.getInventoryProducts().contains(inventoryProduct)) {
                    inventory.getInventoryProducts().add(inventoryProduct);
                }
            });
        }
    }

    private InventoryProducts getOrCreateInventoryProduct(Product product, Inventory inventory) {
        return inventoryProductsRepository
                .findByInventoryAndProductAndDeletedIsFalse(inventory, product)
                .orElseGet(() -> {
                    InventoryProducts newInventoryProduct = new InventoryProducts();
                    newInventoryProduct.setProduct(product);
                    newInventoryProduct.setInventory(inventory);
                    return inventoryProductsRepository.save(newInventoryProduct);
                });
    }


    private ProductStock createProductStock(String productName, UUID productId, BigDecimal sellingPrice,
                                            @Unique String sku, UUID inventoryId, LocalDate nairobiDate,
                                            Warehouse warehouse, Long closingStock, Long availableStock,
                                            Long reservedStock, Long quarantinedStock) {
        Optional<ProductStock> optionalProductStock = productStockRepository.findByProductPublicIdAndInventoryPublicIdAndRecordTodayAndWarehouseAndDeletedFalse(productId,
                inventoryId, nairobiDate, warehouse);
        Product product = productRepository.findByPublicIdAndDeletedIsFalse(productId).orElse(null);
        if (!optionalProductStock.isPresent()) {
            ProductStock ps = new ProductStock();
            ps.setProductName(productName);
            ps.setProductPublicId(productId);
            ps.setInventoryPublicId(inventoryId);
            ps.setRecordToday(nairobiDate);
            ps.setAvailableStock(availableStock == null ? 0L : availableStock);
            ps.setReservedStock(reservedStock == null ? 0L : reservedStock);
            ps.setOnHoldStock(0L);
            ps.setQuarantinedStock(quarantinedStock == null ? 0L : quarantinedStock);
            ps.setTotalStock(closingStock == null ? 0L : closingStock);
            ps.setClosingStock(closingStock == null ? 0L : closingStock);
            ps.setWarehouse(warehouse);
            ps.setSellPrice(sellingPrice);
            ps.setProductSku(sku);
            ps.setProduct(product);
            return productStockRepository.save(ps);
        } else {
            return optionalProductStock.get();
        }
    }

    private void updateLiveStock(UUID wareHousePublicId, int quantity, String adjustmentType, String productId) {

        if ("Decrease".equalsIgnoreCase(adjustmentType)) {
            liveStockHelper.updateLiveStockValue(wareHousePublicId, UUID.fromString(productId), quantity,
                    ProductUpdateType.STOCK_ADJUSTMENT_DECREASE,
                    LocalDate.now());
        } else {
            liveStockHelper.updateLiveStockValue(wareHousePublicId, UUID.fromString(productId), quantity,
                    ProductUpdateType.STOCK_ADJUSTMENT,
                    LocalDate.now());
        }
    }

    @Transactional
    public void calculateAmountWithTax(Product savedProduct) {
        BigDecimal sellPrice = savedProduct.getSellPrice();
        if (sellPrice == null) {
            log.warn("Sell price cannot be null for product {}", savedProduct.getPublicId());
            return;
        }

        BigDecimal amountBeforeTax;
        BigDecimal taxAmount;

        if (savedProduct.getTaxCode() != null &&
                NON_TAXABLE_CODES.contains(savedProduct.getTaxCode().getCodeName())) {

            amountBeforeTax = sellPrice;
            taxAmount = BigDecimal.ZERO;

        } else {
            BigDecimal taxRate = savedProduct.getTaxCode() != null
                    ? savedProduct.getTaxCode().getCodeValue()
                    : BigDecimal.ZERO;

            BigDecimal taxValue = taxRate.add(BigDecimal.ONE);
            amountBeforeTax = sellPrice.divide(taxValue, SCALE, RoundingMode.HALF_UP);
            taxAmount = sellPrice.subtract(amountBeforeTax);
        }

        savedProduct.setSellingPriceBeforeTax(amountBeforeTax);
        savedProduct.setTaxAmount(taxAmount);

        log.debug("Product {}: sellPrice={}, taxRate={}, beforeTax={}, taxAmount={}",
                savedProduct.getPublicId(), sellPrice,
                savedProduct.getTaxCode() != null ? savedProduct.getTaxCode().getCodeValue() : BigDecimal.ZERO,
                amountBeforeTax, taxAmount);

        productRepository.save(savedProduct);
    }

    @Override
    public List<String> getProductBrands(String searchParam) {
        List<String> brands = searchParam != null && searchParam.trim().length() > 3
                ? productRepository.findDistinctProductBrandsBySearch(searchParam.trim())
                : productRepository.findDistinctProductBrands();
        return normalizeDistinctBrands(brands);
    }

    @Override
    public List<Product> listProduct(List<UUID> productPublicId, String name) {

        return productRepository.getAllInList(productPublicId);
    }

    static List<String> normalizeDistinctBrands(Collection<String> brands) {
        Map<String, String> uniqueBrands = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String brand : brands) {
            if (brand == null) {
                continue;
            }
            String trimmedBrand = brand.trim();
            if (trimmedBrand.isEmpty()) {
                continue;
            }
            uniqueBrands.putIfAbsent(trimmedBrand.toLowerCase(Locale.ROOT), trimmedBrand);
        }
        return new ArrayList<>(uniqueBrands.values());
    }
}
