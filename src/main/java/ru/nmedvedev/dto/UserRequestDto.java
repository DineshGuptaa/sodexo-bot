package ru.nmedvedev.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRequestDto {
    public String userName;
    public Long chatId;
    public String cardNumber;
    public boolean subscribed;
    public boolean reminderEnabled;
}
