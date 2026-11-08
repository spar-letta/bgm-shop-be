package com.app.itibari.converter;

import com.app.itibari.models.entities.HelpDeskComment;
import com.app.itibari.models.response.HelpDeskCommentResponseDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class HelpDeskCommentToDtoResponse implements Converter<HelpDeskComment, HelpDeskCommentResponseDTO.CommentDto> {

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE dd, MMM yyyy");
    DateTimeFormatter formatterTime = DateTimeFormatter.ofPattern("HH:mm a");

    @Override
    public HelpDeskCommentResponseDTO.CommentDto convert(HelpDeskComment helpDeskComment) {
        if (helpDeskComment == null) {
            return null;
        }

        LocalDate dateCreated = helpDeskComment.getCreationTime().toLocalDate();
        String dateString = dateCreated.format(formatter);
        LocalDateTime creationTime = helpDeskComment.getCreationTime();
        String timeString = creationTime.format(formatterTime);

        String raisedBy = helpDeskComment.getCreatedBy() != null
                ? helpDeskComment.getCreatedBy().getFullName()
                : null;

        return HelpDeskCommentResponseDTO.CommentDto.builder()
                .publicId(helpDeskComment.getPublicId())
                .content(helpDeskComment.getCommentBody())
                .createdByPublicId(helpDeskComment.getCreatedBy().getPublicId())
                .createdByName(raisedBy)
                .helpDeskPublicId(helpDeskComment.getHelpDesk().getPublicId())
                .createdDate(dateString)
                .read(helpDeskComment.getRead())
                .isExternal(helpDeskComment.getExternal())
                .createdTime(timeString).build();
    }
}
