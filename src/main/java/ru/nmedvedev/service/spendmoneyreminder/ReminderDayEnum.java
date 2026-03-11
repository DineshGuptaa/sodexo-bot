package ru.nmedvedev.service.spendmoneyreminder;

public enum ReminderDayEnum {
    NOT_A_DAY_FOR_A_REMINDER(""),
    HALF_MONTH("Buddy. Half a month has passed. You have %.2f rub left. Don't forget to spend)"),
    LAST_WORKING_DAY_MINUS_FOUR("You have %.2f rub in your account. They will burn out in 3 days."),
    LAST_WORKING_DAY_MINUS_TREE("Second to last day to spend your %.2f rub."),
    LAST_WORKING_DAY_MINUS_ONE("Achtung, comrade! You still have %.2f rub on your card, and today is the last day to spend them!!!");

    public final String messageFormat;

    ReminderDayEnum(String messageFormat) {
        this.messageFormat = messageFormat;
    }
}
