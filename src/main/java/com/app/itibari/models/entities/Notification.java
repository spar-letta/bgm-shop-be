package com.app.itibari.models.entities;

import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "notification", schema = "itibari")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView(BaseView.NotificationView.class)
    @Column(name = "public_id")
    private UUID publicId;

    @JsonView(BaseView.NotificationView.class)
    @Column(name = "title")
    private String title;

    @JsonView(BaseView.NotificationView.class)
    @Column(name = "message")
    private String message;

    @JsonView(BaseView.NotificationView.class)
    @Column(name = "is_read")
    private boolean isRead = false;

    @JsonView(BaseView.NotificationView.class)
    @Column(name = "is_deleted")
    private boolean isDeleted = false;

    @JsonView(BaseView.NotificationView.class)
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @JsonView(BaseView.NotificationView.class)
    @Column(name = "entity_public_id")
    private UUID entityPublicId;

    @JsonView(BaseView.NotificationView.class)
    @Column(name = "entity_type")
    private String entityType;

    @JsonView(BaseView.NotificationView.class)
    @Column(name = "required_action")
    private String requiredAction;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "notification_roles", schema = "itibari",
            joinColumns = @JoinColumn(name = "notification_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"))
    private List<Role> roles = new ArrayList<>();

    public boolean containsRole(Role role) {

        return roles.stream().anyMatch(r -> r.getPublicId().equals(role.getPublicId()));
    }

    // add role

    public void addRole(Role role) {

        if (roles.stream().noneMatch(r -> r.getPublicId().equals(role.getPublicId()))) {
            roles.add(role);
        }
    }
}
