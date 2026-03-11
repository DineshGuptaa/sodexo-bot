package ru.nmedvedev.service.spendmoneyreminder;

import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import ru.nmedvedev.config.SpendMoneyReminderConfiguration;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@RequiredArgsConstructor
public class SpendMoneyReminderBusinessLogicGateService {
    @Inject
    SpendMoneyReminderConfiguration configuration;

    public boolean needToSendNotification(ReminderDayEnum day, Double amount) {
        switch (day) {
            case NOT_A_DAY_FOR_A_REMINDER: return false;
            case HALF_MONTH: return amount >= configuration.halfMonthAllowedBalance();
            case LAST_WORKING_DAY_MINUS_FOUR: return amount >= configuration.lastWorkingDayMinusFourAllowedBalance();
            case LAST_WORKING_DAY_MINUS_TREE: return amount >= configuration.lastWorkingDayMinusThreeAllowedBalance();
            case LAST_WORKING_DAY_MINUS_ONE: return amount >= configuration.lastWorkingDayMinusOneAllowedBalance();
            default: throw new IllegalArgumentException("Unknown day type");
        }
    }
}