package ru.nmedvedev.helper;

import ru.nmedvedev.dto.CardResponseDto;

// If CardResponseDto is in a different package, you MUST import it here.
// For example: import ru.nmedvedev.dto.CardResponseDto;

/**
 * Wrapper class to return both the safe DTO and whether a create or update occurred.
 */
public class CardServiceResult {

    // Define the enum type inside the class.
    public enum ActionType {
        CREATED,
        UPDATED
    }

    // Declare the member variables.
    public CardResponseDto responseDto;
    public ActionType action;

    // Define the constructor.
    public CardServiceResult(CardResponseDto responseDto, ActionType action) {
        this.responseDto = responseDto;
        this.action = action;
    }
}