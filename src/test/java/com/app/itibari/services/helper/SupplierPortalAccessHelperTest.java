package com.app.itibari.services.helper;

import com.app.itibari.dataTypes.IdentificationType;
import com.app.itibari.models.entities.Permission;
import com.app.itibari.models.entities.PermissionGroup;
import com.app.itibari.models.entities.Person;
import com.app.itibari.models.entities.Role;
import com.app.itibari.models.entities.Suppliers;
import com.app.itibari.configuration.JwtTokenAuthentication;
import com.app.itibari.models.request.PersonRequest;
import com.app.itibari.models.request.UpdateSupplierTeamMemberRequest;
import com.app.itibari.repository.PermissionGroupRepository;
import com.app.itibari.repository.PermissionRepository;
import com.app.itibari.repository.PersonRepository;
import com.app.itibari.repository.RoleRepository;
import com.app.itibari.repository.SupplierRepository;
import com.app.itibari.services.ItibariSmsService;
import com.app.itibari.services.supplierportal.SupplierPortalUserLimits;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SupplierPortalAccessHelperTest {

    private static final String SUPPLIER_PUBLIC_ID = "30577548-e687-4115-9d51-3a33a363aa56";
    private static final String MAIN_EMAIL = "main@supplier.co.tz";

    private PersonRepository personRepository;
    private RoleRepository roleRepository;
    private PasswordEncoder passwordEncoder;
    private ItibariSmsService itibariSmsService;
    private JwtTokenAuthentication jwtTokenAuthentication;
    private PermissionGroupRepository permissionGroupRepository;
    private PermissionRepository permissionRepository;
    private SupplierRepository supplierRepository;
    private SupplierPortalAccessHelper helper;

    @BeforeEach
    void setUp() {
        personRepository = mock(PersonRepository.class);
        roleRepository = mock(RoleRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        itibariSmsService = mock(ItibariSmsService.class);
        jwtTokenAuthentication = mock(JwtTokenAuthentication.class);
        permissionGroupRepository = mock(PermissionGroupRepository.class);
        permissionRepository = mock(PermissionRepository.class);
        supplierRepository = mock(SupplierRepository.class);
        helper = new SupplierPortalAccessHelper(
                personRepository,
                itibariSmsService,
                passwordEncoder,
                permissionGroupRepository,
                permissionRepository,
                roleRepository,
                supplierRepository,
                jwtTokenAuthentication);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
    }

    @Test
    void createSupplierUser_assignsRoleAndPersistsTeamMember() {
        Person mainSupplier = mainSupplierPerson();
        Role supplierRole = supplierRole();
        PersonRequest request = teamMemberRequest("member@supplier.co.tz");

        when(personRepository.findByEmailAndDeletedIsFalse(MAIN_EMAIL)).thenReturn(Optional.of(mainSupplier));
        when(personRepository.countBySupplierPublicIdAndHasSupplierAccountTrueAndDeletedIsFalse(SUPPLIER_PUBLIC_ID))
                .thenReturn(1L);
        when(personRepository.countTeamMembersBySupplierPublicId(SUPPLIER_PUBLIC_ID)).thenReturn(0L);
        when(personRepository.findByEmailAndDeletedIsFalse(request.getEmail())).thenReturn(Optional.empty());
        when(roleRepository.findFirstBySupplierPublicIdAndSupplierRoleTrueAndDeletedIsFalse(SUPPLIER_PUBLIC_ID))
                .thenReturn(Optional.of(supplierRole));
        when(personRepository.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Person created = helper.createSupplierUser(request, MAIN_EMAIL);

        ArgumentCaptor<Person> captor = ArgumentCaptor.forClass(Person.class);
        verify(personRepository).save(captor.capture());
        Person saved = captor.getValue();

        assertEquals("member@supplier.co.tz", created.getEmail());
        assertFalse(saved.getMainSupplier());
        assertTrue(saved.getHasSupplierAccount());
        assertEquals(SUPPLIER_PUBLIC_ID, saved.getSupplierPublicId());
        assertEquals(1, saved.getRoles().size());
        assertEquals(supplierRole, saved.getRoles().get(0));
    }

    @Test
    void ensureSupplierPortalRolePermissions_addsMissingDashboardAndCustomerPermissions() {
        Role role = supplierRole();
        role.setPermissions(new java.util.ArrayList<>());
        Permission readPortal = permission("READ_SUPPLIER_PORTAL");
        Permission dashboard = permission("VIEW_SP_DASHBOARD");
        Permission customerList = permission("VIEW_SP_CUSTOMER_LIST");
        role.getPermissions().add(readPortal);

        Suppliers supplier = new Suppliers();
        supplier.setPublicId(UUID.fromString(SUPPLIER_PUBLIC_ID));
        supplier.setSupplierName("Test Supplier");

        PermissionGroup permissionGroup = new PermissionGroup();
        permissionGroup.setGroupName("Test Supplier");

        when(roleRepository.findFirstBySupplierPublicIdAndSupplierRoleTrueAndDeletedIsFalse(SUPPLIER_PUBLIC_ID))
                .thenReturn(Optional.of(role));
        when(supplierRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(SUPPLIER_PUBLIC_ID)))
                .thenReturn(Optional.of(supplier));
        when(permissionGroupRepository.findByGroupNameIgnoreCaseAndSupplierPublicIdAndDeletedIsFalse(
                supplier.getSupplierName(), SUPPLIER_PUBLIC_ID)).thenReturn(Optional.of(permissionGroup));
        when(permissionRepository.findByNameIgnoreCaseAndDeletedIsFalse("READ_SUPPLIER_PORTAL"))
                .thenReturn(Optional.of(readPortal));
        when(permissionRepository.findByNameIgnoreCaseAndDeletedIsFalse("VIEW_SP_DASHBOARD"))
                .thenReturn(Optional.of(dashboard));
        when(permissionRepository.findByNameIgnoreCaseAndDeletedIsFalse("VIEW_SP_CUSTOMER_LIST"))
                .thenReturn(Optional.of(customerList));
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        helper.ensureSupplierPortalRolePermissions(SUPPLIER_PUBLIC_ID);

        ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository).save(captor.capture());
        assertEquals(3, captor.getValue().getPermissions().size());
    }

    @Test
    void createSupplierUser_rejectsWhenSeatLimitReached() {
        Person mainSupplier = mainSupplierPerson();
        PersonRequest request = teamMemberRequest("member@supplier.co.tz");

        when(personRepository.findByEmailAndDeletedIsFalse(MAIN_EMAIL)).thenReturn(Optional.of(mainSupplier));
        when(personRepository.countBySupplierPublicIdAndHasSupplierAccountTrueAndDeletedIsFalse(SUPPLIER_PUBLIC_ID))
                .thenReturn((long) SupplierPortalUserLimits.MAX_PORTAL_USERS_PER_SUPPLIER);

        BadRequestRestApiException ex = assertThrows(
                BadRequestRestApiException.class,
                () -> helper.createSupplierUser(request, MAIN_EMAIL));

        assertTrue(ex.getRestApiError().getUserMessage()
                .contains(String.valueOf(SupplierPortalUserLimits.MAX_PORTAL_USERS_PER_SUPPLIER)));
        verify(personRepository, never()).save(any());
    }

    @Test
    void createSupplierUser_rejectsWhenTeamMemberLimitReached() {
        Person mainSupplier = mainSupplierPerson();
        PersonRequest request = teamMemberRequest("member@supplier.co.tz");

        when(personRepository.findByEmailAndDeletedIsFalse(MAIN_EMAIL)).thenReturn(Optional.of(mainSupplier));
        when(personRepository.countBySupplierPublicIdAndHasSupplierAccountTrueAndDeletedIsFalse(SUPPLIER_PUBLIC_ID))
                .thenReturn(1L);
        when(personRepository.countTeamMembersBySupplierPublicId(SUPPLIER_PUBLIC_ID))
                .thenReturn((long) SupplierPortalUserLimits.MAX_TEAM_MEMBERS_PER_SUPPLIER);

        BadRequestRestApiException ex = assertThrows(
                BadRequestRestApiException.class,
                () -> helper.createSupplierUser(request, MAIN_EMAIL));

        assertTrue(ex.getRestApiError().getUserMessage().contains("team members"));
        verify(personRepository, never()).save(any());
    }

    @Test
    void createSupplierUser_rejectsDuplicateMainSupplierEmail() {
        Person mainSupplier = mainSupplierPerson();
        PersonRequest request = teamMemberRequest(MAIN_EMAIL);

        when(personRepository.findByEmailAndDeletedIsFalse(MAIN_EMAIL)).thenReturn(Optional.of(mainSupplier));
        when(personRepository.countBySupplierPublicIdAndHasSupplierAccountTrueAndDeletedIsFalse(SUPPLIER_PUBLIC_ID))
                .thenReturn(1L);
        when(personRepository.countTeamMembersBySupplierPublicId(SUPPLIER_PUBLIC_ID)).thenReturn(0L);

        BadRequestRestApiException ex = assertThrows(
                BadRequestRestApiException.class,
                () -> helper.createSupplierUser(request, MAIN_EMAIL));

        assertTrue(ex.getRestApiError().getUserMessage().contains("main supplier"));
        verify(personRepository, never()).save(any());
    }

    @Test
    void createSupplierUser_rejectsDuplicatePhoneNumber() {
        Person mainSupplier = mainSupplierPerson();
        PersonRequest request = teamMemberRequest("member@supplier.co.tz");
        request.setPhoneNumber("255700000001");

        when(personRepository.findByEmailAndDeletedIsFalse(MAIN_EMAIL)).thenReturn(Optional.of(mainSupplier));
        when(personRepository.countBySupplierPublicIdAndHasSupplierAccountTrueAndDeletedIsFalse(SUPPLIER_PUBLIC_ID))
                .thenReturn(1L);
        when(personRepository.countTeamMembersBySupplierPublicId(SUPPLIER_PUBLIC_ID)).thenReturn(0L);
        when(personRepository.findByEmailAndDeletedIsFalse(request.getEmail())).thenReturn(Optional.empty());
        when(personRepository.existsBySupplierPublicIdAndPhoneNumberAndHasSupplierAccountTrueAndDeletedIsFalse(
                SUPPLIER_PUBLIC_ID, request.getPhoneNumber())).thenReturn(true);

        BadRequestRestApiException ex = assertThrows(
                BadRequestRestApiException.class,
                () -> helper.createSupplierUser(request, MAIN_EMAIL));

        assertTrue(ex.getRestApiError().getUserMessage().contains("phone number"));
        verify(personRepository, never()).save(any());
    }

    @Test
    void createSupplierUser_rejectsNonMainSupplier() {
        Person subUser = mainSupplierPerson();
        subUser.setMainSupplier(false);

        when(personRepository.findByEmailAndDeletedIsFalse(MAIN_EMAIL)).thenReturn(Optional.of(subUser));

        BadRequestRestApiException ex = assertThrows(
                BadRequestRestApiException.class,
                () -> helper.createSupplierUser(teamMemberRequest("member@supplier.co.tz"), MAIN_EMAIL));

        assertTrue(ex.getRestApiError().getUserMessage().contains("main supplier"));
        verify(personRepository, never()).save(any());
    }

    @Test
    void deleteSupplierTeamMember_softDeletesSubUserAndFreesSeat() {
        Person mainSupplier = mainSupplierPerson();
        Person teamMember = teamMemberPerson();

        when(personRepository.findByEmailAndDeletedIsFalse(MAIN_EMAIL)).thenReturn(Optional.of(mainSupplier));
        when(personRepository.findByPublicIdAndDeletedIsFalse(teamMember.getPublicId()))
                .thenReturn(Optional.of(teamMember));
        when(personRepository.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));

        helper.deleteSupplierTeamMember(teamMember.getPublicId(), MAIN_EMAIL);

        ArgumentCaptor<Person> captor = ArgumentCaptor.forClass(Person.class);
        verify(personRepository).save(captor.capture());
        assertTrue(captor.getValue().getDeleted());
    }

    @Test
    void deleteSupplierTeamMember_rejectsDeletingMainSupplierAccount() {
        Person mainSupplier = mainSupplierPerson();

        when(personRepository.findByEmailAndDeletedIsFalse(MAIN_EMAIL)).thenReturn(Optional.of(mainSupplier));
        when(personRepository.findByPublicIdAndDeletedIsFalse(mainSupplier.getPublicId()))
                .thenReturn(Optional.of(mainSupplier));

        BadRequestRestApiException ex = assertThrows(
                BadRequestRestApiException.class,
                () -> helper.deleteSupplierTeamMember(mainSupplier.getPublicId(), MAIN_EMAIL));

        assertTrue(ex.getRestApiError().getUserMessage().contains("main supplier"));
        verify(personRepository, never()).save(any());
    }

    @Test
    void updateSupplierTeamMember_updatesEditableFieldsAndKeepsName() {
        Person mainSupplier = mainSupplierPerson();
        Person teamMember = teamMemberPerson();
        teamMember.setFirstName("Team");
        teamMember.setLastName("Member");
        teamMember.setPhoneNumber("255700000001");

        UpdateSupplierTeamMemberRequest request = new UpdateSupplierTeamMemberRequest();
        request.setEmail("updated@supplier.co.tz");
        request.setPhoneNumber("255700000002");
        request.setAddress("New address");
        request.setCity("Dar");
        request.setState("Dar");
        request.setIdentificationType(IdentificationType.TIN);
        request.setIdentificationNumber("999888777");

        when(personRepository.findByEmailAndDeletedIsFalse(MAIN_EMAIL)).thenReturn(Optional.of(mainSupplier));
        when(personRepository.findByPublicIdAndDeletedIsFalse(teamMember.getPublicId()))
                .thenReturn(Optional.of(teamMember));
        when(personRepository.findByEmailAndDeletedIsFalse(request.getEmail())).thenReturn(Optional.empty());
        when(personRepository.existsBySupplierPublicIdAndPhoneNumberAndHasSupplierAccountTrueAndDeletedIsFalseAndPublicIdNot(
                SUPPLIER_PUBLIC_ID, request.getPhoneNumber(), teamMember.getPublicId())).thenReturn(false);
        when(personRepository.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Person updated = helper.updateSupplierTeamMember(teamMember.getPublicId(), request, MAIN_EMAIL);

        assertEquals("updated@supplier.co.tz", updated.getEmail());
        assertEquals("255700000002", updated.getPhoneNumber());
        assertEquals("Team", updated.getFirstName());
        assertEquals("Member", updated.getLastName());
        verify(jwtTokenAuthentication).logout("member@supplier.co.tz");
    }

    @Test
    void updateSupplierTeamMember_rejectsEmptyPayload() {
        Person mainSupplier = mainSupplierPerson();
        Person teamMember = teamMemberPerson();

        when(personRepository.findByEmailAndDeletedIsFalse(MAIN_EMAIL)).thenReturn(Optional.of(mainSupplier));
        when(personRepository.findByPublicIdAndDeletedIsFalse(teamMember.getPublicId()))
                .thenReturn(Optional.of(teamMember));

        BadRequestRestApiException ex = assertThrows(
                BadRequestRestApiException.class,
                () -> helper.updateSupplierTeamMember(teamMember.getPublicId(), new UpdateSupplierTeamMemberRequest(), MAIN_EMAIL));

        assertTrue(ex.getRestApiError().getUserMessage().contains("field to update"));
        verify(personRepository, never()).save(any());
    }

    @Test
    void updateSupplierTeamMember_rejectsNonMainSupplier() {
        Person subUser = mainSupplierPerson();
        subUser.setMainSupplier(false);
        Person teamMember = teamMemberPerson();
        UpdateSupplierTeamMemberRequest request = new UpdateSupplierTeamMemberRequest();
        request.setPhoneNumber("255700000002");

        when(personRepository.findByEmailAndDeletedIsFalse(MAIN_EMAIL)).thenReturn(Optional.of(subUser));

        BadRequestRestApiException ex = assertThrows(
                BadRequestRestApiException.class,
                () -> helper.updateSupplierTeamMember(teamMember.getPublicId(), request, MAIN_EMAIL));

        assertTrue(ex.getRestApiError().getUserMessage().contains("main supplier"));
        verify(personRepository, never()).save(any());
    }

    private Person mainSupplierPerson() {
        Person person = new Person();
        person.setPublicId(UUID.randomUUID());
        person.setEmail(MAIN_EMAIL);
        person.setSupplierPublicId(SUPPLIER_PUBLIC_ID);
        person.setHasSupplierAccount(true);
        person.setMainSupplier(true);
        return person;
    }

    private Person teamMemberPerson() {
        Person person = new Person();
        person.setPublicId(UUID.randomUUID());
        person.setEmail("member@supplier.co.tz");
        person.setSupplierPublicId(SUPPLIER_PUBLIC_ID);
        person.setHasSupplierAccount(true);
        person.setMainSupplier(false);
        return person;
    }

    private Role supplierRole() {
        Role role = new Role();
        role.setRoleName("ROLE_SUPPLIER_PORTAL + Test Supplier");
        role.setSupplierRole(true);
        role.setSupplierPublicId(SUPPLIER_PUBLIC_ID);
        return role;
    }

    private Permission permission(String name) {
        Permission permission = new Permission();
        permission.setPublicId(UUID.randomUUID());
        permission.setName(name);
        return permission;
    }

    private PersonRequest teamMemberRequest(String email) {
        PersonRequest request = new PersonRequest();
        request.setFirstName("Team");
        request.setLastName("Member");
        request.setEmail(email);
        request.setPhoneNumber("255700000001");
        request.setIdentificationType(IdentificationType.TIN);
        request.setIdentificationNumber("123456789");
        return request;
    }
}
