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
public class CardDisplayHandler implements InputTextHandler {

    private final UserRepository userRepository;
    private final ReplyButtonsProvider replyButtonsProvider;

    @Override
    public String getPattern() {
        return "Show my card";
    }

    @Override
    public Uni<Response> handle(Long chatId, String text) {
        return userRepository.findByChatId(chatId)
                .onItem().ifNotNull().transform(UserDb::getCard)
                .onItem().ifNotNull().transform(card -> Response.withReplyButtons("Your card " + card, replyButtonsProvider.provideMenuButtons()))
                .onItem().ifNull().continueWith(() -> Response.fromText("You have not entered a card."));
    }
}
