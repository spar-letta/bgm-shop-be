package com.app.itibari.services;

import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.AddRolesToPersonRequest;
import com.app.itibari.models.request.ChangePasswordRequest;
import com.app.itibari.models.request.PermissionGroupRequest;
import com.app.itibari.models.request.PermissionRequest;
import com.app.itibari.models.request.PersonRequest;
import com.app.itibari.models.request.ResetUserNameRequest;
import com.app.itibari.models.request.RoleRequest;
import com.app.itibari.models.request.UpdatePersonRequest;
import com.app.itibari.models.request.UpdateSupplierTeamMemberRequest;
import com.app.itibari.models.request.UpdateRoleRequest;
import com.app.itibari.models.response.PermissionResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface PersonService {

    void createPermissionGroup(@Valid PermissionGroupRequest permissionGroupRequest, String loggedInUser);

    void updatePermissionGroup(UUID publicId, @Valid PermissionRequest permissionRequest, String loggedInUser);

    PermissionGroup getPermissionGroup(UUID publicId);

    Page<PermissionGroup> getPermissionGroups(String searchParam, String name, Pageable pageable);

    void deletePermissionGroup(UUID publicId, String loggedInUser);

    void deletePermissionFromPermissionGroup(UUID publicId, UUID permissionId, String loggedInUser);

    Role createRole(@Valid RoleRequest roleRequest, String loggedInUser);

    Page<Role> getRoles(String searchParam, String loggedUser, Pageable pageable);

    void updateRole(UUID publicId, @Valid UpdateRoleRequest updateRoleRequest, String loggedInUser);

    void deleteRole(UUID publicId, String loggedInUser);

    Person createPerson(@Valid PersonRequest personRequest, String loggedInUser);

    void updatePerson(UUID publicId, @Valid UpdatePersonRequest updatePersonRequest, String loggedInUser);

    void addRolesToPerson(UUID publicId, @Valid AddRolesToPersonRequest addRolesToPersonRequest, String loggedInUser);

    void deletePerson(UUID publicId, String loggedInUser);

    Page<Person> getPersons(String searchParam, String loggedInUser, Boolean isFinanceServiceOfficer, Pageable pageable);

    Person getPersonProfile(String name);

    void changePassword(@Valid ChangePasswordRequest changePasswordRequest, String name);

    void resetUserName(@Valid ResetUserNameRequest resetUserNameRequest, String name);

    Person findPersonByRole(String role);

    Page<Role> getRolesOfPerson(UUID publicId, String searchParam, Pageable pageable);

    Page<Notification> getLoggedInPersonNotifications(String searchParam, Boolean isRead, String name, Pageable pageable);

    void markNotificationAsRead(String notificationPublicId, String name);

    Person createSupplierUser(@Valid PersonRequest personRequest, String name);

    void deleteSupplierTeamMember(UUID teamMemberPublicId, String loggedInUser);

    Person updateSupplierTeamMember(UUID teamMemberPublicId,
                                    @Valid UpdateSupplierTeamMemberRequest updateRequest,
                                    String loggedInUser);

    List<Person> getPersonInRole(String roleName);

    Page<Permission> getPermissionsOfRole(UUID publicId, Pageable pageable);

    PermissionResponse fetchRolePermissions(UUID rolePublicId);

    void updateUserCallerId(UUID publicId, String callerId, String name);

    void markUserFinanceServiceOfficer(Set<UUID> publicIds, Boolean markFinanceServiceOfficer, String name);

}
