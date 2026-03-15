package ru.nmedvedev.handler;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import io.quarkus.logging.Log;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import ru.nmedvedev.model.UserDb;
import ru.nmedvedev.service.UserNotificationService;

@QuarkusTest 
public class LiveTelegramIntegrationTest {

    @Inject
    UserNotificationService notificationService;

    private static final Long TEST_CHAT_ID = 6289207486L; 

    @AfterEach
    void cleanup() {
        Log.info("Cleaning up test user...");
        // Use a specific delete to avoid clearing the whole DB if you have other data,
        // or keep deleteAll() if you want a fresh start.
        UserDb.delete("chatId", TEST_CHAT_ID);
        Log.info("... Cleanup finished.");
    }

    @Test
    public void testSendLiveTelegramNotification() {
        Log.info("------ Starting Live Telegram Notification Test ------");

        try {
            // STEP 1: Handle the "Duplicate Key" issue
            // Instead of just persist(), we look for the user first.
            UserDb user = UserDb.find("chatId", TEST_CHAT_ID).firstResult();
            
            if (user == null) {
                Log.info("User not found. Creating fresh user...");
                user = new UserDb();
                user.setChatId(TEST_CHAT_ID);
            } else {
                Log.info("User already exists. Updating existing user to ensure clean state...");
            }

            // Set any required fields here
            user.setCard("1234567890123456"); 
            
            // persistOrUpdate prevents the E11000 Duplicate Key error
            user.persistOrUpdate();
            Log.info("... User record is ready.");

            // STEP 2: Send the notification
            Log.info("Attempting to send notification to Telegram...");
            
            // We call the service. 
            // NOTE: If your service is reactive (returns Uni), add .await().indefinitely()
            notificationService.sendNotification(TEST_CHAT_ID, "Quarkus Integration Test: Live balance update check.");
            
            Log.info("... Send operation completed successfully!");
            Log.info("------ Test Completed ------");

        } catch (Exception e) {
            Log.error("------ Test Failed ------", e);
            // This will now print the actual underlying cause in your logs
            throw new RuntimeException("Test execution failed. Cause: " + e.getMessage(), e); 
        }
    }
}