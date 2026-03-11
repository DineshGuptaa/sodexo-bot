package ru.nmedvedev.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for the response from api.telegram.org (simplest version).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TelegramResponseDto {
    public boolean ok;
    public String description; // Contains error message if ok is false
}