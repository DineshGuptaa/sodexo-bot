package ru.nmedvedev.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserRegistrationRequest {
    @NotNull(message = "chatId is required")
    private Long chatId;

    @NotBlank(message = "userName is required")
    private String userName;

    @NotBlank(message = "cardNumber is required")
    private String cardNumber;

    @Builder.Default
    private boolean subscribed = false;
    @Builder.Default
    private boolean reminderEnabled = false;
    @Builder.Default
    private Double lastBalance = 0.0; // Optional field for initial balance
}