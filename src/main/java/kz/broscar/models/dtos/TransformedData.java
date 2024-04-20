package kz.broscar.models.dtos;

public class TransformedData {
    private String applicationPrimaryAccountNumber;
    private String applicationExpirationDate;
    private String currencyCode;
    private int transactionAmount;
    private String deviceManufacturerIdentifier;
    private String paymentDataType;
    private PaymentDataDetails paymentData;
}

class PaymentDataDetails {
    private String onlinePaymentCryptogram;
}