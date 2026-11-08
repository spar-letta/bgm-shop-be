package com.app.itibari.converter;

import com.app.itibari.models.entities.PrioritySetting;
import com.app.itibari.models.response.PrioritySettingResponse;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
public class PrioritySettingToResponse implements Converter<PrioritySetting, PrioritySettingResponse> {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    @Override
    public PrioritySettingResponse convert(PrioritySetting prioritySetting) {
        if (prioritySetting == null) {
            return null;
        }

        return PrioritySettingResponse.builder()
                .publicId(prioritySetting.getPublicId())
                .creationTime(prioritySetting.getCreationTime() != null ? prioritySetting.getCreationTime().format(formatter) : "--")
                .updateDate(prioritySetting.getUpdateDate() != null ? prioritySetting.getUpdateDate().format(formatter) : "--")
                .priorityName(prioritySetting.getPriorityName())
                .slaType(prioritySetting.getSlaType() != null ? prioritySetting.getSlaType().name() : null)
                .slaDuration(prioritySetting.getSlaDuration())
                .colorName(prioritySetting.getColorName())
                .isActive(prioritySetting.getIsActive())
                .createdBy(prioritySetting.getCreatedBy() != null ? prioritySetting.getCreatedBy().getFullName() : "--")
                .firstTimeResponse(prioritySetting.getFirstTimeResponse())
                .build();
    }
}
