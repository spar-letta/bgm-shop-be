package com.app.itibari.configuration;

import com.app.itibari.models.entities.Person;
import com.app.itibari.models.entities.Role;
import com.app.itibari.repository.PersonRepository;
import com.app.itibari.services.helper.SupplierPortalAccessHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserService implements UserDetailsService {
    private final PersonRepository personRepository;
    private final SupplierPortalAccessHelper supplierPortalAccessHelper;

    public UserService(PersonRepository personRepository,
                       @Lazy SupplierPortalAccessHelper supplierPortalAccessHelper) {
        this.personRepository = personRepository;
        this.supplierPortalAccessHelper = supplierPortalAccessHelper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Person person = personRepository.findByEmailAndDeletedIsFalse(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (Boolean.TRUE.equals(person.getHasSupplierAccount()) && person.getSupplierPublicId() != null) {
            supplierPortalAccessHelper.ensureSupplierPortalRolePermissions(person.getSupplierPublicId());
            person = personRepository.findByEmailAndDeletedIsFalse(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        }

        log.info("permissionList {} ",person.getRoles().stream().map(Role::getPermissionList).toList());

        return new CustomUser(person);
    }
}
