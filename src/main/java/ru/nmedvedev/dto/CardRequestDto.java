package ru.nmedvedev.dto;


import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * DTO representing the incoming JSON body to create or update a card.
 * The card number is omitted as it is provided in the URL path.
 * * @RegisterForReflection is useful for GraalVM native image compilation in Quarkus.
 */
@RegisterForReflection
public class CardRequestDto {
    public Double balance;
    public String currency;
    public String status;
    public Long ownerChatId;

    // A default constructor is required for Jackson deserialization
    public CardRequestDto() {}

    // A convenience constructor is often useful for testing
    public CardRequestDto(Double balance, String currency, String status, Long ownerChatId) {
        this.balance = balance;
        this.currency = currency;
        this.status = status;
        this.ownerChatId = ownerChatId;
    }
}