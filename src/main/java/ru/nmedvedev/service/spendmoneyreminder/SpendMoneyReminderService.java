package ru.nmedvedev.service.spendmoneyreminder;

import java.time.LocalDate;
import java.util.List;

import org.bson.types.ObjectId;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import ru.nmedvedev.model.UserDb;
import ru.nmedvedev.repository.UserRepository;
import ru.nmedvedev.rest.SodexoCustomClient;
import ru.nmedvedev.service.TelegramService;
import ru.nmedvedev.view.ReplyButtonsProvider;
import ru.nmedvedev.view.Response;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class SpendMoneyReminderService {

    private final RemindDayProviderService remindDayProviderService;
    private final SpendMoneyReminderBusinessLogicGateService spendMoneyReminderBusinessLogicGateService;
    private final UserRepository userRepository;
    private final TelegramService telegramService;
    private final ReplyButtonsProvider replyButtonsProvider;
    private final SodexoCustomClient sodexoClient;

    @Scheduled(
        cron = "0 0 12 * * ?", 
        concurrentExecution = Scheduled.ConcurrentExecution.SKIP // Prevent overlapping loops
    )
    public void sendReminders() {
        LocalDate date = LocalDate.now();
        sendRemindersForDate(date);
    }

    public void sendRemindersForDate(LocalDate date) {
        log.info("Reminder triggered for date {}", date);
        ReminderDayEnum day = remindDayProviderService.getDay(date);
        
        if (day == ReminderDayEnum.NOT_A_DAY_FOR_A_REMINDER) {
            log.info("It is NOT a day for a reminder");
            return;
        }

        userRepository.findSubscribedToSpendMoneyReminderWithCardAndChat()
                .onItem().transformToUniAndMerge(user -> 
                    sodexoClient.getAmount(user.getCard())
                        .onItem().transform(amount -> new ExtendedUser(user, amount))
                        // Gracefully handle individual Sodexo API failures
                        .onFailure().recoverWithItem(new ExtendedUser(user, null))
                )
                .filter(user -> user.getAmount() != null)
                .filter(user -> spendMoneyReminderBusinessLogicGateService.needToSendNotification(day, user.getAmount()))
                // FIX: Use .invoke() for synchronous void methods like sendMessage
                .invoke(user -> {
                    String message = String.format(day.messageFormat, user.getAmount());
                    List<String> buttons = replyButtonsProvider.provideMenuButtons();
                    telegramService.sendMessage(user.getChatId(), Response.withReplyButtons(message, buttons));
                })
                .subscribe().with(
                    user -> log.info("Sent notification for user {} to chat {} with amount {}", 
                                    user.getId(), user.getChatId(), user.getAmount()),
                    err -> log.error("Failed to send reminders in batch", err)
                );
    }

    @Value
    private static class ExtendedUser {
        private final UserDb user;
        private final Double amount;

        public ExtendedUser(UserDb user, Double amount) {
            this.user = user;
            this.amount = amount;
        }

        public Double getAmount() { return amount; }
        public Long getChatId() { return user.getChatId(); }
        
        // Add this method to resolve the "cannot find symbol" error
        public ObjectId getId() { 
            return user.id; 
        }
    }
}
