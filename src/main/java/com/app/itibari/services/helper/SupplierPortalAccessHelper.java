package com.app.itibari.services.helper;

import com.app.itibari.dataTypes.IdentificationType;
import com.app.itibari.models.entities.Permission;
import com.app.itibari.models.entities.PermissionGroup;
import com.app.itibari.models.entities.Person;
import com.app.itibari.models.entities.Role;
import com.app.itibari.models.entities.Suppliers;
import com.app.itibari.models.request.PersonRequest;
import com.app.itibari.models.request.UpdateSupplierTeamMemberRequest;
import com.app.itibari.repository.PermissionGroupRepository;
import com.app.itibari.repository.PermissionRepository;
import com.app.itibari.repository.PersonRepository;
import com.app.itibari.repository.RoleRepository;
import com.app.itibari.repository.SupplierRepository;
import com.app.itibari.configuration.JwtTokenAuthentication;
import com.app.itibari.services.ItibariSmsService;
import com.app.itibari.services.supplierportal.SupplierPortalUserLimits;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class SupplierPortalAccessHelper {

    private static final List<String> SUPPLIER_PORTAL_VIEW_PERMISSIONS = List.of(
            "READ_SUPPLIER_PORTAL",
            "VIEW_SP_DASHBOARD",
            "VIEW_SP_CUSTOMER_LIST"
    );

    private final PersonRepository personRepository;

    private final ItibariSmsService itibariSmsService;

    private final PasswordEncoder passwordEncoder;

    private final PermissionGroupRepository permissionGroupRepository;

    private final PermissionRepository permissionRepository;

    private final RoleRepository roleRepository;

    private final SupplierRepository supplierRepository;

    private final JwtTokenAuthentication jwtTokenAuthentication;

    public SupplierPortalAccessHelper(PersonRepository personRepository,
                                      ItibariSmsService itibariSmsService,
                                      PasswordEncoder passwordEncoder,
                                      PermissionGroupRepository permissionGroupRepository,
                                      PermissionRepository permissionRepository,
                                      RoleRepository roleRepository,
                                      SupplierRepository supplierRepository,
                                      JwtTokenAuthentication jwtTokenAuthentication) {

        this.personRepository = personRepository;
        this.itibariSmsService = itibariSmsService;
        this.passwordEncoder = passwordEncoder;
        this.permissionGroupRepository = permissionGroupRepository;
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
        this.supplierRepository = supplierRepository;
        this.jwtTokenAuthentication = jwtTokenAuthentication;
    }

    public void grantMainSupplierPortalAccess(Suppliers supplier, Person person) {

        String supplierPublicId = supplier.getPublicId().toString();

        if (personRepository.existsMainSupplierBySupplierPublicId(supplierPublicId)) {
            supplier.setPortalAccessGranted(true);
            supplierRepository.save(supplier);
            return;
        }

        long existingAccounts = personRepository.countBySupplierPublicIdAndHasSupplierAccountTrueAndDeletedIsFalse(
                supplierPublicId);
        if (existingAccounts >= SupplierPortalUserLimits.MAX_PORTAL_USERS_PER_SUPPLIER) {
            throw new BadRequestRestApiException()
                    .developerMessage("Supplier already has the maximum number of portal users")
                    .userMessage("Sorry. This supplier already has the maximum number of portal users.");
        }

        assertEmailAvailable(supplier.getEmail());

        Person supplierContact = new Person();
        supplierContact.setFirstName(supplier.getSupplierName());
        supplierContact.setLastName("");
        supplierContact.setMainSupplier(true);
        supplierContact.setHasSupplierAccount(true);
        supplierContact.setSupplierPublicId(supplier.getPublicId().toString());
        supplierContact.setEmail(supplier.getEmail());
        supplierContact.setPhoneNumber(supplier.getPhoneNumber());
        supplierContact.setPassword(getPassword(supplier.getEmail(), supplier.getPhoneNumber()));
        supplierContact.setAddress(supplier.getSupplierAddress());
        supplierContact.setCity(supplier.getPostalAddress());
        supplierContact.setState(supplier.getSupplierAddress());
        supplierContact.setIdentificationType(IdentificationType.TIN);
        supplierContact.setIdentificationNumber(supplier.getTinNumber());
        supplierContact.setCreatedBy(person);
        supplierContact.setCreationTime(java.time.LocalDateTime.now());
        supplierContact.addRole(getSupplierRole(supplier));

        personRepository.save(supplierContact);

        supplier.setPortalAccessGranted(true);
        supplierRepository.save(supplier);
    }

    private Role getSupplierRole(Suppliers supplier) {

        PermissionGroup permissionGroup = getSupplierPermissionGroup(supplier);
        List<Permission> permissions = ensureSupplierPortalPermissions(permissionGroup);
        Role role = createRole("ROLE_SUPPLIER_PORTAL + " + supplier.getSupplierName());
        permissions.forEach(role::addPermission);
        role.setSupplierRole(true);
        role.setSupplierPublicId(supplier.getPublicId().toString());
        return roleRepository.save(role);
    }

    /**
     * Ensures the supplier's shared portal role includes all view permissions required by the frontend routes.
     * Safe to call on every supplier login to repair older roles that only had READ_SUPPLIER_PORTAL.
     */
    public void ensureSupplierPortalRolePermissions(String supplierPublicId) {
        Optional<Role> roleOptional =
                roleRepository.findFirstBySupplierPublicIdAndSupplierRoleTrueAndDeletedIsFalse(supplierPublicId);
        if (roleOptional.isEmpty()) {
            return;
        }

        Optional<Suppliers> supplierOptional =
                supplierRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(supplierPublicId));
        if (supplierOptional.isEmpty()) {
            return;
        }

        Role role = roleOptional.get();
        PermissionGroup permissionGroup = getSupplierPermissionGroup(supplierOptional.get());
        List<Permission> permissions = ensureSupplierPortalPermissions(permissionGroup);

        boolean changed = false;
        for (Permission permission : permissions) {
            boolean alreadyAssigned = role.getPermissions().stream()
                    .anyMatch(existing -> existing.getPublicId().equals(permission.getPublicId()));
            if (!alreadyAssigned) {
                role.addPermission(permission);
                changed = true;
            }
        }

        if (changed) {
            roleRepository.save(role);
        }
    }

    private Role createRole(String roleName) {

        Optional<Role> roleOptional = roleRepository.findByRoleNameAndDeletedIsFalse(roleName);

        if (roleOptional.isEmpty()) {
            Role role = new Role();
            role.setRoleName(roleName);
            return roleRepository.save(role);
        }
        return roleOptional.get();
    }

    private PermissionGroup getSupplierPermissionGroup(Suppliers supplier) {

        Optional<PermissionGroup> optionalPermissionGroup = permissionGroupRepository.findByGroupNameIgnoreCaseAndSupplierPublicIdAndDeletedIsFalse(
                supplier.getSupplierName(),
                supplier.getPublicId().toString());

        if (optionalPermissionGroup.isEmpty()) {
            PermissionGroup permissionGroup = new PermissionGroup();
            permissionGroup.setGroupName(supplier.getSupplierName());
            permissionGroup.setSupplierRole(true);
            permissionGroup.setSupplierPublicId(supplier.getPublicId().toString());
            return permissionGroupRepository.save(permissionGroup);
        }
        return optionalPermissionGroup.get();
    }

    private List<Permission> ensureSupplierPortalPermissions(PermissionGroup permissionGroup) {
        List<Permission> permissions = new ArrayList<>();
        for (String permissionName : SUPPLIER_PORTAL_VIEW_PERMISSIONS) {
            permissions.add(ensurePermission(permissionName, permissionGroup));
        }
        return permissions;
    }

    private Permission ensurePermission(String permissionName, PermissionGroup permissionGroup) {

        Optional<Permission> optionalPermission =
                permissionRepository.findByNameIgnoreCaseAndDeletedIsFalse(permissionName);

        if (optionalPermission.isEmpty()) {
            Permission permission = new Permission();
            permission.setName(permissionName);
            permission.setPermissionGroup(permissionGroup);
            permission.setSupplierRole(true);
            Permission saved = permissionRepository.save(permission);

            permissionGroup.addPermission(saved);
            permissionGroupRepository.save(permissionGroup);
            return saved;
        }
        return optionalPermission.get();
    }

    private String getPassword(String email, String phoneNumber) {
//        String randomPassword = RandomStringUtils.randomAlphanumeric(8);
        String randomPassword = "password";
        itibariSmsService.send(phoneNumber, String.format("Your itibari Supplier Portal login password is %s.", randomPassword));

        return passwordEncoder.encode(randomPassword);
    }

    public Person createSupplierUser(PersonRequest personRequest, String name) {
        Person mainSupplier = requireMainSupplierAccount(name);
        assertTeamMemberSlotAvailable(mainSupplier.getSupplierPublicId());
        assertNotDuplicateTeamIdentity(personRequest, mainSupplier);
        assertEmailAvailable(personRequest.getEmail());

        Role supplierRole = requireSupplierRole(mainSupplier.getSupplierPublicId());

        Person newPerson = buildSupplierTeamMember(personRequest, mainSupplier);
        newPerson.addRole(supplierRole);
        return personRepository.save(newPerson);
    }

    public void deleteSupplierTeamMember(UUID teamMemberPublicId, String loggedInUserEmail) {
        Person mainSupplier = requireMainSupplierAccount(loggedInUserEmail);
        Person personToDelete = requireTeamMemberForManagement(teamMemberPublicId, mainSupplier);

        if (mainSupplier.getPublicId().equals(personToDelete.getPublicId())) {
            throw new BadRequestRestApiException()
                    .developerMessage("User cannot delete their own account")
                    .userMessage("Sorry. You cannot remove your own account.");
        }

        personToDelete.setDeleted(true);
        personToDelete.setUpdatedBy(mainSupplier);
        personToDelete.setUpdateDate(LocalDateTime.now());
        personRepository.save(personToDelete);
    }

    public Person updateSupplierTeamMember(UUID teamMemberPublicId,
                                         UpdateSupplierTeamMemberRequest updateRequest,
                                         String loggedInUserEmail) {
        Person mainSupplier = requireMainSupplierAccount(loggedInUserEmail);
        Person teamMember = requireTeamMemberForManagement(teamMemberPublicId, mainSupplier);

        if (!hasUpdatableField(updateRequest)) {
            throw new BadRequestRestApiException()
                    .developerMessage("No team member fields provided for update")
                    .userMessage("Sorry. Provide at least one field to update.");
        }

        String previousEmail = teamMember.getEmail();

        if (updateRequest.getEmail() != null) {
            assertEmailAvailableForUpdate(updateRequest.getEmail(), teamMember, mainSupplier);
            teamMember.setEmail(updateRequest.getEmail());
        }

        if (updateRequest.getPhoneNumber() != null) {
            assertPhoneAvailableForUpdate(updateRequest.getPhoneNumber(), teamMember, mainSupplier);
            teamMember.setPhoneNumber(updateRequest.getPhoneNumber());
        }

        if (updateRequest.getAddress() != null) {
            teamMember.setAddress(updateRequest.getAddress());
        }
        if (updateRequest.getCity() != null) {
            teamMember.setCity(updateRequest.getCity());
        }
        if (updateRequest.getState() != null) {
            teamMember.setState(updateRequest.getState());
        }
        if (updateRequest.getIdentificationType() != null) {
            teamMember.setIdentificationType(updateRequest.getIdentificationType());
        }
        if (updateRequest.getIdentificationNumber() != null) {
            teamMember.setIdentificationNumber(updateRequest.getIdentificationNumber());
        }

        teamMember.setUpdatedBy(mainSupplier);
        teamMember.setUpdateDate(LocalDateTime.now());
        Person saved = personRepository.save(teamMember);

        if (updateRequest.getEmail() != null
                && previousEmail != null
                && !previousEmail.equalsIgnoreCase(updateRequest.getEmail())) {
            jwtTokenAuthentication.logout(previousEmail);
        }

        return saved;
    }

    private Person requireTeamMemberForManagement(UUID teamMemberPublicId, Person mainSupplier) {
        Person teamMember = personRepository.findByPublicIdAndDeletedIsFalse(teamMemberPublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Person with id " + teamMemberPublicId + " does not exist")
                        .userMessage("Sorry. User not found."));

        if (!Boolean.TRUE.equals(teamMember.getHasSupplierAccount())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Person is not a supplier portal user")
                    .userMessage("Sorry. User is not a supplier portal team member.");
        }

        if (!mainSupplier.getSupplierPublicId().equals(teamMember.getSupplierPublicId())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Team member belongs to a different supplier")
                    .userMessage("Sorry. You cannot manage users from another supplier.");
        }

        if (Boolean.TRUE.equals(teamMember.getMainSupplier())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Main supplier account cannot be managed as a team member")
                    .userMessage("Sorry. The main supplier account cannot be updated here.");
        }

        return teamMember;
    }

    private boolean hasUpdatableField(UpdateSupplierTeamMemberRequest updateRequest) {
        return updateRequest.getEmail() != null
                || updateRequest.getPhoneNumber() != null
                || updateRequest.getAddress() != null
                || updateRequest.getCity() != null
                || updateRequest.getState() != null
                || updateRequest.getIdentificationType() != null
                || updateRequest.getIdentificationNumber() != null;
    }

    private void assertEmailAvailableForUpdate(String email, Person teamMember, Person mainSupplier) {
        if (email.equalsIgnoreCase(mainSupplier.getEmail())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Team member email matches main supplier email")
                    .userMessage("Sorry. Team member email cannot match the main supplier email.");
        }

        if (email.equalsIgnoreCase(teamMember.getEmail())) {
            return;
        }

        if (personRepository.findByEmailAndDeletedIsFalse(email).isPresent()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Person with email " + email + " already exists")
                    .userMessage("Person with email " + email + " already exists");
        }
    }

    private void assertPhoneAvailableForUpdate(String phoneNumber, Person teamMember, Person mainSupplier) {
        if (phoneNumber.equals(teamMember.getPhoneNumber())) {
            return;
        }

        if (personRepository.existsBySupplierPublicIdAndPhoneNumberAndHasSupplierAccountTrueAndDeletedIsFalseAndPublicIdNot(
                mainSupplier.getSupplierPublicId(), phoneNumber, teamMember.getPublicId())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Phone number already used by a supplier portal user")
                    .userMessage("Sorry. A team member with this phone number already exists for this supplier.");
        }
    }

    private Person requireMainSupplierAccount(String email) {
        Person person = personRepository.findByEmailAndDeletedIsFalse(email)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Logged in user not found")
                        .userMessage("Logged in user not found"));

        if (person.getSupplierPublicId() == null || !Boolean.TRUE.equals(person.getHasSupplierAccount())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Logged in user is not a supplier")
                    .userMessage("Logged in user is not a supplier");
        }

        if (!Boolean.TRUE.equals(person.getMainSupplier())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Only the main supplier can manage team users")
                    .userMessage("Sorry. Only the main supplier account can manage team users.");
        }

        return person;
    }

    private void assertSeatAvailable(String supplierPublicId) {
        long activeAccounts = personRepository.countBySupplierPublicIdAndHasSupplierAccountTrueAndDeletedIsFalse(
                supplierPublicId);
        if (activeAccounts >= SupplierPortalUserLimits.MAX_PORTAL_USERS_PER_SUPPLIER) {
            throw new BadRequestRestApiException()
                    .developerMessage("Supplier portal user limit reached for supplier " + supplierPublicId)
                    .userMessage("Sorry. This supplier already has the maximum of "
                            + SupplierPortalUserLimits.MAX_PORTAL_USERS_PER_SUPPLIER + " portal users.");
        }
    }

    private void assertTeamMemberSlotAvailable(String supplierPublicId) {
        assertSeatAvailable(supplierPublicId);

        long teamMembers = personRepository.countTeamMembersBySupplierPublicId(supplierPublicId);
        if (teamMembers >= SupplierPortalUserLimits.MAX_TEAM_MEMBERS_PER_SUPPLIER) {
            throw new BadRequestRestApiException()
                    .developerMessage("Supplier portal team member limit reached for supplier " + supplierPublicId)
                    .userMessage("Sorry. This supplier already has the maximum of "
                            + SupplierPortalUserLimits.MAX_TEAM_MEMBERS_PER_SUPPLIER + " team members.");
        }
    }

    private void assertNotDuplicateTeamIdentity(PersonRequest personRequest, Person mainSupplier) {
        if (personRequest.getEmail() != null
                && personRequest.getEmail().equalsIgnoreCase(mainSupplier.getEmail())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Team member email matches main supplier email")
                    .userMessage("Sorry. You cannot add the main supplier account again as a team member.");
        }

        if (personRequest.getPhoneNumber() != null
                && personRepository.existsBySupplierPublicIdAndPhoneNumberAndHasSupplierAccountTrueAndDeletedIsFalse(
                mainSupplier.getSupplierPublicId(), personRequest.getPhoneNumber())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Phone number already used by a supplier portal user")
                    .userMessage("Sorry. A team member with this phone number already exists for this supplier.");
        }
    }

    private void assertEmailAvailable(String email) {
        if (personRepository.findByEmailAndDeletedIsFalse(email).isPresent()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Person with email " + email + " already exists")
                    .userMessage("Person with email " + email + " already exists");
        }
    }

    private Role requireSupplierRole(String supplierPublicId) {
        return roleRepository.findFirstBySupplierPublicIdAndSupplierRoleTrueAndDeletedIsFalse(supplierPublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Supplier portal role not found for supplier " + supplierPublicId)
                        .userMessage("Sorry. Supplier portal access is not configured for this supplier."));
    }

    private Person buildSupplierTeamMember(PersonRequest personRequest, Person mainSupplier) {
        Person newPerson = new Person();
        newPerson.setEmail(personRequest.getEmail());
        newPerson.setFirstName(personRequest.getFirstName());
        newPerson.setLastName(personRequest.getLastName());
        newPerson.setAddress(personRequest.getAddress());
        newPerson.setPhoneNumber(personRequest.getPhoneNumber());
        newPerson.setCity(personRequest.getCity());
        newPerson.setState(personRequest.getState());
        newPerson.setHasSupplierAccount(true);
        newPerson.setSupplierPublicId(mainSupplier.getSupplierPublicId());
        newPerson.setCreatedBy(mainSupplier);
        newPerson.setMainSupplier(false);
        newPerson.setCreationTime(LocalDateTime.now());
        newPerson.setPassword(getPassword(personRequest.getEmail(), personRequest.getPhoneNumber()));
        newPerson.setIdentificationType(personRequest.getIdentificationType());
        newPerson.setIdentificationNumber(personRequest.getIdentificationNumber());
        return newPerson;
    }
}
