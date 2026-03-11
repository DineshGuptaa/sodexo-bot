package ru.nmedvedev.rest;


import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import ru.nmedvedev.dto.SendMessageRequestDto;
import ru.nmedvedev.dto.TelegramResponseDto;

/**
 * Rest Client for the Telegram Bot API.
 */
@Path("/bot{token}") 
@RegisterRestClient(configKey = "telegram-api")
public interface TelegramRestClient {

    @POST
    @Path("/sendMessage")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    // FIX: This MUST be "token" to match the {token} in @Path
    TelegramResponseDto sendMessage(@PathParam("token") String botToken, SendMessageRequestDto request);
}