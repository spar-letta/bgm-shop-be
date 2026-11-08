package com.app.itibari.services.helper;

import com.app.itibari.dataTypes.TaxType;
import com.app.itibari.models.entities.TaxMapping;
import com.app.itibari.repository.TaxMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TaxCalculatorHelper {

    private final TaxMappingRepository taxMappingRepository;

    public BigDecimal calculateTax(UUID taxPublicId, BigDecimal amount) {

        BigDecimal taxAmount = BigDecimal.ZERO;

        Optional<TaxMapping> optionalTaxMapping = taxMappingRepository.findByPublicIdAndDeletedIsFalse(taxPublicId);

        if (optionalTaxMapping.isEmpty()) {
            return taxAmount;
        }

        TaxMapping taxMapping = optionalTaxMapping.get();

        if (taxMapping.getTaxType() == TaxType.Fixed) {
            taxAmount = taxMapping.getTaxRate().multiply(amount).setScale(4, RoundingMode.HALF_UP);
        } else if (taxMapping.getTaxType() == TaxType.Percentage) {
            taxAmount = amount.multiply(taxMapping.getTaxRate()
                                                .divide(new BigDecimal(100), RoundingMode.HALF_UP)).setScale(4, RoundingMode.HALF_UP);
        }

        return taxAmount;
    }

    // amount after tax
    public BigDecimal calculateAmountAfterTax(UUID taxPublicId, BigDecimal priceAmount) {

        BigDecimal amount = BigDecimal.ZERO;

        Optional<TaxMapping> optionalTaxMapping = taxMappingRepository.findByPublicIdAndDeletedIsFalse(taxPublicId);

        if (optionalTaxMapping.isEmpty()) {
            return priceAmount;
        }

        TaxMapping taxMapping = optionalTaxMapping.get();

        // tax 18%
        // amount afet tax = amount/ (1 + taxRate/100)

        if (taxMapping.getTaxRate() == null || taxMapping.getTaxRate().compareTo(BigDecimal.ZERO) <= 0) {
            return priceAmount;
        }

        if (taxMapping.getTaxType() == TaxType.Fixed) {
            amount = priceAmount.subtract(taxMapping.getTaxRate())
                    .setScale(4, RoundingMode.HALF_UP);
        } else if (taxMapping.getTaxType() == TaxType.Percentage) {
            amount = priceAmount.divide(new BigDecimal(1).add(taxMapping.getTaxRate()
                                                                      .divide(new BigDecimal(100), RoundingMode.HALF_UP)),
                                        RoundingMode.HALF_UP).setScale(4, RoundingMode.HALF_UP);
        }

        return amount;
    }
}
