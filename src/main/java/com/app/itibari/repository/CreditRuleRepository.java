package com.app.itibari.repository;

import com.app.itibari.dataTypes.CustomerType;
import com.app.itibari.models.entities.CreditRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CreditRuleRepository extends JpaRepository<CreditRule,Long> {
    Optional<CreditRule> findCreditRuleByCustomerTypeAndRepaymentTermAndDeletedIsFalse(CustomerType customerType, boolean b);

    Optional<CreditRule> findCreditRuleByCustomerTypeAndCreditLimitAndDeletedIsFalse(CustomerType customerType, boolean b);

    Page<CreditRule> findAllByDeletedIsFalse(Pageable pageable);

    Optional<CreditRule> findByPublicIdAndDeletedIsFalse(UUID publicId);

    Optional<CreditRule> findCreditRuleByCustomerTypeAndInterestRateAndDeletedIsFalse(CustomerType customerType, boolean b);

    Optional<CreditRule> findCreditRuleByCustomerTypeAndOverdueFineAndDeletedIsFalse(CustomerType customerType, boolean b);
}
