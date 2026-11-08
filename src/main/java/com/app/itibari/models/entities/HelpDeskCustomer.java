package com.app.itibari.models.entities;

import com.app.itibari.models.request.TicketDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "help_desk_customers", schema = "itibari")
public class HelpDeskCustomer extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type")
    private TicketDto.CustomerType customerType;

    @Column(name = "district")
    private String district;

    @Column(name = "ward")
    private String ward;

    @Column(name = "district_id")
    private String districtId;

    @Column(name = "ward_id")
    private String wardId;

    @Column(name = "postal_address")
    private String postalAddress;

    @Column(name = "existing_customer_public_id")
    private UUID existingCustomerPublicId;

    @Column(name = "store_name")
    private String storeName;

    @OneToMany(mappedBy = "helpDeskCustomer")
    private List<HelpDesk> helpDesks = new ArrayList<>();
}
