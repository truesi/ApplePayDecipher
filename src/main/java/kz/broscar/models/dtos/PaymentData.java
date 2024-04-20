package kz.broscar.models.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentData {
    TransformedData data;
    String signature;
    ApplePayHeader header;
    String version;
}