package com.app.itibari.web;

import com.app.itibari.docs.Examples;
import com.app.itibari.models.entities.Permission;
import com.app.itibari.models.entities.PermissionGroup;
import com.app.itibari.models.entities.Person;
import com.app.itibari.models.entities.Role;
import com.app.itibari.models.request.*;
import com.app.itibari.models.response.PermissionResponse;
import com.app.itibari.services.PersonService;
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
import java.util.Set;
import java.util.UUID;

@Tag(name = "person-controller")
@RestController
@RequestMapping("/system")
public class PersonRestController {

    private final PersonService personService;

    public PersonRestController(PersonService personService) {

        this.personService = personService;

    }

    @PreAuthorize("hasAuthority('CREATE_PERMISSION_GROUP')")
    @PostMapping("/permissionGroup")
    @Operation(summary = "Create permission group", description = "Create permission group",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "CREATE_PERMISSION_GROUP_REQUEST", value = Examples.CREATE_PERMISSION_GROUP_REQUEST),
    })))
    public void createPermissionGroup(@RequestBody @Valid PermissionGroupRequest permissionGroupRequest,
                                      @Parameter(hidden = true) Principal principal) {

        personService.createPermissionGroup(permissionGroupRequest, principal.getName());
    }

    @PreAuthorize("hasAnyAuthority('UPDATE_PERMISSION_GROUP','READ_SUPPLIER_PORTAL')")
    @PatchMapping("/permissionGroup/{publicId}")
    @Operation(summary = "Update permission group", description = "add  permissions to permission group",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "CREATE_PERMISSION_GROUP_REQUEST", value = Examples.CREATE_PERMISSION_GROUP_REQUEST),
    })))
    public void updatePermissionGroup(@PathVariable(name = "publicId") UUID publicId,
                                      @RequestBody @Valid PermissionRequest permissionRequest,
                                      @Parameter(hidden = true) Principal principal) {

        personService.updatePermissionGroup(publicId, permissionRequest, principal.getName());
    }

    @PreAuthorize("hasAnyAuthority('VIEW_PERMISSION_GROUP','READ_SUPPLIER_PORTAL')")
    @JsonView(BaseView.PermissionGroupView.class)
    @GetMapping("/permissionGroup/{publicId}")
    @Operation(summary = "Get permission group", description = "Get permission group",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public PermissionGroup getPermissionGroup(@PathVariable(name = "publicId") UUID publicId) {

        return personService.getPermissionGroup(publicId);
    }

    @PreAuthorize("hasAnyAuthority('VIEW_PERMISSION_GROUP','READ_SUPPLIER_PORTAL')")
    @JsonView(BaseView.PermissionGroupView.class)
    @GetMapping("/permissionGroup")
    @Operation(summary = "Get all permission groups", description = "Get all permission groups",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<PermissionGroup> getPermissionGroups(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return personService.getPermissionGroups(searchParam, principal.getName(), pageable);
    }

    @PreAuthorize("hasAnyAuthority('DELETE_PERMISSION_GROUP','READ_SUPPLIER_PORTAL')")
    @DeleteMapping("/permissionGroup/{publicId}")
    @Operation(summary = "Delete permission group", description = "Delete permission group",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void deletePermissionGroup(@PathVariable(name = "publicId") UUID publicId,
                                      @Parameter(hidden = true) Principal principal) {

        personService.deletePermissionGroup(publicId, principal.getName());
    }

    @PreAuthorize("hasAnyAuthority('DELETE_PERMISSION','READ_SUPPLIER_PORTAL')")
    @DeleteMapping("/permissionGroup/{publicId}/permission/{permissionId}")
    @Operation(summary = "Delete permission from permission group", description = "Delete permission from permission group",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void deletePermissionFromPermissionGroup(@PathVariable(name = "publicId") UUID publicId,
                                                    @PathVariable(name = "permissionId") UUID permissionId,
                                                    @Parameter(hidden = true) Principal principal) {

        personService.deletePermissionFromPermissionGroup(publicId, permissionId, principal.getName());
    }

    @PreAuthorize("hasAuthority('CREATE_ROLE')")
    @JsonView(BaseView.RoleView.class)
    @PostMapping("/role")
    @Operation(summary = "Create role", description = "Create role",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "CREATE_ROLE_REQUEST", value = Examples.CREATE_ROLE_REQUEST),
    })))
    public Role createRole(@RequestBody @Valid RoleRequest roleRequest,
                           @Parameter(hidden = true) Principal principal) {

        return personService.createRole(roleRequest, principal.getName());
    }

    @PreAuthorize("hasAnyAuthority('VIEW_ROLE','READ_SUPPLIER_PORTAL')")
    @JsonView(BaseView.RoleView.class)
    @GetMapping("/roles")
    @Operation(summary = "Get all roles", description = "Get all roles",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<Role> getRoles(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 1000, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return personService.getRoles(searchParam, principal.getName(), pageable);
    }

    @PreAuthorize("hasAuthority('UPDATE_ROLE')")
    @PatchMapping("/role/{publicId}")
    @Operation(summary = "Update role", description = "Add permissions to role",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "UPDATE_ROLE_REQUEST", value = Examples.CREATE_ROLE_REQUEST),
    })))
    public void updateRole(@PathVariable(name = "publicId") UUID publicId,
                           @RequestBody @Valid UpdateRoleRequest updateRoleRequest,
                           @Parameter(hidden = true) Principal principal) {

        personService.updateRole(publicId, updateRoleRequest, principal.getName());
    }

    @PreAuthorize("hasAuthority('DELETE_ROLE')")
    @DeleteMapping("/role/{publicId}")
    @Operation(summary = "Delete role", description = "Delete role",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void deleteRole(@PathVariable(name = "publicId") UUID publicId,
                           @Parameter(hidden = true) Principal principal) {

        personService.deleteRole(publicId, principal.getName());
    }

    @PreAuthorize("hasAuthority('CREATE_PERSON')")
    @JsonView(BaseView.UserView.class)
    @PostMapping("/person")
    @Operation(summary = "Create person", description = "Create person",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Person createPerson(@RequestBody @Valid PersonRequest personRequest,
                               @Parameter(hidden = true) Principal principal) {

        return personService.createPerson(personRequest, principal.getName());
    }

    @PreAuthorize("hasAuthority('UPDATE_PERSON')")
    @PatchMapping("/person/{publicId}")
    @Operation(summary = "Update person", description = "Update person",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "UPDATE_PERSON_REQUEST", value = Examples.UPDATE_PERSON_REQUEST),
    })))
    public void updatePerson(@PathVariable(name = "publicId") UUID publicId,
                             @RequestBody @Valid UpdatePersonRequest updatePersonRequest,
                             @Parameter(hidden = true) Principal principal) {

        personService.updatePerson(publicId, updatePersonRequest, principal.getName());
    }

    //add or delete roles from person

    @PreAuthorize("hasAnyAuthority('UPDATE_PERSON','READ_SUPPLIER_PORTAL')")
    @PatchMapping("/person/{publicId}/roles")
    @Operation(summary = "Add roles to person", description = "Add roles to person",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "ADD_ROLES_TO_PERSON_REQUEST", value = Examples.ADD_ROLES_TO_PERSON_REQUEST),
    })))
    public void addRolesToPerson(@PathVariable(name = "publicId") UUID publicId,
                                 @RequestBody @Valid AddRolesToPersonRequest addRolesToPersonRequest,
                                 @Parameter(hidden = true) Principal principal) {

        personService.addRolesToPerson(publicId, addRolesToPersonRequest, principal.getName());
    }

    @PreAuthorize("hasAuthority('DELETE_PERSON')")
    @DeleteMapping("/person/{publicId}")
    @Operation(summary = "Delete person", description = "Delete person",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void deletePerson(@PathVariable(name = "publicId") UUID publicId,
                             @Parameter(hidden = true) Principal principal) {

        personService.deletePerson(publicId, principal.getName());
    }

    @PreAuthorize("hasAnyAuthority('VIEW_PERSON','READ_SUPPLIER_PORTAL')")
//    @JsonView(BaseView.UserView.class)
    @JsonView(BaseView.UserViewDetails.class)
    @GetMapping("/persons")
    @Operation(summary = "Get all persons", description = "Get all persons",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<Person> getPersons(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(name = "isFinanceServiceOfficer", required = false) Boolean isFinanceServiceOfficer,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return personService.getPersons(searchParam, principal.getName(), isFinanceServiceOfficer, pageable);
    }

    @JsonView(BaseView.Profile.class)
    @GetMapping("/person/profile")
    @Operation(summary = "Get person profile", description = "Get person profile",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Person getPersonProfile(@Parameter(hidden = true) Principal principal) {

        return personService.getPersonProfile(principal.getName());
    }

    // change password

    @PatchMapping("/person/changePassword")
    @Operation(summary = "Change password", description = "Change password",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "CHANGE_PASSWORD_REQUEST", value = Examples.CHANGE_PASSWORD_REQUEST),
    })))
    public void changePassword(@RequestBody @Valid ChangePasswordRequest changePasswordRequest,
                               @Parameter(hidden = true) Principal principal) {

        personService.changePassword(changePasswordRequest, principal.getName());
    }

    //profile reset

    @PostMapping("/person/resetUserName")
    @Operation(summary = "Reset user name", description = "Reset user name",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "RESET_USER_NAME_REQUEST", value = Examples.RESET_USER_NAME_REQUEST),
    })))
    public void resetUserName(@RequestBody @Valid ResetUserNameRequest resetUserNameRequest,
                              @Parameter(hidden = true) Principal principal) {

        personService.resetUserName(resetUserNameRequest, principal.getName());
    }

    // get all roles of a person

    // @PreAuthorize("hasAuthority('VIEW_PERSON')")
    @PreAuthorize("hasAnyAuthority('VIEW_PERSON','READ_SUPPLIER_PORTAL')")
    @JsonView(BaseView.RoleView.class)
    @GetMapping("/person/{publicId}/roles")
    @Operation(summary = "Get all roles of a person", description = "Get all roles of a person",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET_ROLES", value = Examples.FETCH_ROLES_OK)})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Page<Role> getRolesOfPerson(@PathVariable(name = "publicId") UUID publicId,
                                       @RequestParam(name = "searchParam", required = false) String searchParam,
                                       @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return personService.getRolesOfPerson(publicId, searchParam, pageable);
    }

    //supplier users

    @PreAuthorize("hasAuthority('READ_SUPPLIER_PORTTAL')")
    @JsonView(BaseView.UserView.class)
    @PostMapping("/createSupplierUser")
    @Operation(summary = "Create supplier user", description = "Create supplier user",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Person createSupplierUser(@RequestBody @Valid PersonRequest personRequest,
                                     @Parameter(hidden = true) Principal principal) {

        return personService.createSupplierUser(personRequest, principal.getName());
    }

    @PreAuthorize("hasAuthority('READ_SUPPLIER_PORTAL')")
    @JsonView(BaseView.UserView.class)
    @PostMapping("/supplierTeamUser")
    @Operation(summary = "Create supplier portal team user", description = "Create a supplier portal team user (main supplier only, max 4 accounts per supplier)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Person createSupplierTeamUser(@RequestBody @Valid PersonRequest personRequest,
                                         @Parameter(hidden = true) Principal principal) {

        return personService.createSupplierUser(personRequest, principal.getName());
    }

    @PreAuthorize("hasAuthority('READ_SUPPLIER_PORTAL')")
    @DeleteMapping("/supplierTeamUser/{publicId}")
    @Operation(summary = "Remove supplier portal team user", description = "Soft-delete a supplier portal team member (main supplier only)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void deleteSupplierTeamUser(@PathVariable(name = "publicId") UUID publicId,
                                       @Parameter(hidden = true) Principal principal) {

        personService.deleteSupplierTeamMember(publicId, principal.getName());
    }

    @PreAuthorize("hasAuthority('READ_SUPPLIER_PORTAL')")
    @JsonView(BaseView.UserView.class)
    @PatchMapping("/supplierTeamUser/{publicId}")
    @Operation(summary = "Update supplier portal team user", description = "Update a supplier portal team member (main supplier only; first and last name are not editable)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Person updateSupplierTeamUser(@PathVariable(name = "publicId") UUID publicId,
                                         @RequestBody @Valid UpdateSupplierTeamMemberRequest updateRequest,
                                         @Parameter(hidden = true) Principal principal) {

        return personService.updateSupplierTeamMember(publicId, updateRequest, principal.getName());
    }


    @JsonView(BaseView.ProfileFewDetails.class)
    @GetMapping("/personInRole")
    @Operation(summary = "Get persons in role", description = "Get persons in role",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET_ROLES", value = Examples.FETCH_ROLES_OK)})),})
    public List<Person> getPersonInRole(@RequestParam(name = "roleName") String roleName) {

        return personService.getPersonInRole(roleName);
    }

    // get all permissions from a role

    @Deprecated
    @JsonView(BaseView.PermissionGroupView.class)
    @GetMapping("/role/{publicId}/permissions")
    @Operation(summary = "Get all permissions of a role", description = "Get all permissions of a role",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "")})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Page<Permission> getPermissionsOfRole(@PathVariable(name = "publicId") UUID publicId,
                                                 @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return personService.getPermissionsOfRole(publicId, pageable);
    }


    @GetMapping("/{rolePublicId}/rolePermissions")
    public PermissionResponse getPermissionsOfRole(@PathVariable(name = "rolePublicId") UUID rolePublicId) {
        return personService.fetchRolePermissions(rolePublicId);
    }


    // set caller id

    @PatchMapping("/person/{publicId}/updateCallerId")
    @Operation(summary = "Update callerId", description = "Update callerId",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void updateCallerId(@PathVariable(name = "publicId") UUID publicId,
                               @RequestParam(name = "callerId") String callerId,
                               @Parameter(hidden = true) Principal principal) {
        personService.updateUserCallerId(publicId, callerId, principal.getName());

    }

    @JsonView(BaseView.RoleView.class)
    @PostMapping("/markUserFinanceServiceOfficer")
    @Operation(summary = "mark user as finance service officer", description = "mark user as finance service officer",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public void markUserFinanceServiceOfficer(@RequestParam(name = "publicIds") Set<UUID> publicIds,
                                              @RequestParam(name = "isFinanceServiceOfficer") Boolean markFinanceServiceOfficer,
                                              @Parameter(hidden = true) Principal principal) {

        personService.markUserFinanceServiceOfficer(publicIds, markFinanceServiceOfficer, principal.getName());
    }
}
