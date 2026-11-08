package com.app.itibari.web;

import com.app.itibari.dataTypes.*;
import com.app.itibari.docs.Examples;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.*;
import com.app.itibari.models.response.ProductPageable;
import com.app.itibari.models.response.ProductResponse;
import com.app.itibari.models.response.ProductViewResponse;
import com.app.itibari.services.ProductService;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Tag(name = "products")
@RestController
@RequestMapping("/products")
public class ProductRestController {

    private final ProductService productService;

    public ProductRestController(ProductService productService) {

        this.productService = productService;
    }

    /*
    category
     */

    @PreAuthorize("hasAuthority('CREATE_PRODUCT_CATEGORY')")
    @JsonView(BaseView.ProductSummaryView.class)
    @PostMapping("/category")
    @Operation(summary = "Create a new product category", description = "Create a new product category",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "CREATE_CATEGORY", value = Examples.CREATE_CATEGORY),
    })))
    public Category createCategory(@RequestBody CategoryRequest categoryRequest,
                                   @Parameter(hidden = true) Principal principal) {

        return productService.createCategory(categoryRequest, principal.getName());
    }

    @PreAuthorize("hasAuthority('UPDATE_PRODUCT_CATEGORY')")
    @PatchMapping("/category/{publicId}")
    @Operation(summary = "Update a product category", description = "Update a product category",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "UPDATE_CATEGORY", value = Examples.UPDATE_CATEGORY),
    })))
    public void updateCategory(@PathVariable UUID publicId, @RequestBody CategoryRequest categoryRequest,
                               @Parameter(hidden = true) Principal principal) {

        productService.updateCategory(publicId, categoryRequest, principal.getName());
    }

    @PreAuthorize("hasAuthority('VIEW_PRODUCT_CATEGORY')")
    @JsonView(BaseView.ProductSummaryView.class)
    @GetMapping("/category/{publicId}")
    @Operation(summary = "Get a product category", description = "Get a product category",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Category getCategory(@PathVariable UUID publicId) {

        return productService.getCategory(publicId);
    }

    @JsonView(BaseView.ProductSummaryView.class)
    @GetMapping("/categories")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT_CATEGORY')")
    @Operation(summary = "Get all product categories", description = "Get all product categories",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<Category> getCategories(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return productService.getCategories(searchParam, pageable);
    }

    @PreAuthorize("hasAuthority('DELETE_PRODUCT_CATEGORY')")
    @DeleteMapping("/category/{publicId}")
    @Operation(summary = "Delete a product category", description = "Delete a product category",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void deleteCategory(@PathVariable UUID publicId,
                               @Parameter(hidden = true) Principal principal) {

        productService.deleteCategory(publicId, principal.getName());
    }

    /*

    product class
     */

    @PreAuthorize("hasAuthority('CREATE_PRODUCT_CLASS')")
    @JsonView(BaseView.ProductSummaryView.class)
    @PostMapping("/class")
    @Operation(summary = "Create a new product class", description = "Create a new product class",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "CREATE_CLASS", value = Examples.CREATE_CLASS),
    })))
    public ProductClass createClass(@RequestBody ProductClassRequest productClassRequest,
                                    @Parameter(hidden = true) Principal principal) {

        return productService.createClass(productClassRequest, principal.getName());
    }

    @PreAuthorize("hasAuthority('UPDATE_PRODUCT_CLASS')")
    @PatchMapping("/class/{publicId}")
    @Operation(summary = "Update a product class", description = "Update a product class",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "UPDATE_CLASS", value = Examples.CREATE_CLASS),
    })))
    public void updateClass(@PathVariable UUID publicId, @RequestBody ProductClassRequest productClassRequest,
                            @Parameter(hidden = true) Principal principal) {

        productService.updateClass(publicId, productClassRequest, principal.getName());
    }

    @PreAuthorize("hasAuthority('VIEW_PRODUCT_CLASS')")
    @JsonView(BaseView.ProductSummaryView.class)
    @GetMapping("/class/{publicId}")
    @Operation(summary = "Get a product class", description = "Get a product class",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public ProductClass getProductClass(@PathVariable UUID publicId) {

        return productService.getProductClass(publicId);
    }

    @PreAuthorize("hasAuthority('VIEW_PRODUCT_CLASS')")
    @JsonView(BaseView.ProductSummaryView.class)
    @GetMapping("/classes")
    @Operation(summary = "Get all product classes", description = "Get all product classes",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<ProductClass> getProductClasses(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(name = "categoryPublicId", required = false) UUID categoryPublicId,
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return productService.getProductClasses(searchParam, categoryPublicId, pageable);
    }

    @PreAuthorize("hasAuthority('DELETE_PRODUCT_CLASS')")
    @DeleteMapping("/class/{publicId}")
    @Operation(summary = "Delete a product class", description = "Delete a product class",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void deleteClass(@PathVariable UUID publicId,
                            @Parameter(hidden = true) Principal principal) {

        productService.deleteClass(publicId, principal.getName());
    }

    /*
     product sub classes
     */

    @PreAuthorize("hasAuthority('CREATE_PRODUCT_CLASS')")
    @JsonView(BaseView.ProductSummaryView.class)
    @PostMapping("/class/{publicId}/subClass")
    @Operation(summary = "Create a new product sub class", description = "Create a new product sub class",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "CREATE_SUB_CLASS", value = Examples.CREATE_SUB_CLASS),
    })))
    public ProductSubClass createSubClass(@PathVariable UUID publicId, @RequestBody ProductSubClassRequest productSubClassRequest,
                                          @Parameter(hidden = true) Principal principal) {

        return productService.createSubClass(publicId, productSubClassRequest, principal.getName());
    }

    @PreAuthorize("hasAuthority('UPDATE_PRODUCT_CLASS')")
    @PatchMapping("/class/{publicId}/subClass/{subClassPublicId}")
    @Operation(summary = "Update a product sub class", description = "Update a product sub class",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "UPDATE_SUB_CLASS", value = Examples.CREATE_CLASS),
    })))
    public void updateSubClass(@PathVariable UUID publicId,
                               @PathVariable UUID subClassPublicId,
                               @RequestBody ProductSubClassRequest productSubClassRequest,
                               @Parameter(hidden = true) Principal principal) {

        productService.updateSubClass(publicId, subClassPublicId, productSubClassRequest, principal.getName());
    }

    @PreAuthorize("hasAuthority('VIEW_PRODUCT_CLASS')")
    @JsonView(BaseView.ProductSummaryView.class)
    @GetMapping("/class/subClass/{subClassPublicId}")
    @Operation(summary = "Get a product sub class", description = "Get a product sub class",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public ProductSubClass getSubClass(@PathVariable UUID subClassPublicId) {

        return productService.getSubClass(subClassPublicId);
    }

    @PreAuthorize("hasAuthority('VIEW_PRODUCT_CLASS')")
    @JsonView(BaseView.ProductSummaryView.class)
    @GetMapping("/class/subClasses")
    @Operation(summary = "Get all product sub classes", description = "Get all product sub classes",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<ProductSubClass> getSubClasses(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(name = "classPublicId", required = false) UUID classPublicId,
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return productService.getSubClasses(searchParam, classPublicId, pageable);
    }

    @PreAuthorize("hasAuthority('DELETE_PRODUCT_CLASS')")
    @DeleteMapping("/class/subClass/{subClassPublicId}")
    @Operation(summary = "Delete a product sub class", description = "Delete a product sub class",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void deleteSubClass(@PathVariable UUID subClassPublicId,
                               @Parameter(hidden = true) Principal principal) {

        productService.deleteSubClass(subClassPublicId, principal.getName());
    }

    /*
    product
     */

    @PreAuthorize("hasAuthority('CREATE_PRODUCT')")
    @JsonView(BaseView.ProductDetailView.class)
    @PostMapping
    @Operation(summary = "Create a new product", description = "Create a new product",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "CREATE_PRODUCT", value = Examples.CREATE_PRODUCT),
    })))
    public Product createProduct(@RequestBody @Valid ProductRequest productRequest,
                                 @Parameter(hidden = true) Principal principal) {

        return productService.createProduct(productRequest, principal.getName());
    }

    @PreAuthorize("hasAuthority('UPDATE_PRODUCT')")
    @PatchMapping("/{publicId}")
    @Operation(summary = "Update a product", description = "Update a product",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "UPDATE_PRODUCT", value = Examples.CREATE_PRODUCT),
    })))
    public void updateProduct(@PathVariable UUID publicId, @RequestBody @Valid ProductRequest productRequest,
                              @Parameter(hidden = true) Principal principal) {

        productService.updateProduct(publicId, productRequest, principal.getName());
    }

    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    @JsonView(BaseView.ProductDetailView.class)
    @GetMapping("/{publicId}")
    @Operation(summary = "Get a product", description = "Get a product",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Product getProduct(@PathVariable UUID publicId) {

        return productService.getProduct(publicId);
    }

    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    @JsonView(BaseView.ProductDetailView.class)
    @GetMapping
    @Operation(summary = "Get all products", description = "Get all products",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<Product> getProducts(@RequestParam(name = "searchParam", required = false) String searchParam,
                                     @RequestParam(name = "productName", required = false) String productName,
                                     @RequestParam(name = "status", required = false) ProductStatus status,
                                     @RequestParam(name = "supplier", required = false) UUID supplier,
                                     @RequestParam(name = "category", required = false) UUID category,
                                     @RequestParam(name = "class", required = false) UUID classPublicId,
                                     @RequestParam(name = "subClass", required = false) UUID subClassPublicId,
                                     @RequestParam(name = "brandPublicId", required = false) UUID brandPublicId,
                                     @RequestParam(name = "isActive", required = false) Boolean isActive,
                                     @PageableDefault(size = 20, sort = "productName", direction = Sort.Direction.ASC) Pageable pageable) {

        return productService.getProducts(searchParam, productName, status, supplier, category, classPublicId, subClassPublicId, isActive
                , null, brandPublicId, pageable);
    }

    @PreAuthorize("hasAnyAuthority('VIEW_PRODUCT','READ_SUPPLIER_PORTAL')")
    @JsonView(BaseView.ProductDetailView.class)
    @GetMapping("/supplier/{supplierPublicId}")
    @Operation(summary = "Get all products by supplier", description = "Get all products by supplier",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "FETCH_PRODUCTS", value = Examples.FETCH_PRODUCTS)})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<Product> getProductsBySupplier(@PathVariable UUID supplierPublicId,
                                               @RequestParam(name = "searchParam", required = false) String searchParam,
                                               @PageableDefault(size = 20, sort = "productName", direction = Sort.Direction.ASC) Pageable pageable) {

        return productService.getProductsBySupplier(supplierPublicId, searchParam, pageable);
    }

    @PreAuthorize("hasAuthority('DELETE_PRODUCT')")
    @DeleteMapping("/{publicId}")
    @Operation(summary = "Delete a product", description = "Delete a product",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void deleteProduct(@PathVariable UUID publicId,
                              @Parameter(hidden = true) Principal principal) {

        productService.deleteProduct(publicId, principal.getName());
    }

    // product view per warehouse

    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    @JsonView(BaseView.InventorySummaryView.class)
    @GetMapping("/{productPublicId}/view")
    @Operation(summary = "Get Product View per Warehouse", description = "Get Product View per Warehouse",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET_PRODUCT_VIEW", value = Examples.GET_PRODUCT_VIEW)})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public List<ProductViewResponse> getProductViewPerWarehouse(@PathVariable UUID productPublicId) {

        return productService.getProductViewPerWarehouse(productPublicId);
    }

    // check if product is available

    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    @GetMapping("/{productPublicId}/availability")
    @Operation(summary = "check if product is available", description = "check if product is available",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET_PRODUCT_VIEW", value = Examples.GET_PRODUCT_VIEW)})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public ProductResponse productResponse(@PathVariable(name = "productPublicId") UUID productId) {

        return productService.getAvailabilityResponse(productId);
    }

    // product adjustments

    @PreAuthorize("hasAuthority('CREATE_PRODUCT_ADJUSTMENT')")
    @JsonView(BaseView.ProductDetailView.class)
    @PostMapping("/{productPublicId}/adjustment")
    @Operation(summary = "Create a product adjustment", description = "Create a product adjustment",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "CREATE_PRODUCT_ADJUSTMENT", value = Examples.CREATE_PRODUCT_ADJUSTMENT),
    })))
    public ProductAdjustment createProductAdjustment(@PathVariable UUID productPublicId,
                                                     @RequestBody @Valid ProductAdjustmentRequest productAdjustmentRequest,
                                                     @Parameter(hidden = true) Principal principal) {

        return productService.createProductAdjustment(productPublicId, productAdjustmentRequest, principal.getName());
    }

    @PreAuthorize("hasAuthority('VIEW_PRODUCT_ADJUSTMENT')")
    @JsonView(BaseView.ProductDetailView.class)
    @GetMapping("/adjustments")
    @Operation(summary = "Get all product adjustments", description = "Get all product adjustments",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<ProductAdjustment> getProductAdjustments(@RequestParam(name = "searchParam", required = false) String searchParam,
                                                         @RequestParam(name = "status", required = false) AdjustmentStatus status,
                                                         @RequestParam(name = "adjustmentType", required = false) AdjustmentType adjustmentType,
                                                         @RequestParam(name = "warehousePublicId", required = false) String warehousePublicId,
                                                         @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return productService.getProductAdjustments(searchParam, status, adjustmentType, warehousePublicId, pageable);
    }

    // approve product adjustment
    @PreAuthorize("hasAuthority('UPDATE_PRODUCT_ADJUSTMENT')")
    @PatchMapping("/adjustments/{publicId}/actions")
    @Operation(summary = "Approve a product adjustment", description = "Approve a product adjustment",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void actionProductAdjustment(@PathVariable UUID publicId,
                                        @RequestParam(name = "action") ProductAdjustmentAction action,
                                        @RequestParam(name = "approver") AdjustmentApprover approver,
                                        @Parameter(hidden = true) Principal principal) {

        productService.approveProductAdjustment(publicId, approver, action, principal.getName());
    }

    // delete product adjustment
    @PreAuthorize("hasAuthority('DELETE_PRODUCT_ADJUSTMENT')")
    @DeleteMapping("/adjustments/{publicId}")
    @Operation(summary = "Delete a product adjustment", description = "Delete a product adjustment",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void deleteProductAdjustment(@PathVariable UUID publicId,
                                        @Parameter(hidden = true) Principal principal) {

        productService.deleteProductAdjustment(publicId, principal.getName());
    }

    // grant supplier access to supplier portal

    //@PreAuthorize("hasAuthority('GRANT_SUPPLIER_PORTAL_ACCESS')")
    @PostMapping("/supplier/{supplierPublicId}/grant-portal-access")
    @Operation(summary = "Grant supplier access to supplier portal", description = "Grant supplier access to supplier portal",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void grantSupplierPortalAccess(@PathVariable UUID supplierPublicId,
                                          @Parameter(hidden = true) Principal principal) {

        productService.grantSupplierPortalAccess(supplierPublicId, principal.getName());
    }

    @PatchMapping("/{publicId}/markAsBestSeller")
    @Operation(summary = "Mark product as best seller", description = "Mark product as best seller",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void markProductAsBestSeller(@PathVariable UUID publicId,
                                        @RequestParam(name = "isBestSeller") boolean isBestSeller,
                                        @Parameter(hidden = true) Principal principal) {

        productService.markProductAsBestSeller(publicId, isBestSeller, principal.getName());
    }

    // inactivate product

    @PatchMapping("/{publicId}/markAsActive")
    @Operation(summary = "Mark product as inactive", description = "Mark product as inactive",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void markProductAsInactive(@PathVariable UUID publicId,
                                      @RequestParam(name = "isInactive") boolean isInactive,
                                      @Parameter(hidden = true) Principal principal) {

        productService.markProductAsInactive(publicId, isInactive, principal.getName());
    }

    /**
     *
     * v2 product endpoints
     */
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    @GetMapping("/v2")
    @Operation(summary = "Get all products", description = "Get all products",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public ProductPageable getProductForMobile(@RequestParam(name = "searchParam", required = false) String searchParam,
                                               @RequestParam(name = "productName", required = false) String productName,
                                               @RequestParam(name = "status", required = false) ProductStatus status,
                                               @RequestParam(name = "supplier", required = false) UUID supplier,
                                               @RequestParam(name = "category", required = false) UUID category,
                                               @RequestParam(name = "class", required = false) UUID classPublicId,
                                               @RequestParam(name = "subClass", required = false) UUID subClassPublicId,
                                               @RequestParam(name = "brandPublicId", required = false) UUID brandPublicId,
                                               @PageableDefault(size = 20, sort = "productName", direction = Sort.Direction.ASC) Pageable pageable) {

        return productService.getProductsMobile(searchParam, productName, status, supplier, category, classPublicId,
                subClassPublicId,
                Boolean.TRUE, Boolean.TRUE, brandPublicId, pageable);
    }

    //TODO to be deleted once done
    @PatchMapping("/updateTaxCode/recalculate")
    public void updateTaxCodeProduct(@Parameter(hidden = true) Principal principal) {

        productService.updateTaxCodeProduct(principal.getName());
    }

    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    @JsonView(BaseView.SupplierTargetView.class)
    @GetMapping("/supplierTarget")
    @Operation(summary = "Supplier Target", description = "Supplier Target",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public List<SupplierTarget> getSupplierTarget(@RequestParam(value = "productPublicId", required = false) UUID productPublicId,
                                                  @RequestParam(value = "supplierPublicId", required = false) UUID supplierPublicId,
                                                  @PageableDefault(size = 100, sort = "productName", direction =
                                                          Sort.Direction.ASC) Pageable pageable,
                                                  @Parameter(hidden = true) Principal principal) {

        return productService.getSupplierTarget(productPublicId, supplierPublicId, principal, pageable);
    }

    @GetMapping("/brands")
    @Operation(summary = "get all product brands", description = "Product brands",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public List<String> getProductBrands(@RequestParam(value = "searchParam", required = false) String searchParam) {

        return productService.getProductBrands(searchParam);
    }

    @JsonView(BaseView.MiniProdView.class)
    @GetMapping("/get-products")
    @Operation(summary = "get list of product", description = "get list of product",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public List<Product> listProduct(@RequestParam(name = "productPublicId") List<UUID> productPublicId,
                                 @Parameter(hidden = true) Principal principal) {

        return productService.listProduct(productPublicId, principal.getName());
    }
}
