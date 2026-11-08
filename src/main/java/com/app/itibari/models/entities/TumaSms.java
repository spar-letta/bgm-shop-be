package com.app.itibari.models.entities;

import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "tuma_smses", schema = "itibari")
public class TumaSms extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView(BaseView.TumaSmsView.class)
    @Column(name = "text_message")
    private String textMessage;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "tuma_sms_kwa_kastoma", schema = "itibari",
            joinColumns = @JoinColumn(name = "tuma_sms_id"),
            inverseJoinColumns = @JoinColumn(name = "customa_id"))
    private List<Customers> recipients = new ArrayList<>();

    @JsonView(BaseView.TumaSmsView.class)
    @Column(name = "is_customer")
    private Boolean isCustomer;

    @JsonView(BaseView.TumaSmsView.class)
    @Column(name = "is_sales_agent")
    private Boolean isSalesAgent;

    @JsonView(BaseView.TumaSmsView.class)
    @Column(name = "is_delivery_agent")
    private Boolean isDeliveryAgent;

    @JsonView(BaseView.TumaSmsView.class)
    @Column(name = "is_group")
    private Boolean isGroup;

    @JsonView(BaseView.TumaSmsView.class)
    @Column(name = "phone_number")
    private String phoneNumber;

    @JsonView(BaseView.TumaSmsView.class)
    @Column(name = "recipient_name")
    private String recipientName;

    @JsonView(BaseView.TumaSmsView.class)
    @Column(name = "is_success")
    private Boolean isSuccess;

    @JsonView(BaseView.TumaSmsView.class)
    @Column(name = "is_campaign")
    private Boolean isCampaign;

    @JsonView(BaseView.TumaSmsView.class)
    @Column(name = "campaign_name")
    private String campaignName;

    @JsonView(BaseView.TumaSmsView.class)
    @Column(name = "buy_now_pay_later")
    private boolean buyNowPayLater;
}
