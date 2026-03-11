package ru.nmedvedev.service;

import org.jboss.logging.Logger;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import ru.nmedvedev.model.UserDb;

@ApplicationScoped
public class DataInitializer {

    private static final Logger LOG = Logger.getLogger(DataInitializer.class);

    @Inject
    UserService userService;

    @Inject
    CardService cardService;

    void onStart(@Observes StartupEvent ev) {               
        LOG.info("Initializing unique sample data...");

        if (UserDb.count() == 0) {
            // Unique ID for the user (Dinesh)
            Long dineshChatId = 323456789L;
            String dineshCardNumber = "8011-0000-1111-2222";

            // 1. Create the Card first with the owner's unique ID
            cardService.upsertCard(dineshCardNumber, 1500.50, "INR");
            
            // 2. Register the User and link them to that specific card
            userService.registerOrUpdateUser(dineshChatId, "DineshG", dineshCardNumber);
            
            // 3. Enable their reminders specifically
            userService.updateSubscription(dineshChatId, true);

            LOG.info("Sample data linked via Chat ID " + dineshChatId + " successfully.");
        } else {
            LOG.info("Database already contains data. Skipping initialization.");
        }
    }
}