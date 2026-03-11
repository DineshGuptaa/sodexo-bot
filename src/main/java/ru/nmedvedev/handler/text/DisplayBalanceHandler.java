package ru.nmedvedev.handler.text;

import io.smallrye.mutiny.Uni;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import ru.nmedvedev.handler.InputTextHandler;
import ru.nmedvedev.model.SodexoResponse;
import ru.nmedvedev.model.UserDb;
import ru.nmedvedev.repository.UserRepository;
import ru.nmedvedev.rest.SodexoClient;
import ru.nmedvedev.view.ReplyButtonsProvider;
import ru.nmedvedev.view.Response;

import jakarta.enterprise.context.ApplicationScoped;

import static ru.nmedvedev.rest.Constants.CARD_IS_NOT_ACTIVE_STATUS;
import static ru.nmedvedev.rest.Constants.OK_STATUS;

@Slf4j
@ApplicationScoped
public class DisplayBalanceHandler implements InputTextHandler {

    private final UserRepository userRepository;
    private final SodexoClient sodexoClient;
    private final ReplyButtonsProvider replyButtonsProvider;

    public DisplayBalanceHandler(UserRepository userRepository,
                                 @RestClient SodexoClient sodexoClient,
                                 ReplyButtonsProvider replyButtonsProvider) {
        this.userRepository = userRepository;
        this.sodexoClient = sodexoClient;
        this.replyButtonsProvider = replyButtonsProvider;
    }

    @Override
    public String getPattern() {
        return "Show balance";
    }

    @Override
    public Uni<Response> handle(Long chatId, String text) {
        return userRepository.findByChatId(chatId)
                .onItem().ifNotNull().transform(UserDb::getCard)
                .onItem().ifNotNull().transformToUni(sodexoClient::getByCard)
                .onItem().ifNotNull().transform(this::getResponse)
                .onItem().ifNull().continueWith(() -> Response.fromText("You have not entered a card."));
    }

    private Response getResponse(SodexoResponse sodexoResponse) {
        log.info("Got response {}", sodexoResponse);
        if (sodexoResponse.getStatus().equals(OK_STATUS)) {
            var text = String.format(
                    "Your balance is %.2f %s",
                    sodexoResponse.getData().getBalance().getAvailableAmount(),
                    "INR");
            return Response.withReplyButtons(text, replyButtonsProvider.provideMenuButtons());
        } else if (sodexoResponse.getStatus().equals(CARD_IS_NOT_ACTIVE_STATUS)) {
            return Response.fromText(
                    "Your card is out of date or otherwise inactive. Please delete it and enter a new one."
            );
        } else {
            log.error("Unknown status: {}", sodexoResponse.getStatus());
            return Response.fromText("An error occurred, please try again");
        }
    }
}
