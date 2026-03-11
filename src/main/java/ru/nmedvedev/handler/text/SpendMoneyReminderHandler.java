package ru.nmedvedev.handler.text;

import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;
import ru.nmedvedev.handler.InputTextHandler;
import ru.nmedvedev.model.UserDb;
import ru.nmedvedev.repository.UserRepository;
import ru.nmedvedev.view.ReplyButtonsProvider;
import ru.nmedvedev.view.Response;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@RequiredArgsConstructor
public class SpendMoneyReminderHandler implements InputTextHandler {

  public static final String SPEND_MONEY_REMINDER_BUTTON_TEXT = "Notify or stop notifying me so I don't forget to spend money";
  public static final String CARD_IS_MISSING = "You haven't entered a card";
  public static final String SUBSCRIBE_MESSAGE = "Now you will receive reminders that you need to spend your balance";
  public static final String UNSUBSCRIBE_MESSAGE = "Balance spending reminders are disabled";

  private final UserRepository userRepository;
  private final ReplyButtonsProvider replyButtonsProvider;

  @Override
  public String getPattern() {
    return SPEND_MONEY_REMINDER_BUTTON_TEXT;
  }

  @Override
  public Uni<Response> handle(Long chatId, String text) {
    return userRepository.findByChatId(chatId)
            .onItem().ifNotNull().transformToUni(this::handleIfUserExists)
            .onItem().ifNull().continueWith(() -> Response.fromText(CARD_IS_MISSING));
  }

  private Uni<Response> handleIfUserExists(UserDb userDb) {
    userDb.setSubscribedToSpendMoneyReminder(!userDb.isSubscribedToSpendMoneyReminder());
    var message = userDb.isSubscribedToSpendMoneyReminder()
            ? SUBSCRIBE_MESSAGE
            : UNSUBSCRIBE_MESSAGE;
    return Uni.createFrom().item(userDb)
            .onItem()
            .call(userRepository::persistOrUpdate)
            .map(v -> Response.withReplyButtons(message, replyButtonsProvider.provideMenuButtons()));
  }
}
