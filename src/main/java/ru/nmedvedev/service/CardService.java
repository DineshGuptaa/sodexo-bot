package ru.nmedvedev.service;


import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import ru.nmedvedev.dto.CardRequestDto;
import ru.nmedvedev.dto.CardResponseDto;
import ru.nmedvedev.helper.CardServiceResult;
import ru.nmedvedev.model.CardDb;

/**
 * Service class responsible for business logic and DB CRUD operations.
 * This class is agnostic of HTTP or JAX-RS concerns.
 */
@ApplicationScoped // Marks this class for CDI injection
public class CardService {

    public Optional<CardDb> findByNumber(String cardNumber) {
        return CardDb.find("cardNumber", cardNumber).firstResultOptional();
    }
    /**
     * Creates or updates a card in DB. This method returns a wrapper object
     * to indicate whether a creation or an update occurred.
     */
    public CardServiceResult createOrUpdateCard(String cardNumber, CardRequestDto requestDto) {
        
        // --- 1. CRUD - Create or Update Logic ---

        CardDb existingCard = CardDb.find("cardNumber", cardNumber).firstResult();
        CardDb cardToSave;
        CardServiceResult.ActionType action;

        if (existingCard != null) {
            // UPDATE scenario
            cardToSave = existingCard;
            action = CardServiceResult.ActionType.UPDATED;
        } else {
            // CREATE scenario
            cardToSave = new CardDb();
            cardToSave.cardNumber = cardNumber; // Set the card number from path
            action = CardServiceResult.ActionType.CREATED;
        }

        // --- 2. Map Request DTO to Entity ---
        
        if (requestDto.balance != null) {
            cardToSave.balance = requestDto.balance;
        }
        if (requestDto.currency != null) {
            cardToSave.currency = requestDto.currency;
        }
        if (requestDto.status != null) {
            cardToSave.status = requestDto.status;
        }
        if (requestDto.ownerChatId != null) {
            cardToSave.ownerChatId = requestDto.ownerChatId;
        }

        // --- 3. CRUD - Save ---
        cardToSave.persist();

        // --- 4. Map Saved Entity to Safe Response DTO ---
        CardResponseDto responseDto = CardResponseDto.fromEntity(cardToSave);

        // --- 5. Return safe DTO wrapped in result info ---
        return new CardServiceResult(responseDto, action);
    }

    @Transactional
    public CardDb upsertCard(String cardNumber, Double balance, String currency) {
        return findByNumber(cardNumber)
            .map(existingCard -> {
                existingCard.balance = balance;
                existingCard.currency = currency;
                // No need to call update() manually if within @Transactional
                return existingCard;
            })
            .orElseGet(() -> {
                CardDb newCard = new CardDb();
                newCard.cardNumber = cardNumber;
                newCard.balance = balance;
                newCard.currency = currency;
                newCard.persist();
                return newCard;
            });
    }
}