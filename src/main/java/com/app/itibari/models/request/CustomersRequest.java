package com.app.itibari.models.request;

import com.app.itibari.dataTypes.CustomerType;
import com.app.itibari.dataTypes.SourceChannelEnum;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Setter
@Getter
@Builder
public class CustomersRequest {

    private String name;

    private String email;

    private String nationalId;

    private String phoneNumber;

    private LocalDate registrationDate;

    private SourceChannelEnum sourceChannel;

    private String latitude;

    private String longitude;

    private String address;

    private String landmark;

    private String tin;

    private UUID organizationRegionId;

    private UUID organizationLocationId;

    private boolean selfRegistered;

    private boolean parentCustomerRegistered;

    private boolean saleAgentRegistered;

    private CustomerType customerType;

    private String firstName;

    private String lastName;

    private String storeName;

    private UUID parentCustomerPublicId;

    private String ownerNumber;

    private String shopKeeperNumber;

    private Boolean hasOtherStores;

    private Boolean canChildCustomerLogin;

    private Boolean canChildCustomerPlaceOrder;


    public boolean isHasOtherStores() {
        return hasOtherStores != null && hasOtherStores;
    }


    public Boolean getCanChildCustomerLogin() {
        return canChildCustomerLogin != null ? canChildCustomerLogin : true;
    }

    public Boolean getCanChildCustomerPlaceOrder() {
        return canChildCustomerPlaceOrder != null ? canChildCustomerPlaceOrder : true;
    }
}
