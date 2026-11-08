package com.app.itibari.models.entities;

import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "permission_group", schema = "itibari")
public class PermissionGroup extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView({BaseView.PermissionGroupView.class, BaseView.SummaryView.class})
    @Column(name = "name")
    private String groupName;

    @Column(name = "supplier_role")
    private Boolean supplierRole = false;

    @Column(name = "supplier_public_id")
    private String supplierPublicId;

    @JsonView({BaseView.PermissionGroupView.class, BaseView.SummaryView.class})
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "permissionGroup")
    private List<Permission> permissions = new ArrayList<>();

    public void addPermission(Permission permission) {

        Optional<Permission> existingPermission = permissions.stream()
                .filter(p -> p.getName().equalsIgnoreCase(permission.getName()))
                .findFirst();
        if (existingPermission.isEmpty()) {
            permissions.add(permission);
        }
    }
}
