package com.app.itibari.services.helper;

import com.app.itibari.models.entities.Notification;
import com.app.itibari.models.entities.Person;
import com.app.itibari.models.entities.Role;
import com.app.itibari.repository.NotificationRepository;
import com.app.itibari.repository.PersonRepository;
import com.app.itibari.repository.RoleRepository;
import com.app.itibari.services.ItibariSmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationHelper {

    private final PersonRepository personRepository;

    private final RoleRepository roleRepository;

    private final ItibariSmsService itibariSmsService;

    private final NotificationRepository notificationRepository;

    public void sendNotificationSms(String message, String role) {

        Optional<Role> roleOptional = roleRepository.findByRoleNameAndDeletedIsFalse(role);

        if (roleOptional.isEmpty()) {
            log.error("Role not found: {}", role);
            return;
        }

        Role foundRole = roleOptional.get();

        List<Person> persons = personRepository.findAllByRolesContaining(foundRole);

        if (persons.isEmpty()) {
            log.error("No persons found with role: {}", role);
            return;
        }

        for (Person person : persons) {
            var phoneNumber = person.getPhoneNumber();
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                itibariSmsService.send(phoneNumber, message);
            } else {
                log.warn("Person {} does not have a valid phone number", person.getPublicId());
            }
        }
    }

    public void createNotificationHelper(String title, String message, String role) {

        createNotificationHelper(title, message, role, null, null, null);
    }

    public void createNotificationHelper(String title,
                                         String message,
                                         String role,
                                         UUID entityPublicId,
                                         String entityType,
                                         String requiredAction) {

        Optional<Role> roleOptional = roleRepository.findByRoleNameAndDeletedIsFalse(role);

        if (roleOptional.isEmpty()) {
            log.error("Cannot create notification. Role not found: {}", role);
            return;
        }

        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setPublicId(UUID.randomUUID());
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRead(false);
        notification.setDeleted(false);
        notification.setEntityPublicId(entityPublicId);
        notification.setEntityType(entityType);
        notification.setRequiredAction(requiredAction);
        notification.addRole(roleOptional.get());
        notificationRepository.save(notification);
    }
}
