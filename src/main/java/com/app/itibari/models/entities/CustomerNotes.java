package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.NoteType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Optional;

@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "customer_notes", schema = "itibari")
public class CustomerNotes extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "content")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "note_type")
    private NoteType noteType;

    @ManyToOne
    @JoinColumn(name = "customer_id_fk", referencedColumnName = "id")
    private Customers customer;

    @ManyToOne
    @JoinColumn(name = "reason_code_id_fk", referencedColumnName = "id")
    private RejectionsCode reasonCode;

    @ManyToOne
    @JoinColumn(name = "bnpl_reason_code_id_fk", referencedColumnName = "id")
    private BnplReasonCode bnplReasonCode;

    @ManyToOne
    @JoinColumn(name = "bnpl_lead_id_fk", referencedColumnName = "id")
    private BnplLead bnplLead;

    @Column(name = "notification_send")
    private boolean notificationSend;

    public String getCodeDescription() {
        if (reasonCode == null) {
            return "--";
        }

        String code = Optional.ofNullable(reasonCode.getCode()).orElse("");
        String description = Optional.ofNullable(reasonCode.getDescription()).orElse("");

        if (!description.isEmpty()) {
            return code.isEmpty() ? description : code + " - " + description;
        }
        return code.isEmpty() ? "--" : code;
    }


}
