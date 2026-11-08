package com.app.itibari.models.response;

import com.app.itibari.dataTypes.AssignToEnum;
import com.app.itibari.dataTypes.WorkflowStatus;
import com.app.itibari.models.entities.Department;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;

import java.math.BigDecimal;
import java.sql.Time;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Builder
public record HelpDeskDTOResponse(UUID publicId,
                                  String createdDate,
                                  String createdTime,
                                  String createdByName,
                                  UUID entityUserPublicId,
                                  String entityUserName,
                                  String title,
                                  String content,
                                  String department,
                                  String ticketNumber,
                                  String status,
                                  String ticketPriority,
                                  String entityPhone,
                                  District region,
                                  Ward location,
                                  String storeName,
                                  String assignedToName,
                                  Boolean read,
                                  UUID helpDeskCustomerPublicId,
                                  String customerType,
                                  Boolean assignedToDepartment,
                                  Boolean assignedToPoc,
                                  String pocEmail,
                                  List<String> emailInCopy,
                                  String orderNumber,
                                  BigDecimal amountPaid,
                                  String paymentMode,
                                  UUID issueTypePublicId,
                                  JsonNode customField,
                                  List<Product> productList,
                                  List<Customer> customerList,
                                  String summary,
                                  UUID attachedWorkflowPublicId,
                                  String workflowStatus,
                                  String institution,
                                  UUID templatePublicId,
                                  List<DepartmentInCopy> departmentInCopy,
                                  SalesAgent salesAgent,
                                  DeliveryAgent deliveryAgent
) {

    @Builder
    public record District(String publicId, String name) {

    }

    @Builder
    public record Ward(String publicId, String name) {

    }

    @Builder
    public record Product(UUID publicId, String name){

    }

    @Builder
    public record Customer(UUID publicId, String name){

    }

    @Builder
    public record DepartmentInCopy(UUID publicId, String name){

    }

    @Builder
    public record SalesAgent(UUID publicId, String name){

    }

    @Builder
    public record DeliveryAgent(UUID publicId, String name){

    }
}
