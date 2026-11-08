package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.WhatsAppStatus;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "whats_app_responses", schema = "itibari")
public class WhatsAppResponse extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonView(BaseView.WhatsAppView.class)
    @Column(name = "phone_number")
    private String phoneNumber;

    @JsonView(BaseView.WhatsAppView.class)
    @Column(name = "message")
    private String message;

    @JsonView(BaseView.WhatsAppView.class)
    @Column(name = "customer_name")
    private String customerName;

    @JsonView(BaseView.WhatsAppView.class)
    @Column(name = "source_type")
    private String sourceType;

    @JsonView(BaseView.WhatsAppView.class)
    @Column(name = "message_id")
    private String messageId;

    @JsonView(BaseView.WhatsAppView.class)
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private WhatsAppStatus status;

    @JsonView(BaseView.WhatsAppView.class)
    @Column(name = "reply_by_name")
    private String replyByName;

    @JsonView(BaseView.WhatsAppView.class)
    @Column(name = "is_cx_reply")
    private boolean isCxReply;

    @JsonView(BaseView.WhatsAppView.class)
    @Column(name = "cx_read")
    private boolean cxRead;

    @JsonView(BaseView.WhatsAppView.class)
    @Column(name = "store_name")
    private String storeName;

    @JsonView(BaseView.WhatsAppView.class)
    @Column(name = "message_type")
    private String messageType;

    @JsonView(BaseView.WhatsAppView.class)
    @Column(name = "media_mime_type")
    private String mediaMimeType;

    @Transient
    @JsonView(BaseView.WhatsAppView.class)
    private String mediaUrl;

}
