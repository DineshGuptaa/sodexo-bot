package ru.nmedvedev.handler.text;

import io.quarkus.test.InjectMock; // Use Quarkus mocking
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import ru.nmedvedev.model.UserDb;
import ru.nmedvedev.repository.UserRepository;
import ru.nmedvedev.view.ReplyButtonsProvider;
import ru.nmedvedev.view.Response;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ru.nmedvedev.Helper.CARD;
import static ru.nmedvedev.Helper.CHAT;

@QuarkusTest
class CardDisplayHandlerTest {

    @Inject
    private CardDisplayHandler handler;
    @InjectMock
    private UserRepository userRepository;
    @InjectMock
    private ReplyButtonsProvider replyButtonsProvider;

    @Test
    public void shouldReturnCardWithButtonsIfCardIsPresent() {
        var buttons = List.of("Elem1", "elem2");

        when(userRepository.findByChatId(CHAT))
                .thenReturn(Uni.createFrom().item(UserDb.builder().card(CARD).build()));
        when(replyButtonsProvider.provideMenuButtons())
                .thenReturn(buttons);

        var actual = handler.handle(CHAT, "").await().indefinitely();

        var expected = Response.withReplyButtons("Your card " + CARD, buttons);
        assertEquals(expected, actual);
    }

    @Test
    public void shouldReturnErrorMessageWithNoButtonsIfCardIsNotPresent() {
        when(userRepository.findByChatId(CHAT))
                .thenReturn(Uni.createFrom().item(new UserDb()));

        var actual = handler.handle(CHAT, "").await().indefinitely();

        var expected = Response.fromText("You have not entered a card.");
        assertEquals(expected, actual);
        verifyNoInteractions(replyButtonsProvider);
    }

    @Test
    public void shouldReturnErrorMessageIfUserIsNotPresent() {
        when(userRepository.findByChatId(CHAT))
                .thenReturn(Uni.createFrom().nullItem());

        var actual = handler.handle(CHAT, "").await().indefinitely();

        var expected = Response.fromText("You have not entered a card.");
        assertEquals(expected, actual);
        verifyNoInteractions(replyButtonsProvider);
    }
}
