package com.app.itibari.services;

import com.app.itibari.configuration.JwtTokenAuthentication;
import com.app.itibari.dataTypes.ActionType;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.*;
import com.app.itibari.models.response.PermissionDTO;
import com.app.itibari.models.response.PermissionGroupResponse;
import com.app.itibari.models.response.PermissionResponse;
import com.app.itibari.repository.*;
import com.app.itibari.services.helper.SupplierPortalAccessHelper;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class PersonServiceImpl implements PersonService {

    private final PersonRepository personRepository;

    private final PermissionGroupRepository permissionGroupRepository;

    private final PermissionRepository permissionRepository;

    private final RoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder;

    private final ItibariSmsService itibariSmsService;

    private final AuthenticationManager authenticationManager;

    private final JwtTokenAuthentication jwtTokenAuthentication;

    private final NotificationRepository notificationRepository;

    private final SupplierPortalAccessHelper supplierPortalAccessHelper;
    private final DepartmentRepository departmentRepository;

    public PersonServiceImpl(PersonRepository personRepository,
                             PermissionGroupRepository permissionGroupRepository,
                             PermissionRepository permissionRepository,
                             RoleRepository roleRepository,
                             PasswordEncoder passwordEncoder,
                             ItibariSmsService itibariSmsService,
                             AuthenticationManager authenticationManager,
                             JwtTokenAuthentication jwtTokenAuthentication,
                             NotificationRepository notificationRepository,
                             SupplierPortalAccessHelper supplierPortalAccessHelper, DepartmentRepository departmentRepository) {

        this.personRepository = personRepository;
        this.permissionGroupRepository = permissionGroupRepository;
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.itibariSmsService = itibariSmsService;
        this.authenticationManager = authenticationManager;
        this.jwtTokenAuthentication = jwtTokenAuthentication;
        this.notificationRepository = notificationRepository;
        this.supplierPortalAccessHelper = supplierPortalAccessHelper;
        this.departmentRepository = departmentRepository;
    }

    @Override
    public void createPermissionGroup(PermissionGroupRequest permissionGroupRequest, String loggedInUser) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(loggedInUser);
        Optional<PermissionGroup> permissionGroup = permissionGroupRepository.findByGroupNameIgnoreCaseAndDeletedIsFalse(
                permissionGroupRequest.getGroupName());
        if (permissionGroup.isPresent()) {
            throw new BadRequestRestApiException()
                    .userMessage("Sorry, a permission group with the name " + permissionGroupRequest.getGroupName() + " already exists.")
                    .developerMessage("A permission group with the name " + permissionGroupRequest.getGroupName() + " already exists.");
        }

        PermissionGroup newPermissionGroup = new PermissionGroup();
        newPermissionGroup.setGroupName(permissionGroupRequest.getGroupName());
        personOptional.ifPresent(newPermissionGroup::setCreatedBy);
        newPermissionGroup.setCreationTime(LocalDateTime.now());

        PermissionGroup savedPermissionGroup = permissionGroupRepository.save(newPermissionGroup);

        permissionGroupRequest.getPermissions().forEach(permission -> {
            Optional<Permission> permissionOptional = permissionRepository.findByNameIgnoreCaseAndDeletedIsFalse(permission);
            if (permissionOptional.isEmpty()) {

                Permission newPermission = new Permission();
                newPermission.setName(permission);
                personOptional.ifPresent(newPermission::setCreatedBy);
                newPermission.setCreationTime(LocalDateTime.now());
                newPermission.setPermissionGroup(savedPermissionGroup);

                Permission savedPermission = permissionRepository.save(newPermission);

                savedPermissionGroup.getPermissions().add(savedPermission);
            }
        });
    }

    @Override
    public void updatePermissionGroup(UUID publicId, PermissionRequest permissionRequest, String loggedInUser) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(loggedInUser);
        Optional<PermissionGroup> permissionGroupOptional = permissionGroupRepository.findByPublicIdAndDeletedIsFalse(publicId);
        if (permissionGroupOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .userMessage("Sorry, a permission group with the id " + publicId + " does not exist.")
                    .developerMessage("A permission group with the id " + publicId + " does not exist.");
        }

        PermissionGroup permissionGroup = permissionGroupOptional.get();

        permissionRequest.getPermissions().forEach(permission -> {

            Optional<Permission> permissionOptional = permissionRepository.findByNameIgnoreCaseAndDeletedIsFalse(permission);
            if (permissionOptional.isEmpty()) {

                Permission newPermission = new Permission();
                newPermission.setName(permission);
                personOptional.ifPresent(newPermission::setCreatedBy);
                newPermission.setCreationTime(LocalDateTime.now());
                newPermission.setPermissionGroup(permissionGroup);

                Permission savedPermission = permissionRepository.save(newPermission);

                permissionGroup.getPermissions().add(savedPermission);
            }
        });
    }

    @Override
    public PermissionGroup getPermissionGroup(UUID publicId) {

        Optional<PermissionGroup> permissionGroupOptional = permissionGroupRepository.findByPublicIdAndDeletedIsFalse(publicId);
        if (permissionGroupOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .userMessage("Sorry, a permission group with the id " + publicId + " does not exist.")
                    .developerMessage("A permission group with the id " + publicId + " does not exist.");
        }

        return permissionGroupOptional.get();
    }

    @Override
    public Page<PermissionGroup> getPermissionGroups(String searchParam, String name, Pageable pageable) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
        if (personOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .userMessage("Sorry, logged in user with not found.")
                    .developerMessage("A person with the email " + name + " does not exist.");
        }
        Person person = personOptional.get();
        if (person.getHasSupplierAccount() != null && person.getHasSupplierAccount() && person.getSupplierPublicId() != null) {
            Specification<PermissionGroup> specification = (root, query, criteriaBuilder) ->
                    criteriaBuilder.and(
                            criteriaBuilder.equal(root.get("supplierRole"), true),
                            criteriaBuilder.equal(root.get("deleted"), false),
                            criteriaBuilder.equal(root.get("supplierPublicId"), person.getSupplierPublicId())
                    );
            if (searchParam != null && !searchParam.isEmpty()) {
                specification = specification.and((root, query, criteriaBuilder) ->
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("groupName")),
                                "%" + searchParam.toLowerCase() + "%")
                );
            }
            return permissionGroupRepository.findAll(specification, pageable);
        }

        if (searchParam == null || searchParam.isEmpty()) {
            return permissionGroupRepository.findAllByDeletedIsFalse(pageable);
        }
        return permissionGroupRepository.findBySearchParam(searchParam.toLowerCase(), pageable);
    }


    @Override
    public void deletePermissionGroup(UUID publicId, String loggedInUser) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(loggedInUser);
        Optional<PermissionGroup> permissionGroupOptional = permissionGroupRepository.findByPublicIdAndDeletedIsFalse(publicId);
        if (permissionGroupOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .userMessage("Sorry, a permission group with the id " + publicId + " does not exist.")
                    .developerMessage("A permission group with the id " + publicId + " does not exist.");
        }

        PermissionGroup permissionGroup = permissionGroupOptional.get();
        permissionGroup.setDeleted(true);
        personOptional.ifPresent(permissionGroup::setUpdatedBy);
        permissionGroup.setUpdateDate(LocalDateTime.now());

        permissionGroupRepository.save(permissionGroup);

        permissionGroup.getPermissions().forEach(permission -> {
            permission.setDeleted(true);
            personOptional.ifPresent(permission::setUpdatedBy);
            permission.setUpdateDate(LocalDateTime.now());
            permissionRepository.save(permission);
        });
    }

    @Override
    public void deletePermissionFromPermissionGroup(UUID publicId, UUID permissionId, String loggedInUser) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(loggedInUser);
        Optional<PermissionGroup> permissionGroupOptional = permissionGroupRepository.findByPublicIdAndDeletedIsFalse(publicId);
        if (permissionGroupOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .userMessage("Sorry, a permission group with the id " + publicId + " does not exist.")
                    .developerMessage("A permission group with the id " + publicId + " does not exist.");
        }

        Optional<Permission> permissionOptional = permissionRepository.findByPublicIdAndDeletedIsFalse(permissionId);
        if (permissionOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .userMessage("Sorry, a permission with the id " + permissionId + " does not exist.")
                    .developerMessage("A permission with the id " + permissionId + " does not exist.");
        }

        PermissionGroup permissionGroup = permissionGroupOptional.get();
        Permission permission = permissionOptional.get();

        permission.setDeleted(true);
        personOptional.ifPresent(permission::setUpdatedBy);
        permission.setUpdateDate(LocalDateTime.now());
        permissionRepository.save(permission);

        permissionGroup.getPermissions().remove(permission);
    }

    @Override
    public Role createRole(RoleRequest roleRequest, String loggedInUser) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(loggedInUser);
        Optional<Role> roleOptional = roleRepository.findByRoleNameIgnoreCaseAndDeletedIsFalse(roleRequest.getRoleName());
        if (roleOptional.isPresent()) {
            throw new BadRequestRestApiException()
                    .userMessage("Sorry, a role with the name " + roleRequest.getRoleName() + " already exists.")
                    .developerMessage("A role with the name " + roleRequest.getRoleName() + " already exists.");
        }

        Role newRole = new Role();
        newRole.setRoleName(roleRequest.getRoleName());
        personOptional.ifPresent(newRole::setCreatedBy);
        newRole.setCreationTime(LocalDateTime.now());

        return roleRepository.save(newRole);
    }

    @Override
    public Page<Role> getRoles(String searchParam, String logged, Pageable pageable) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(logged);
        if (personOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .userMessage("Sorry, logged in user with not found.")
                    .developerMessage("A person with the email " + logged + " does not exist.");
        }

        Person person = personOptional.get();

        if (person.getHasSupplierAccount() != null && person.getHasSupplierAccount() && person.getSupplierPublicId() != null) {
            Specification<Role> specification = (root, query, criteriaBuilder) ->
                    criteriaBuilder.and(
                            criteriaBuilder.equal(root.get("supplierRole"), true),
                            criteriaBuilder.equal(root.get("deleted"), false),
                            criteriaBuilder.equal(root.get("supplierPublicId"), person.getSupplierPublicId())
                    );
            if (searchParam != null && !searchParam.isEmpty()) {
                specification = specification.and((root, query, criteriaBuilder) ->
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("roleName")),
                                "%" + searchParam.toLowerCase() + "%")
                );
            }
            return roleRepository.findAll(specification, pageable);
        }
        if (searchParam == null || searchParam.isEmpty()) {
            return roleRepository.findAllByDeletedIsFalse(pageable);
        }
        return roleRepository.findAllByRoleNameContainingAndDeletedIsFalse(searchParam, pageable);
    }

    @Override
    public void updateRole(UUID publicId, UpdateRoleRequest updateRoleRequest, String loggedInUser) {

        Optional<Person> loggedInPersonOptional = personRepository.findByEmailAndDeletedIsFalse(loggedInUser);
        Optional<Role> roleOptional = roleRepository.findByPublicIdAndDeletedIsFalse(publicId);
        if (roleOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .userMessage("Sorry, a role with the id " + publicId + " does not exist.")
                    .developerMessage("A role with the id " + publicId + " does not exist.");
        }

        Role role = roleOptional.get();

        if (updateRoleRequest.getPermissionIds().isEmpty()) {
            throw new BadRequestRestApiException()
                    .userMessage("Sorry, you must provide at least one permission to update the role.")
                    .developerMessage("You must provide at least one permission to update the role.");
        }

        if (updateRoleRequest.getActionType().equals(ActionType.ADD)) {

            updateRoleRequest.getPermissionIds().forEach(permissionId -> {
                Optional<Permission> permissionOptional = permissionRepository.findByPublicIdAndDeletedIsFalse(permissionId);
                permissionOptional.ifPresent(role::addPermission);
            });
        } else if (updateRoleRequest.getActionType().equals(ActionType.REMOVE)) {

            updateRoleRequest.getPermissionIds().forEach(permissionId -> {
                Optional<Permission> permissionOptional = permissionRepository.findByPublicIdAndDeletedIsFalse(permissionId);
                permissionOptional.ifPresent(role::removePermission);
            });
        }

        loggedInPersonOptional.ifPresent(role::setUpdatedBy);
        role.setUpdateDate(LocalDateTime.now());

        roleRepository.save(role);
    }

    @Override
    public void deleteRole(UUID publicId, String loggedInUser) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(loggedInUser);
        Optional<Role> roleOptional = roleRepository.findByPublicIdAndDeletedIsFalse(publicId);
        if (roleOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .userMessage("Sorry, a role with the id " + publicId + " does not exist.")
                    .developerMessage("A role with the id " + publicId + " does not exist.");
        }

        Role role = roleOptional.get();
        role.setDeleted(true);
        personOptional.ifPresent(role::setUpdatedBy);
        role.setUpdateDate(LocalDateTime.now());

        roleRepository.save(role);

        // remove the role from all persons
        personRepository.findAllByRolesContaining(role).forEach(person1 -> {
            person1.removeRole(role);
            personRepository.save(person1);
        });
    }

    @Override
    public Person createPerson(PersonRequest personRequest, String loggedInUser) {

        Optional<Person> loggedInPersonOptional = personRepository.findByEmailAndDeletedIsFalse(loggedInUser);
        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(personRequest.getEmail());
        if (personOptional.isPresent()) {
            throw new BadRequestRestApiException()
                    .userMessage("Sorry, a person with the email " + personRequest.getEmail() + " already exists.")
                    .developerMessage("A person with the email " + personRequest.getEmail() + " already exists.");
        }

        if (personRequest.getDepartmentPublicId() == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Missing department details.")
                    .userMessage("Sorry, Select a department");
        }

        Department foundDepartment = departmentRepository.findByPublicIdAndDeletedIsFalse(personRequest.getDepartmentPublicId()).orElse(null);
        if (foundDepartment == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Missing department.")
                    .userMessage("Sorry, Missing department");
        }


        Person newPerson = new Person();
        newPerson.setEmail(personRequest.getEmail());
        newPerson.setFirstName(personRequest.getFirstName());
        newPerson.setLastName(personRequest.getLastName());
        newPerson.setAddress(personRequest.getAddress());
        newPerson.setPhoneNumber(personRequest.getPhoneNumber());
        newPerson.setCity(personRequest.getCity());
        newPerson.setState(personRequest.getState());
        loggedInPersonOptional.ifPresent(newPerson::setCreatedBy);
        newPerson.setCreationTime(LocalDateTime.now());
        newPerson.setPassword(getPassword(personRequest.getEmail(), personRequest.getPhoneNumber()));
        newPerson.setIdentificationType(personRequest.getIdentificationType());
        newPerson.setIdentificationNumber(personRequest.getIdentificationNumber());
        newPerson.setDepartment(foundDepartment);

        return personRepository.save(newPerson);
    }

    private String getPassword(String email, String phoneNumber) {

        String randomPassword = RandomStringUtils.randomAlphanumeric(8);
        itibariSmsService.send(phoneNumber, String.format("Your itibari login password is %s", randomPassword));

        return passwordEncoder.encode(randomPassword);
    }

    @Override
    public void updatePerson(UUID publicId, UpdatePersonRequest updatePersonRequest, String loggedInUser) {

        Optional<Person> loggedInPersonOptional = personRepository.findByEmailAndDeletedIsFalse(loggedInUser);
        Optional<Person> personOptional = personRepository.findByPublicIdAndDeletedIsFalse(publicId);
        if (personOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .userMessage("Sorry, a person with the id " + publicId + " does not exist.")
                    .developerMessage("A person with the id " + publicId + " does not exist.");
        }
        Person personToUpdate = personOptional.get();

        if (updatePersonRequest.getEmail() != null && !updatePersonRequest.getEmail().equalsIgnoreCase(personToUpdate.getEmail())) {
            Optional<Person> personWithEmailOptional = personRepository.findByEmailAndDeletedIsFalse(updatePersonRequest.getEmail());
            if (personWithEmailOptional.isPresent()) {
                throw new BadRequestRestApiException()
                        .userMessage("Sorry, a person with the email " + updatePersonRequest.getEmail() + " already exists.")
                        .developerMessage("A person with the email " + updatePersonRequest.getEmail() + " already exists.");
            }
        }

        if (loggedInPersonOptional.isPresent() && loggedInPersonOptional.get().getPublicId().equals(publicId)) {
            throw new BadRequestRestApiException()
                    .userMessage("Sorry, you cannot update your own profile. Please reset your password on your profile.")
                    .developerMessage("You cannot update your own profile. Please reset your password on your profile.");
        }

        if (updatePersonRequest.getDepartmentPublicId() == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Missing department details.")
                    .userMessage("Sorry, Select a department");
        }

        Department foundDepartment = departmentRepository.findByPublicIdAndDeletedIsFalse(updatePersonRequest.getDepartmentPublicId()).orElse(null);
        if (foundDepartment == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Missing department.")
                    .userMessage("Sorry, Missing department");
        }

        personToUpdate.setFirstName(updatePersonRequest.getFirstName());
        personToUpdate.setLastName(updatePersonRequest.getLastName());
        personToUpdate.setAddress(updatePersonRequest.getAddress());
        personToUpdate.setPhoneNumber(updatePersonRequest.getPhoneNumber());
        personToUpdate.setCity(updatePersonRequest.getCity());
        personToUpdate.setState(updatePersonRequest.getState());
        loggedInPersonOptional.ifPresent(personToUpdate::setUpdatedBy);
        personToUpdate.setUpdateDate(LocalDateTime.now());
        personToUpdate.setIdentificationType(updatePersonRequest.getIdentificationType());
        personToUpdate.setIdentificationNumber(updatePersonRequest.getIdentificationNumber());
        personToUpdate.setEmail(updatePersonRequest.getEmail());
        personToUpdate.setDepartment(foundDepartment);

        personRepository.save(personToUpdate);
    }

    @Override
    public void addRolesToPerson(UUID publicId, AddRolesToPersonRequest addRolesToPersonRequest, String loggedInUser) {

        Optional<Person> loggedInPersonOptional = personRepository.findByEmailAndDeletedIsFalse(loggedInUser);
        Optional<Person> personOptional = personRepository.findByPublicIdAndDeletedIsFalse(publicId);
        if (personOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .userMessage("Sorry, a person with the id " + publicId + " does not exist.")
                    .developerMessage("A person with the id " + publicId + " does not exist.");
        }

        if (addRolesToPersonRequest.getRoleIds().isEmpty()) {
            throw new BadRequestRestApiException()
                    .userMessage("Sorry, you must provide at least one role to add to the person.")
                    .developerMessage("You must provide at least one role to add to the person.");
        }

        Person personToUpdate = personOptional.get();

        if (addRolesToPersonRequest.getActionType().equals(ActionType.ADD)) {

            addRolesToPersonRequest.getRoleIds().forEach(roleId -> {
                Optional<Role> roleOptional = roleRepository.findByPublicIdAndDeletedIsFalse(roleId);
                roleOptional.ifPresent(personToUpdate::addRole);
            });
        } else if (addRolesToPersonRequest.getActionType().equals(ActionType.REMOVE)) {

            addRolesToPersonRequest.getRoleIds().forEach(roleId -> {
                Optional<Role> roleOptional = roleRepository.findByPublicIdAndDeletedIsFalse(roleId);
                roleOptional.ifPresent(personToUpdate::removeRole);
            });
        }

        loggedInPersonOptional.ifPresent(personToUpdate::setUpdatedBy);
        personToUpdate.setUpdateDate(LocalDateTime.now());

        personRepository.save(personToUpdate);
    }

    @Override
    public void deletePerson(UUID publicId, String loggedInUser) {

        Optional<Person> loggedInPersonOptional = personRepository.findByEmailAndDeletedIsFalse(loggedInUser);
        Optional<Person> personOptional = personRepository.findByPublicIdAndDeletedIsFalse(publicId);
        if (personOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .userMessage("Sorry, a person with the id " + publicId + " does not exist.")
                    .developerMessage("A person with the id " + publicId + " does not exist.");
        }

        Person personToDelete = personOptional.get();
        personToDelete.setDeleted(true);
        loggedInPersonOptional.ifPresent(personToDelete::setUpdatedBy);
        personToDelete.setUpdateDate(LocalDateTime.now());

        personRepository.save(personToDelete);
    }

    @Override
    public Page<Person> getPersons(String searchParam, String logged, Boolean isFinanceServiceOfficer, Pageable pageable) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(logged);
        if (personOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .userMessage("Sorry, logged in user with not found.")
                    .developerMessage("A person with the email " + logged + " does not exist.");
        }
        Person person = personOptional.get();
        if (person.getHasSupplierAccount() != null && person.getHasSupplierAccount() && person.getSupplierPublicId() != null) {

            Specification<Person> specification = (root, query, criteriaBuilder) -> criteriaBuilder.and(
                    criteriaBuilder.equal(root.get("supplierPublicId"), person.getSupplierPublicId()),
                    criteriaBuilder.isTrue(root.get("hasSupplierAccount")),
                    criteriaBuilder.isFalse(root.get("deleted"))
            );
            if (searchParam != null && !searchParam.isEmpty()) {
                specification = specification.and((root, query, criteriaBuilder) ->
                        criteriaBuilder.or(
                                criteriaBuilder.like(criteriaBuilder.lower(root.get("email")),
                                        "%" + searchParam.toLowerCase() + "%"),
                                criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")),
                                        "%" + searchParam.toLowerCase() + "%"),
                                criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")),
                                        "%" + searchParam.toLowerCase() + "%")
                        )
                );
            }
            return personRepository.findAll(specification, pageable);
        }

        if (isFinanceServiceOfficer != null && isFinanceServiceOfficer == Boolean.TRUE) {
            return personRepository.findByFinanceServiceOfficerTrueAndDeletedIsFalse(pageable);
        }

        return StringUtils.isEmpty(searchParam) ?
                personRepository.findAllByDeletedIsFalse(pageable) :
                personRepository.findAllByEmailContainingOrFirstNameContainingOrLastNameContainingAndDeletedIsFalse(searchParam,
                        searchParam,
                        searchParam,
                        pageable);
    }

    @Override
    public Person getPersonProfile(String name) {

        return personRepository.findByEmailAndDeletedIsFalse(name).orElseThrow(() -> new BadRequestRestApiException()
                .userMessage("Sorry, a person with the email " + name + " does not exist.")
                .developerMessage("A person with the email " + name + " does not exist."));
    }

    @Override
    public void changePassword(ChangePasswordRequest changePasswordRequest, String name) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
        if (personOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .userMessage("Sorry, a person with the email " + name + " does not exist.")
                    .developerMessage("A person with the email " + name + " does not exist.");
        }

        Person person = personOptional.get();
        if (!passwordEncoder.matches(changePasswordRequest.getOldPassword(), person.getPassword())) {
            throw new BadRequestRestApiException()
                    .userMessage("Sorry, the old password is incorrect.")
                    .developerMessage("The old password is incorrect.");
        }

        if (changePasswordRequest.getOldPassword().equalsIgnoreCase(changePasswordRequest.getNewPassword())) {
            throw new BadRequestRestApiException()
                    .userMessage("Sorry, the new password cannot be the same as the old password.")
                    .developerMessage("The new password cannot be the same as the old password.");
        }

        if (!changePasswordRequest.getConfirmPassword().equalsIgnoreCase(changePasswordRequest.getNewPassword())) {
            throw new BadRequestRestApiException()
                    .userMessage("Sorry, the new password and confirm password do not match.")
                    .developerMessage("The new password and confirm password do not match.");
        }

        person.setPassword(passwordEncoder.encode(changePasswordRequest.getNewPassword()));

        itibariSmsService.send(person.getPhoneNumber(), "Your password has been changed successfully.");
        personRepository.save(person);
    }

    @Override
    @Transactional
    public void resetUserName(ResetUserNameRequest resetUserNameRequest, String currentUsername) {

        Person person = personRepository.findByEmailAndDeletedIsFalse(currentUsername)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .userMessage("Sorry, a person with the email " + currentUsername + " does not exist.")
                        .developerMessage("A person with the email " + currentUsername + " does not exist."));

        String newUsername = resetUserNameRequest.getNewUserName();
        if (StringUtils.isEmpty(newUsername)) {
            throw new BadRequestRestApiException()
                    .userMessage("Sorry, the new username cannot be empty.")
                    .developerMessage("The new username cannot be empty.");
        }

        if (newUsername.equalsIgnoreCase(currentUsername)) {
            throw new BadRequestRestApiException()
                    .userMessage("Sorry, the new username cannot be the same as the current username.")
                    .developerMessage("The new username cannot be the same as the current username.");
        }
        person.setEmail(newUsername);
        personRepository.save(person);

        itibariSmsService.send(person.getPhoneNumber(), "Your username has been updated to: " + newUsername);

        // Force logout
        jwtTokenAuthentication.logout(person.getEmail());
    }

    @Override
    public Person findPersonByRole(String role) {

        Optional<Role> roleOptional = roleRepository.findByRoleNameIgnoreCaseAndDeletedIsFalse(role);
        if (roleOptional.isPresent()) {
            Role role1 = roleOptional.get();
            Optional<Person> personOptional = personRepository.findByRolesContainingAndDeletedIsFalse(role1);

            return personOptional.orElse(null);
        }

        return null;
    }

    @Override
    public Page<Role> getRolesOfPerson(UUID publicId, String searchParam, Pageable pageable) {

        Optional<Person> personOptional = personRepository.findByPublicIdAndDeletedIsFalse(publicId);
        if (personOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .userMessage("Sorry, a person with the id " + publicId + " does not exist.")
                    .developerMessage("A person with the id " + publicId + " does not exist.");
        }
        Person person = personOptional.get();
        return new PageImpl<>(person.getRoles(), pageable, person.getRoles().size());
    }

    @Override
    public Page<Notification> getLoggedInPersonNotifications(String searchParam, Boolean isRead, String name, Pageable pageable) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
        if (personOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .userMessage("Sorry, a person with the email " + name + " does not exist.")
                    .developerMessage("A person with the email " + name + " does not exist.");
        }
        Person person = personOptional.get();

        List<Role> roles = person.getRoles();

        if (roles.isEmpty()) {
            return Page.empty();
        }

        List<Notification> notifications = notificationRepository.findAllByPersonRolesAndNotDeleted(roles);

        if (searchParam != null && searchParam.length() > 3) {
            notifications = notifications.stream()
                    .filter(notification -> notification.getTitle().toLowerCase().contains(searchParam.toLowerCase())
                            || notification.getMessage().toLowerCase().contains(searchParam.toLowerCase()))
                    .toList();
        }
        if (isRead != null) {
            notifications = notifications.stream()
                    .filter(notification -> notification.isRead() == isRead)
                    .toList();
        }

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), notifications.size());
        List<Notification> paginatedNotifications = notifications.subList(start, end);
        return new PageImpl<>(paginatedNotifications, pageable, notifications.size());
    }

    @Override
    public void markNotificationAsRead(String notificationPublicId, String name) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
        if (personOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .userMessage("Sorry, a person with the email " + name + " does not exist.")
                    .developerMessage("A person with the email " + name + " does not exist.");
        }
        Person person = personOptional.get();

        Optional<Notification> notificationOptional = notificationRepository.findByPublicIdAndNotDeleted(UUID.fromString(
                notificationPublicId));
        if (notificationOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .userMessage("Sorry, a notification with the id " + notificationPublicId + " does not exist.")
                    .developerMessage("A notification with the id " + notificationPublicId + " does not exist.");
        }

        Notification notification = notificationOptional.get();
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Override
    public Person createSupplierUser(PersonRequest personRequest, String name) {

        return supplierPortalAccessHelper.createSupplierUser(personRequest, name);
    }

    @Override
    public void deleteSupplierTeamMember(UUID teamMemberPublicId, String loggedInUser) {
        supplierPortalAccessHelper.deleteSupplierTeamMember(teamMemberPublicId, loggedInUser);
    }

    @Override
    public Person updateSupplierTeamMember(UUID teamMemberPublicId,
                                           UpdateSupplierTeamMemberRequest updateRequest,
                                           String loggedInUser) {
        return supplierPortalAccessHelper.updateSupplierTeamMember(teamMemberPublicId, updateRequest, loggedInUser);
    }

    @Override
    public List<Person> getPersonInRole(String roleName) {
        Role role = roleRepository.findByRoleNameAndDeletedIsFalse(roleName).orElse(null);
        if (role == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Role not found")
                    .userMessage("Sorry, role not found");
        }
        return personRepository.findAllByRolesContaining(role);
    }

    @Override
    public Page<Permission> getPermissionsOfRole(UUID publicId, Pageable pageable) {
        Optional<Role> roleOptional = roleRepository.findByPublicIdAndDeletedIsFalse(publicId);
        if (roleOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .userMessage("Sorry, a role with the id " + publicId + " does not exist.")
                    .developerMessage("A role with the id " + publicId + " does not exist.");
        }
        Role role = roleOptional.get();
        List<Permission> permissions = role.getPermissions().stream().filter(permission -> !permission.getDeleted()).toList();
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), permissions.size());
        List<Permission> paginatedPermissions = permissions.subList(start, end);
        return new PageImpl<>(paginatedPermissions, pageable, permissions.size());
    }

    @Override
    public PermissionResponse fetchRolePermissions(UUID rolePublicId) {
        Optional<Role> roleOptional = roleRepository.findByPublicIdAndDeletedIsFalse(rolePublicId);
        if (roleOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .userMessage("Sorry, a role with the id " + rolePublicId + " does not exist.")
                    .developerMessage("A role with the id " + rolePublicId + " does not exist.");
        }
        Role role = roleOptional.get();
        List<Permission> permissions = role.getPermissions().stream().filter(permission -> !permission.getDeleted()).toList();

        PermissionResponse permissionResponse = new PermissionResponse();

        Map<PermissionGroup, List<Permission>> groupPermissions = new HashMap<>();

        permissions.forEach(permission -> {
            PermissionGroup group = permission.getPermissionGroup();
            if (groupPermissions.containsKey(group)) {
                groupPermissions.get(group).add(permission);
            } else {
                List<Permission> permissionList = new ArrayList<>();
                permissionList.add(permission);
                groupPermissions.put(group, permissionList);
            }
        });

        List<PermissionGroupResponse> permissionGroupResponses = new ArrayList<>();

        groupPermissions.forEach((group, permissionList) -> {
            PermissionGroupResponse groupResponse = new PermissionGroupResponse();
            groupResponse.setPublicId(group.getPublicId());
            groupResponse.setName(group.getGroupName());
            List<PermissionDTO> permissionResponseItems = permissionList.stream().map(permission -> {
                PermissionDTO item = new PermissionDTO();
                item.setPublicId(permission.getPublicId());
                item.setName(permission.getName());
                return item;
            }).toList();
            groupResponse.setPermissions(permissionResponseItems);
            permissionGroupResponses.add(groupResponse);
        });

        permissionResponse.setRolePublicId(role.getPublicId());
        permissionResponse.setRoleName(role.getRoleName());
        permissionResponse.setPermissionGroups(permissionGroupResponses);

        return permissionResponse;


    }

    @Override
    public void updateUserCallerId(UUID publicId, String callerId, String name) {
        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
        if (personOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .userMessage("Sorry, a person with the email " + name + " does not exist.")
                    .developerMessage("A person with the email " + name + " does not exist.");
        }
        Person loggedInUser = personOptional.get();


        Person person = personRepository.findByPublicIdAndDeletedIsFalse(publicId).orElseThrow(
                () -> new BadRequestRestApiException()
                        .userMessage("Sorry, a person not found")
                        .developerMessage("Person with the id " + publicId + " does not exist.")
        );

        person.setCallerId(callerId);
        person.setUpdatedBy(loggedInUser);
        person.setUpdateDate(LocalDateTime.now());
        personRepository.save(person);
    }

    @Override
    @Transactional
    public void markUserFinanceServiceOfficer(Set<UUID> publicIds, Boolean markFinanceServiceOfficer, String email) {
        Person loggedInUser = personRepository.findByEmailAndDeletedIsFalse(email)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .userMessage("Sorry, a person with the email " + email + " does not exist.")
                        .developerMessage("A person with the email " + email + " does not exist."));

        boolean flag = Boolean.TRUE.equals(markFinanceServiceOfficer);

        publicIds.forEach(publicId -> {
            Person person = personRepository.findByPublicIdAndDeletedIsFalse(publicId)
                    .orElseThrow(() -> new BadRequestRestApiException()
                            .userMessage("Sorry, a person not found")
                            .developerMessage("Person with the id " + publicId + " does not exist."));

            person.setFinanceServiceOfficer(flag);
            person.setUpdatedBy(loggedInUser);
            person.setUpdateDate(LocalDateTime.now());
            personRepository.save(person);
        });
    }
}