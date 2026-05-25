package com.mediflow.platform.lab.exception;

import com.mediflow.platform.common.exception.ResourceNotFoundException;

public class LabReportNotFoundException extends ResourceNotFoundException {
    public LabReportNotFoundException(String code) {
        super("Lab report not found: " + code);
    }
}
