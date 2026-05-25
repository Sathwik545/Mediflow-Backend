package com.mediflow.platform.lab.exception;

import com.mediflow.platform.common.exception.BusinessRuleViolationException;

public class InvalidReportFileException extends BusinessRuleViolationException {
    public InvalidReportFileException(String message) {
        super(message);
    }
}
