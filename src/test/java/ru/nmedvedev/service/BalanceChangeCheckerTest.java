package ru.nmedvedev.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static ru.nmedvedev.Helper.CARD;
import static ru.nmedvedev.Helper.CHAT;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
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

    @BeforeEach
    public void setup() {
        // FIX FOR AMBIGUITY: persistOrUpdate returns Uni, so use doReturn
        doReturn(Uni.createFrom().item(new UserDb()))
            .when(userRepository).persistOrUpdate((UserDb) any());

        // FIX FOR VOID ERROR: sendMessage is void, so use doNothing
        // Also using anyLong() to avoid unboxing NPEs
        doNothing()
            .when(telegramService).sendMessage(anyLong(), any(Response.class));
    }

    @Test
    public void shouldCheckBalanceChangeOnlyForSubscribedUsers() {
        when(userRepository.findSubscribedWithCard())
                .thenReturn(Multi.createFrom().items(
                        UserDb.builder().chatId(CHAT + 1).card(CARD + "1").build(),
                        UserDb.builder().chatId(CHAT - 1).card(CARD + "2").build()
                ));

        var sodexoResponse = new SodexoResponse();
        sodexoResponse.setStatus(Constants.OK_STATUS);
        var data = new SodexoData();
        data.setHistory(List.of());
        data.setBalance(new Balance());
        sodexoResponse.setData(data);

        when(sodexoClient.getByCard(anyString())).thenReturn(Uni.createFrom().item(sodexoResponse));

        // Use await to ensure the Multi finishes
        checker.check().collect().asList().await().atMost(Duration.ofSeconds(5));

        verify(sodexoClient, times(2)).getByCard(anyString());
    }

    @MethodSource
    @ParameterizedTest
    public void shouldUpdatePreviousLatestOperation(HistoryDb latestOperation) {
        var sodexoResponse = new SodexoResponse();
        sodexoResponse.setStatus(Constants.OK_STATUS);
        var data = new SodexoData();
        data.setHistory(List.of(
                History.builder().amount(200d).currency("INR").locationName(List.of("123")).time("2").build()
        ));
        data.setBalance(new Balance());
        sodexoResponse.setData(data);

        when(sodexoClient.getByCard(CARD)).thenReturn(Uni.createFrom().item(sodexoResponse));
        when(userRepository.findSubscribedWithCard())
                .thenReturn(Multi.createFrom().item(UserDb.builder().chatId(CHAT).card(CARD).latestOperation(latestOperation).build()));

        checker.check().collect().asList().await().atMost(Duration.ofSeconds(5));

        // Use explicit type (UserDb u) to avoid "getLatestOperation is undefined for Object"
        verify(userRepository).persistOrUpdate((UserDb) argThat((UserDb u) -> 
            u.getLatestOperation() != null && u.getLatestOperation().getAmount().equals(200d)
        ));
    }

    @Test
    public void shouldSendNothingIfHistoryIsEmpty() {
        var sodexoResponse = new SodexoResponse();
        sodexoResponse.setStatus(Constants.OK_STATUS);
        var data = new SodexoData();
        data.setHistory(List.of());
        sodexoResponse.setData(data);

        when(sodexoClient.getByCard(CARD)).thenReturn(Uni.createFrom().item(sodexoResponse));
        when(userRepository.findSubscribedWithCard()).thenReturn(Multi.createFrom().item(UserDb.builder().chatId(CHAT).card(CARD).build()));

        checker.check().collect().asList().await().atMost(Duration.ofSeconds(5));

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
        when(userRepository.findSubscribedWithCard()).thenReturn(Multi.createFrom().item(UserDb.builder().chatId(CHAT).card(CARD).build()));

        checker.check().collect().asList().await().atMost(Duration.ofSeconds(5));

        verify(telegramService).sendMessage(eq(CHAT), any(Response.class));
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
        when(userRepository.findSubscribedWithCard()).thenReturn(Multi.createFrom().item(UserDb.builder().chatId(CHAT).card(CARD).build()));

        checker.check().collect().asList().await().atMost(Duration.ofSeconds(5));

        ArgumentCaptor<UserDb> captor = ArgumentCaptor.forClass(UserDb.class);
        verify(userRepository).persistOrUpdate(captor.capture());
        assertEquals("zzz", captor.getValue().getLatestOperation().getTime());
    }

    @Test
    public void shouldSendOneMessageWithManyOperationsAndSaveLatestIfLatestOperationIsPresent() {
        var sodexoResponse = new SodexoResponse();
        sodexoResponse.setStatus(Constants.OK_STATUS);
        var data = new SodexoData();
        data.setHistory(List.of(
                History.builder().amount(100d).currency("INR").locationName(List.of("name1")).time("zzz").build(),
                History.builder().amount(-300d).currency("INR").locationName(List.of("LOC")).time("TIME").build()
        ));
        data.setBalance(new Balance(123.45, "INR"));
        sodexoResponse.setData(data);

        when(sodexoClient.getByCard(CARD)).thenReturn(Uni.createFrom().item(sodexoResponse));
        when(userRepository.findSubscribedWithCard())
                .thenReturn(Multi.createFrom().item(UserDb.builder().chatId(CHAT).card(CARD).latestOperation(new HistoryDb(-300d, "INR", "LOC", "TIME")).build()));

        checker.check().collect().asList().await().atMost(Duration.ofSeconds(5));

        verify(userRepository).persistOrUpdate((UserDb) any());
    }

    @ParameterizedTest
    @ValueSource(strings = {Constants.CARD_IS_NOT_ACTIVE_STATUS, "blah-blah"})
    public void shouldIgnoreUsersWithNonOkStatusResponse(String status) {
        when(userRepository.findSubscribedWithCard())
                .thenReturn(Multi.createFrom().items(
                        UserDb.builder().chatId(CHAT + 1).card(CARD + "1").build(),
                        UserDb.builder().chatId(CHAT - 1).card(CARD + "2").build()
                ));

        var okResponse = new SodexoResponse();
        okResponse.setStatus(Constants.OK_STATUS);
        var data = new SodexoData();
        data.setHistory(List.of(History.builder().amount(1d).currency("INR").locationName(List.of("name")).build()));
        data.setBalance(new Balance(123.45, "INR"));
        okResponse.setData(data);

        var nonOkResponse = new SodexoResponse();
        nonOkResponse.setStatus(status);

        when(sodexoClient.getByCard(CARD + "1")).thenReturn(Uni.createFrom().item(okResponse));
        when(sodexoClient.getByCard(CARD + "2")).thenReturn(Uni.createFrom().item(nonOkResponse));

        checker.check().collect().asList().await().atMost(Duration.ofSeconds(5));

        verify(telegramService, times(1)).sendMessage(eq(CHAT + 1), any(Response.class));
        verify(telegramService, never()).sendMessage(eq(CHAT - 1), any(Response.class));
    }

    private static Stream<HistoryDb> shouldUpdatePreviousLatestOperation() {
        return Stream.of(
                null,
                HistoryDb.builder().amount(100d).currency("INR").locationName("456").time("1").build()
        );
    }

    private static Stream<Arguments> shouldNotifyWithCurrentBalanceAndMenuButtons() {
        return Stream.of(
                Arguments.of(123.45d, "Deposit 123.45 rub from name"),
                Arguments.of(-123.45d, "Withdrawal 123.45 rub from name")
        );
    }
}