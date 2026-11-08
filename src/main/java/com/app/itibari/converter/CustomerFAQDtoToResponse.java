package com.app.itibari.converter;

import com.app.itibari.models.entities.CustomerFAQNote;
import com.app.itibari.models.entities.CustomerFAQ;
import com.app.itibari.models.response.CustomerFAQToResponse;
import com.app.itibari.utils.formatter.DateFormatter;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class CustomerFAQDtoToResponse implements Converter<CustomerFAQ, CustomerFAQToResponse>{

    @Override
    public CustomerFAQToResponse convert(CustomerFAQ customerFAQ) {
        if (customerFAQ == null){
            return null;
        }

        CustomerFAQToResponse.Department department = CustomerFAQToResponse.Department.builder()
                .publicId(customerFAQ.getDepartment() != null ? customerFAQ.getDepartment().getPublicId() : null)
                .name(customerFAQ.getDepartment() != null ? customerFAQ.getDepartment().getName() : "--")
                .build();

        List<CustomerFAQToResponse.CustomerFAQNote> customerFAQNotes = new ArrayList<>();

        for (CustomerFAQNote customerFAQNote : customerFAQ.getNoteList()) {
            CustomerFAQToResponse.CustomerFAQNote.IssueType issueType = CustomerFAQToResponse.CustomerFAQNote.IssueType.builder()
                    .publicId(customerFAQNote.getIssueType() != null ? customerFAQNote.getIssueType().getPublicId() : null)
                    .name(customerFAQNote.getIssueType() != null ? customerFAQNote.getIssueType().getName() : "--")
                    .build();

            CustomerFAQToResponse.CustomerFAQNote customerFAQNote1 = CustomerFAQToResponse.CustomerFAQNote.builder()
                    .publicId(customerFAQNote.getPublicId())
                    .question(customerFAQNote.getQuestion())
                    .answer(customerFAQNote.getAnswer())
                    .issueType(issueType).build();
            customerFAQNotes.add(customerFAQNote1);
        }


        return CustomerFAQToResponse.builder()
                .publicId(customerFAQ.getPublicId())
                .creationTime(DateFormatter.formatDate(customerFAQ.getCreationTime()))
                .createdBy(customerFAQ.getCreatedBy().getFullName())
                .department(department)
                .noteList(customerFAQNotes)
                .build();
    }
}
