package ru.nmedvedev.config;

import com.pengrad.telegrambot.TelegramBot;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;
import io.smallrye.config.ConfigMapping;
import jakarta.enterprise.inject.Produces;
import lombok.NoArgsConstructor;

@ConfigMapping(prefix = "spend-money-reminder")
@ApplicationScoped
@NoArgsConstructor
public class TelegramConfiguration {

    @Produces
    TelegramBot telegramBot(@ConfigProperty(name = "telegram.bot.token") String botToken) {
        return new TelegramBot(botToken);
    }
}
