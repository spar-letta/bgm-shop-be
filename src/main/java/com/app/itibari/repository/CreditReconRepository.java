package com.app.itibari.repository;

import com.app.itibari.models.entities.CreditRecon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CreditReconRepository extends JpaRepository<CreditRecon, Long> {
    Optional<CreditRecon> findCreditReconByOrderPublicIdAndDeletedIsFalse(String string);

    Optional<CreditRecon> findCreditReconByPublicIdAndDeletedIsFalse(UUID creditReconId);

    Optional<CreditRecon> findByOrderNumberAndDeletedIsFalse(String orderNumber);

    List<CreditRecon> findCreditReconByOrderNumberIgnoreCaseAndDeletedIsFalse(String orderNumber);
}
