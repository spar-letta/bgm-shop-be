package com.app.itibari.converter;

import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.TicketDto;
import com.app.itibari.models.response.HelpDeskCustomerDTOResponse;
import com.app.itibari.models.response.HelpDeskDTOResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class HelpDeskToDtoResponse implements Converter<HelpDesk, HelpDeskDTOResponse> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE dd, MMM yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");

    @Override
    public HelpDeskDTOResponse convert(HelpDesk helpDesk) {
        if (helpDesk == null) {
            return null;
        }

        LocalDateTime creationTime = helpDesk.getCreationTime();
        String dateString = creationTime.toLocalDate().format(DATE_FORMATTER);
        String timeString = creationTime.format(TIME_FORMATTER);

        String raisedBy = helpDesk.getCreatedBy() != null
                ? helpDesk.getCreatedBy().getFullName()
                : "--";

        String assignedToNames = helpDesk.getAssignedTo() != null
                ? helpDesk.getAssignedTo().getFullName()
                : "--";

        String entityUserName = "--";
        UUID entityUserPublicId = null;
        UUID helpDeskCustomerPublicId = null;
        String entityUserPhone = "--";
        String customerType = "--";
        String storeName = "--";
        String institution = "--";
        List<HelpDeskDTOResponse.DepartmentInCopy> departmentInCopyList = new ArrayList<>();

        if (helpDesk.getHelpDeskCustomer() != null) {

            entityUserName = helpDesk.getHelpDeskCustomer().getName();
            entityUserPhone = helpDesk.getHelpDeskCustomer().getPhoneNumber();
            entityUserPublicId = helpDesk.getHelpDeskCustomer().getExistingCustomerPublicId();
            helpDeskCustomerPublicId = helpDesk.getHelpDeskCustomer().getPublicId();
            customerType = helpDesk.getHelpDeskCustomer().getCustomerType().name();

            if (helpDesk.getHelpDeskCustomer().getCustomerType().equals(TicketDto.CustomerType.New_Customer)){
                institution = helpDesk.getHelpDeskCustomer().getStoreName();
            }else {
                storeName = helpDesk.getHelpDeskCustomer().getStoreName();
            }
        }

        List<HelpDeskDTOResponse.Product> productList = new ArrayList<>();
        List<HelpDeskDTOResponse.Customer> customerList = new ArrayList<>();
        if (!helpDesk.getProductList().isEmpty()){
            for (Product product : helpDesk.getProductList()) {
                HelpDeskDTOResponse.Product buildedProduct = HelpDeskDTOResponse.Product.builder().publicId(product.getPublicId()).name(product.getProductName()).build();
                productList.add(buildedProduct);
            }
        }

        if (!helpDesk.getCustomersList().isEmpty()){
            for (Customers customers : helpDesk.getCustomersList()) {
                HelpDeskDTOResponse.Customer buildedCustomer = HelpDeskDTOResponse.Customer.builder().publicId(customers.getPublicId()).name(customers.getFullName()).build();
                customerList.add(buildedCustomer);
            }
        }

        String ticketPriority = "--";

        if (helpDesk != null
                && helpDesk.getIssueType() != null
                && helpDesk.getIssueType().getPrioritySetting() != null
                && helpDesk.getIssueType().getPrioritySetting().getPriorityName() != null) {
            ticketPriority = helpDesk.getIssueType().getPrioritySetting().getPriorityName();
        }


        HelpDeskDTOResponse.District district = HelpDeskDTOResponse.District.builder()
                .publicId(helpDesk.getHelpDeskCustomer() != null ? helpDesk.getHelpDeskCustomer().getDistrictId(): null)
                .name(helpDesk.getHelpDeskCustomer() != null ? helpDesk.getHelpDeskCustomer().getDistrict(): null).build();

        HelpDeskDTOResponse.Ward ward = HelpDeskDTOResponse.Ward.builder()
                .publicId(helpDesk.getHelpDeskCustomer() != null ? helpDesk.getHelpDeskCustomer().getWardId() : null)
                .name(helpDesk.getHelpDeskCustomer() != null ? helpDesk.getHelpDeskCustomer().getWard() : null).build();

        departmentInCopyList =
                Optional.ofNullable(helpDesk.getTemplate())
                        .map(t -> t.getDepartmentInCopy())
                        .orElse(Collections.emptyList())
                        .stream()
                        .map(item -> HelpDeskDTOResponse.DepartmentInCopy.builder()
                                .publicId(item.getPublicId())
                                .name(item.getName())
                                .build()
                        )
                        .collect(Collectors.toList());

        String ticketStatus = "--";
        if (helpDesk.getWorkflowNewStatus() != null){
            ticketStatus = helpDesk.getWorkflowNewStatus().equalsIgnoreCase("resolve") ? "RESOLVED" : helpDesk.getWorkflowNewStatus();
        }else {
            ticketStatus = "--";
        }

        HelpDeskDTOResponse.SalesAgent salesAgents = null;
        if (helpDesk.getSalesAgents() != null){
            salesAgents = HelpDeskDTOResponse.SalesAgent.builder()
                    .publicId(helpDesk.getSalesAgents().getPublicId())
                    .name(helpDesk.getSalesAgents().getName())
                    .build();
        }

        HelpDeskDTOResponse.DeliveryAgent deliveryAgent = null;
        if (helpDesk.getDeliveryAgents() != null){
            deliveryAgent = HelpDeskDTOResponse.DeliveryAgent.builder()
                    .publicId(helpDesk.getDeliveryAgents().getPublicId())
                    .name(helpDesk.getDeliveryAgents().getFullName())
                    .build();
        }


        return HelpDeskDTOResponse.builder()
                .createdDate(dateString)
                .createdTime(timeString)
                .publicId(helpDesk.getPublicId())
                .createdByName(raisedBy)
                .entityUserName(entityUserName)
                .entityUserPublicId(entityUserPublicId)
                .title(helpDesk.getIssueType() != null ? helpDesk.getIssueType().getName(): null)
                .content(helpDesk.getContent())
                .department(helpDesk.getSection() != null ? helpDesk.getSection().getName() : "--")
                .ticketNumber(helpDesk.getTicketNumber())
                .status(ticketStatus)
                .ticketPriority(ticketPriority)
                .entityPhone(entityUserPhone)
                .region(district)
                .location(ward)
                .storeName(storeName)
                .assignedToName(assignedToNames)
                .read(helpDesk.getRead() != null ? helpDesk.getRead() : Boolean.FALSE)
                .helpDeskCustomerPublicId(helpDeskCustomerPublicId)
                .customerType(customerType)
                .assignedToDepartment(helpDesk.getAssignedToDepartment())
                .assignedToPoc(helpDesk.getAssignedToPoc())
                .pocEmail(helpDesk.getPocEmail())
                .emailInCopy(helpDesk.getEmailInCopy())
                .orderNumber(helpDesk.getOrderNumber())
                .amountPaid(helpDesk.getAmountPaid())
                .paymentMode(helpDesk.getPaymentMode())
                .issueTypePublicId(helpDesk.getIssueType() != null ? helpDesk.getIssueType().getPublicId(): null)
                .customField(helpDesk.getCustomField())
                .productList(productList)
                .customerList(customerList)
                .summary(helpDesk.getSummary())
                .attachedWorkflowPublicId(helpDesk.getWorkflow() != null ? helpDesk.getWorkflow().getPublicId(): null)
                .workflowStatus(helpDesk.getWorkflowStatus())
                .institution(institution)
                .templatePublicId(helpDesk.getTemplatePublicId())
                .departmentInCopy(departmentInCopyList)
                .deliveryAgent(deliveryAgent)
                .salesAgent(salesAgents)
                .build();
    }
}
