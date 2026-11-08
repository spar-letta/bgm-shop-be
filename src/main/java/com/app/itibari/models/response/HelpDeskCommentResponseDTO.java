package com.app.itibari.models.response;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record HelpDeskCommentResponseDTO(Long unReadComment, List<CommentDto> commentDtoList) {

    @Builder
    public record CommentDto(UUID publicId,
                             String content,
                             UUID createdByPublicId,
                             String createdByName,
                             UUID helpDeskPublicId,
                             String createdDate,
                             String createdTime,
                             Boolean read,
                             Boolean isExternal){}
}
