package ru.nmedvedev.handler.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ru.nmedvedev.Helper.CARD;
import static ru.nmedvedev.Helper.CHAT;

import java.util.List;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.test.InjectMock; // Use Quarkus mocking
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject; // Add this import
import ru.nmedvedev.model.SodexoResponse;
import ru.nmedvedev.model.UserDb;
import ru.nmedvedev.repository.UserRepository;
import ru.nmedvedev.rest.Constants;
import ru.nmedvedev.rest.SodexoClient;
import ru.nmedvedev.view.ReplyButtonsProvider;
import ru.nmedvedev.view.Response;

@QuarkusTest
class DefaultHandlerTest {

    @Inject
    private DefaultHandler defaultHandler;

    @InjectMock
    private UserRepository userRepository;
    @InjectMock
    @RestClient
    private SodexoClient sodexoClient;
    @InjectMock
    private ReplyButtonsProvider replyButtonsProvider;

    @Test
    public void shouldReturnAcceptMessageWithMenuButtonsAndPersistIfCardIsValid() {
        var sodexoResponse = new SodexoResponse();
        sodexoResponse.setStatus("OK");

        when(sodexoClient.getByCard(CARD))
                .thenReturn(Uni.createFrom().item(sodexoResponse));
        when(userRepository.findByChatId(CHAT))
                .thenReturn(Uni.createFrom().nullItem());
        when(replyButtonsProvider.provideMenuButtons())
                .thenReturn(List.of("1", "2"));
        when(userRepository.persistOrUpdate((UserDb) any()))
                .thenReturn(Uni.createFrom().nullItem());

        var actual = defaultHandler.handle(CHAT, CARD).await().indefinitely();

        verify(userRepository, times(1)).persistOrUpdate(UserDb.builder().chatId(CHAT).card(CARD).build());
        verify(replyButtonsProvider, times(1)).provideMenuButtons();
        assertEquals(Response.withReplyButtons("I saved card " + CARD, replyButtonsProvider.provideMenuButtons()), actual);
        verify(sodexoClient, times(1)).getByCard(CARD);
    }

    @Test
    public void shouldReturnAcceptMessageWithMenuButtonsAndUpdateUserIfCardIsValid() {
        var user = UserDb.builder().chatId(CHAT).card(CARD).build();

        var sodexoResponse = new SodexoResponse();
        sodexoResponse.setStatus("OK");

        when(sodexoClient.getByCard(CARD))
                .thenReturn(Uni.createFrom().item(sodexoResponse));
        when(userRepository.findByChatId(CHAT))
                .thenReturn(Uni.createFrom().item(user));
        when(replyButtonsProvider.provideMenuButtons())
                .thenReturn(List.of("1", "2"));
        when(userRepository.persistOrUpdate((UserDb) any()))
                .thenReturn(Uni.createFrom().nullItem());

        var actual = defaultHandler.handle(CHAT, CARD).await().indefinitely();

        verify(sodexoClient, times(1)).getByCard(CARD);
        var expectedToSave = UserDb.builder().chatId(CHAT).card(CARD).build();
        verify(userRepository, times(1)).persistOrUpdate(expectedToSave);
        verify(replyButtonsProvider, times(1)).provideMenuButtons();
        assertEquals(Response.withReplyButtons("I saved card " + CARD, replyButtonsProvider.provideMenuButtons()), actual);
    }

    @Test
    public void shouldReturnErrorTextIfSodexoClientReturnedError() {
        var sodexoResponse = new SodexoResponse();
        sodexoResponse.setStatus("ERROR");

        when(userRepository.findByChatId(CHAT))
                .thenReturn(Uni.createFrom().nullItem());
        when(sodexoClient.getByCard(CARD))
                .thenReturn(Uni.createFrom().item(sodexoResponse));

        var actual = defaultHandler.handle(CHAT, CARD).await().indefinitely();

        assertEquals(Response.fromText("Cards " + CARD + " does not exist, please try again"), actual);
        verify(sodexoClient, times(1)).getByCard(CARD);
        verify(userRepository, never()).persist((UserDb) any());
        verifyNoInteractions(replyButtonsProvider);
    }

