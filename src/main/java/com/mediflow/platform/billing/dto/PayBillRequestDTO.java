package com.mediflow.platform.billing.dto;

import com.mediflow.platform.billing.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayBillRequestDTO {

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;
}
