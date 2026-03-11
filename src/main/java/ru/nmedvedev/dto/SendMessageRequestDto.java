package ru.nmedvedev.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * DTO for sending a message to a chat.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(Include.NON_NULL)
public class SendMessageRequestDto {
    @JsonProperty("chat_id")
    public Long chatId;

    public String text;
    
    /**
     * Set this to NULL to match your successful curl (plain text).
     * Set this to "Markdown", "MarkdownV2", or "HTML" for formatting (Strict parser!).
     */
    @JsonProperty("parse_mode") 
    public String parseMode;
}
