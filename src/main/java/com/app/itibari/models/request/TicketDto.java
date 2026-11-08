package com.app.itibari.models.request;

import com.app.itibari.dataTypes.TicketPriority;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Builder
public record TicketDto(UUID entityUserPublicId,
                        UUID issueTypeId,
                        String content,
                        UUID departmentPublicId,
                        TicketPriority ticketPriority,
                        String storeName,
                        UUID assignedToId,
                        @NotNull CustomerType customerType,
                        String customerName,
                        String phone,
                        String district,
                        String ward,
                        String postalAddress,
                        String resolutionStatus,
                        Boolean assignedToDepartment,
                        Boolean assignedToPoc,
                        String pocEmail,
                        List<String> emailInCopy,
                        String orderNumber,
                        BigDecimal amountPaid,
                        String paymentMode,
                        List<Map<String, Object>> customFields,
                        List<UUID> productPublicIds,
                        List<UUID> customerPublicIds,
                        String summary,
                        UUID workflowPublicId,
                        String institution,
                        @NotNull UUID templatePublicId,
                        UUID salesAgentPublicId,
                        UUID deliveryAgentPublicId) {
    public enum CustomerType {
        New_Customer, Existing_Customer
    }
    public TicketDto {
        if (assignedToDepartment == null) {
            assignedToDepartment = Boolean.FALSE;
        }

        if (assignedToPoc == null) {
            assignedToPoc = Boolean.FALSE;
        }

        if (emailInCopy == null){
            emailInCopy = new ArrayList<>();
        }

        if (productPublicIds == null){
            productPublicIds = new ArrayList<>();
        }

        if (customerPublicIds == null){
            customerPublicIds = new ArrayList<>();
        }
    }
}
