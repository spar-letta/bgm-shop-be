package com.app.itibari.repository;

import com.app.itibari.models.entities.OrganizationLocations;
import com.app.itibari.models.entities.OrganizationRegions;
import com.app.itibari.models.entities.Person;
import com.app.itibari.models.entities.SalesAgents;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SalesAgentsRepository extends JpaRepository<SalesAgents, Long> {

    Optional<SalesAgents> findByEmailAndDeletedIsFalse(String email);

    Page<SalesAgents> findByNameContainingIgnoreCaseAndDeletedIsFalseOrEmailContainingIgnoreCaseAndDeletedIsFalse(String searchParam,
                                                                                                                  String searchParam1,
                                                                                                                  Pageable pageable);
    Page<SalesAgents> findByDeletedIsFalse(Pageable pageable);

    Optional<SalesAgents> findByPublicIdAndDeletedIsFalse(UUID agentPublicId);

    Optional<SalesAgents> findSalesAgentsByPhoneNumberAndDeletedIsFalse(String phoneNumber);

    Optional<SalesAgents> findByPhoneNumberAndDeletedIsFalse(String phoneNumber);

    List<SalesAgents> findByDeletedIsFalseAndPhoneNumber(String phoneNumber);

    Page<SalesAgents> findByOrganizationRegion(OrganizationRegions organizationRegions, Pageable pageable);

    Page<SalesAgents> findByOrganizationRegionAndOrganizationLocationsIn(OrganizationRegions organizationRegions,
                                                                         List<OrganizationLocations> organizationLocations,
                                                                         Pageable pageable);

    Page<SalesAgents> findByOrganizationLocationsIn(List<OrganizationLocations> organizationLocations, Pageable pageable);

    Optional<SalesAgents> findBySalesAgentManagerAndDeletedIsFalse(Person person);

    SalesAgents findByPublicId(UUID uuid);

    @Query("select sa from SalesAgents sa where ?1 member of sa.organizationLocations and sa.deleted = false")
    List<SalesAgents> findByOrganizationLocation(OrganizationLocations organizationLocations);

    List<SalesAgents> findByNameContainingIgnoreCaseAndDeletedIsFalse(String searchParam, Sort sort);

    List<SalesAgents> findSalesAgentsByDeletedIsFalse(Sort sort);
}
