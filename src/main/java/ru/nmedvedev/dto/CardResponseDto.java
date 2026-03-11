package ru.nmedvedev.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import ru.nmedvedev.model.CardDb;

/**
 * DTO representing the outgoing JSON response.
 * It hides internal details like the MongoDB ObjectId.
 */
@RegisterForReflection
public class CardResponseDto {
    // This is often good to include in the response body as well
    public String cardNumber;
    public Double balance;
    public String currency;
    public String status;
    public Long ownerChatId;

    public CardResponseDto() {}

    /**
     * Create a convenience constructor for creating a response DTO.
     * Often, you can use a mapper library like MapStruct for this.
     */
    public static CardResponseDto fromEntity(CardDb entity) {
        CardResponseDto dto = new CardResponseDto();
        dto.cardNumber = entity.cardNumber;
        dto.balance = entity.balance;
        dto.currency = entity.currency;
        dto.status = entity.status;
        dto.ownerChatId = entity.ownerChatId;
        return dto;
    }
}