package com.app.itibari.configuration;

import com.app.itibari.models.entities.Person;
import com.app.itibari.models.entities.Role;
import lombok.AllArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@AllArgsConstructor
public class CustomUser implements UserDetails {
    private final Person person;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();

        if (!person.getRoles().isEmpty()) {
            for (Role role : person.getRoles()) {
                authorities.add(new SimpleGrantedAuthority(role.getRoleName()));
                if (!role.getPermissionList().isEmpty()) {
                    List<GrantedAuthority> grantedAuthorities = role.getPermissionList().stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());
                    authorities.addAll(grantedAuthorities);

                }
            }
        }
        return authorities;
    }

    @Override
    public String getPassword() {
        return person.getPassword();
    }

    @Override
    public String getUsername() {
        return person.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public String getFullName() {
        return person.getFirstName() + " " + person.getLastName();
    }


}
