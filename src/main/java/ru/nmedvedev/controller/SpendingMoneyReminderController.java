package ru.nmedvedev.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.ws.rs.QueryParam;
import ru.nmedvedev.service.spendmoneyreminder.SpendMoneyReminderService;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.time.LocalDate;

/**
 * This one is for testing purposes only.
 */
@Path("/api/spend-money-reminder")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Slf4j
public class SpendingMoneyReminderController {

    private final SpendMoneyReminderService spendMoneyReminderService;

    @GET
    public void tryForParticularDate(@QueryParam("date") String localDate) {
        // If no date is provided in URL, use Today's date
        LocalDate targetDate = (localDate == null || localDate.isBlank()) 
        ? LocalDate.now() 
        : LocalDate.parse(localDate);
        try {
            spendMoneyReminderService.sendRemindersForDate(targetDate);
        } catch (Exception e) {
            log.error("Failed to send reminders for " + targetDate, e);
            // Do not rethrow; let the request finish with an error log
        }
    }

}
