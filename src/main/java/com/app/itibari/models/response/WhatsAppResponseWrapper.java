package com.app.itibari.models.response;

import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Builder;

@Builder
public record WhatsAppResponseWrapper(@JsonView(BaseView.WhatsAppView.class) String dateTime,
                                      @JsonView(BaseView.WhatsAppView.class) String customerName,
                                      @JsonView(BaseView.WhatsAppView.class) String phone,
                                      @JsonView(BaseView.WhatsAppView.class) String storeName,
                                      @JsonView(BaseView.WhatsAppView.class) Long unreadCount
                                      ) {
}
