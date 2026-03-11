package ru.nmedvedev.service;

import com.pengrad.telegrambot.TelegramBot;
import io.quarkus.test.Mock;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import static org.mockito.Mockito.mock;

public class TelegramBotMockProvider {

    @Produces
    @Mock
    @Singleton // This provides a stable scope that Quarkus can manage
    public TelegramBot mockTelegramBot() {
        return mock(TelegramBot.class);
    }
}
