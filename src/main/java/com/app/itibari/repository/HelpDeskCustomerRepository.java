package com.app.itibari.repository;

import com.app.itibari.models.entities.HelpDeskCustomer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface HelpDeskCustomerRepository extends JpaRepository<HelpDeskCustomer, Long> {
    Optional<HelpDeskCustomer> findByPhoneNumberIgnoreCaseAndDeletedIsFalse(String phoneNumber);

    Optional<HelpDeskCustomer> findByExistingCustomerPublicIdAndDeletedIsFalse(UUID existingCustomerPublicId);

    Optional<HelpDeskCustomer> findTopByExistingCustomerPublicIdAndDeletedIsFalse(UUID helpDeskCustomerPublicId);

}
