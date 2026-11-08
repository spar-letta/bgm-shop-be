package com.app.itibari.web;

import com.app.itibari.docs.Examples;
import com.app.itibari.models.request.DepartmentRequest;
import com.app.itibari.models.response.DepartmentDtoResponse;
import com.app.itibari.services.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@Tag(name = "Departments", description = "Departments API")
@RestController
@RequestMapping("/departments")
@RequiredArgsConstructor
public class DepartmentRestController {

    private final DepartmentService departmentService;

    @PostMapping
//    @PreAuthorize("hasAuthority('CREATE_DEPARTMENT')")
    @Operation(summary = "create department", description = "create department", responses = {
            @ApiResponse(responseCode = "200", description = "OK")
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "Action CREATE_DEPARTMENT", value = Examples.CREATE_DEPARTMENT_request),})))
    public DepartmentDtoResponse createDepartment(@Valid @RequestBody DepartmentRequest departmentRequest,
                                                  @Parameter(hidden = true) Principal principal) {

        return departmentService.createDepartment(departmentRequest, principal);
    }

    @PutMapping("/{publicId}")
//    @PreAuthorize("hasAuthority('UPDATE_DEPARTMENT')")
    @Operation(summary = "update department", description = "update department", responses = {
            @ApiResponse(responseCode = "200", description = "OK")
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "Action UPDATE_DEPARTMENT", value = Examples.UPDATE_DEPARTMENT_request),})))
    public DepartmentDtoResponse updateDepartment(@PathVariable(name = "publicId") UUID publicId,
                                                  @Valid @RequestBody DepartmentRequest departmentRequest,
                                                  @Parameter(hidden = true) Principal principal) {

        return departmentService.updateDepartment(publicId, departmentRequest, principal);
    }

    @GetMapping
    @Operation(summary = "Get all department", description = "Get all department",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public Page<DepartmentDtoResponse> getDepartments(@RequestParam(name = "searchParam", required = false) String searchParam,
                                                      @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return departmentService.getDepartmentList(searchParam, pageable);
    }

    @GetMapping("/{publicId}")
    @Operation(summary = "Get department", description = "Get department",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public DepartmentDtoResponse getDepartment(@PathVariable(name = "publicId", required = false) UUID publicId) {

        return departmentService.getDepartment(publicId);
    }

    @DeleteMapping("/{publicId}")
    @Operation(summary = "Delete department", description = "Delete department",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public void deleteDepartment(@PathVariable(name = "publicId", required = false) UUID publicId, @Parameter(hidden = true) Principal principal) {

        departmentService.deleteDepartment(publicId, principal);
    }
}
