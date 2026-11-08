package com.app.itibari.repository;

import com.app.itibari.models.entities.Department;
import com.app.itibari.models.entities.Person;
import com.app.itibari.models.entities.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonRepository extends JpaRepository<Person, UUID>, JpaSpecificationExecutor<Person> {

    Optional<Person> findByEmailAndDeletedIsFalse(String email);

    List<Person> findAllByRolesContaining(Role role);

    Optional<Person> findByPublicIdAndDeletedIsFalse(UUID publicId);

    Page<Person> findAllByDeletedIsFalse(Pageable pageable);

    @Query("SELECT p FROM Person p JOIN p.roles r WHERE r = :role AND p.deleted = false")
    Optional<Person> findByRolesContainingAndDeletedIsFalse(@Param("role") Role role);

    Page<Person> findAllByEmailContainingOrFirstNameContainingOrLastNameContainingAndDeletedIsFalse(String searchParam,
                                                                                                    String searchParam1,
                                                                                                    String searchParam2,
                                                                                                    Pageable pageable);

    List<Person> findByDepartmentAndDeletedIsFalse(Department department);

    Page<Person> findByFinanceServiceOfficerTrueAndDeletedIsFalse(Pageable pageable);

    long countBySupplierPublicIdAndHasSupplierAccountTrueAndDeletedIsFalse(String supplierPublicId);

    @Query("""
            SELECT COUNT(p) FROM Person p
            WHERE p.supplierPublicId = :supplierPublicId
              AND p.hasSupplierAccount = true
              AND p.deleted = false
              AND COALESCE(p.mainSupplier, false) = false
            """)
    long countTeamMembersBySupplierPublicId(@Param("supplierPublicId") String supplierPublicId);

    @Query("""
            SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Person p
            WHERE p.supplierPublicId = :supplierPublicId
              AND p.hasSupplierAccount = true
              AND p.deleted = false
              AND p.mainSupplier = true
            """)
    boolean existsMainSupplierBySupplierPublicId(@Param("supplierPublicId") String supplierPublicId);

    boolean existsBySupplierPublicIdAndPhoneNumberAndHasSupplierAccountTrueAndDeletedIsFalse(
            String supplierPublicId, String phoneNumber);

    boolean existsBySupplierPublicIdAndPhoneNumberAndHasSupplierAccountTrueAndDeletedIsFalseAndPublicIdNot(
            String supplierPublicId, String phoneNumber, UUID publicId);
}
