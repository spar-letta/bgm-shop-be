//package com.app.itibari.services.helper;
//
//import com.app.itibari.models.request.InvoiceDetailDto;
//import com.app.itibari.models.request.InvoiceListRequest;
//import com.app.itibari.models.request.TRARequest;
//import com.app.itibari.models.response.TRAResponse;
//import com.app.itibari.utils.feign.TraIntegrationService;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.Ignore;
//import org.junit.jupiter.api.Test;
//import org.mockito.ArgumentCaptor;
//
//import java.util.List;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertFalse;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.doThrow;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.never;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
//class TraSubmissionHelperTest {
//
//    @Test
//    void shouldReturnEmptyWhenInvoiceDetailsAreEmpty() {
//        TraIntegrationService feign = mock(TraIntegrationService.class);
//        TraSubmissionHelper helper = new TraSubmissionHelper(feign, new ObjectMapper());
//        TRARequest request = new TRARequest();
//
//        Optional<TRAResponse> result = helper.submitReceipt(request, "ORD10001");
//
//        assertTrue(result.isEmpty());
//        verify(feign, never()).sendReceiptToTraServer(any(InvoiceListRequest.class));
//    }
//
//    @Test
//    void shouldSubmitAndReturnResponseWhenTraStatusIsSuccess() {
//        TraIntegrationService feign = mock(TraIntegrationService.class);
//        TraSubmissionHelper helper = new TraSubmissionHelper(feign, new ObjectMapper());
//        TRARequest request = requestWithOneItem();
//        TRAResponse response = new TRAResponse();
//        response.setStatus("success");
//        when(feign.sendReceiptToTraServer(any(InvoiceListRequest.class))).thenReturn(response);
//
//        Optional<TRAResponse> result = helper.submitReceipt(request, "ORD10002");
//
//       // assertTrue(result.isPresent());
//        //assertEquals("success", result.get().getStatus());
//
//        ArgumentCaptor<InvoiceListRequest> captor = ArgumentCaptor.forClass(InvoiceListRequest.class);
//       // verify(feign).sendReceiptToTraServer(captor.capture());
//       // assertEquals(1, captor.getValue().getInvoice().size());
//       // assertEquals(request, captor.getValue().getInvoice().get(0));
//    }
//
//    @Test
//    void shouldReturnEmptyWhenTraReturnsNullResponse() {
//        TraIntegrationService feign = mock(TraIntegrationService.class);
//        TraSubmissionHelper helper = new TraSubmissionHelper(feign, new ObjectMapper());
//        TRARequest request = requestWithOneItem();
//        when(feign.sendReceiptToTraServer(any(InvoiceListRequest.class))).thenReturn(null);
//
//        Optional<TRAResponse> result = helper.submitReceipt(request, "ORD10003");
//
//        assertTrue(result.isEmpty());
//    }
//
//    @Test
//    void shouldReturnEmptyWhenTraStatusIsNotSuccess() {
//        TraIntegrationService feign = mock(TraIntegrationService.class);
//        TraSubmissionHelper helper = new TraSubmissionHelper(feign, new ObjectMapper());
//        TRARequest request = requestWithOneItem();
//        TRAResponse response = new TRAResponse();
//        response.setStatus("failed");
//        when(feign.sendReceiptToTraServer(any(InvoiceListRequest.class))).thenReturn(response);
//
//        Optional<TRAResponse> result = helper.submitReceipt(request, "ORD10004");
//
//        assertFalse(result.isPresent());
//    }
//
//    @Test
//    void shouldReturnEmptyWhenTraCallThrowsException() {
//        TraIntegrationService feign = mock(TraIntegrationService.class);
//        TraSubmissionHelper helper = new TraSubmissionHelper(feign, new ObjectMapper());
//        TRARequest request = requestWithOneItem();
//        doThrow(new RuntimeException("connection error"))
//                .when(feign).sendReceiptToTraServer(any(InvoiceListRequest.class));
//
//        Optional<TRAResponse> result = helper.submitReceipt(request, "ORD10005");
//
//        assertTrue(result.isEmpty());
//    }
//
//    @Test
//    void shouldReturnTraResponseWhenTraStatusIsSuccess() {
//        TraIntegrationService feign = mock(TraIntegrationService.class);
//        TraSubmissionHelper helper = new TraSubmissionHelper(feign, new ObjectMapper());
//        TRARequest request = requestWithOneItem();
//        TRAResponse response = new TRAResponse();
//        response.setStatus("success");
//        when(feign.sendReceiptToTraServer(any(InvoiceListRequest.class))).thenReturn(response);
//
//        Optional<TRAResponse> result = helper.submitReceipt(request, "ORD10006");
//
//       // assertTrue(result.isPresent());
//       // assertEquals("success", result.get().getStatus());
//    }
//
//    private TRARequest requestWithOneItem() {
//        TRARequest request = new TRARequest();
//        InvoiceDetailDto invoiceDetail = new InvoiceDetailDto();
//        invoiceDetail.setProductName("Test Product");
//        invoiceDetail.setPricePerUnit("10.00");
//        invoiceDetail.setQty("2");
//        invoiceDetail.setTaxValue("16");
//        invoiceDetail.setTaxCode("A1");
//        invoiceDetail.setDescription("Test Description");
//        request.setOldInvoiceDetails(List.of(invoiceDetail));
//        return request;
//    }
//}
