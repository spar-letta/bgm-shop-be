package com.app.itibari.models.entities;

import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
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
@Table(name = "roles", schema = "itibari")
public class Role extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView({BaseView.SummaryView.class, BaseView.RoleView.class, BaseView.UserView.class})
    @Column(name = "name")
    private String roleName;

    @Column(name = "supplier_role")
    private Boolean supplierRole = false;

    @Column(name = "supplier_public_id")
    private String supplierPublicId;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "role_permissions",
            schema = "itibari",
            joinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id", referencedColumnName = "id"))
    private List<Permission> permissions = new ArrayList<>();

    // @JsonView({BaseView.SummaryView.class, BaseView.RoleView.class})
    @Transient
    private List<String> permissionList = new ArrayList<>();

    public void addPermission(Permission permission) {

        Optional<Permission> optionalPermission = permissions.stream()
                .filter(permission1 -> permission1.getPublicId().equals(permission.getPublicId()))
                .findFirst();

        if (optionalPermission.isEmpty()) {
            permissions.add(permission);
        }
    }

    public void removePermission(Permission permission) {

        permissions.removeIf(permission1 -> permission1.getPublicId().equals(permission.getPublicId()));
    }

    public List<String> getPermissionList() {

        permissions.forEach(permission -> permissionList.add(permission.getName()));
        return permissionList;
    }
}
