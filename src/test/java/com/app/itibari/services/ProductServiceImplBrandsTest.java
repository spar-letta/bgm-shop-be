package com.app.itibari.services;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ProductServiceImplBrandsTest {

    @Test
    public void normalizeDistinctBrands_trimsWhitespaceAndRemovesExactDuplicates() {
        List<String> brands = Arrays.asList("Coca-Cola", " Coca-Cola", "Coca-Cola ", "Korie", "Korie");

        List<String> result = ProductServiceImpl.normalizeDistinctBrands(brands);

        assertEquals(List.of("Coca-Cola", "Korie"), result);
    }

    @Test
    public void normalizeDistinctBrands_deduplicatesCaseInsensitiveVariants() {
        List<String> brands = Arrays.asList("MARHABA", "Marhaba", "marhaba");

        List<String> result = ProductServiceImpl.normalizeDistinctBrands(brands);

        assertEquals(1, result.size());
        assertEquals("MARHABA", result.get(0));
    }
}
