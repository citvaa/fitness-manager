package com.example.demo.dto.notification;

import com.example.demo.dto.PaymentDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentConfirmationNotificationDTO {
    private String message;

    public PaymentConfirmationNotificationDTO(@NotNull PaymentDTO payment) {
        this.message = "Uplata evidentirana: " + payment.getPaidAppointments()
                + " termina (" + payment.getSession().getType() + ") na dan " + payment.getPaymentDate();
    }
}
