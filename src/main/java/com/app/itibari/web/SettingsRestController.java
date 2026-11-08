package com.app.itibari.web;

import com.app.itibari.dataTypes.EntityType;
import com.app.itibari.dataTypes.TemplateVariable;
import com.app.itibari.docs.Examples;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.*;
import com.app.itibari.repository.ReminderLogRepository;
import com.app.itibari.services.*;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
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
import java.util.UUID;

@Tag(name = "Settings", description = "Settings")
@RestController
@RequestMapping("/settings")
public class SettingsRestController {

    private final RegionWarehouseMappingService regionWarehouseMappingService;
    private final DocumentRuleService documentRuleService;
    private final CreditRuleService creditRuleService;
    private final TaxCodeService taxCodeService;
    private final ReminderSettingService reminderSettingService;
    private final ReminderJobService reminderJobService;
    private final ReminderLogRepository reminderLogRepository;
    private final ProductBrandService productBrandService;
    private final TraSwitchService traSwitchService;

    public SettingsRestController(RegionWarehouseMappingService regionWarehouseMappingService, DocumentRuleService documentRuleService, CreditRuleService creditRuleService, TaxCodeService taxCodeService, ReminderSettingService reminderSettingService, ReminderJobService reminderJobService, ReminderLogRepository reminderLogRepository, ProductBrandService productBrandService, TraSwitchService traSwitchService) {

        this.regionWarehouseMappingService = regionWarehouseMappingService;
        this.documentRuleService = documentRuleService;
        this.creditRuleService = creditRuleService;
        this.taxCodeService = taxCodeService;
        this.reminderSettingService = reminderSettingService;
        this.reminderJobService = reminderJobService;
        this.reminderLogRepository = reminderLogRepository;
        this.productBrandService = productBrandService;
        this.traSwitchService = traSwitchService;
    }

