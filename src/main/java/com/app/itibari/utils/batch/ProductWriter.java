package com.app.itibari.utils.batch;

import com.app.itibari.dataTypes.TaxType;
import com.app.itibari.models.entities.Category;
import com.app.itibari.models.entities.ProductClass;
import com.app.itibari.models.entities.ProductSubClass;
import com.app.itibari.models.entities.Suppliers;
import com.app.itibari.models.entities.TaxMapping;
import com.app.itibari.models.request.CategoryRequest;
import com.app.itibari.models.request.ProductClassRequest;
import com.app.itibari.models.request.ProductProcess;
import com.app.itibari.models.request.ProductRequest;
import com.app.itibari.models.request.ProductSubClassRequest;
import com.app.itibari.models.request.TaxMappingRequest;
import com.app.itibari.repository.CategoryRepository;
import com.app.itibari.repository.ProductClassRepository;
import com.app.itibari.repository.ProductSubClassRepository;
import com.app.itibari.repository.SupplierRepository;
import com.app.itibari.repository.TaxMappingRepository;
import com.app.itibari.services.FinancialService;
import com.app.itibari.services.ProductService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class ProductWriter implements ItemWriter<ProductProcess>, InitializingBean {

    @Autowired
    private ProductService productService;

    @Autowired
    private TaxMappingRepository taxMappingRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private FinancialService financialService;

    @Autowired
    private ProductClassRepository productClassRepository;

    @Autowired
    private ProductSubClassRepository productSubClassRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Override
    public void write(Chunk<? extends ProductProcess> chunk) {

        chunk.getItems().forEach(item -> {

            try {

                TaxMapping taxMapping = null;
                Category category = null;
                ProductClass productClass = null;
                ProductSubClass productSubClass = null;
                List<Suppliers> suppliers = new ArrayList<>();
                if (item.getTaxMapping() != null) {
                    String taxMappingValue = item.getTaxMapping().replace("%", "");
                    BigDecimal taxMappingDecimal = new BigDecimal(taxMappingValue);

                    Optional<TaxMapping> optionalTaxMapping = taxMappingRepository.findByTaxRateAndDeletedIsFalse(taxMappingDecimal);

                    if (optionalTaxMapping.isPresent()) {
                        taxMapping = optionalTaxMapping.get();
                    } else {
                        TaxMappingRequest taxMappingRequest = new TaxMappingRequest();
                        taxMappingRequest.setTaxRate(taxMappingDecimal);
                        taxMappingRequest.setTaxType(TaxType.Percentage);
                        taxMappingRequest.setTaxName(String.format("Tax Mapping %s", taxMappingDecimal));
                        taxMappingRequest.setTaxCode("Tax Code");
                        taxMapping = financialService.createTaxMapping(taxMappingRequest, "itibari@gmail.com");
                    }
                }

                if (item.getCategory() != null) {
                    Optional<Category> optionalCategory = categoryRepository.findByNameIgnoreCaseAndDeletedIsFalse(item.getCategory());

                    if (optionalCategory.isPresent()) {
                        category = optionalCategory.get();
                    } else {
                        CategoryRequest categoryRequest = new CategoryRequest();
                        categoryRequest.setCategoryName(item.getCategory());
                        categoryRequest.setCategoryDescription(String.format("Category %s", item.getCategory()));
                        category = productService.createCategory(categoryRequest, "itibari@gmail.com");
                    }
                }

                if (item.getProductClass() != null && category != null) {
                    Optional<ProductClass> optionalProductClass = productClassRepository.findByCategoryAndNameIgnoreCaseAndDeletedIsFalse(
                            category,
                            item.getProductClass());

                    if (optionalProductClass.isPresent()) {
                        productClass = optionalProductClass.get();
                    } else {
                        ProductClassRequest productClassRequest = new ProductClassRequest();
                        productClassRequest.setDescription(item.getProductClass());
                        productClassRequest.setName(item.getProductClass());
                        productClassRequest.setCategoryPublicId(category.getPublicId());
                        productClass = productService.createClass(productClassRequest, "itibari@gmail.com");
                    }
                }

                if (item.getSubClass() != null && productClass != null) {

                    Optional<ProductSubClass> optionalProductSubClass = productSubClassRepository.findByProductClassAndNameIgnoreCaseAndDeletedIsFalse(
                            productClass,
                            item.getSubClass());

                    if (optionalProductSubClass.isPresent()) {
                        productSubClass = optionalProductSubClass.get();
                    } else {
                        ProductSubClassRequest productSubClassRequest = new ProductSubClassRequest();
                        productSubClassRequest.setDescription(item.getSubClass());
                        productSubClassRequest.setName(item.getSubClass());
                        productSubClass = productService.createSubClass(productClass.getPublicId(),
                                                                        productSubClassRequest,
                                                                        "itibari@gmail.com");
                    }
                }

                if (item.getSuppliers() != null && !item.getSuppliers().isEmpty()) {
                    for (String supplierName : item.getSuppliers()) {
                        Optional<Suppliers> optionalSupplier = supplierRepository.findBySupplierNameIgnoreCaseAndDeletedIsFalse(supplierName);
                        optionalSupplier.ifPresent(suppliers::add);
                    }
                }
                // create the productRequest

                ProductRequest productRequest = new ProductRequest();
                productRequest.setProductName(item.getProductName());
                productRequest.setBuyingPrice(new BigDecimal(item.getBuyingPrice()));
                productRequest.setSellPrice(new BigDecimal(item.getSellingPrice()));
                productRequest.setTaxMappingPublicId(taxMapping != null ? taxMapping.getPublicId() : null);
                productRequest.setCategoryPublicId(category != null ? category.getPublicId() : null);
                productRequest.setClassPublicId(productClass != null ? productClass.getPublicId() : null);
                productRequest.setSubClassPublicId(productSubClass != null ? productSubClass.getPublicId() : null);
                productRequest.setWeight(item.getWeight() != null ? new BigDecimal(extractNumericValue(item.getWeight())) : null);
                productRequest.setLength(item.getLength() != null ? parseInteger(extractNumericValue(item.getLength())) : null);
                productRequest.setWidth(item.getWidth() != null ? parseInteger(extractNumericValue(item.getWidth())) : null);
                productRequest.setHeight(item.getHeight() != null ? parseInteger(extractNumericValue(item.getHeight())) : null);
                productRequest.setUnitOfMeasurement(item.getUnitOfMeasurement());
                productRequest.setBarcode(item.getBarCode());
                productRequest.setPackageSize(item.getPurchaseUnitSize());
                productRequest.setBulkPurchaseUnit(item.getPurchaseUnitName() != null ? parseInteger(extractNumericValue(item.getPurchaseUnitName())) : null);
                productRequest.setBulkPurchaseUnitName(item.getPurchaseUnitName());
                productRequest.setSellingUnit(item.getSellingUnit() != null ? Integer.parseInt(extractNumericValue(item.getSellingUnit())) : null);
                if (!suppliers.isEmpty()) {
                    List<UUID> supplierPublicIds = new ArrayList<>();
                    for (Suppliers supplier : suppliers) {
                        supplierPublicIds.add(supplier.getPublicId());
                    }
                    productRequest.setSupplierPublicIds(supplierPublicIds);
                }
                productRequest.setMargin(new BigDecimal(item.getBuyingPrice()).subtract(new BigDecimal(item.getSellingPrice())));

                // create the product

                if (item.getProductName() != null && category != null && productClass != null && productSubClass != null) {
                    productService.createProduct(productRequest, "itibari@gmail.com");
                }
            } catch (Exception e) {
                try {
                    log.info(new ObjectMapper().writeValueAsString(item));
                } catch (JsonProcessingException ex) {
                    log.info("Error converting item to JSON: {}", ex.getMessage());
                }
                log.info("Error creating product: {}", e.getMessage());
            }
        });
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }

    private String extractNumericValue(String value) {

        return value.replaceAll("[^0-9.]", ""); // Remove all non-numeric characters except the decimal point
    }

    private Integer parseInteger(String value) {

        try {
            return (int) Math.round(Double.parseDouble(value)); // Parse as double and round to the nearest integer
        } catch (NumberFormatException e) {
            return null; // Return null if parsing fails
        }
    }
}
