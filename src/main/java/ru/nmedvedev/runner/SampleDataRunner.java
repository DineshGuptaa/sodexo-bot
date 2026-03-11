package ru.nmedvedev.runner;

import com.github.javafaker.Faker;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import ru.nmedvedev.model.HistoryDb;
import ru.nmedvedev.model.UserDb;
import ru.nmedvedev.service.UserNotificationService;

@Singleton
public class SampleDataRunner {

    @Inject
    UserNotificationService notificationService;

    @ConfigProperty(name = "sample-data-runner.enabled", defaultValue = "false")
    boolean isEnabled;

    private final Faker faker = new Faker();

    @Transactional 
    void onStart(@Observes StartupEvent ev) {
        if (!isEnabled) {
            Log.info("SampleDataRunner is disabled by configuration.");
            return;
        }

        Log.info("------ Starting Sample Data & Notification Test ------");

        try {
            // 1. CLEAN SLATE: This prevents the 'Duplicate Key' error globally
            // Note: In Panache Mongo, deleteAll() is a static method on the Entity
            
            UserDb.deleteAll();
            HistoryDb.deleteAll();

            // 2. CREATE SAMPLE DATA
            String sampleCardNumber = faker.number().digits(18);
            Long sampleChatId = 6289207486L;
            
            Log.info("Creating sample data for card: " + sampleCardNumber + " and chat ID: " + sampleChatId);

            HistoryDb sampleOperation = HistoryDb.builder()
                    .amount(100.0)
                    .currency("INR")
                    .locationName(faker.company().name() + " Restaurant")
                    .time("zzz")
                    .build();
            sampleOperation.persist();

            UserDb sampleUser = UserDb.builder()
                    .chatId(sampleChatId)
                    .card(sampleCardNumber) // Ensure the field name in UserDb is 'card'
                    .subscribed(true)
                    .subscribedToSpendMoneyReminder(false)
                    .latestOperation(sampleOperation)
                    .lastBalance(0.0)
                    .build();
            
            sampleUser.persist();

            // 3. RETRIEVE AND NOTIFY
            UserDb retrievedUser = UserDb.find("chatId", sampleChatId).firstResult();
            
            if (retrievedUser == null) {
                Log.error("CRITICAL ERROR: Failed to retrieve the sample user!");
                return;
            }
            
            Double currentSodexoBalance = 250.0;
            Log.info("Simulating balance change detection for Telegram: 0.0 -> 250.0");

            // This calls your TelegramRestClient
            notificationService.checkAndNotifyBalanceChange(retrievedUser, currentSodexoBalance);

            Log.info("Notification sent successfully.");

        } catch (Exception e) {
            Log.error("Error in SampleDataRunner execution", e);
        }

        Log.info("------ Sample Data & Notification Test Finished ------");
    }
}