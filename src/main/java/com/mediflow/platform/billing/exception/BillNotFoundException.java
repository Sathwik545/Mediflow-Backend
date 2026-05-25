package com.mediflow.platform.billing.exception;

import com.mediflow.platform.common.exception.ResourceNotFoundException;

public class BillNotFoundException extends ResourceNotFoundException {

    public BillNotFoundException(String billCode) {
        super("Bill not found with code: " + billCode);
    }
}
