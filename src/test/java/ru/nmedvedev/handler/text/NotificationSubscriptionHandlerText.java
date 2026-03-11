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

import org.junit.jupiter.api.Test;

import io.quarkus.test.InjectMock; // Use Quarkus mocking
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject; // Add this import
import ru.nmedvedev.model.History;
import ru.nmedvedev.model.HistoryDb;
import ru.nmedvedev.model.SodexoData;
import ru.nmedvedev.model.SodexoResponse;
import ru.nmedvedev.model.UserDb;
import ru.nmedvedev.repository.UserRepository;
import ru.nmedvedev.rest.SodexoClient;
import ru.nmedvedev.view.ReplyButtonsProvider;
import ru.nmedvedev.view.Response;

@QuarkusTest
public class NotificationSubscriptionHandlerText {

    @Inject
    private NotificationSubscriptionHandler handler;

    @InjectMock
    private UserRepository userRepository;
    @InjectMock
    private ReplyButtonsProvider replyButtonsProvider;
    @InjectMock
    private SodexoClient sodexoClient;

    @Test
    public void shouldSaveEnabledNotificationWithLatestOperationAndReturnTextWithMenuButtons() {
        when(sodexoClient.getByCard(CARD))
                .thenReturn(Uni.createFrom().item(sodexoResponse()));
        when(userRepository.findByChatId(CHAT))
                .thenReturn(Uni.createFrom().item(UserDb.builder().card(CARD).subscribed(false).build()));
        when(userRepository.persistOrUpdate((UserDb) any()))
                .thenReturn(Uni.createFrom().nullItem());
        when(replyButtonsProvider.provideMenuButtons())
                .thenReturn(List.of("1", "2", "3"));

        var actual = handler.handle(CHAT, "").await().indefinitely();

        var historyDb = HistoryDb.builder()
                .amount(100d)
                .currency("currency")
                .locationName("location")
                .time("time")
                .build();
        verify(userRepository, times(1)).persistOrUpdate(UserDb.builder().card(CARD).subscribed(true).latestOperation(historyDb).build());
        verify(replyButtonsProvider, times(1)).provideMenuButtons();
        assertEquals(Response.withReplyButtons("Now I will inform you about all deposits and debits.", replyButtonsProvider.provideMenuButtons()), actual);
    }

    @Test
    public void shouldSaveDisabledNotificationAndReturnTextWithMenuButtons() {
        when(userRepository.findByChatId(CHAT))
                .thenReturn(Uni.createFrom().item(UserDb.builder().subscribed(true).build()));
        when(userRepository.persistOrUpdate((UserDb) any()))
                .thenReturn(Uni.createFrom().nullItem());
        when(replyButtonsProvider.provideMenuButtons())
                .thenReturn(List.of("1", "2", "3"));

        var actual = handler.handle(CHAT, "").await().indefinitely();

        verify(userRepository, times(1)).persistOrUpdate(UserDb.builder().subscribed(false).build());
        verify(replyButtonsProvider, times(1)).provideMenuButtons();
        assertEquals(Response.withReplyButtons("Now I won't inform you about all the deposits and debits.", replyButtonsProvider.provideMenuButtons()), actual);
        verifyNoInteractions(sodexoClient);
    }

    @Test
    public void shouldReturnTextErrorWithNoButtonsIfUserIsNotPresent() {
        when(userRepository.findByChatId(CHAT))
                .thenReturn(Uni.createFrom().nullItem());

        var actual = handler.handle(CHAT, "").await().indefinitely();

        verify(userRepository, never()).persistOrUpdate((UserDb) any());
        verifyNoInteractions(replyButtonsProvider);
        assertEquals(Response.fromText("You have not entered a card."), actual);
    }

    private SodexoResponse sodexoResponse() {
        var sodexoData = new SodexoData();
        sodexoData.setHistory(List.of(
                new History(100d, "currency", List.of("location"), "mcc", "merchantId", "time", 10)
        ));
        return new SodexoResponse("OK", sodexoData);
    }
}
