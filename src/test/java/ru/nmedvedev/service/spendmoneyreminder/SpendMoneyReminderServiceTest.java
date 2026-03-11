package ru.nmedvedev.service.spendmoneyreminder;

import io.quarkus.test.InjectMock; // Use Quarkus mocking
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import ru.nmedvedev.model.UserDb;
import ru.nmedvedev.repository.UserRepository;
import ru.nmedvedev.rest.SodexoCustomClient;
import ru.nmedvedev.service.TelegramService;
import ru.nmedvedev.view.ReplyButtonsProvider;

import java.util.Random;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
class SpendMoneyReminderServiceTest {

    @Inject
    private SpendMoneyReminderService service;

    @InjectMock
    private RemindDayProviderService remindDayProviderService;
    @InjectMock
    private SpendMoneyReminderBusinessLogicGateService spendMoneyReminderBusinessLogicGateService;
    @InjectMock
    private UserRepository userRepository;
    @InjectMock
    private TelegramService telegramService;
    @InjectMock
    private ReplyButtonsProvider replyButtonsProvider;
    @InjectMock
    private SodexoCustomClient sodexoClient;

    private static final Long CHAT_ID = new Random().nextLong();
    public static final String CARD = "CARD";

    @Test
    public void doNotFireAnyReminderOnAWrongDay() {
        when(remindDayProviderService.getDay(any())).thenReturn(ReminderDayEnum.NOT_A_DAY_FOR_A_REMINDER);
        service.sendReminders();
        verifyNoInteractions(spendMoneyReminderBusinessLogicGateService);
        verifyNoInteractions(userRepository);
        verifyNoInteractions(telegramService);
        verifyNoInteractions(replyButtonsProvider);
        verifyNoInteractions(sodexoClient);
    }

    @Test
    public void doNotSendIfUserFailedBusinessLogicGate() {
        when(remindDayProviderService.getDay(any())).thenReturn(ReminderDayEnum.LAST_WORKING_DAY_MINUS_ONE);
        when(userRepository.findSubscribedToSpendMoneyReminderWithCardAndChat())
                .thenReturn(Multi.createFrom().items(
                        UserDb.builder().card(CARD).build()
                ));
        when(sodexoClient.getAmount(CARD)).thenReturn(Uni.createFrom().item(100.));
        when(spendMoneyReminderBusinessLogicGateService.needToSendNotification(any(), eq(100.))).thenReturn(false);

        service.sendReminders();
        verifyNoInteractions(telegramService);
    }

    @Test
    public void sendIfEverythingIsOk() {
        when(remindDayProviderService.getDay(any())).thenReturn(ReminderDayEnum.LAST_WORKING_DAY_MINUS_ONE);
        when(userRepository.findSubscribedToSpendMoneyReminderWithCardAndChat())
                .thenReturn(Multi.createFrom().items(
                        UserDb.builder().card(CARD).chatId(CHAT_ID).build()
                ));
        when(sodexoClient.getAmount(CARD)).thenReturn(Uni.createFrom().item(100.));
        when(spendMoneyReminderBusinessLogicGateService.needToSendNotification(any(), eq(100.))).thenReturn(true);

        service.sendReminders();
        verify(telegramService, times(1)).sendMessage(eq(CHAT_ID), any());
    }
}
