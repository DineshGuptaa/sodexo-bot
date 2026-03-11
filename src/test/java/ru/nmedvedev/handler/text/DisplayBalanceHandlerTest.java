package ru.nmedvedev.handler.text;

import io.quarkus.test.InjectMock; // Use Quarkus mocking
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject; // Add this import
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import ru.nmedvedev.model.Balance;
import ru.nmedvedev.model.SodexoData;
import ru.nmedvedev.model.SodexoResponse;
import ru.nmedvedev.model.UserDb;
import ru.nmedvedev.repository.UserRepository;
import ru.nmedvedev.rest.SodexoClient;
import ru.nmedvedev.view.ReplyButtonsProvider;
import ru.nmedvedev.view.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static ru.nmedvedev.Helper.CARD;
import static ru.nmedvedev.Helper.CHAT;

@QuarkusTest
class DisplayBalanceHandlerTest {

    @Inject
    private DisplayBalanceHandler handler;
    @InjectMock
    private UserRepository userRepository;
    @InjectMock
    @RestClient
    private SodexoClient sodexoClient;
    @InjectMock
    private ReplyButtonsProvider replyButtonsProvider;

    @Test
    public void shouldReturnBalanceWithMenuButtonsIfCardIsPresent() {
        when(replyButtonsProvider.provideMenuButtons())
                .thenReturn(List.of("1", "2"));

        when(userRepository.findByChatId(CHAT))
                .thenReturn(Uni.createFrom().item(UserDb.builder().card(CARD).build()));

        var sodexoData = new SodexoData();
        sodexoData.setBalance(new Balance(10d, "INR"));
        when(sodexoClient.getByCard(CARD))
                .thenReturn(Uni.createFrom().item(new SodexoResponse("OK", sodexoData)));
        // When
        var actual = handler.handle(CHAT, "").await().indefinitely();
        // Then
        verify(replyButtonsProvider, times(1)).provideMenuButtons();
        var expected = Response.withReplyButtons("Your balance is 10.00 INR", replyButtonsProvider.provideMenuButtons());
        assertEquals(expected, actual);
    }

    @Test
    public void shouldReturnErrorWithNoButtonsIfCardIsNotPresent() {
        when(userRepository.findByChatId(CHAT))
                .thenReturn(Uni.createFrom().item(new UserDb()));

        // When
        var actual = handler.handle(CHAT, "").await().indefinitely();
        // Then
        verifyNoInteractions(replyButtonsProvider, sodexoClient);
        assertEquals(Response.fromText("You have not entered a card."), actual);
    }

    @Test
    public void shouldReturnErrorWithNoButtonsIfUserIsNotPresent() {
        when(userRepository.findByChatId(CHAT))
                .thenReturn(Uni.createFrom().nullItem());

        // When
        var actual = handler.handle(CHAT, "").await().indefinitely();
        // Then
        verifyNoInteractions(replyButtonsProvider, sodexoClient);
        assertEquals(Response.fromText("You have not entered a card."), actual);
    }

    @Test
    public void shouldReturnCardIsInactiveForAppropriateStatus() {
        when(userRepository.findByChatId(CHAT))
                .thenReturn(Uni.createFrom().item(UserDb.builder().card(CARD).build()));

        when(sodexoClient.getByCard(CARD))
                .thenReturn(Uni.createFrom().item(new SodexoResponse("CARD_IS_NOT_ACTIVE", null)));
        // When
        var actual = handler.handle(CHAT, "").await().indefinitely();
        // Then
        verify(replyButtonsProvider, never()).provideMenuButtons();
        var expected = Response.fromText("Your card is out of date or otherwise inactive. Please delete it and enter a new one.");
        assertEquals(expected, actual);
    }
}
