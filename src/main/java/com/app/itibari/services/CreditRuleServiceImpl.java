package com.app.itibari.services;

import com.app.itibari.models.entities.CreditRule;
import com.app.itibari.models.entities.Person;
import com.app.itibari.models.request.CreateCreditRuleRequest;
import com.app.itibari.repository.CreditRuleRepository;
import com.app.itibari.repository.PersonRepository;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.helpers.Validators;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class CreditRuleServiceImpl implements CreditRuleService {
    private final CreditRuleRepository creditRuleRepository;
    private final PersonRepository personRepository;

    public CreditRuleServiceImpl(CreditRuleRepository creditRuleRepository, PersonRepository personRepository) {
        this.creditRuleRepository = creditRuleRepository;
        this.personRepository = personRepository;
    }

    @Override
    public CreditRule createCreditRule(CreateCreditRuleRequest createCreditRuleRequest, String name) {
        Person loggedInUser = getLoggedInUser(name);


        // validate ...only one is true && all cannot be false

        if (Validators.countTrue(createCreditRuleRequest.getIsCreditLimit(),
                createCreditRuleRequest.getIsRepaymentTerm(),
                createCreditRuleRequest.getIsInterestRate(),
                createCreditRuleRequest.getIsOverdueFine()) != 1) {
            throw new BadRequestRestApiException()
                    .developerMessage("Exactly one of credit limit, repayment term, interest rate, or overdue fine must be set")
                    .userMessage("Exactly one of credit limit, repayment term, interest rate, or overdue fine must be set");
        }

        if (createCreditRuleRequest.getIsRepaymentTerm()) {
            if (createCreditRuleRequest.getRepaymentTermDays() == null || createCreditRuleRequest.getRepaymentTermDays() <= 0) {
                throw new BadRequestRestApiException()
                        .developerMessage("Repayment term days must be a positive integer")
                        .userMessage("Repayment term days must be a positive integer");
            }


            creditRuleRepository.findCreditRuleByCustomerTypeAndRepaymentTermAndDeletedIsFalse(
                    createCreditRuleRequest.getCustomerType(),
                    true
            ).ifPresent(existingRule -> {
                throw new BadRequestRestApiException()
                        .developerMessage("A repayment term rule for this customer type already exists")
                        .userMessage("A repayment term rule for this customer type already exists");
            });
        }

        if (createCreditRuleRequest.getIsCreditLimit()) {
            if (createCreditRuleRequest.getCreditLimitPercentage() == null || createCreditRuleRequest.getCreditLimitPercentage().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestRestApiException()
                        .developerMessage("Credit limit percentage must be a positive value")
                        .userMessage("Credit limit percentage must be a positive value");
            }

            creditRuleRepository.findCreditRuleByCustomerTypeAndCreditLimitAndDeletedIsFalse(
                    createCreditRuleRequest.getCustomerType(),
                    true
            ).ifPresent(existingRule -> {
                throw new BadRequestRestApiException()
                        .developerMessage("A credit limit rule for this customer type already exists")
                        .userMessage("A credit limit rule for this customer type already exists");
            });
        }

        if (createCreditRuleRequest.getIsInterestRate()) {
            if (createCreditRuleRequest.getInterestRatePercentage() == null || createCreditRuleRequest.getInterestRatePercentage().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestRestApiException()
                        .developerMessage("Interest rate percentage must be a positive value")
                        .userMessage("Interest rate percentage must be a positive value");
            }

            creditRuleRepository.findCreditRuleByCustomerTypeAndInterestRateAndDeletedIsFalse(
                    createCreditRuleRequest.getCustomerType(),
                    true
            ).ifPresent(existingRule -> {
                throw new BadRequestRestApiException()
                        .developerMessage("An interest rate rule for this customer type already exists")
                        .userMessage("An interest rate rule for this customer type already exists");
            });
        }

        creditRuleRepository.findCreditRuleByCustomerTypeAndOverdueFineAndDeletedIsFalse(
                createCreditRuleRequest.getCustomerType(),
                true
        ).ifPresent(existingRule -> {
            throw new BadRequestRestApiException()
                    .developerMessage("An overdue fine rule for this customer type already exists")
                    .userMessage("An overdue fine rule for this customer type already exists");
        });

        CreditRule creditRule = new CreditRule();
        creditRule.setCustomerType(createCreditRuleRequest.getCustomerType());
        creditRule.setRuleName(createCreditRuleRequest.getRuleName());
        creditRule.setCreditLimit(createCreditRuleRequest.getIsCreditLimit());
        creditRule.setCreditLimitPercentage(createCreditRuleRequest.getIsCreditLimit() ? createCreditRuleRequest.getCreditLimitPercentage() : BigDecimal.ZERO);
        creditRule.setRepaymentTerm(createCreditRuleRequest.getIsRepaymentTerm());
        creditRule.setRepaymentTermDays(createCreditRuleRequest.getIsRepaymentTerm() ? createCreditRuleRequest.getRepaymentTermDays() : 0);
        creditRule.setInterestRate(createCreditRuleRequest.getIsInterestRate());
        creditRule.setInterestRatePercentage(createCreditRuleRequest.getIsInterestRate() ? createCreditRuleRequest.getInterestRatePercentage() : BigDecimal.ZERO);
        creditRule.setOverdueFine(createCreditRuleRequest.getIsOverdueFine());
        creditRule.setOverdueFinePercentage(createCreditRuleRequest.getIsOverdueFine() ? createCreditRuleRequest.getOverdueFinePercentage() : BigDecimal.ZERO);
        creditRule.setCreatedBy(loggedInUser);
        creditRule.setCreationTime(LocalDateTime.now());

        return creditRuleRepository.save(creditRule);

    }

    @Override
    public void updateCreditRule(UUID publicId, CreateCreditRuleRequest createCreditRuleRequest, String name) {
        CreditRule creditRule = creditRuleRepository.findByPublicIdAndDeletedIsFalse(publicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Credit rule not found")
                        .userMessage("Credit rule not found"));


        if (Validators.countTrue(createCreditRuleRequest.getIsCreditLimit(),
                createCreditRuleRequest.getIsRepaymentTerm(),
                createCreditRuleRequest.getIsInterestRate(),
                createCreditRuleRequest.getIsOverdueFine()) != 1) {
            throw new BadRequestRestApiException()
                    .developerMessage("Exactly one of credit limit, repayment term, interest rate, or overdue fine must be set")
                    .userMessage("Exactly one of credit limit, repayment term, interest rate, or overdue fine must be set");
        }

        if (createCreditRuleRequest.getIsCreditLimit()) {
            if (createCreditRuleRequest.getCreditLimitPercentage() == null || createCreditRuleRequest.getCreditLimitPercentage().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestRestApiException()
                        .developerMessage("Credit limit percentage must be a positive value")
                        .userMessage("Credit limit percentage must be a positive value");
            }
            if (creditRule.getCustomerType() != createCreditRuleRequest.getCustomerType()) {
                creditRuleRepository.findCreditRuleByCustomerTypeAndCreditLimitAndDeletedIsFalse(
                        createCreditRuleRequest.getCustomerType(),
                        true
                ).ifPresent(existingRule -> {
                    throw new BadRequestRestApiException()
                            .developerMessage("A credit limit rule for this customer type already exists")
                            .userMessage("A credit limit rule for this customer type already exists");
                });
            }
        }

        if (createCreditRuleRequest.getIsRepaymentTerm()) {
            if (createCreditRuleRequest.getRepaymentTermDays() == null || createCreditRuleRequest.getRepaymentTermDays() <= 0) {
                throw new BadRequestRestApiException()
                        .developerMessage("Repayment term days must be a positive integer")
                        .userMessage("Repayment term days must be a positive integer");
            }
            if (creditRule.getCustomerType() != createCreditRuleRequest.getCustomerType()) {
                creditRuleRepository.findCreditRuleByCustomerTypeAndRepaymentTermAndDeletedIsFalse(
                        createCreditRuleRequest.getCustomerType(),
                        true
                ).ifPresent(existingRule -> {
                    throw new BadRequestRestApiException()
                            .developerMessage("A repayment term rule for this customer type already exists")
                            .userMessage("A repayment term rule for this customer type already exists");
                });
            }
        }

        if (createCreditRuleRequest.getIsInterestRate()) {
            if (createCreditRuleRequest.getInterestRatePercentage() == null || createCreditRuleRequest.getInterestRatePercentage().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestRestApiException()
                        .developerMessage("Interest rate percentage must be a positive value")
                        .userMessage("Interest rate percentage must be a positive value");
            }
            if (creditRule.getCustomerType() != createCreditRuleRequest.getCustomerType()) {
                creditRuleRepository.findCreditRuleByCustomerTypeAndInterestRateAndDeletedIsFalse(
                        createCreditRuleRequest.getCustomerType(),
                        true
                ).ifPresent(existingRule -> {
                    throw new BadRequestRestApiException()
                            .developerMessage("An interest rate rule for this customer type already exists")
                            .userMessage("An interest rate rule for this customer type already exists");
                });
            }
        }

        if (createCreditRuleRequest.getIsOverdueFine()) {
            if (createCreditRuleRequest.getOverdueFinePercentage() == null || createCreditRuleRequest.getOverdueFinePercentage().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestRestApiException()
                        .developerMessage("Overdue fine percentage must be a positive value")
                        .userMessage("Overdue fine percentage must be a positive value");
            }
            if (creditRule.getCustomerType() != createCreditRuleRequest.getCustomerType()) {
                creditRuleRepository.findCreditRuleByCustomerTypeAndOverdueFineAndDeletedIsFalse(
                        createCreditRuleRequest.getCustomerType(),
                        true
                ).ifPresent(existingRule -> {
                    throw new BadRequestRestApiException()
                            .developerMessage("An overdue fine rule for this customer type already exists")
                            .userMessage("An overdue fine rule for this customer type already exists");
                });
            }
        }

        Person loggedInUser = getLoggedInUser(name);

        creditRule.setCustomerType(createCreditRuleRequest.getCustomerType());
        creditRule.setRuleName(createCreditRuleRequest.getRuleName());
        creditRule.setCreditLimit(createCreditRuleRequest.getIsCreditLimit());
        creditRule.setCreditLimitPercentage(createCreditRuleRequest.getIsCreditLimit() ? createCreditRuleRequest.getCreditLimitPercentage() : BigDecimal.ZERO);
        creditRule.setRepaymentTerm(createCreditRuleRequest.getIsRepaymentTerm());
        creditRule.setRepaymentTermDays(createCreditRuleRequest.getIsRepaymentTerm() ? createCreditRuleRequest.getRepaymentTermDays() : 0);
        creditRule.setInterestRate(createCreditRuleRequest.getIsInterestRate());
        creditRule.setInterestRatePercentage(createCreditRuleRequest.getIsInterestRate() ? createCreditRuleRequest.getInterestRatePercentage() : BigDecimal.ZERO);
        creditRule.setOverdueFine(createCreditRuleRequest.getIsOverdueFine());
        creditRule.setOverdueFinePercentage(createCreditRuleRequest.getIsOverdueFine() ? createCreditRuleRequest.getOverdueFinePercentage() : BigDecimal.ZERO);
        creditRule.setUpdatedBy(loggedInUser);
        creditRule.setUpdateDate(LocalDateTime.now());

        creditRuleRepository.save(creditRule);


    }

    @Override
    public Page<CreditRule> getAllCreditRules(String name, Pageable pageable) {
        getLoggedInUser(name);
        return creditRuleRepository.findAllByDeletedIsFalse(pageable);
    }

    @Override
    public void deleteCreditRule(UUID publicId, String name) {
        Person loggedInUser = getLoggedInUser(name);
        CreditRule creditRule = creditRuleRepository.findByPublicIdAndDeletedIsFalse(publicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Credit rule not found")
                        .userMessage("Credit rule not found"));
        creditRule.setDeleted(true);
        creditRule.setUpdatedBy(loggedInUser);
        creditRule.setUpdateDate(LocalDateTime.now());
        creditRuleRepository.save(creditRule);
    }

    private Person getLoggedInUser(String name) {
        return personRepository.findByEmailAndDeletedIsFalse(name)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Logged in user not found")
                        .userMessage("Logged in user not found"));
    }
}
