package ru.nmedvedev.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ru.nmedvedev.Helper.CARD;
import static ru.nmedvedev.Helper.CHAT;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatcher;

import io.quarkus.test.InjectMock; // Use Quarkus mocking
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import jakarta.inject.Inject;
import ru.nmedvedev.model.Balance;
import ru.nmedvedev.model.History;
import ru.nmedvedev.model.HistoryDb;
import ru.nmedvedev.model.SodexoData;
import ru.nmedvedev.model.SodexoResponse;
import ru.nmedvedev.model.UserDb;
import ru.nmedvedev.repository.UserRepository;
import ru.nmedvedev.rest.Constants;
import ru.nmedvedev.rest.SodexoClient;
import ru.nmedvedev.view.ReplyButtonsProvider;
import ru.nmedvedev.view.Response;

@QuarkusTest
public class BalanceChangeCheckerTest {

    @Inject
    private BalanceChangeChecker checker;

    @InjectMock
    @RestClient
    private SodexoClient sodexoClient;
    @InjectMock
    private UserRepository userRepository;
    @InjectMock
    private TelegramService telegramService;
    @InjectMock
    private ReplyButtonsProvider replyButtonsProvider;

    private Consumer<Map.Entry<UserDb, Response>> stubConsumer = (e) -> {
    };


    @Test
    public void shouldCheckBalanceChangeOnlyForSubscribedUsers() {
        var chat1 = CHAT + 1;
        var chat2 = CHAT - 1;

        var card1 = CARD + "1";
        var card2 = CARD + "2";
        when(userRepository.findSubscribedWithCard())
                .thenReturn(Multi.createFrom().items(
                        UserDb.builder().chatId(chat1).card(card1).build(),
                        UserDb.builder().chatId(chat2).card(card2).build()
                ));

        var sodexoResponse = new SodexoResponse();
        sodexoResponse.setStatus(Constants.OK_STATUS);
        var data = new SodexoData();
        data.setHistory(List.of());
        data.setBalance(new Balance());
        sodexoResponse.setData(data);

        when(sodexoClient.getByCard(anyString()))
                .thenReturn(Uni.createFrom().item(sodexoResponse));

        //checker.check().subscribe().with(stubConsumer);
        checker.check()
        .subscribe().withSubscriber(AssertSubscriber.create())
        .awaitItems(1) // Wait for at least one item
        .assertCompleted();

        verify(sodexoClient, times(1)).getByCard(card1);
        verify(sodexoClient, times(1)).getByCard(card2);
    }

    @MethodSource
    @ParameterizedTest
    public void shouldUpdatePreviousLatestOperation(HistoryDb latestOperation) {
        var sodexoResponse = new SodexoResponse();
        sodexoResponse.setStatus(Constants.OK_STATUS);
        var data = new SodexoData();
        data.setHistory(List.of(
                History.builder().amount(200d).currency("INR").locationName(List.of("123")).time("2").build(),
                History.builder().amount(100d).currency("INR").locationName(List.of("456")).time("1").build()
        ));
        data.setBalance(new Balance());
        sodexoResponse.setData(data);

        when(sodexoClient.getByCard(CARD))
                .thenReturn(Uni.createFrom().item(sodexoResponse));

        when(userRepository.findSubscribedWithCard())
                .thenReturn(Multi.createFrom().items(
                        UserDb.builder().chatId(CHAT).card(CARD).latestOperation(latestOperation).build()
                ));
        when(userRepository.persistOrUpdate(any(UserDb.class)))
        .thenReturn(Uni.createFrom().item(UserDb.builder().build()));

        checker.check().subscribe().with(stubConsumer);

        verify(userRepository, times(1))
                .persistOrUpdate(argThat((ArgumentMatcher<UserDb>) userDb -> userDb.getLatestOperation().equals(
                        new HistoryDb(200d, "INR", "123", "2")
                )));
    }

