package com.app.itibari.services.supplierportal;

import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupplierPortalGeographyQueryParamsTest {

    private static final UUID ID_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ID_B = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ID_C = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Test
    void parseUuidList_nullOrEmptyReturnsEmptyList() {
        assertTrue(SupplierPortalGeographyQueryParams.parseUuidList((List<String>) null, "regionId").isEmpty());
        assertTrue(SupplierPortalGeographyQueryParams.parseUuidList(List.of(), "regionId").isEmpty());
    }

    @Test
    void parseUuidList_singleValue() {
        assertEquals(List.of(ID_A), SupplierPortalGeographyQueryParams.parseUuidList(
                List.of(ID_A.toString()), "regionId"));
    }

    @Test
    void parseUuidList_commaSeparatedValues() {
        assertEquals(List.of(ID_A, ID_B, ID_C), SupplierPortalGeographyQueryParams.parseUuidList(
                List.of(ID_A + "," + ID_B + "," + ID_C), "regionId"));
    }

    @Test
    void parseUuidList_bracketedValues() {
        assertEquals(List.of(ID_A, ID_B, ID_C), SupplierPortalGeographyQueryParams.parseUuidList(
                List.of("[" + ID_A + "," + ID_B + "," + ID_C + "]"), "regionId"));
    }

    @Test
    void parseUuidList_repeatedParams() {
        assertEquals(List.of(ID_A, ID_B), SupplierPortalGeographyQueryParams.parseUuidList(
                List.of(ID_A.toString(), ID_B.toString()), "regionId"));
    }

    @Test
    void parseUuidList_arrayKeyParams() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("regionId[]", ID_A.toString());
        params.add("regionId[]", ID_B.toString());

        assertEquals(List.of(ID_A, ID_B), SupplierPortalGeographyQueryParams.parseUuidList(params, "regionId"));
    }

    @Test
    void parseUuidList_combinesNormalAndArrayKeyParams() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("locationId", "[" + ID_A + "," + ID_B + "]");
        params.add("locationId[]", ID_C.toString());

        assertEquals(List.of(ID_A, ID_B, ID_C), SupplierPortalGeographyQueryParams.parseUuidList(params, "locationId"));
    }

    @Test
    void parseUuidList_mixedRepeatedAndCommaSeparated() {
        assertEquals(List.of(ID_A, ID_B, ID_C), SupplierPortalGeographyQueryParams.parseUuidList(
                List.of("[" + ID_A + "," + ID_B + "]", ID_C.toString()), "regionId"));
    }

    @Test
    void parseUuidList_trimsWhitespaceAndDedupes() {
        assertEquals(List.of(ID_A, ID_B), SupplierPortalGeographyQueryParams.parseUuidList(
                List.of("  " + ID_A + " , " + ID_B + " , " + ID_A + "  "), "regionId"));
    }

    @Test
    void parseUuidList_rejectsInvalidUuid() {
        assertThrows(BadRequestRestApiException.class, () ->
                SupplierPortalGeographyQueryParams.parseUuidList(List.of("not-a-uuid"), "regionId"));
    }
}
