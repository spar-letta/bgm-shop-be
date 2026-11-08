package com.app.itibari.web;

import com.app.itibari.configuration.JwtToken;
import com.app.itibari.configuration.JwtTokenAuthentication;
import com.app.itibari.dataTypes.MobileUserType;
import com.app.itibari.dataTypes.ProductStatus;
import com.app.itibari.dataTypes.ReferenceType;
import com.app.itibari.docs.Examples;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.*;
import com.app.itibari.models.response.*;
import com.app.itibari.services.*;
import com.app.itibari.transformer.ProductTransformer;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.helpers.VersionHelper;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Tag(name = "Authentication")
@SecurityRequirements
@RestController
@RequestMapping("/authentication")
@RequiredArgsConstructor
public class AuthenticationRestController {

    private final JwtTokenAuthentication jwtTokenAuthentication;

    private final CustomersService customersService;

    private final JwtToken jwtToken;

    private final OrganizationRegionsService organizationRegionsService;

    private final OrganizationLocationsService organizationLocationsService;

    private final OrganizationCountriesService organizationCountriesService;

    private final OrganizationCitiesService organizationCitiesService;

    private final VersionHelper versionHelper;

    private final ProductService productService;

    private final FileService fileService;

    private final ProductTransformer productTransformer;

    @PostMapping("/token")
    @Operation(summary = "Create token", description = "Create token",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public ResponseEntity<LoginResponse> generateAccessToken(@RequestBody LoginRequest loginRequest) {

        log.info("Login request: {}, received", loginRequest.getUsername());
        LoginResponse loginResponse = jwtTokenAuthentication.generateJwtToken(loginRequest);
        log.info("Login response: {}, generated", loginResponse.getUsername());
        return new ResponseEntity<>(loginResponse, HttpStatus.OK);
    }

    @PostMapping("/resetPassword")
    @Operation(summary = "Reset password", description = "Reset password",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "RESET_PASSWORD_REQUEST", value = Examples.RESET_PASSWORD_REQUEST),
    })))
    public void resetPassword(@RequestBody ResetPasswordRequest resetPasswordRequest) {

        jwtTokenAuthentication.resetPassword(resetPasswordRequest);
    }

    @PostMapping("/confirmPassword")
    @Operation(summary = "Confirm password", description = "Confirm password",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "RESET_PASSWORD_REQUEST", value = Examples.CONFIRM_PASSWORD_REQUEST),
    })))
    public void confirmPassword(@RequestBody ConfirmPasswordRequest confirmPasswordRequest) {

        jwtTokenAuthentication.confirmPassword(confirmPasswordRequest);
    }

    // customer login

    @PostMapping("/mobilePlatform/login")
    @Operation(summary = "Customer login", description = "Customer login",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "MOBILE LOGIN REQUEST", value = Examples.MOBILE_LOGIN_REQUEST),
    })))
    public ResponseEntity<MobileResponse> customerLogin(@RequestBody MobileLoginRequest mobileLoginRequest,
                                                        @RequestHeader(value = "X-App-Version", required = false) String appVersion) {

        log.info("Customer login request: {}, received", mobileLoginRequest.getMobileUserType().name());

        if (mobileLoginRequest.getMobileUserType() == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Invalid user type")
                    .userMessage("Sorry.Invalid user type");
        }

        if (appVersion != null && !StringUtils.isEmpty(appVersion)) {
            versionHelper.checkIfAppVersionIsValid(appVersion, mobileLoginRequest.getMobileUserType());
        }

        if (mobileLoginRequest.getPhoneNumber() == null || StringUtils.isEmpty(mobileLoginRequest.getPhoneNumber())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Invalid phone number")
                    .userMessage("Sorry.Invalid phone number");
        }

        MobileResponse mobileResponse = jwtTokenAuthentication.mobileResponse(mobileLoginRequest, appVersion);
        return new ResponseEntity<>(mobileResponse, HttpStatus.OK);
    }

    @Deprecated
    @PostMapping("/mobilePlatform/OTPConfirm")
    @Operation(summary = "Customer confirm", description = "Customer confirm",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "CONFIRM OTP", value = Examples.CONFIRM_OTP_REQUEST),
    })))
    public ResponseEntity<LoginResponse> mobileOTPConfirm(@RequestBody ConfirmRequest confirmRequest,
                                                          @RequestHeader(value = "X-App-Version", required = false) String appVersion) {

        if (confirmRequest.getPhoneNumber() == null || StringUtils.isEmpty(confirmRequest.getPhoneNumber())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Invalid phone number")
                    .userMessage("Sorry.Invalid phone number");
        }

        if (confirmRequest.getOtp() == null || StringUtils.isEmpty(confirmRequest.getOtp())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Invalid OTP")
                    .userMessage("Sorry.Invalid OTP");
        }

        if (confirmRequest.getMobileUserType() == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Invalid user type")
                    .userMessage("Sorry.Invalid user type");
        }
        LoginResponse loginResponse = jwtTokenAuthentication.mobileOTPConfirm(confirmRequest, appVersion);
        return new ResponseEntity<>(loginResponse, HttpStatus.OK);
    }


    @PostMapping("/mobilePlatform/validatePhoneNumber")
    @Operation(summary = "Validate phone number", description = "Validate phone number",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public boolean validatePhoneNumber(@RequestParam(name = "phoneNumber")  String phoneNumber,
                                       @RequestParam(name = "mobileUserType") MobileUserType mobileUserType) {
        return jwtTokenAuthentication.validatePhoneNumber(phoneNumber,mobileUserType);
    }

    @PostMapping("/mobilePlatform/forgotPassword")
    @Operation(summary = "Mobile forgot password", description = "Mobile forgot password",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public ResponseEntity<MobileResponse> mobileForgotPassword(@RequestBody MobileForgotPasswordRequest forgotPasswordRequest) {

        MobileResponse mobileResponse = jwtTokenAuthentication.mobileForgotPassword(forgotPasswordRequest);
        return new ResponseEntity<>(mobileResponse, HttpStatus.OK);
    }

    @PostMapping("/mobilePlatform/resetPassword")
    @Operation(summary = "Mobile reset password", description = "Mobile reset password",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public ResponseEntity<LoginResponse> mobileResetPassword(@RequestBody MobileResetPasswordRequest resetPasswordRequest,
                                                             @RequestHeader(value = "X-App-Version", required = false) String appVersion) {

        LoginResponse loginResponse = jwtTokenAuthentication.mobileResetPassword(resetPasswordRequest, appVersion);
        return new ResponseEntity<>(loginResponse, HttpStatus.OK);
    }

    @PostMapping("/mobilePlatform/customerRegister")
    @Operation(summary = "Customer register", description = "Customer register",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "CUSTOMER REGISTER REQUEST", value = Examples.CREATE_CUSTOMER),
    })))
    public ResponseEntity<LoginResponse> customerRegister(@RequestBody CustomersRequest customersRequest) {

        log.info("Customer register request: {}, received", customersRequest.getName());
        customersRequest.setSelfRegistered(true);
        Customers customers = customersService.createCustomer(customersRequest, null);

        LoginResponse loginResponse = new LoginResponse(jwtToken.generateMobilePlatformJwtToken(MobileUserType.Customer,
                                                                                                customers.getPhoneNumber()),
                                                        customers.getEmail());

        return new ResponseEntity<>(loginResponse, HttpStatus.OK);
    }

    @JsonView(BaseView.OrganizationRegionsView.class)
    @GetMapping("/regions")
    @Operation(summary = "Get all Organization Regions", description = "Get all Organization Regions",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<OrganizationRegions> getAllOrganizationRegions(@RequestParam(name = "searchParam", required = false) String searchParam,
                                                               @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return organizationRegionsService.getAllOrganizationRegions(searchParam, pageable);
    }

    @JsonView(BaseView.OrganizationLocationsView.class)
    @GetMapping("/locations")
    @Operation(summary = "Get all Organization Locations", description = "Get all Organization Locations",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<OrganizationLocations> getAllOrganizationLocations(@RequestParam(name = "searchParam", required = false) String searchParam,
                                                                   @RequestParam(name = "name", required = false) String name,
                                                                   @RequestParam(name = "organizationRegionId", required = false) UUID organizationRegionId,
                                                                   @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return organizationLocationsService.getAllOrganizationLocations(searchParam, name, organizationRegionId, pageable);
    }

    @JsonView(BaseView.OrganizationCountriesView.class)
    @GetMapping("/countries")
    @Operation(summary = "Get all Organization Countries", description = "Get all Organization Countries",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<OrganizationCountries> getAllOrganizationCountries(@RequestParam(name = "searchParam", required = false) String searchParam,
                                                                   @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return organizationCountriesService.getAllOrganizationCountries(searchParam, pageable);
    }

    @JsonView(BaseView.OrganizationCitiesView.class)
    @GetMapping("/cities")
    @Operation(summary = "Get all Organization Cities", description = "Get all Organization Cities",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<OrganizationCities> getAllOrganizationCities(@RequestParam(name = "searchParam", required = false) String searchParam,
                                                             @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return organizationCitiesService.getAllOrganizationCities(searchParam, pageable);
    }

    // products fetch for customers
    @JsonView(BaseView.ProductDetailView.class)
    @GetMapping("/products")
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
                                     @PageableDefault(size = 20, sort = "productName", direction = Sort.Direction.ASC) Pageable pageable) {

        return productService.getProducts(searchParam, productName, status, supplier, category, classPublicId, subClassPublicId,
                                          Boolean.TRUE, Boolean.TRUE, brandPublicId, pageable);
    }

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

    @JsonView(BaseView.ProductSummaryView.class)
    @GetMapping("/categories")
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

    @GetMapping("/productImage/{referenceId}")
    @Operation(summary = "Get Product Images", description = "Get Product Images",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(example = ""))),
                    @ApiResponse(responseCode = "404", description = "Input Error", content = @Content(mediaType = "application/json", schema = @Schema(example = "")))
            }
    )
    public ResponseEntity<FileResponseDTO> getProductImages(@PathVariable("referenceId") UUID referenceId) throws IOException {

        FileResponseDTO fileResponseDTO = fileService.getImages(referenceId, ReferenceType.PRODUCT);
        return ResponseEntity.ok(fileResponseDTO);
    }

    /**
     * new product fetch implementation for mobile app
     */

    // products fetch for customers
    @GetMapping("/v2/products")
    @Operation(summary = "Get all products", description = "Get all products",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public ProductPageable getProductForCustomer(@RequestParam(name = "searchParam", required = false) String searchParam,
                                                 @RequestParam(name = "productName", required = false) String productName,
                                                 @RequestParam(name = "status", required = false) ProductStatus status,
                                                 @RequestParam(name = "supplier", required = false) UUID supplier,
                                                 @RequestParam(name = "category", required = false) UUID category,
                                                 @RequestParam(name = "class", required = false) UUID classPublicId,
                                                 @RequestParam(name = "subClass", required = false) UUID subClassPublicId,
                                                 @RequestParam(name = "brandPublicId", required = false) UUID brandPublicId,
                                                 @SortDefault.SortDefaults({
                                                         @SortDefault(sort = "productName", direction = Sort.Direction.ASC),
                                                         @SortDefault(sort = "availableStock", direction = Sort.Direction.DESC)
                                                 })
                                                 Pageable pageable) {

        return productService.getProductsMobile(searchParam, productName, status, supplier, category, classPublicId,
                                                subClassPublicId,
                                                Boolean.TRUE, null, brandPublicId, pageable);
    }

// get mobile banner image

    @GetMapping("/mobileBanner")
    @Operation(summary = "Get Mobile Banner Image", description = "Get Mobile Banner Image",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(example = ""))),
                    @ApiResponse(responseCode = "404", description = "Input Error", content = @Content(mediaType = "application/json", schema = @Schema(example = "")))
            }
    )
    public ResponseEntity<List<BannerResponseDTO>> getMobileBannerImage() {
        List<BannerResponseDTO> fileResponseDTO = fileService.getMobileBanner(true, null);
        return ResponseEntity.ok(fileResponseDTO);
    }


//    @PostMapping("/generateTestToken")
//    public String getTestTokenForDA(){
//       return jwtTokenAuthentication.getTestTokenForDA();
//    }
}
