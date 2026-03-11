package ru.nmedvedev.handler.text;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import ru.nmedvedev.handler.InputTextHandler;
import ru.nmedvedev.model.HistoryDb;
import ru.nmedvedev.model.UserDb;
import ru.nmedvedev.repository.UserRepository;
import ru.nmedvedev.rest.SodexoClient;
import ru.nmedvedev.view.ReplyButtonsProvider;
import ru.nmedvedev.view.Response;

@ApplicationScoped
public class NotificationSubscriptionHandler implements InputTextHandler {

    private final UserRepository userRepository;
    private final ReplyButtonsProvider replyButtonsProvider;
    private final SodexoClient sodexoClient;

    public NotificationSubscriptionHandler(UserRepository userRepository,
                                           ReplyButtonsProvider replyButtonsProvider,
                                           @RestClient SodexoClient sodexoClient) {
        this.userRepository = userRepository;
        this.replyButtonsProvider = replyButtonsProvider;
        this.sodexoClient = sodexoClient;
    }

    @Override
    public String getPattern() {
        return "Subscribe or unsubscribe from balance notifications";
    }

    @Override
    public Uni<Response> handle(Long chatId, String text) {
        return userRepository.findByChatId(chatId)
                .onItem().ifNotNull().transformToUni(this::handleIfUserExists)
                .onItem().ifNull().continueWith(() -> Response.fromText("You haven't entered a card"));
    }

    private Uni<Response> handleIfUserExists(UserDb userDb) {
        userDb.setSubscribed(!userDb.getSubscribed());
        
        var message = userDb.getSubscribed()
                ? "Now I will inform you about all deposits and withdrawals"
                : "Now I will not inform you about all deposits and withdrawals";

        return Uni.createFrom().item(userDb)
                .onItem().transformToUni(this::updateLatestOperation) // Calls the method below
                .call(userRepository::persistOrUpdate)
                .map(ignored -> Response.withReplyButtons(message, replyButtonsProvider.provideMenuButtons()));
    }

    // ADD THIS METHOD TO FIX THE COMPILATION ERROR
    private Uni<UserDb> updateLatestOperation(UserDb userDb) {
        if (Boolean.TRUE.equals(userDb.getSubscribed())) {
            return sodexoClient.getByCard(userDb.getCard())
                    .onItem().transform(response -> {
                        if (response != null && response.getData() != null && response.getData().getHistory() != null && !response.getData().getHistory().isEmpty()) {
                            var latest = response.getData().getHistory().get(0);
                            userDb.setLatestOperation(new HistoryDb(
                                    latest.getAmount(),
                                    latest.getCurrency(),
                                    latest.getLocationName().get(0),
                                    latest.getTime()
                            ));
                        }
                        return userDb;
                    })
                    // If the Sodexo API fails, we still want to toggle the subscription
                    .onFailure().recoverWithItem(userDb);
        } else {
            return Uni.createFrom().item(userDb);
        }
    }
}