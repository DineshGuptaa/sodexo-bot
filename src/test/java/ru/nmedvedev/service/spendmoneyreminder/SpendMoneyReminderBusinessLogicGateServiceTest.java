package ru.nmedvedev.service.spendmoneyreminder;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.test.Mock;
import org.junit.jupiter.api.Test;
import ru.nmedvedev.config.SpendMoneyReminderConfiguration;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@QuarkusTest
class SpendMoneyReminderBusinessLogicGateServiceTest {

    @Inject
    SpendMoneyReminderBusinessLogicGateService service;

    @InjectMock
    SpendMoneyReminderConfiguration configuration;

    // This Producer forces the mock into ApplicationScoped
    // which satisfies the requirement for a Normal Scope Proxy.
    @Produces
    @ApplicationScoped
    @Mock
    SpendMoneyReminderConfiguration mockConfig() {
        return Mockito.mock(SpendMoneyReminderConfiguration.class);
    }

    @Test
    public void fireReminderOnHalfMonthIfAmountIsGreaterThanHalf() {
        when(configuration.halfMonthAllowedBalance()).thenReturn(1500.);
        assertTrue(service.needToSendNotification(ReminderDayEnum.HALF_MONTH, 1501.));
        assertFalse(service.needToSendNotification(ReminderDayEnum.HALF_MONTH, 1400.));
    }

    @Test
    public void fireReminderOnLastDay() {
        when(configuration.lastWorkingDayMinusOneAllowedBalance()).thenReturn(400.);
        assertTrue(service.needToSendNotification(ReminderDayEnum.LAST_WORKING_DAY_MINUS_ONE, 401.));
    }
}
