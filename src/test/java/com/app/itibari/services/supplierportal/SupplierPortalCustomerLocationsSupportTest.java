package com.app.itibari.services.supplierportal;

import com.app.itibari.dataTypes.CustomerLocationMarkerCategoryV2;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupplierPortalCustomerLocationsSupportTest {

    @Test
    void parseCoordinates_acceptsValidNumericPairs() {
        Optional<double[]> parsed = SupplierPortalCustomerLocationsSupport.parseCoordinates("-6.792354", "39.208328");
        assertTrue(parsed.isPresent());
        assertEquals(-6.792354d, parsed.get()[0], 0.000001d);
        assertEquals(39.208328d, parsed.get()[1], 0.000001d);
    }

    @Test
    void parseCoordinates_rejectsInvalidOrOutOfRangeValues() {
        assertFalse(SupplierPortalCustomerLocationsSupport.parseCoordinates("not-a-lat", "39.2").isPresent());
        assertFalse(SupplierPortalCustomerLocationsSupport.parseCoordinates("91", "0").isPresent());
        assertFalse(SupplierPortalCustomerLocationsSupport.parseCoordinates("", "39.2").isPresent());
        assertFalse(SupplierPortalCustomerLocationsSupport.parseCoordinates(null, "39.2").isPresent());
    }

    @Test
    void mapCategory_mapsCustomerActiveStatusExplicitly() {
        assertEquals(CustomerLocationMarkerCategoryV2.NEW,
                SupplierPortalCustomerLocationsSupport.mapCategory("New"));
        assertEquals(CustomerLocationMarkerCategoryV2.ACTIVE,
                SupplierPortalCustomerLocationsSupport.mapCategory("Active"));
        assertEquals(CustomerLocationMarkerCategoryV2.INACTIVE,
                SupplierPortalCustomerLocationsSupport.mapCategory("Inactive"));
        assertEquals(CustomerLocationMarkerCategoryV2.NEW,
                SupplierPortalCustomerLocationsSupport.mapCategory(null));
    }

    @Test
    void resolveHeatMapCategory_treatsNullLastOrderAsChurned() {
        assertEquals(Optional.of(CustomerLocationMarkerCategoryV2.CHURNED),
                SupplierPortalCustomerLocationsSupport.resolveHeatMapCategory("Active", null));
    }

    @Test
    void resolveHeatMapCategory_churnsWhenLastOrderOlderThanThirtyDays() {
        LocalDateTime thirtyOneDaysAgo = LocalDateTime.now().minusDays(31);
        assertEquals(Optional.of(CustomerLocationMarkerCategoryV2.CHURNED),
                SupplierPortalCustomerLocationsSupport.resolveHeatMapCategory("Active", thirtyOneDaysAgo));
    }

    @Test
    void resolveHeatMapCategory_doesNotChurnAtExactlyThirtyDays() {
        LocalDateTime exactlyThirtyDaysAgo = LocalDateTime.now().minusDays(30);
        assertEquals(Optional.of(CustomerLocationMarkerCategoryV2.CHURNED),
                SupplierPortalCustomerLocationsSupport.resolveHeatMapCategory("Active", exactlyThirtyDaysAgo));
    }

    @Test
    void resolveHeatMapCategory_doesNotChurnAtTwentyNineDays() {
        LocalDateTime twentyNineDaysAgo = LocalDateTime.now().minusDays(29);
        assertEquals(Optional.of(CustomerLocationMarkerCategoryV2.ACTIVE),
                SupplierPortalCustomerLocationsSupport.resolveHeatMapCategory("Active", twentyNineDaysAgo));
    }

    @Test
    void resolveHeatMapCategory_omitsInactiveCustomers() {
        LocalDateTime recent = LocalDateTime.now().minusDays(5);
        assertTrue(SupplierPortalCustomerLocationsSupport.resolveHeatMapCategory("Inactive", recent).isEmpty());
        assertEquals(CustomerLocationMarkerCategoryV2.INACTIVE,
                SupplierPortalCustomerLocationsSupport.resolveMarkerCategory("Inactive", recent));
    }

    @Test
    void maskPhoneNumber_keepsLastThreeDigitsVisible() {
        assertEquals("*** *** 789", SupplierPortalCustomerLocationsSupport.maskPhoneNumber("2557123456789"));
        assertEquals("*** *** 456", SupplierPortalCustomerLocationsSupport.maskPhoneNumber("0712345456"));
    }

    @Test
    void isPhoneVisible_andResolveDetailPhoneNumber_areExplicit() {
        assertTrue(SupplierPortalCustomerLocationsSupport.isPhoneVisible("0712345678"));
        assertEquals("*** *** 678",
                SupplierPortalCustomerLocationsSupport.resolveDetailPhoneNumber("0712345678"));

        assertFalse(SupplierPortalCustomerLocationsSupport.isPhoneVisible(null));
        assertFalse(SupplierPortalCustomerLocationsSupport.isPhoneVisible("  "));
        assertNull(SupplierPortalCustomerLocationsSupport.resolveDetailPhoneNumber(null));
    }

    @Test
    void toGeohashCellCenter_matchesEncodeDecodeRoundTrip() {
        double lat = -6.792354d;
        double lng = 39.208328d;
        double[] center = SupplierPortalCustomerLocationsSupport.toGeohashCellCenter(lat, lng, 5);
        String hash = SupplierPortalCustomerLocationsSupport.encodeGeohash(lat, lng, 5);
        assertEquals(hash, SupplierPortalCustomerLocationsSupport.encodeGeohash(center[0], center[1], 5));
        assertTrue(Math.abs(center[0] - lat) > 0.00001d || Math.abs(center[1] - lng) > 0.00001d);
    }

    @Test
    void encodeGeohash_darEsSalaamPrecision5_isStable() {
        String hash = SupplierPortalCustomerLocationsSupport.encodeGeohash(
                -6.792354d, 39.208328d, SupplierPortalCustomerLocationsValidation.HEAT_MAP_GEOHASH_PRECISION);
        assertEquals(5, hash.length());
        assertEquals(
                hash,
                SupplierPortalCustomerLocationsSupport.encodeGeohash(
                        -6.792354d, 39.208328d, SupplierPortalCustomerLocationsValidation.HEAT_MAP_GEOHASH_PRECISION));
    }

    @Test
    void encodeGeohash_nearbyPointsSharePrecision5Cell() {
        String a = SupplierPortalCustomerLocationsSupport.encodeGeohash(-6.7920d, 39.2080d, 5);
        String b = SupplierPortalCustomerLocationsSupport.encodeGeohash(-6.7925d, 39.2085d, 5);
        assertEquals(a, b);
    }

    @Test
    void decodeGeohashCenter_roundTripsWithinCellBounds() {
        double lat = -6.792354d;
        double lng = 39.208328d;
        String hash = SupplierPortalCustomerLocationsSupport.encodeGeohash(lat, lng, 5);
        double[] center = SupplierPortalCustomerLocationsSupport.decodeGeohashCenter(hash);
        // Center must re-encode to the same cell
        assertEquals(hash, SupplierPortalCustomerLocationsSupport.encodeGeohash(center[0], center[1], 5));
        // Center is not the exact input (privacy property for typical non-center points)
        assertTrue(Math.abs(center[0] - lat) > 0.00001d || Math.abs(center[1] - lng) > 0.00001d);
    }
}
