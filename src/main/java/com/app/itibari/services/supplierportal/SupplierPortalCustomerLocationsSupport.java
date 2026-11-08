package com.app.itibari.services.supplierportal;

import com.app.itibari.dataTypes.CustomerActiveStatus;
import com.app.itibari.dataTypes.CustomerLocationMarkerCategoryV2;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Shared parsing/mapping helpers for Supplier Portal customer locations V2.
 */
public final class SupplierPortalCustomerLocationsSupport {

    private static final String GEOHASH_BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz";

    private SupplierPortalCustomerLocationsSupport() {
    }

    /**
     * Encodes WGS84 coordinates to a geohash of the given precision (1–12).
     * Shared by heat-map aggregation and privacy-preserving marker pins so exact
     * customer coordinates never leave the API.
     */
    public static String encodeGeohash(double latitude, double longitude, int precision) {
        if (precision < 1 || precision > 12) {
            throw new IllegalArgumentException("geohash precision must be between 1 and 12");
        }
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)
                || latitude < -90.0d || latitude > 90.0d
                || longitude < -180.0d || longitude > 180.0d) {
            throw new IllegalArgumentException("latitude/longitude out of range for geohash");
        }

        double[] latInterval = {-90.0d, 90.0d};
        double[] lngInterval = {-180.0d, 180.0d};
        StringBuilder hash = new StringBuilder(precision);
        boolean evenBit = true;
        int bit = 0;
        int ch = 0;

        while (hash.length() < precision) {
            if (evenBit) {
                double mid = (lngInterval[0] + lngInterval[1]) / 2.0d;
                if (longitude >= mid) {
                    ch = (ch << 1) | 1;
                    lngInterval[0] = mid;
                } else {
                    ch = ch << 1;
                    lngInterval[1] = mid;
                }
            } else {
                double mid = (latInterval[0] + latInterval[1]) / 2.0d;
                if (latitude >= mid) {
                    ch = (ch << 1) | 1;
                    latInterval[0] = mid;
                } else {
                    ch = ch << 1;
                    latInterval[1] = mid;
                }
            }
            evenBit = !evenBit;
            if (++bit == 5) {
                hash.append(GEOHASH_BASE32.charAt(ch));
                bit = 0;
                ch = 0;
            }
        }
        return hash.toString();
    }

    /**
     * Returns the geographic center of a geohash cell as {@code [lat, lng]}.
     */
    public static double[] decodeGeohashCenter(String geohash) {
        if (geohash == null || geohash.isBlank()) {
            throw new IllegalArgumentException("geohash required");
        }
        String normalized = geohash.trim().toLowerCase();
        double[] latInterval = {-90.0d, 90.0d};
        double[] lngInterval = {-180.0d, 180.0d};
        boolean evenBit = true;

        for (int i = 0; i < normalized.length(); i++) {
            int cd = GEOHASH_BASE32.indexOf(normalized.charAt(i));
            if (cd < 0) {
                throw new IllegalArgumentException("invalid geohash character: " + normalized.charAt(i));
            }
            for (int mask = 16; mask > 0; mask >>= 1) {
                if (evenBit) {
                    refineInterval(lngInterval, (cd & mask) != 0);
                } else {
                    refineInterval(latInterval, (cd & mask) != 0);
                }
                evenBit = !evenBit;
            }
        }
        return new double[]{
                (latInterval[0] + latInterval[1]) / 2.0d,
                (lngInterval[0] + lngInterval[1]) / 2.0d
        };
    }

    /**
     * Privacy-preserving coordinates for map APIs: encode to a geohash cell then return that
     * cell's center. Same precision as heat-map cells; markers use this without a count.
     */
    public static double[] toGeohashCellCenter(double latitude, double longitude, int precision) {
        return decodeGeohashCenter(encodeGeohash(latitude, longitude, precision));
    }

    private static void refineInterval(double[] interval, boolean upperHalf) {
        double mid = (interval[0] + interval[1]) / 2.0d;
        if (upperHalf) {
            interval[0] = mid;
        } else {
            interval[1] = mid;
        }
    }

    public static Optional<double[]> parseCoordinates(String latitude, String longitude) {
        if (latitude == null || longitude == null) {
            return Optional.empty();
        }
        String latRaw = latitude.trim();
        String lngRaw = longitude.trim();
        if (latRaw.isEmpty() || lngRaw.isEmpty()) {
            return Optional.empty();
        }
        try {
            double lat = Double.parseDouble(latRaw);
            double lng = Double.parseDouble(lngRaw);
            if (!Double.isFinite(lat) || !Double.isFinite(lng)) {
                return Optional.empty();
            }
            if (lat < -90.0d || lat > 90.0d || lng < -180.0d || lng > 180.0d) {
                return Optional.empty();
            }
            return Optional.of(new double[]{lat, lng});
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    /**
     * Categories returned by the customer-locations heat-map API.
     * {@link CustomerLocationMarkerCategoryV2#INACTIVE} is intentionally excluded — businesses do not
     * surface inactive customers on this map (markers/detail may still use INACTIVE).
     */
    public static final List<CustomerLocationMarkerCategoryV2> HEAT_MAP_CATEGORIES = List.of(
            CustomerLocationMarkerCategoryV2.NEW,
            CustomerLocationMarkerCategoryV2.ACTIVE,
            CustomerLocationMarkerCategoryV2.CHURNED);

    public static boolean isHeatMapCategory(CustomerLocationMarkerCategoryV2 category) {
        return category != null && category != CustomerLocationMarkerCategoryV2.INACTIVE;
    }

    public static CustomerLocationMarkerCategoryV2 mapCategory(String customerActiveStatus) {
        if (customerActiveStatus == null || customerActiveStatus.isBlank()) {
            return CustomerLocationMarkerCategoryV2.NEW;
        }
        String normalized = customerActiveStatus.trim();
        if (CustomerActiveStatus.New.name().equalsIgnoreCase(normalized)) {
            return CustomerLocationMarkerCategoryV2.NEW;
        }
        if (CustomerActiveStatus.Active.name().equalsIgnoreCase(normalized)) {
            return CustomerLocationMarkerCategoryV2.ACTIVE;
        }
        if (CustomerActiveStatus.Inactive.name().equalsIgnoreCase(normalized)) {
            return CustomerLocationMarkerCategoryV2.INACTIVE;
        }
        return CustomerLocationMarkerCategoryV2.NEW;
    }

    /**
     * Marker category including {@link CustomerLocationMarkerCategoryV2#INACTIVE}.
     * Applies the 30-day churn override used by the markers endpoint.
     */
    public static CustomerLocationMarkerCategoryV2 resolveMarkerCategory(String activeStatus,
                                                                         LocalDateTime lastSupplierOrder) {
        if (lastSupplierOrder == null
                || lastSupplierOrder.isBefore(LocalDateTime.now().minusDays(30))) {
            return CustomerLocationMarkerCategoryV2.CHURNED;
        }
        return mapCategory(activeStatus);
    }

    /**
     * Heat-map bucket for a snapshot row. Returns empty when the customer is inactive so they are
     * omitted from heat-map cells and counts (not remapped into another category).
     */
    public static Optional<CustomerLocationMarkerCategoryV2> resolveHeatMapCategory(String activeStatus,
                                                                                    LocalDateTime lastSupplierOrder) {
        CustomerLocationMarkerCategoryV2 category = resolveMarkerCategory(activeStatus, lastSupplierOrder);
        if (category == CustomerLocationMarkerCategoryV2.INACTIVE) {
            return Optional.empty();
        }
        return Optional.of(category);
    }

    /**
     * Masks a phone number for supplier portal display — keeps the last three digits visible.
     */
    public static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return null;
        }
        String digits = phoneNumber.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return null;
        }
        if (digits.length() <= 3) {
            return "***";
        }
        String suffix = digits.substring(digits.length() - 3);
        return "*** *** " + suffix;
    }

    /**
     * Whether the detail response should expose the phone field (masked). Independent of {@link #maskPhoneNumber(String)}.
     */
    public static boolean isPhoneVisible(String rawPhoneNumber) {
        return rawPhoneNumber != null && !rawPhoneNumber.isBlank();
    }

    public static String resolveDetailPhoneNumber(String rawPhoneNumber) {
        if (!isPhoneVisible(rawPhoneNumber)) {
            return null;
        }
        return maskPhoneNumber(rawPhoneNumber);
    }

    public static String normalizeCustomerName(Object raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.toString().trim();
        return value.isEmpty() ? null : value;
    }
}
