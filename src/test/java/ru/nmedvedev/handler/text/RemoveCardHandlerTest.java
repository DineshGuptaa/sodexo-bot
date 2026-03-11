package ru.nmedvedev.handler.text;

import io.quarkus.test.InjectMock; // Use Quarkus mocking
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject; // Add this import
import org.junit.jupiter.api.Test;
import ru.nmedvedev.model.UserDb;
import ru.nmedvedev.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static ru.nmedvedev.Helper.CARD;
import static ru.nmedvedev.Helper.CHAT;

@QuarkusTest
class RemoveCardHandlerTest {

    @Inject
    private RemoveCardHandler removeCardHandler;
    @InjectMock
    private UserRepository userRepository;

    @Test
    public void shouldRemoveCardForUserAndReturnRemovedMessage() {
        when(userRepository.findByChatId(CHAT))
                .thenReturn(Uni.createFrom().item(UserDb.builder().chatId(CHAT).card(CARD).build()));
        when(userRepository.persistOrUpdate((UserDb) any()))
                .thenReturn(Uni.createFrom().nullItem());

        var actual = removeCardHandler.handle(CHAT, "").await().indefinitely();

        assertEquals("Card " + CARD + " deleted, enter a new one", actual.getText());
        verify(userRepository, times(1))
                .persistOrUpdate(UserDb.builder().chatId(CHAT).build());
    }

    @Test
    public void shouldSaveNothingAndReturnCardIsNotSetMessageIfCardIsNotPresent() {
        when(userRepository.findByChatId(CHAT))
                .thenReturn(Uni.createFrom().item(new UserDb()));

        var actual = removeCardHandler.handle(CHAT, "").await().indefinitely();

        assertEquals("You haven't set a card", actual.getText());
        verify(userRepository, never())
                .persistOrUpdate((UserDb) any());
    }

    @Test
    public void shouldSaveNothingAndReturnCardIsNotSetMessageIfUserIsNotPresent() {
        when(userRepository.findByChatId(CHAT))
                .thenReturn(Uni.createFrom().nullItem());

        var actual = removeCardHandler.handle(CHAT, "").await().indefinitely();

        assertEquals("You haven't set a card", actual.getText());
        verify(userRepository, never())
                .persistOrUpdate((UserDb) any());
    }
}
