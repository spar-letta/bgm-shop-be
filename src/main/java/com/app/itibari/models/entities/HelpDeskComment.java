package com.app.itibari.models.entities;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "help_desk_comments", schema = "itibari")
public class HelpDeskComment extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "comment_body")
    private String commentBody;

    @Column(name = "read")
    private Boolean read;

    @Column(name = "is_external")
    private Boolean external = Boolean.FALSE;

    @ManyToOne
    @JoinColumn(name = "help_desk_id_fk", referencedColumnName = "id")
    private HelpDesk helpDesk;
}