    @JsonView(BaseView.RegionMappingView.class)
    //@PreAuthorize("hasAuthority('CREATE_REGION_WAREHOUSE_MANAGEMENT')")
    @PostMapping("/regionWarehouseMapping")
    @Operation(summary = "Create a region warehouse mapping", description = "Create a new region warehouse mapping.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "CREATE_REGION_WAREHOUSE_MAPPING", value = Examples.CREATE_REGION_WAREHOUSE_MAPPING)
    })))
    public RegionWarehouseMapping createRegionWarehouseMapping(@RequestBody @Valid RegionWarehouseMappingRequest regionWarehouseMappingRequest,
                                                               @Parameter(hidden = true) Principal principal) {

        return regionWarehouseMappingService.createRegionWarehouseMapping(regionWarehouseMappingRequest, principal.getName());
    }

    @PreAuthorize("hasAuthority('CREATE_REGION_WAREHOUSE_MANAGEMENT')")
    @PatchMapping("/regionWarehouseMapping/{publicId}")
    @Operation(summary = "Update a region warehouse mapping", description = "Update an existing region warehouse mapping.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "UPDATE_REGION_WAREHOUSE_MAPPING", value = Examples.UPDATE_REGION_WAREHOUSE_MAPPING)
    })))
    public void updateRegionWarehouseMapping(@PathVariable("publicId") UUID publicId,
                                             @RequestBody @Valid RegionMappingUpdateRequest regionWarehouseMappingRequest,
                                             @Parameter(hidden = true) Principal principal) {

        regionWarehouseMappingService.updateRegionWarehouseMapping(publicId, regionWarehouseMappingRequest, principal.getName());
    }

    // get region warehouse mapping by publicId
    @JsonView(BaseView.RegionMappingView.class)
    @PreAuthorize("hasAuthority('READ_REGION_WAREHOUSE_MANAGEMENT')")
    @GetMapping("/regionWarehouseMapping/{publicId}")
    @Operation(summary = "Get a region warehouse mapping by public ID", description = "Retrieve a region warehouse mapping by its public ID.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public RegionWarehouseMapping getRegionWarehouseMappingByPublicId(@PathVariable("publicId") UUID publicId,
                                                                      @Parameter(hidden = true) Principal principal) {

        return regionWarehouseMappingService.getRegionWarehouseMappingByPublicId(publicId, principal.getName());
    }

    @JsonView(BaseView.RegionMappingView.class)
    @PreAuthorize("hasAuthority('READ_REGION_WAREHOUSE_MANAGEMENT')")
    @GetMapping("/regionWarehouseMappings")
    @Operation(summary = "Get all region warehouse mappings", description = "Retrieve all region warehouse mappings.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<RegionWarehouseMapping> getAllRegionWarehouseMappings(
            @PageableDefault(sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable,
            @Parameter(hidden = true) Principal principal) {

        return regionWarehouseMappingService.getAllRegionWarehouseMappings(pageable, principal.getName());
    }

    @PreAuthorize("hasAuthority('DELETE_REGION_WAREHOUSE_MANAGEMENT')")
    @DeleteMapping("/regionWarehouseMapping/{publicId}")
    @Operation(summary = "Delete a region warehouse mapping", description = "Delete an existing region warehouse mapping by its public ID.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void deleteRegionWarehouseMapping(@PathVariable("publicId") UUID publicId,
                                             @Parameter(hidden = true) Principal principal) {

        regionWarehouseMappingService.deleteRegionWarehouseMapping(publicId, principal.getName());
    }


    // document rules

    @PreAuthorize("hasAuthority('CREATE_DOCUMENT_RULE')")
    @JsonView({BaseView.DetailedView.class})
    @PostMapping("/documentRule")
    @Operation(summary = "Create Document Type | Rules", description = "Create Document Type | Rules",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.GET_DOCUMENT_RULE_OK_Response))),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.CREATE_DOCUMENT_RULE_ERROR_Response)))
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Scheme Actions", required = true, content = @Content(examples = {
            @ExampleObject(name = "Example 1", value = Examples.CREATE_DOCUMENT_RULE_REQUEST_1),
            @ExampleObject(name = "Example 2", value = Examples.CREATE_DOCUMENT_RULE_REQUEST_2)
    })))
    public DocumentRule createDocument(@Valid @RequestBody CreateDocumentRuleRequest createDocumentRuleRequest,
                                       @Parameter(hidden = true) Principal principal) {

        return documentRuleService.createDocumentRule(createDocumentRuleRequest, principal.getName());
    }


    // @PreAuthorize("hasAuthority('READ_DOCUMENT_RULE')")
    @JsonView({BaseView.DetailedView.class})
    @GetMapping("/documentRules")
    @Operation(summary = "Fetch Document Types & Rules", description = "Fetch Document Types & Rules",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.GET_DOCUMENT_RULES_OK_Response))),
            })
    public Page<DocumentRule> fetchAllDocumentRule(@RequestParam(name = "entityType", required = false) EntityType entityType,
                                                   @RequestParam(name = "entitySubType", required = false) String entitySubType,
                                                   @Parameter(hidden = true) Principal principal,
                                                   @Parameter(hidden = true) @PageableDefault(size = 20, sort = {"creationTime"}, direction = Sort.Direction.DESC) Pageable pageable) {

        return documentRuleService.retrieveDocumentRule(entityType, entitySubType, principal.getName(), pageable);
    }


    @PreAuthorize("hasAuthority('UPDATE_DOCUMENT_RULE')")
    @JsonView({BaseView.DetailedView.class})
    @PatchMapping("/documentRule")
    @Operation(summary = "Update Document Types & rules", description = "Update Document Types & rules",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.GET_DOCUMENT_RULE_OK_Response))),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.UPDATE_DOCUMENT_RULE_Error_Response))),
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Update", required = true, content = @Content(examples = {
                    @ExampleObject(name = "Example 1", value = Examples.UPDATE_DOCUMENT_RULE_REQUEST)
            })))
    public DocumentRule updateDocument(@RequestBody @Valid UpdateDocumentRuleRequest updateDocumentRuleRequest,
                                       @Parameter(hidden = true) Principal principal) {

        return documentRuleService.updateDocumentRule(updateDocumentRuleRequest, principal.getName());
    }


    // fetch one document rule

    // @PreAuthorize("hasAuthority('READ_DOCUMENT_RULE')")
    @JsonView({BaseView.DetailedView.class})
    @GetMapping("/documentRule/{documentPublicId}")
    @Operation(summary = "Fetch One Document Rule", description = "Fetch One Document Rule",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.GET_DOCUMENT_RULE_OK_Response))),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.GET_DOCUMENT_RULE_Error_Response))),
            })
    public DocumentRule fetchOneDocumentRule(@PathVariable("documentPublicId") UUID documentPublicId) {
        return documentRuleService.getDocumentRuleByPublicId(documentPublicId);
    }


    // delete document rule
    @PreAuthorize("hasAuthority('DELETE_DOCUMENT_RULE')")
    @DeleteMapping("/documentRule/{documentPublicId}")
    @Operation(summary = "Delete Document Rule", description = "Delete Document Rule",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.DELETE_DOCUMENT_RULE_Error_Response))),
            })
    public void deleteDocumentRule(@PathVariable("documentPublicId") UUID documentPublicId,
                                   @Parameter(hidden = true) Principal principal) {
        documentRuleService.deleteDocumentRule(documentPublicId, principal.getName());
    }


    // credit rules will be added here

    @JsonView(BaseView.DetailedView.class)
    @PostMapping("/creditRule")
    @PreAuthorize("hasAuthority('CREATE_CREDIT_RULE')")
    @Operation(summary = "Create Credit Rule", description = "Create Credit Rule",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
                    @ExampleObject(name = "CREATE_CREDIT_RULE", value = Examples.CREATE_CREDIT_RULE_REQUEST),
                    @ExampleObject(name = "CREATE_OVERDEUE_FINE_CREDIT_RULE", value = Examples.CREATE_OVERDUE_FINE_CREDIT_RULE_REQUEST)
            })))
    public CreditRule createCreditRule(@RequestBody CreateCreditRuleRequest createCreditRuleRequest,
                                       @Parameter(hidden = true) Principal principal) {
        return creditRuleService.createCreditRule(createCreditRuleRequest, principal.getName());
    }


    // update credit rule

    @JsonView(BaseView.DetailedView.class)
    @PatchMapping("/creditRule/{publicId}")
    @PreAuthorize("hasAuthority('CREATE_CREDIT_RULE')")
    @Operation(summary = "Update Credit Rule", description = "Update Credit Rule",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
                    @ExampleObject(name = "UPDATE_CREDIT_RULE", value = "")
            })))
    public void updateCreditRule(@PathVariable UUID publicId,
                                 @RequestBody CreateCreditRuleRequest createCreditRuleRequest,
                                 @Parameter(hidden = true) Principal principal) {
        creditRuleService.updateCreditRule(publicId, createCreditRuleRequest, principal.getName());
    }


    // get all credit rules

    @JsonView(BaseView.DetailedView.class)
    @GetMapping("/creditRules")
    @PreAuthorize("hasAuthority('READ_CREDIT_RULE')")
    @Operation(summary = "Get All Credit Rules", description = "Get All Credit Rules",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.GET_CREDIT_RULES_OK_Response))),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<CreditRule> getAllCreditRules(@Parameter(hidden = true) Principal principal,
                                              @Parameter(hidden = true) @PageableDefault(size = 20, sort = {"creationTime"}, direction = Sort.Direction.DESC) Pageable pageable) {
        return creditRuleService.getAllCreditRules(principal.getName(), pageable);
    }

    // delete

    @DeleteMapping("/creditRule/{publicId}")
    @PreAuthorize("hasAuthority('DELETE_CREDIT_RULE')")
    @Operation(summary = "Delete Credit Rule", description = "Delete Credit Rule",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void deleteCreditRule(@PathVariable("publicId") UUID publicId,
                                 @Parameter(hidden = true) Principal principal) {
        creditRuleService.deleteCreditRule(publicId, principal.getName());
    }

    @PostMapping("/taxCodes")
    @JsonView({BaseView.TaxCodeView.class})
    @Operation(summary = "create tax code", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json",
                    schema = @Schema(example = Examples.TAX_CODE_RESPONSE)))
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "update audited account rule", value = Examples.CREATE_TAX_CODE),})))
    public TaxCode createTaxCode(@Valid @RequestBody TaxCodeRequest taxCodeRequest,
                                 @Parameter(hidden = true) Principal principal) {

        return taxCodeService.createCreateTaxCode(taxCodeRequest, principal.getName());
    }

    @GetMapping("/taxCodes")
    @JsonView(BaseView.TaxCodeView.class)
    @Operation(summary = "Get All Tax Code", description = "Get All Tax Code",
            responses = {@ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.GET_CREDIT_RULES_OK_Response)))})
    @PageableAsQueryParam
    public Page<TaxCode> getTaxCodes(@Parameter(hidden = true) Principal principal,
                                     @Parameter(hidden = true) @PageableDefault(size = 10, sort = {"creationTime"},
                                             direction = Sort.Direction.DESC) Pageable pageable) {
        return taxCodeService.getTaxCodes(principal.getName(), pageable);
    }

    @JsonView({BaseView.TaxCodeView.class})
    @GetMapping("/taxCodes/{taxCodePublicId}")
    @Operation(summary = "Fetch One Tax code", description = "Fetch One Tax code",
            responses = {@ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.GET_DOCUMENT_RULE_OK_Response))),})
    public TaxCode fetchOneTaxCode(@PathVariable("taxCodePublicId") UUID taxCodePublicId) {
        return taxCodeService.fetchOneTaxCode(taxCodePublicId);
    }

    @PatchMapping("/taxCodes/{taxCodePublicId}")
    @JsonView(BaseView.TaxCodeView.class)
    @Operation(summary = "Update Credit Rule", description = "Update Credit Rule",
            responses = {@ApiResponse(responseCode = "200", description = "OK")},
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
                    @ExampleObject(name = "UPDATE_TAX_CODE", value = Examples.UPDATE_TAX_CODE)})))
    public TaxCode updateTaxCode(@PathVariable UUID taxCodePublicId,
                                 @Valid @RequestBody TaxCodeRequest taxCodeRequest,
                                 @Parameter(hidden = true) Principal principal) {
        return taxCodeService.updateTaxCode(taxCodePublicId, taxCodeRequest, principal.getName());
    }

    @DeleteMapping("/taxCodes/{taxCodePublicId}")
    @Operation(summary = "Delete TaxCode", description = "Delete TaxCode",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public void deleteTaxCode(@PathVariable("taxCodePublicId") UUID taxCodePublicId,
                              @Parameter(hidden = true) Principal principal) {
        taxCodeService.deleteTaxCode(taxCodePublicId, principal.getName());
    }


    // ─── Reminder Settings ──────────────────────────────────────────────────────

    /**
     * Supported template placeholders for the messageTemplate field:
     * {{customerName}}, {{invoiceNumber}}, {{dueDate}},
     * {{daysOverdue}}, {{amountDue}}, {{companyName}}
     */
    @JsonView(BaseView.ReminderSettingView.class)
    @PostMapping("/reminderSetting")
    @Operation(summary = "Create Reminder Setting", description = "Create a reminder setting. " +
            "The messageTemplate field supports dynamic placeholders: " +
            "{{customerName}}, {{invoiceNumber}}, {{dueDate}}, {{daysOverdue}}, {{amountDue}}, {{companyName}}",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public ReminderSetting createReminderSetting(@Valid @RequestBody ReminderSettingRequest request,
                                                 @Parameter(hidden = true) Principal principal) {
        return reminderSettingService.createReminderSetting(request, principal.getName());
    }

    @JsonView(BaseView.ReminderSettingView.class)
    @PatchMapping("/reminderSetting/{publicId}")
    @Operation(summary = "Update Reminder Setting", description = "Update an existing reminder setting.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void updateReminderSetting(@PathVariable UUID publicId,
                                      @Valid @RequestBody ReminderSettingRequest request,
                                      @Parameter(hidden = true) Principal principal) {
        reminderSettingService.updateReminderSetting(publicId, request, principal.getName());
    }

    @JsonView(BaseView.ReminderSettingView.class)
    @GetMapping("/reminderSetting/{publicId}")
    @Operation(summary = "Get Reminder Setting", description = "Retrieve a reminder setting by its public ID.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public ReminderSetting getReminderSetting(@PathVariable UUID publicId) {
        return reminderSettingService.getReminderSetting(publicId);
    }

    @JsonView(BaseView.ReminderSettingView.class)
    @GetMapping("/reminderSettings")
    @PageableAsQueryParam
    @Operation(summary = "Get All Reminder Settings", description = "Retrieve all reminder settings.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Page<ReminderSetting> getAllReminderSettings(
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable,
            @Parameter(hidden = true) Principal principal) {
        return reminderSettingService.getAllReminderSettings(pageable, principal.getName());
    }

    @DeleteMapping("/reminderSetting/{publicId}")
    @Operation(summary = "Delete Reminder Setting", description = "Soft-delete a reminder setting by its public ID.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void deleteReminderSetting(@PathVariable UUID publicId,
                                      @Parameter(hidden = true) Principal principal) {
        reminderSettingService.deleteReminderSetting(publicId, principal.getName());
    }

    @GetMapping("/reminderSettings/templateVariables")
    @Operation(summary = "Get Available Template Variables",
            description = "Returns the list of supported placeholder variables for reminder message templates.")
    public TemplateVariable[] getTemplateVariables() {
        return TemplateVariable.values();
    }


    @JsonView(BaseView.ReminderJobView.class)
    @GetMapping("/reminderJob/{publicId}")
    @Operation(summary = "Get Reminder Job", description = "Retrieve a reminder job by its public ID.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public ReminderJob getReminderJob(@PathVariable UUID publicId) {
        return reminderJobService.getReminderJob(publicId);
    }

    @JsonView(BaseView.ReminderJobView.class)
    @GetMapping("/reminderJobs")
    @PageableAsQueryParam
    @Operation(summary = "Get All Reminder Jobs", description = "Retrieve all reminder jobs paginated.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Page<ReminderJob> getAllReminderJobs(
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable,
            @Parameter(hidden = true) Principal principal) {
        return reminderJobService.getAllReminderJobs(pageable, principal.getName());
    }


    @JsonView(BaseView.ReminderLogView.class)
    @GetMapping("/reminderLogs")
    @PageableAsQueryParam
    @Operation(summary = "Get All Reminder Logs", description = "Retrieve the full audit log of all reminder dispatches.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Page<ReminderLog> getAllReminderLogs(@RequestParam(name = "searchParam", required = false) String searchParam,
                                                @RequestParam(name = "from", required = false) String from,
                                                @RequestParam(name = "to", required = false) String to,
                                                @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable,
                                                @Parameter(hidden = true) Principal principal) {
        return reminderSettingService.fetchReminderLogs(searchParam, from, to, pageable);
    }

    @JsonView(BaseView.ProductBrandView.class)
    @PostMapping("/product-brands")
    @Operation(summary = "Create product brand", description = "Create product brand.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK")}, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "CREATE_PRODUCT_BRAND", value = Examples.CREATE_PRODUCT_BRAND)})))
    public ProductBrand createProductBrand(@RequestBody @Valid ProductBrandRequest productBrandRequest,
                                           @Parameter(hidden = true) Principal principal) {

        return productBrandService.createProductBrand(productBrandRequest, principal.getName());
    }

    @JsonView(BaseView.ProductBrandView.class)
    @GetMapping("/product-brands")
    @PageableAsQueryParam
    @Operation(summary = "Get All product brand", description = "Retrieve product brands",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public Page<ProductBrand> getAllProductBrands(@RequestParam(name = "searchParam", required = false) String searchParam,
                                                  @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable,
                                                  @Parameter(hidden = true) Principal principal) {
        return productBrandService.fetchProductBrands(searchParam, pageable, principal);
    }

    @JsonView(BaseView.ProductBrandView.class)
    @PutMapping("/product-brands/{publicId}")
    @Operation(summary = "Update product brand", description = "Update product brand.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK")}, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "CREATE_PRODUCT_BRAND", value = Examples.CREATE_PRODUCT_BRAND)})))
    public ProductBrand updateProductBrand(@PathVariable UUID publicId,
                                           @RequestBody @Valid ProductBrandRequest productBrandRequest,
                                           @Parameter(hidden = true) Principal principal) {

        return productBrandService.updateProductBrand(publicId, productBrandRequest, principal.getName());
    }

    @DeleteMapping("/product-brands/{publicId}")
    @Operation(summary = "Delete product brand", description = "Delete product brand.")
    public void deleteProductBrand(@PathVariable UUID publicId,
                                   @Parameter(hidden = true) Principal principal) {

        productBrandService.deleteProductBrand(publicId, principal.getName());
    }


    @JsonView(BaseView.TraSwitchView.class)
    @PostMapping("/switchTraOn")
    @Operation(summary = "Switch TRA On/Off", description = "Switch TRA On/Off",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public TraSwitch switchTraOn(@RequestParam(name = "switchOn") Boolean switchOn,
                            @Parameter(hidden = true) Principal principal) {
       return traSwitchService.switchTraOn(switchOn, principal.getName());
    }


    // get

    @JsonView(BaseView.TraSwitchView.class)
    @GetMapping("/switchTraOn")
    @Operation(summary = "Get TRA Switch Status", description = "Get TRA Switch Status",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public TraSwitch getTraSwitchStatus(@Parameter(hidden = true) Principal principal) {
        return traSwitchService.getTraSwitchStatus(principal.getName());
    }
}
