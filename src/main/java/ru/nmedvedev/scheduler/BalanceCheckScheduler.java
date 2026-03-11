package ru.nmedvedev.scheduler;

import io.quarkus.logging.Log;
//import io.quarkus.scheduler.Scheduled;
import ru.nmedvedev.model.UserDb;
import ru.nmedvedev.service.UserNotificationService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

/**
 * Periodically iterates over all registered users, checks their Sodexo balance,
 * and delegates to the notification service if it has changed.
 */
@ApplicationScoped
public class BalanceCheckScheduler {

    @Inject
    UserNotificationService notificationService;

    // You need to implement the service that actually talks to Sodexo!
    // @Inject
    // SodexoApiService sodexoApiService; 

    /**
     * Runs every 15 minutes (using a cron expression).
     */
    //@Scheduled(cron = "0 0/15 * * * ?") 
    void checkBalances() {
        Log.info("Starting scheduled balance check...");

        // 1. Get all registered users from MongoDB
        List<UserDb> allUsers = UserDb.listAll();

        for (UserDb user : allUsers) {
            try {
                // 2. Call the Sodexo API to get the current balance for this card
                // Double currentBalance = sodexoApiService.getBalance(user.cardNumber);
                Double currentBalance = 150.0; // Placeholder until you implement SodexoApi

                // 3. Delegate to the notification service to handle comparison and sending
                notificationService.checkAndNotifyBalanceChange(user, currentBalance);

            } catch (Exception e) {
                Log.error("Error checking balance for user: " + user.getChatId(), e);
            }
        }
        
        Log.info("Finished scheduled balance check.");
    }
}
