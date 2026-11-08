package com.app.itibari.dataTypes;

public enum CreditNoteOrderAction {
    CLOSE_PURCHASE_ORDER,  // does not permit another note to be raised on the same purchase order
    SAVE_PURCHASE_ORDER,  // this allows other note to be added to the same purchase order, but does not close the purchase order
}