    @Test
    public void shouldSendNothingIfHistoryIsEmpty() {
        var sodexoResponse = new SodexoResponse();
        sodexoResponse.setStatus(Constants.OK_STATUS);
        var data = new SodexoData();
        data.setHistory(List.of());
        sodexoResponse.setData(data);

        when(sodexoClient.getByCard(CARD))
                .thenReturn(Uni.createFrom().item(sodexoResponse));

        when(userRepository.findSubscribedWithCard())
                .thenReturn(Multi.createFrom().items(
                        UserDb.builder().chatId(CHAT).card(CARD).build()
                ));

        checker.check().subscribe().with(stubConsumer);

        verify(userRepository, never()).persistOrUpdate((UserDb) any());
        verifyNoInteractions(telegramService);
    }

    @MethodSource
    @ParameterizedTest
    public void shouldNotifyWithCurrentBalanceAndMenuButtons(Double amount, String operationMessage) {
        var sodexoResponse = new SodexoResponse();
        sodexoResponse.setStatus(Constants.OK_STATUS);
        var data = new SodexoData();
        data.setHistory(List.of(History.builder().amount(amount).currency("INR").locationName(List.of("name")).build()));
        data.setBalance(new Balance(123.45, "INR"));
        sodexoResponse.setData(data);
        when(sodexoClient.getByCard(CARD)).thenReturn(Uni.createFrom().item(sodexoResponse));

        when(replyButtonsProvider.provideMenuButtons()).thenReturn(List.of("1", "2"));

        when(userRepository.findSubscribedWithCard())
                .thenReturn(Multi.createFrom().items(
                        UserDb.builder().chatId(CHAT).card(CARD).build()
                ));
        when(userRepository.persistOrUpdate(any(UserDb.class)))
        .thenReturn(Uni.createFrom().item(UserDb.builder().build()));

        checker.check().subscribe().with(stubConsumer);

        verify(replyButtonsProvider, times(1)).provideMenuButtons();
        verify(telegramService, times(1))
                .sendMessage(CHAT, Response.withReplyButtons(operationMessage + "\nCurrent balance 123.45 rub", replyButtonsProvider.provideMenuButtons()));
    }

    @Test
    public void shouldSendOneMessageWithManyOperationsAndSaveLatestIfLatestOperationIsNotPresent() {
        var sodexoResponse = new SodexoResponse();
        sodexoResponse.setStatus(Constants.OK_STATUS);
        var data = new SodexoData();
        data.setHistory(List.of(
                History.builder().amount(100d).currency("INR").locationName(List.of("name1")).time("zzz").build(),
                History.builder().amount(-200d).currency("INR").locationName(List.of("name2")).build()
        ));
        data.setBalance(new Balance(123.45, "INR"));
        sodexoResponse.setData(data);
        when(sodexoClient.getByCard(CARD)).thenReturn(Uni.createFrom().item(sodexoResponse));

        when(replyButtonsProvider.provideMenuButtons()).thenReturn(List.of("1", "2"));

        when(userRepository.findSubscribedWithCard())
                .thenReturn(Multi.createFrom().items(
                        UserDb.builder().chatId(CHAT).card(CARD).build()
                ));
        when(userRepository.persistOrUpdate(any(UserDb.class)))
        .thenReturn(Uni.createFrom().item(UserDb.builder().build()));

        checker.check().subscribe().with(stubConsumer);

        verify(replyButtonsProvider, times(1)).provideMenuButtons();
        verify(telegramService, times(1))
                .sendMessage(CHAT, Response.withReplyButtons("Withdrawal 200.00 rub from name2\nDeposit 100.00 rub from name1\nCurrent balance 123.45 rub", replyButtonsProvider.provideMenuButtons()));
        verify(userRepository, times(1)).persistOrUpdate(UserDb
                .builder()
                .chatId(CHAT)
                .card(CARD)
                .latestOperation(new HistoryDb(100d, "INR", "name1", "zzz"))
                .build());
    }

