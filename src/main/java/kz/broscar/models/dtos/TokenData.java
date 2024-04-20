package kz.broscar.models.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TokenData {
    String transactionIdentifier;
    PaymentData paymentData;
    PaymentMethod paymentMethod;
}