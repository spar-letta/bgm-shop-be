package com.app.itibari.web;

import com.app.itibari.dataTypes.GoodsNoteType;
import com.app.itibari.dataTypes.GoodsReturnStatus;
import com.app.itibari.dataTypes.PromoType;
import com.app.itibari.dataTypes.PurchaseOrderStatus;
import com.app.itibari.docs.Examples;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.GoodsReturnNoteRequest;
import com.app.itibari.models.request.PromoRequestNoteRequest;
import com.app.itibari.models.response.ReceivedProductItemResponse;
import com.app.itibari.services.GoodsReturnNoteService;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.UUID;

@Tag(name = "Goods Return", description = "Goods Return API")
@RestController
@RequestMapping("/goodsReturnNote")
public class GoodsReturnRestController {

    private final GoodsReturnNoteService goodsReturnNoteService;

    public GoodsReturnRestController(GoodsReturnNoteService goodsReturnNoteService) {

        this.goodsReturnNoteService = goodsReturnNoteService;
    }

    @PreAuthorize("hasAuthority('CREATE_GOODS_RETURN_NOTE')")
    @PostMapping
    @Operation(summary = "Create Goods Return Note", description = "Create Goods Return Note",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "GENERATE_NOTE_REQUEST", value = ""),
    })))
    public void createGoodsReturnNote(@RequestBody @Valid GoodsReturnNoteRequest goodsReturnNoteRequest,
                                      @Parameter(hidden = true) Principal principal) {

        goodsReturnNoteService.createGoodsReturnNote(goodsReturnNoteRequest, principal.getName());
    }

    // get the goods return note

    @PreAuthorize("hasAuthority('VIEW_GOODS_RETURN_NOTE')")
    @JsonView(BaseView.GoodsReturned.class)
    @GetMapping
    @Operation(summary = "Get Goods Return Note", description = "Get Goods Return Note",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET_GRN", value = Examples.FETCH_GRN_OK_RESPONSE)})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Page<GoodsReturnNote> getGoodsReturnNote(@RequestParam(required = false, name = "status") GoodsReturnStatus status,
                                                    @RequestParam(required = false, name = "searchParam") String search,
                                                    @RequestParam(required = false, name = "purchaseOrderNumber") String purchaseOrderNumber,
                                                    @RequestParam(required = false, name = "supplierPublicId") UUID supplierPublicId,
                                                    @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                    @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                    @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return goodsReturnNoteService.getGoodsReturnNote(status,
                search,
                purchaseOrderNumber,
                supplierPublicId,
                to,
                from,
                GoodsNoteType.RETURN_GOODS,
                pageable);
    }

    @PreAuthorize("hasAuthority('VIEW_GOODS_RETURN_NOTE')")
    @JsonView(BaseView.GoodsReceived.class)
    @GetMapping("/receive")
    @Operation(summary = "Get Goods Receive Note", description = "Get Goods Receive Note",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET_GRN", value = Examples.FETCH_GRN1_OK_RESPONSE)})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Page<GoodsReturnNote> getGoodsReceiveNote(@RequestParam(required = false, name = "status") GoodsReturnStatus status,
                                                     @RequestParam(required = false, name = "searchParam") String search,
                                                     @RequestParam(required = false, name = "purchaseOrderNumber") String purchaseOrderNumber,
                                                     @RequestParam(required = false, name = "supplierPublicId") UUID supplierPublicId,
                                                     @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                     @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                     @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return goodsReturnNoteService.getGoodsReturnNote(status,
                search,
                purchaseOrderNumber,
                supplierPublicId,
                to,
                from,
                GoodsNoteType.RECEIVED_GOODS,
                pageable);
    }

    @PreAuthorize("hasAuthority('VIEW_GOODS_RETURN_NOTE')")
    @JsonView(BaseView.GoodsReceived.class)
    @GetMapping("/receive/{goodsReturnNotePublicId}/items")
    @Operation(summary = "Get Goods Receive Note items", description = "Get Goods Receive Note items",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET_GRN", value = Examples.FETCH_GRN1_RECEIVE_ITEMS)})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Page<ProductItem> getGoodsReceiveNoteItems(@PathVariable(name = "goodsReturnNotePublicId") UUID goodsReturnNotePublicId,
                                                      @RequestParam(required = false, name = "searchParam") String search,
                                                      @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return goodsReturnNoteService.getGoodsReceiveNoteItems(search, goodsReturnNotePublicId, GoodsNoteType.RECEIVED_GOODS, pageable);
    }

    @PreAuthorize("hasAuthority('VIEW_GOODS_RETURN_NOTE')")
    @JsonView(BaseView.GoodsReturned.class)
    @GetMapping("/return/{goodsReturnNotePublicId}/items")
    @Operation(summary = "Get Goods return Note items", description = "Get Goods return Note items",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET_GRN", value = Examples.FETCH_GRN1_RETURN_ITEMS_OK_RESPONSE)})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Page<ProductItem> getGoodsReturnNoteItems(@PathVariable(name = "goodsReturnNotePublicId") UUID goodsReturnNotePublicId,
                                                     @RequestParam(required = false, name = "searchParam") String search,
                                                     @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return goodsReturnNoteService.getGoodsReturnNoteItems(search, goodsReturnNotePublicId, GoodsNoteType.RETURN_GOODS, pageable);
    }

    // receive promo goods

    @PreAuthorize("hasAuthority('VIEW_GOODS_RETURN_NOTE')")
    @JsonView(BaseView.GoodsReceived.class)
    @PatchMapping("/promo/receive")
    @Operation(summary = "Get Promo Goods Receive Note", description = "Get Promo Goods Receive Note",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET_GRN", value = "")})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
                    @ExampleObject(name = "GENERATE_NOTE_PROMO_REQUEST", value = Examples.GENERATE_NOTE_PROMO_REQUEST),
            })))
    public void receivePromoGoods(@RequestBody @Valid PromoRequestNoteRequest promoNoteRequest,
                                  @Parameter(hidden = true) Principal principal) {

        goodsReturnNoteService.receivePromoGoods(PromoType.Promo, promoNoteRequest, principal.getName());
    }

    @PreAuthorize("hasAuthority('VIEW_GOODS_RETURN_NOTE')")
    @JsonView(BaseView.GoodsReceived.class)
    @GetMapping("/shipmentReceived")
    @Operation(summary = "Get Goods Receive Note", description = "Get Goods Receive Note",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET_GRN", value = Examples.FETCH_GRN1_OK_RESPONSE)})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Page<GoodsReceivedNote> getShipmentReceiveNote(@RequestParam(required = false, name = "status") GoodsReturnStatus status,
                                                          @RequestParam(required = false, name = "poStatus") PurchaseOrderStatus poStatus,
                                                          @RequestParam(required = false, name = "searchParam") String search,
                                                          @RequestParam(required = false, name = "purchaseOrderNumber") String purchaseOrderNumber,
                                                          @RequestParam(required = false, name = "supplierPublicId") UUID supplierPublicId,
                                                          @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                          @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                          @PageableDefault(size = 20, sort = "noteNumber", direction = Sort.Direction.DESC) Pageable pageable) {

        return goodsReturnNoteService.getShipmentReceiveNote(status,
                search,
                purchaseOrderNumber,
                supplierPublicId,
                to,
                from,
                GoodsNoteType.RECEIVED_GOODS,
                poStatus,
                pageable);
    }

    @PreAuthorize("hasAuthority('VIEW_GOODS_RETURN_NOTE')")
    @JsonView(BaseView.GoodsReceived.class)
    @GetMapping("/shipmentReceived/{goodsReceivedNotePublicId}/items")
    @Operation(summary = "Get Goods Receive Note items", description = "Get Goods Receive Note items",
            responses = {@ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET_GRN", value = Examples.FETCH_GRN1_RECEIVE_ITEMS)})),})
    public ReceivedProductItemResponse getShipmentNoteItems(@PathVariable(name = "goodsReceivedNotePublicId") UUID goodsReceivedNotePublicId,
                                                            @RequestParam(required = false, name = "searchParam") String search,
                                                            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return goodsReturnNoteService.getShipmentNoteItems(search, goodsReceivedNotePublicId, pageable);
    }


    @PreAuthorize("hasAuthority('CREATE_GOODS_RETURN_NOTE')")
    @PostMapping("/createGoodsReturnNotes")
    @Operation(summary = "Create Goods Return Note", description = "Create Goods Return Note",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK")
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "GENERATE_NOTE_REQUEST", value = ""),
    })))
    public void createGoodsReturnDeliveryNote(@RequestBody @Valid GoodsReturnNoteRequest goodsReturnNoteRequest,
                                              @Parameter(hidden = true) Principal principal) {

        goodsReturnNoteService.createGoodsReturnDeliveryNote(goodsReturnNoteRequest, principal.getName());
    }

    @PreAuthorize("hasAuthority('VIEW_GOODS_RETURN_NOTE')")
    @JsonView(BaseView.GoodsReturned.class)
    @GetMapping("/returnedGoodsNotes")
    @Operation(summary = "Get Goods Return Note", description = "Get Goods Return Note",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET_GRN", value = Examples.FETCH_GRN_OK_RESPONSE)})),
            })
    public Page<GoodsReturnDeliveryNote> listReturnedGoodsNotes(@RequestParam(required = false, name = "status") GoodsReturnStatus status,
                                                                @RequestParam(required = false, name = "searchParam") String search,
                                                                @RequestParam(required = false, name = "purchaseOrderNumber") String purchaseOrderNumber,
                                                                @RequestParam(required = false, name = "supplierPublicId") UUID supplierPublicId,
                                                                @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                                @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                                @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return goodsReturnNoteService.listReturnedGoodsNotes(status,
                search,
                purchaseOrderNumber,
                supplierPublicId,
                to,
                from,
                GoodsNoteType.RETURN_GOODS,
                pageable);
    }

    @PreAuthorize("hasAuthority('VIEW_GOODS_RETURN_NOTE')")
    @JsonView(BaseView.GoodsReturned.class)
    @GetMapping("/returnedGoodsNotes/{goodsReturnedNotePublicId}/items")
    @Operation(summary = "Get Goods return Note items", description = "Get Goods return Note items",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET_GRN", value = Examples.FETCH_GRN1_RETURN_ITEMS_OK_RESPONSE)})),
            })
    public Page<ReturnedProductItem> getReturnedProductItems(@PathVariable(name = "goodsReturnedNotePublicId") UUID goodsReturnedNotePublicId,
                                                             @RequestParam(required = false, name = "searchParam") String search,
                                                             @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return goodsReturnNoteService.getReturnedProductItems(search, goodsReturnedNotePublicId, pageable);
    }
}
