package com.app.itibari.converter;

import com.app.itibari.models.entities.HelpDeskNotification;
import com.app.itibari.models.response.HelpDeskNotificationResponseDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class HelpDeskNotificationToDtoResponse implements Converter<HelpDeskNotification,
        HelpDeskNotificationResponseDTO.DeskDto> {

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE dd, MMM yyyy");
    DateTimeFormatter formatterTime = DateTimeFormatter.ofPattern("HH:mm a");

    @Override
    public HelpDeskNotificationResponseDTO.DeskDto convert(HelpDeskNotification helpDeskNotification) {

        if (helpDeskNotification == null){
            return null;
        }

        LocalDate dateCreated = helpDeskNotification.getCreationTime().toLocalDate();
        String dateString = dateCreated.format(formatter);
        LocalDateTime creationTime = helpDeskNotification.getCreationTime();
        String timeString = creationTime.format(formatterTime);

        return HelpDeskNotificationResponseDTO.DeskDto.builder()
                .createdDate(dateString)
                .createdTime(timeString)
                .title(helpDeskNotification.getMessageAction())
                .read(helpDeskNotification.getRead())
                .publicId(helpDeskNotification.getPublicId())
                .build();
    }
}
