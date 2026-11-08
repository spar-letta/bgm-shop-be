package com.app.itibari.repository;

import com.app.itibari.dataTypes.IdentificationType;
import com.app.itibari.models.entities.DeliveryAgents;
import com.app.itibari.models.entities.Suppliers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryAgentsRepository extends JpaRepository<DeliveryAgents, Long>, JpaSpecificationExecutor<DeliveryAgents> {

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DeliveryAgents d where d.id = :id")
    Optional<DeliveryAgents> findByIdForUpdate(@Param("id") Long id);

    Optional<DeliveryAgents> findByNationalIdAndDeletedIsFalse(String nationalId);

    Page<DeliveryAgents> findByFirstNameContainingIgnoreCaseAndDeletedIsFalseOrLastNameContainingIgnoreCaseAndDeletedIsFalse(String searchParam,
                                                                                                                             String searchParam1,
                                                                                                                             Pageable pageable);
    Page<DeliveryAgents> findByDeletedIsFalse(Pageable pageable);

    Optional<DeliveryAgents> findByPublicIdAndDeletedIsFalse(UUID agentPublicId);

    List<DeliveryAgents> findByPublicIdInAndDeletedIsFalse(List<UUID> publicIds);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DeliveryAgents d where d.publicId = :publicId and d.deleted = false")
    Optional<DeliveryAgents> findByPublicIdForUpdate(@Param("publicId") UUID publicId);

    Optional<DeliveryAgents> findDeliveryAgentsByPhoneNumberAndDeletedIsFalse(String phoneNumber);

    Optional<DeliveryAgents> findByIdentificationTypeAndIdentificationNumberIgnoreCaseAndDeletedIsFalse(IdentificationType identificationType,
                                                                                                        String identificationNumber);

    List<DeliveryAgents> findByDeletedIsFalseAndPhoneNumber(String phoneNumber);

    @Query("SELECT d FROM DeliveryAgents d " +
            "WHERE d.deleted = false AND " +
            "(LOWER(d.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(d.lastName) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<DeliveryAgents> searchByName(@Param("search") String search, Sort sort);

    List<DeliveryAgents> findDeliveryAgentsByDeletedIsFalse(Sort sortByNameAsc);
}
