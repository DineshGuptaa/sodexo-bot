package ru.nmedvedev.controller;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import ru.nmedvedev.handler.exception.ErrorResponse;
import ru.nmedvedev.model.CardDb;
import jakarta.ws.rs.core.Response;
import java.util.Map;

@Path("/api/1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped // Required for Quarkus CDI
public class CardController {
    /**
     * GET /api/1/cards/{card}
     * Finds a card document in MongoDB by its cardNumber field.
     */
    @GET
    @Path("/cards/{card}")
    public Response get(@PathParam(value = "card") String card) {
        // Panache provides the .find() method directly on the entity class.
        // We search the 'cardNumber' field in DB for the value from the URL.
        CardDb foundCard = CardDb.find("card", card).firstResult();

            if (foundCard == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Card not found: " + card, 404))
                    .build();
            }

            return Response.ok(foundCard).build();
    }

    /**
     * POST /api/1/cards/{card}
     * Creates or updates a card document in MongoDB.
     */
    @POST
    @Path("/cards/{card}")
    public Response ppost(@PathParam("card") String card, Map<String, Object> body) {
    // 1. Check if the card already exists in the 'userData' (or 'cards') collection
    CardDb cardToSave = CardDb.find("cardNumber", card).firstResult();
    jakarta.ws.rs.core.Response.Status responseStatus;

    if (cardToSave != null) {
        // UPDATE scenario
        responseStatus = jakarta.ws.rs.core.Response.Status.OK;
    } else {
        // CREATE scenario
        cardToSave = new CardDb();
        cardToSave.cardNumber = card;
        responseStatus = jakarta.ws.rs.core.Response.Status.CREATED;
    }

    // 2. Safe Type Mapping (Handling JSON Number to Double conversion)
    if (body.containsKey("balance")) {
        Object balanceObj = body.get("balance");
        if (balanceObj instanceof Number n) {
            cardToSave.balance = n.doubleValue();
        }
    }

    // Use Optional-style or simple null-safe casting for Strings
    cardToSave.currency = (String) body.getOrDefault("currency", cardToSave.currency);
    cardToSave.status = (String) body.getOrDefault("status", cardToSave.status);
    cardToSave.ownerChatId = (Long) body.getOrDefault("owner", cardToSave.ownerChatId);

    // 3. The Idempotent Save
    // For PanacheMongoEntity, persistOrUpdate handles both new and existing records
    cardToSave.persistOrUpdate();

    return Response.status(responseStatus).entity(cardToSave).build();
    }

}
