package com.app.itibari.services.supplierportal;

public final class SupplierPortalUserLimits {

    public static final int MAX_PORTAL_USERS_PER_SUPPLIER = 4;

    public static final int MAX_TEAM_MEMBERS_PER_SUPPLIER = MAX_PORTAL_USERS_PER_SUPPLIER - 1;

    private SupplierPortalUserLimits() {
    }
}