    @Test
    public void shouldReturnCardInvalidIfSodexoClientReturnedCardInactive() {
        var sodexoResponse = new SodexoResponse();
        sodexoResponse.setStatus(Constants.CARD_IS_NOT_ACTIVE_STATUS);

        when(userRepository.findByChatId(CHAT))
                .thenReturn(Uni.createFrom().nullItem());
        when(sodexoClient.getByCard(CARD))
                .thenReturn(Uni.createFrom().item(sodexoResponse));

        var actual = defaultHandler.handle(CHAT, CARD).await().indefinitely();

        assertEquals(Response.fromText("Cards " + CARD + " is outdated or inactive for other reasons, please try again"), actual);
        verify(sodexoClient, times(1)).getByCard(CARD);
        verify(userRepository, never()).persist((UserDb) any());
        verifyNoInteractions(replyButtonsProvider);
    }

    @Test
    public void shouldReturnErrorMessageWithButtonsForBadMessageIfCardIsPresent() {
        when(userRepository.findByChatId(CHAT))
                .thenReturn(Uni.createFrom().item(UserDb.builder().card(CARD).build()));
        when(replyButtonsProvider.provideMenuButtons())
                .thenReturn(List.of("1", "2"));

        var actual = defaultHandler.handle(CHAT, "ZZZZZZZ").await().indefinitely();

        verify(replyButtonsProvider, times(1)).provideMenuButtons();
        verify(userRepository, never()).persistOrUpdate((UserDb) any());
        verifyNoInteractions(sodexoClient);
        assertEquals(Response.withReplyButtons("Unknown request :(", replyButtonsProvider.provideMenuButtons()), actual);
    }

    @Test
    public void shouldReturnInvalidCardTextWithNoButtonsForBadMessageIfCardIsNotPresent() {
        when(userRepository.findByChatId(CHAT))
                .thenReturn(Uni.createFrom().item(new UserDb()));

        var actual = defaultHandler.handle(CHAT, "ZZZZZZZ").await().indefinitely();

        assertEquals(Response.fromText("Invalid card format"), actual);
        verify(userRepository, never()).persistOrUpdate((UserDb) any());
        verifyNoInteractions(sodexoClient, replyButtonsProvider);
    }

    @Test
    public void shouldReturnErrorTextWithNoButtonsForBadMessageIfUserIsNotPresent() {
        when(userRepository.findByChatId(CHAT))
                .thenReturn(Uni.createFrom().nullItem());

        var actual = defaultHandler.handle(CHAT, "ZZZZZZZ").await().indefinitely();

        assertEquals(Response.fromText("Invalid card format"), actual);
        verify(userRepository, never()).persistOrUpdate((UserDb) any());
        verifyNoInteractions(sodexoClient, replyButtonsProvider);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1 2 3", "123", " 123", "123  "})
    public void shouldRemoveSpaces(String card) {
        var trimmed = "123";
        var sodexoResponse = new SodexoResponse();
        sodexoResponse.setStatus("OK");

        when(sodexoClient.getByCard(trimmed))
                .thenReturn(Uni.createFrom().item(sodexoResponse));
        when(userRepository.findByChatId(CHAT))
                .thenReturn(Uni.createFrom().nullItem());
        when(replyButtonsProvider.provideMenuButtons())
                .thenReturn(List.of("1", "2"));
        when(userRepository.persistOrUpdate((UserDb) any()))
                .thenReturn(Uni.createFrom().nullItem());

        var actual = defaultHandler.handle(CHAT, card).await().indefinitely();

        assertEquals(Response.withReplyButtons("I saved card 123", replyButtonsProvider.provideMenuButtons()), actual);
        verify(sodexoClient, times(1)).getByCard(trimmed);
        verify(userRepository, times(1)).persistOrUpdate(UserDb.builder().chatId(CHAT).card(trimmed).build());
    }

    @Test
    public void shouldReturnInternalErrorTextIfRestClientFailed() {

    }


}
