package ru.nmedvedev.handler.text;

import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;
import ru.nmedvedev.handler.InputTextHandler;
import ru.nmedvedev.repository.UserRepository;
import ru.nmedvedev.view.Response;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@RequiredArgsConstructor
public class RemoveCardHandler implements InputTextHandler {

    private final UserRepository userRepository;

    @Override
    public String getPattern() {
        return "Delete card";
    }

    @Override
    public Uni<Response> handle(Long chatId, String text) {
        return userRepository.findByChatId(chatId)
                .onItem().ifNotNull().transformToUni(userDb -> {
                    // If card is already null, treat as "not set"
                    if (userDb.getCard() == null) {
                        return Uni.createFrom().item(Response.fromText("You haven't set a card"));
                    }

                    // Perform the update logic
                    var card = userDb.getCard();
                    userDb.setCard(null);

                    return userRepository.persistOrUpdate(userDb)
                            .replaceWith(Response.fromText("Card " + card + " deleted, enter a new one"));
                })
                // If the user record itself is null
                .onItem().ifNull().continueWith(() -> Response.fromText("You haven't set a card"));
    }
}