    @Test
    public void shouldSendOneMessageWithManyOperationsAndSaveLatestIfLatestOperationIsPresent() {
        var sodexoResponse = new SodexoResponse();
        sodexoResponse.setStatus(Constants.OK_STATUS);
        var data = new SodexoData();
        data.setHistory(List.of(
                History.builder().amount(100d).currency("INR").locationName(List.of("name1")).time("zzz").build(),
                History.builder().amount(-200d).currency("INR").locationName(List.of("name2")).build(),
                History.builder().amount(-300d).currency("INR").locationName(List.of("LOC")).time("TIME").build()
        ));
        data.setBalance(new Balance(123.45, "INR"));
        sodexoResponse.setData(data);
        when(sodexoClient.getByCard(CARD)).thenReturn(Uni.createFrom().item(sodexoResponse));

        when(replyButtonsProvider.provideMenuButtons()).thenReturn(List.of("1", "2"));

        when(userRepository.findSubscribedWithCard())
                .thenReturn(Multi.createFrom().items(
                        UserDb.builder().chatId(CHAT).card(CARD).latestOperation(new HistoryDb(-300d, "INR", "LOC", "TIME")).build()
                ));
        when(userRepository.persistOrUpdate(any(UserDb.class)))
        .thenReturn(Uni.createFrom().item(UserDb.builder().build()));

        checker.check().subscribe().with(stubConsumer);

        verify(replyButtonsProvider, times(1)).provideMenuButtons();
        verify(telegramService, times(1))
                .sendMessage(CHAT, Response.withReplyButtons("Withdrawal 200.00 rub from name2\nDeposit 100.00 rub from name1\nCurrent balance 123.45 rub", replyButtonsProvider.provideMenuButtons()));
        verify(userRepository, times(1)).persistOrUpdate(UserDb
                .builder()
                .chatId(CHAT)
                .card(CARD)
                .latestOperation(new HistoryDb(100d, "INR", "name1", "zzz"))
                .build());
    }

    @ParameterizedTest
    @ValueSource(strings = {Constants.CARD_IS_NOT_ACTIVE_STATUS, "blah-blah"})
    public void shouldIgnoreUsersWithNonOkStatusResponse(String status) {
        var okChat = CHAT + 1;
        var nonOkChat = CHAT - 1;

        var okCard = CARD + "1";
        var nonOkCard = CARD + "2";
        when(userRepository.findSubscribedWithCard())
                .thenReturn(Multi.createFrom().items(
                        UserDb.builder().chatId(okChat).card(okCard).build(),
                        UserDb.builder().chatId(nonOkChat).card(nonOkCard).build()
                ));

        var okResponse = new SodexoResponse();
        okResponse.setStatus(Constants.OK_STATUS);
        var data = new SodexoData();
        data.setHistory(List.of(History.builder().amount(1d).currency("INR").locationName(List.of("name")).build()));
        data.setBalance(new Balance(123.45, "INR"));
        okResponse.setData(data);

        var nonOkResponse = new SodexoResponse();
        nonOkResponse.setStatus(status);

        when(sodexoClient.getByCard(okCard))
                .thenReturn(Uni.createFrom().item(okResponse));
        when(sodexoClient.getByCard(nonOkCard))
                .thenReturn(Uni.createFrom().item(nonOkResponse));

        when(userRepository.persistOrUpdate(any(UserDb.class)))
        .thenReturn(Uni.createFrom().item(UserDb.builder().build()));

        // may be removed
        when(replyButtonsProvider.provideMenuButtons()).thenReturn(List.of("1", "2"));

        checker.check().subscribe().with(stubConsumer);

        verify(replyButtonsProvider, times(1)).provideMenuButtons();
        verify(telegramService, times(1)).sendMessage(eq(okChat), any());
    }

    // Providers
    private static Stream<HistoryDb> shouldUpdatePreviousLatestOperation() {
        return Stream.of(
                null,
                HistoryDb
                        .builder()
                        .amount(100d)
                        .currency("INR")
                        .locationName("456")
                        .time("1")
                        .build()
        );
    }

    private static Stream<Arguments> shouldNotifyWithCurrentBalanceAndMenuButtons() {
        return Stream.of(
                Arguments.of(123.45d, "Deposit 123.45 rub from name"),
                Arguments.of(-123.45d, "Withdrawal 123.45 rub from name")
        );
    }
}

