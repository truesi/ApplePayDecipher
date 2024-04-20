package kz.broscar.models.dtos;

public record ApplePayHeader(String publicKeyHash, String ephemeralPublicKey, String transactionId) {
}
