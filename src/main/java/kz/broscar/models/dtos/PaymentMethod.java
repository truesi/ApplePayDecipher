package kz.broscar.models.dtos;

public record PaymentMethod(
        String network,
        String type,
        String displayName
){}
