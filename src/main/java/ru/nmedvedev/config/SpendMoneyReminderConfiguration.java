package ru.nmedvedev.config;

import jakarta.enterprise.context.ApplicationScoped;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "spend-money-reminder")
@ApplicationScoped
public interface SpendMoneyReminderConfiguration {

    @WithName("half-month-allowed-balance")
    @WithDefault("500") // This prevents the crash!
    double halfMonthAllowedBalance();
    @WithName("last-working-day-minus-four-allowed-balance")
    @WithDefault("200")
    double lastWorkingDayMinusFourAllowedBalance();
    @WithName("last-working-day-minus-three-allowed-balance")
    @WithDefault("100") // This prevents the crash!     
    double lastWorkingDayMinusThreeAllowedBalance();
    @WithName("last-working-day-minus-one-allowed-balance")
    @WithDefault("50")
    double lastWorkingDayMinusOneAllowedBalance();

}
