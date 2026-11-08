package com.app.itibari.services;

import com.app.itibari.dataTypes.AdjustmentApprover;
import com.app.itibari.dataTypes.AdjustmentStatus;
import com.app.itibari.dataTypes.AdjustmentType;
import com.app.itibari.dataTypes.ProductAdjustmentAction;
import com.app.itibari.dataTypes.ProductStatus;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.CategoryRequest;
import com.app.itibari.models.request.ProductAdjustmentRequest;
import com.app.itibari.models.request.ProductClassRequest;
import com.app.itibari.models.request.ProductRequest;
import com.app.itibari.models.request.ProductSubClassRequest;
import com.app.itibari.models.response.ProductPageable;
import com.app.itibari.models.response.ProductResponse;
import com.app.itibari.models.response.ProductViewResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ProductService {

    Category createCategory(CategoryRequest categoryRequest, String name);

    void updateCategory(UUID publicId, CategoryRequest categoryRequest, String name);

    Category getCategory(UUID publicId);

    Page<Category> getCategories(String searchParam, Pageable pageable);

    void deleteCategory(UUID publicId, String name);

    ProductClass createClass(ProductClassRequest productClassRequest, String name);

    void updateClass(UUID publicId, ProductClassRequest productClassRequest, String name);

    ProductClass getProductClass(UUID publicId);

    Page<ProductClass> getProductClasses(String searchParam, UUID categoryPublicId, Pageable pageable);

    void deleteClass(UUID publicId, String name);

    ProductSubClass createSubClass(UUID publicId, ProductSubClassRequest productSubClassRequest, String name);

    void updateSubClass(UUID publicId, UUID subClassPublicId, ProductSubClassRequest productSubClassRequest, String name);

    ProductSubClass getSubClass(UUID subClassPublicId);

    Page<ProductSubClass> getSubClasses(String searchParam, UUID classPublicId, Pageable pageable);

    void deleteSubClass(UUID subClassPublicId, String name);

    Product createProduct(@Valid ProductRequest productRequest, String name);

    void updateProduct(UUID publicId, @Valid ProductRequest productRequest, String name);

    Product getProduct(UUID publicId);

    Page<Product> getProducts(String searchParam, String productName, ProductStatus status, UUID supplier, UUID category,
                              UUID classPublicId, UUID subClassPublicId, Boolean isActive, Boolean hasStock, UUID brandPublicId, Pageable pageable);

    void deleteProduct(UUID publicId, String name);

    Page<Product> getProductsBySupplier(UUID supplierPublicId, String searchParam, Pageable pageable);

    List<ProductViewResponse> getProductViewPerWarehouse(UUID productPublicId);

    ProductResponse getAvailabilityResponse(UUID productId);

    ProductAdjustment createProductAdjustment(UUID productPublicId, @Valid ProductAdjustmentRequest productAdjustmentRequest, String name);

    Page<ProductAdjustment> getProductAdjustments(String searchParam,
                                                  AdjustmentStatus status,
                                                  AdjustmentType adjustmentType,
                                                  String warehousePublicId,
                                                  Pageable pageable);

    void approveProductAdjustment(UUID publicId, AdjustmentApprover approver, ProductAdjustmentAction adjustmentAction, String name);

    void deleteProductAdjustment(UUID publicId, String name);

    void updateProductStock(LocalDate dateNow);

    void grantSupplierPortalAccess(UUID supplierPublicId, String name);

    void markProductAsBestSeller(UUID publicId, boolean isBestSeller, String name);

    void markProductAsInactive(UUID publicId, boolean isInactive, String name);

    ProductPageable getProductsMobile(String searchParam, String productName, ProductStatus status, UUID supplier, UUID category,
                                      UUID classPublicId, UUID subClassPublicId, Boolean aTrue, Boolean aTrue1, UUID brandPublicId, Pageable pageable);

    void updateTaxCodeProduct(String name);

    void runProductStock(Principal principal);

    void runProductStockByNextDay(Principal principal);

    void updateProductStockManualRun(LocalDate runDate);

    List<SupplierTarget> getSupplierTarget(UUID productPublicId, UUID supplierPublicId, Principal principal, Pageable pageable);

    void calculateAmountWithTaxV2(Product product);

    List<String> getProductBrands(String searchParam);

    List<Product> listProduct(List<UUID> productPublicId, String name);
}
