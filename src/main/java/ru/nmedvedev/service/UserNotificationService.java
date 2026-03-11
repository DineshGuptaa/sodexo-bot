package ru.nmedvedev.service;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import ru.nmedvedev.dto.SendMessageRequestDto;
import ru.nmedvedev.dto.TelegramResponseDto;
import ru.nmedvedev.model.UserDb;
import ru.nmedvedev.rest.TelegramRestClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class UserNotificationService {

    @Inject
    @RestClient // Inject the Rest Client
    TelegramRestClient telegramClient;

    @ConfigProperty(name = "telegram.bot.token") // Inject the token from properties
    String botToken;

    /**
     * Registers a new relationship. 
     * In a real bot, this would be called by a /register command handler.
     */
    public void registerUser(Long chatId, String cardNumber) {
        UserDb existing = UserDb.find("chatId", chatId).firstResult();
        
        if (existing == null) {
            UserDb newUser = UserDb.builder()
                    .chatId(chatId)
                    .card(cardNumber)
                    .lastBalance(0.0) // Initial balance
                    .build();
            newUser.persist();
            log.info("Registered new Telegram user: " + chatId + " with card: " + cardNumber);
            sendNotification(chatId, "Card " + cardNumber + " registered successfully!");
        } else {
            // Logic for updating the card number if needed
            log.info("User " + chatId + " is already registered.");
        }
    }

    /**
     * This method would be called by a scheduled task (cron job) 
     * that iterates over all users and checks their balance.
     */
    public void checkAndNotifyBalanceChange(UserDb user, Double currentBalance) {
        if (currentBalance == null) {
            // Log.error("Could not retrieve current balance for card: " + user.getCard());
            return; // Error retrieving balance
        }

        // --- 1. ACCESS fields using public GETTERS ---
        
        // Getter names are generated as: get + FieldName (capitalized)
        // For 'private Double lastBalance', Lombok generates: public Double getLastBalance()
        Double previousBalance = user.getLastBalance();
        String cardNumber = user.getCard();
        Long chatId = user.getChatId();

        // Perform the comparison safely
        if (!currentBalance.equals(previousBalance)) {
            // Balance changed!
            
            // Generate the notification message using safe strings
            String message = String.format("Balance update for card %s:Previous: %.2f | Current: %.2f", 
                    cardNumber, previousBalance, currentBalance);
            
            log.info("Balance changed for user " + chatId + ". Sending notification.");
            
            // Send the notification (no change needed here)
            sendNotification(chatId, message);

            // --- 2. UPDATE the field using public SETTER ---
            
            // Setter name is generated as: set + FieldName (capitalized)
            // For 'private Double lastBalance', Lombok generates: public void setLastBalance(Double value)
            user.setLastBalance(currentBalance);

            // Important: Call the inherited update() method to save changes to MongoDB
            user.update();
        }
    }
    

    /**
     * Low-level method to send a raw notification.
     */
    public void sendNotification(Long chatId, String text) {
        if (text == null || text.trim().isEmpty()) {
            log.error("Attempted to send null or empty message to Telegram user " + chatId);
            return;
        }
        
        // Log the message being sent for easier debugging
        log.info("Sending message to Telegram [{}] : [{}] : Token [{}]", chatId , text , botToken);

        try {
            // --- THIS PART IS CRITICAL ---
            // Instead of using complex constructors, use the builder pattern (if available) 
            // or explicitly set the fields to ensure parseMode is explicitly NULL.

            SendMessageRequestDto request = new SendMessageRequestDto();
            request.chatId = chatId;
            request.text = text;
            request.parseMode = null; // Explicitly set to NULL to match your successful curl.
            // SendMessageRequestDto request1 = SendMessageRequestDto.builder()
            //     .chatId(chatId) // Must be your actual chat ID from the logs: 6289207486
            //     .text(simpleMessageText)
            //     // --- CRITICAL FIX ---
            //     // Set parseMode to null (PLAIN TEXT) to disable Markdown/HTML parsing, preventing formatting errors.
            //     .parseMode(null) 
            //     .build();

            // Approach 2 (Builder, recommended if available):
            // SendMessageRequestDto request = SendMessageRequestDto.builder()
            //         .chatId(chatId)
            //         .text(text)
            //         .parseMode(null) // Ensures plain text processing
            //         .build();

            // Send the request via the Rest Client
            TelegramResponseDto response = telegramClient.sendMessage(botToken, request);
            log.error("TelegramResponseDto :{}", ReflectionToStringBuilder.toString(response, ToStringStyle.SHORT_PREFIX_STYLE));
            if (!response.isOk()) {
                log.error("Failed to send Telegram message to " + chatId + ": " + response.getDescription());
            }
        } catch (Exception e) {
            log.error("Error connecting to Telegram API", e);
        }
    }
}