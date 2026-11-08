package com.app.itibari.services;

import com.app.itibari.models.entities.CreditRule;
import com.app.itibari.models.request.CreateCreditRuleRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CreditRuleService {
    CreditRule createCreditRule(CreateCreditRuleRequest createCreditRuleRequest, String name);

    Page<CreditRule> getAllCreditRules(String name, Pageable pageable);

    void deleteCreditRule(UUID publicId, String name);

    void updateCreditRule(UUID publicId, CreateCreditRuleRequest createCreditRuleRequest, String name);
}
