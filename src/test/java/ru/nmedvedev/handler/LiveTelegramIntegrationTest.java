package ru.nmedvedev.handler;


import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import io.quarkus.logging.Log;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import ru.nmedvedev.model.UserDb;
import ru.nmedvedev.service.UserNotificationService;

/**
 * Live Integration Test for Telegram Notification.
 * This class is a proper JUnit test and MUST reside in 'src/test/java'.
 * It will clear the MongoDB 'userData' collection, create a fresh user entry, 
 * and execute the real notification service to send a message to your Telegram chat.
 */
@QuarkusTest // The standard Quarkus test annotation. This forces the test lifecycle to run.
public class LiveTelegramIntegrationTest {

    @Inject
    UserNotificationService notificationService;

    // --- TASK: REPLACE THIS ID ---
    // Change this to your actual, real numerical ID from @userinfobot
    private static final Long TEST_CHAT_ID = 6289207486L; 

    /**
     * Clean up MongoDB after each test execution to prevent data collision.
     */
    @AfterEach
    @Transactional // Database operations must be transactional
    void cleanup() {
        Log.info("Cleaning MongoDB 'userData' collection after test...");
        UserDb.deleteAll();
        Log.info("... Database cleared.");
    }

    /**
     * The core test case. This method will be executed by JUnit.
     */
    @Test
    @Transactional // Ensures database operations are transactional
    public void testSendLiveTelegramNotification() {
        Log.info("------ Starting Live Telegram Notification Test ------");

        try {
            // STEP 1: VERIFY A CLEAN DATABASE (Optional, but good practice)
            long userCount = UserDb.count();
            Log.info("Verifying clean database (Found " + userCount + " users).");

            // STEP 2: CREATE A FRESH USER
            Log.info("Creating fresh user with Chat ID: " + TEST_CHAT_ID);
            // Create a minimal UserDb entity
            UserDb sampleUser = new UserDb();
            sampleUser.setChatId(TEST_CHAT_ID);
            // We do not need fake card number or historical balance for this test.
            
            // Persist the entity to MongoDB
            sampleUser.persist();
            Log.info("... Fresh user persisted.");


            // STEP 3: SEND THE NOTIFICATION TEST
            Log.info("Attempting to send notification test...");
            // Simulate a balance retrieval
            //Double simulatedBalance = 150.00;
            
            // Invoke the Notification Service's direct send method, which we updated to parseMode=null
            notificationService.sendNotification(TEST_CHAT_ID, "Quarkus Integration Test: Balance updated to 150");
            Log.info("... Send operation completed. Check your Telegram chat!");

            Log.info("------ Live Telegram Notification Test Completed Successfully ------");
            // No assertions are strictly needed, as the fact that the service 
            // completed without throwing an exception is the success condition.
        } catch (Exception e) {
            Log.error("------ Live Telegram Notification Test Failed ------", e);
            throw new RuntimeException("Test execution failed.", e); // Propagate the exception for JUnit to mark failure
        }
    }
}