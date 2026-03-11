package ru.nmedvedev.handler.text;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import ru.nmedvedev.model.UserDb;
import ru.nmedvedev.repository.UserRepository;
import ru.nmedvedev.view.ReplyButtonsProvider;
import ru.nmedvedev.view.Response;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class SpendMoneyReminderHandlerTest {

    public static final long CHAT_ID = new Random().nextLong();

    @Inject // CDI managed injection
    SpendMoneyReminderHandler spendMoneyReminderHandler;

    @InjectMock // Register as a mock within the Quarkus container
    UserRepository userRepository;

    @InjectMock
    ReplyButtonsProvider replyButtonsProvider;

    @BeforeEach
    public void setUp() {
        when(replyButtonsProvider.provideMenuButtons()).thenReturn(List.of("1", "2"));
        // Fixed: Ensure the return type matches Uni<UserDb>
        when(userRepository.persistOrUpdate((UserDb) any()))
                .thenReturn(Uni.createFrom().nullItem());
    }

    @Test
    public void shouldBeAbleToSubscribe() {
        when(userRepository.findByChatId(CHAT_ID))
                .thenReturn(Uni.createFrom().item(UserDb.builder().subscribedToSpendMoneyReminder(false).build()));

        var actual = spendMoneyReminderHandler.handle(CHAT_ID, "some text").await().indefinitely();

        verify(userRepository, times(1))
                .persistOrUpdate(argThat((ArgumentMatcher<UserDb>) UserDb::isSubscribedToSpendMoneyReminder));

        assertEquals(Response.withReplyButtons("Now you will receive reminders that you need to spend your balance",
                List.of("1", "2")), actual);
    }

    @Test
    public void shouldBeAbleToUnsubscibe() {
        when(userRepository.findByChatId(CHAT_ID))
                .thenReturn(Uni.createFrom().item(UserDb.builder().subscribedToSpendMoneyReminder(true).build()));

        var actual = spendMoneyReminderHandler.handle(CHAT_ID, "some text").await().indefinitely();

        verify(userRepository, times(1))
                .persistOrUpdate(argThat(((UserDb user) -> !user.isSubscribedToSpendMoneyReminder())));

        assertEquals(Response.withReplyButtons("Balance spending reminders are disabled", List.of("1", "2")), actual);
    }
}
