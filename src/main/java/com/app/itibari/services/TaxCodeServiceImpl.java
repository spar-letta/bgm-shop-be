package com.app.itibari.services;

import com.app.itibari.models.entities.Person;
import com.app.itibari.models.entities.TaxCode;
import com.app.itibari.models.request.TaxCodeRequest;
import com.app.itibari.repository.PersonRepository;
import com.app.itibari.repository.TaxCodeRepository;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class TaxCodeServiceImpl implements TaxCodeService {

    private final TaxCodeRepository taxCodeRepository;
    private final PersonRepository personRepository;


    @Override
    public TaxCode createCreateTaxCode(TaxCodeRequest taxCodeRequest, String name) {
        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);

        taxCodeRepository.findByCodeNameIgnoreCase(taxCodeRequest.codeName())
                .ifPresent(existing -> {
                    throw new BadRequestRestApiException()
                            .developerMessage("Tax code already exists")
                            .userMessage("Sorry, Tax code already exists");
                });

        TaxCode newTaxCode = new TaxCode();
        newTaxCode.setCodeName(taxCodeRequest.codeName());
        newTaxCode.setCodeValue(BigDecimal.valueOf(taxCodeRequest.codeValue()).divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP));

        int codeNumber = getNextAvailableCode();
        newTaxCode.setCode(codeNumber);


        personOptional.ifPresent(newTaxCode::setCreatedBy);

        return taxCodeRepository.save(newTaxCode);
    }

    @Override
    public Page<TaxCode> getTaxCodes(String name, Pageable pageable) {
        List<TaxCode> allTaxCodes = taxCodeRepository.findByDeletedIsFalse();
        return new PageImpl<>(allTaxCodes, pageable, allTaxCodes.size());
    }

    @Override
    public TaxCode fetchOneTaxCode(UUID taxCodePublicId) {
        TaxCode taxCode = taxCodeRepository.findByPublicId(taxCodePublicId).orElse(null);
        if (taxCode == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Tax code not found")
                    .userMessage("Sorry, Tax code not found");
        }
        return taxCode;
    }

    @Override
    public TaxCode updateTaxCode(UUID taxCodePublicId, TaxCodeRequest taxCodeRequest, String name) {
        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
        TaxCode taxCode = fetchOneTaxCode(taxCodePublicId);

        taxCode.setCodeName(taxCodeRequest.codeName());
        taxCode.setCodeValue(BigDecimal.valueOf(taxCodeRequest.codeValue()).divide(BigDecimal.valueOf(100),RoundingMode.HALF_UP));
        personOptional.ifPresent(taxCode::setUpdatedBy);
        return taxCodeRepository.save(taxCode);
    }

    @Override
    public void deleteTaxCode(UUID taxCodePublicId, String name) {
        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
        TaxCode taxCode = fetchOneTaxCode(taxCodePublicId);
        taxCode.setDeleted(true);
        personOptional.ifPresent(taxCode::setUpdatedBy);
        taxCodeRepository.save(taxCode);
    }

    public int getNextAvailableCode() {
        List<Integer> codes = taxCodeRepository.findAllByDeletedIsFalseOrderByCodeAsc()
                .stream()
                .map(TaxCode::getCode)
                .toList();

        int expected = 1;
        for (Integer code : codes) {
            if (code != expected) {
                return expected;
            }
            expected++;
        }
        return expected;
    }

}
