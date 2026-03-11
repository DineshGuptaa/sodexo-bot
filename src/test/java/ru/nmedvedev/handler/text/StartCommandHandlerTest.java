package ru.nmedvedev.handler.text;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import ru.nmedvedev.view.Response;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest // Add this
class StartCommandHandlerTest {
    
    @Inject
    private StartCommandHandler startCommandHandler;

    @Test
    public void shouldReturnProperTextForStartCommand() {
        var response = startCommandHandler.handle(0L, "");

        assertEquals(Response.fromText("Please enter your card number"), response.await().indefinitely());
    }
}
