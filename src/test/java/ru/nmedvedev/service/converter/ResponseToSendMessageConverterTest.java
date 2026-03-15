package ru.nmedvedev.service.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.nmedvedev.Helper.CHAT;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.pengrad.telegrambot.model.request.ReplyKeyboardRemove;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import ru.nmedvedev.view.Response;

@QuarkusTest
class ResponseToSendMessageConverterTest {

    private static final String TEXT = "text";
    
    @Inject
    private ResponseToSendMessageConverter converter;

    @Test
    public void shouldConvertWithReplyButtons() {
        var response = Response.withReplyButtons(TEXT, List.of("b1", "b2", "b3"));

        var actual = converter.convert(response, CHAT);

        // FIX: The library stores chat_id as a String internally. 
        // We must convert our Long CHAT to String for the assertion to pass.
        assertEquals(String.valueOf(CHAT), String.valueOf(actual.getParameters().get("chat_id")));
        assertEquals(TEXT, actual.getParameters().get("text"));
    }

    @MethodSource
    @ParameterizedTest
    public void shouldConvertWithRemoveReplyKeyboardIfButtonsAreAbsent(Response response) {
        var actual = converter.convert(response, CHAT);

        // FIX: Use String.valueOf() to match the internal library type
        assertEquals(String.valueOf(CHAT), String.valueOf(actual.getParameters().get("chat_id")));
        assertEquals(TEXT, actual.getParameters().get("text"));
        assertEquals(ReplyKeyboardRemove.class, actual.getParameters().get("reply_markup").getClass());
    }

    public static Stream<Response> shouldConvertWithRemoveReplyKeyboardIfButtonsAreAbsent() {
        return Stream.of(
                Response.withReplyButtons(TEXT, null),
                Response.withReplyButtons(TEXT, List.of())
        );
    }
}