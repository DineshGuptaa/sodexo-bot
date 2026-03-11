package ru.nmedvedev.handler;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import io.quarkus.logging.Log;
import ru.nmedvedev.model.UserDb;
import ru.nmedvedev.service.UserNotificationService;

/**
 * Standalone Test Harness. This script is intended to be run manually from the command line.
 * It will clear the MongoDB 'userData' collection, create a fresh user entry, and attempt 
 * to send a notification to your Telegram chat.
 */
//@QuarkusMain // CRITICAL: This class is the definitive entry point.
public class RunTelegramTest implements QuarkusApplication {

    @Inject
    UserNotificationService notificationService;

    // Replace this with your actual numerical ID from @userinfobot
    private static final Long TEST_CHAT_ID = 6289207486L; 

    // The static main method remains. Execution *starts* here.
    public static void main(String... args) {
        // This launches Quarkus, which will then automatically find this 
        // @QuarkusMain class and execute its run method.
        Quarkus.run(RunTelegramTest.class, args);
    }

    // This method is now automatically executed by Quarkus startup.
    @Override
    @Transactional // Ensures database operations are transactional
    public int run(String... args) throws Exception {
        Log.info("------ Start Telegram Notification Test Harness ------");

        try {
            // STEP 1: CLEAR THE DATABASE
            Log.info("1. Cleaning MongoDB 'userData' collection...");
            // Using Panache Active Record to delete all entries in the collection
            UserDb.deleteAll();
            Log.info("... Database cleared.");


            // STEP 2: CREATE A FRESH USER
            Log.info("2. Creating fresh user with Chat ID: " + TEST_CHAT_ID);
            // Create a minimal UserDb entity
            UserDb sampleUser = new UserDb();
            sampleUser.setChatId(TEST_CHAT_ID);
            // We do not need fake card number or historical balance for this test.
            
            // Persist the entity to MongoDB
            sampleUser.persist();
            Log.info("... Fresh user persisted.");


            // STEP 3: SEND THE NOTIFICATION TEST
            Log.info("3. Attempting to send notification test...");
            
            
            // Invoke the Notification Service's direct send method, which we updated to parseMode=null
            // Use the notificationService instance injected by Quarkus.
            notificationService.sendNotification(TEST_CHAT_ID, "Test Harness Notification: Balance updated to 150");
            Log.info("... Send operation completed. Check your Telegram chat!");

            Log.info("------ Test Harness Completed Successfully ------");
            return 0; // Exit successfully
        } catch (Exception e) {
            Log.error("------ Test Harness Failed ------", e);
            return 1; // Exit with error
        } finally {
            // Signal to Quarkus that the test is done and it should shut down.
            Quarkus.asyncExit();
        }
    }
}
