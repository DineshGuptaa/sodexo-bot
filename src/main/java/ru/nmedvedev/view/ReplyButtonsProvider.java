package ru.nmedvedev.view;

import static ru.nmedvedev.handler.text.SpendMoneyReminderHandler.SPEND_MONEY_REMINDER_BUTTON_TEXT;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class ReplyButtonsProvider {
    public List<String> provideMenuButtons() {
        return List.of(
                "Show balance",
                "Delete card",
                "Subscribe or unsubscribe from balance notifications",
                SPEND_MONEY_REMINDER_BUTTON_TEXT,
                "Show my card"
        );
    }
}
