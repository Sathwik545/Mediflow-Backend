package com.mediflow.platform.lab.exception;

import com.mediflow.platform.common.exception.ResourceNotFoundException;

public class LabOrderNotFoundException extends ResourceNotFoundException {
    public LabOrderNotFoundException(String code) {
        super("Lab order not found: " + code);
    }
}
