package ru.nmedvedev.controller;

import java.util.List;
import java.util.Map;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import ru.nmedvedev.dto.UserRegistrationRequest;
import ru.nmedvedev.dto.UserRequestDto;
import ru.nmedvedev.model.UserDb;
import ru.nmedvedev.repository.UserRepository;
import ru.nmedvedev.service.UserService;

@Slf4j
@Path("/api/1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserController {

    @Inject
    UserService userService;

    @Inject
    UserRepository userRepository; // Inject repository for direct lookups

    // GET List of all users
    @GET
    @Path("/users")
    public Uni<List<UserDb>> getUserList() {
        return userRepository.listAll();
    }

    // GET a single user by their unique Chat ID
    @GET
    @Path("/users/{chatId}")
    public Uni<Response> getUser(@PathParam("chatId") Long chatId) {
        return userRepository.findByChatId(chatId)
                .onItem().ifNotNull().transform(user -> Response.ok(user).build())
                .onItem().ifNull().continueWith(Response.status(Response.Status.NOT_FOUND).build());
    }

    // @POST
    // @Path("/users/{chatId}")
    // public Uni<Response> createUser(UserRequestDto request) {
    //     return Uni.createFrom().item(request)
    //             .emitOn(Infrastructure.getDefaultWorkerPool())
    //             .map(req -> {
    //                 // Perform the blocking service calls
    //                 UserDb user = userService.registerOrUpdateUser(req.chatId, req.userName, req.cardNumber);
    //                 userService.updateSubscription(req.chatId, req.subscribed);
    //                 return user;
    //             })
    //             // FIX: Explicitly tell the map it's returning a Response
    //             .<Response>map(user -> {
    //                 return Response.status(Response.Status.CREATED)
    //                         .entity(Map.of(
    //                                 "status", "success",
    //                                 "message", "User " + user.getUserName() + " created/updated",
    //                                 "data", user))
    //                         .build();
    //             })
    //             .onFailure().recoverWithItem(e -> {
    //                 return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
    //                         .entity(Map.of(
    //                                 "status", "error",
    //                                 "message", "Failed to create/update user",
    //                                 "exception", e.getMessage() != null ? e.getMessage() : "Unknown Error"))
    //                         .build();
    //             });
    // }

    @DELETE
    @Path("/users/{chatId}")
    public Uni<Response> deleteUser(@PathParam("chatId") Long chatId) {
        return userRepository.delete("chatId", chatId)
                .map(deletedCount -> {
                    if (deletedCount > 0) {
                        // Success JSON: {"message": "User deleted successfully", "id": 123456789}
                        return Response.ok(Map.of(
                                "status", "success",
                                "message", "User deleted successfully",
                                "chatId", chatId)).build();
                    } else {
                        // Failure JSON: {"error": "User not found", "id": 123456789}
                        return Response.status(Response.Status.NOT_FOUND)
                                .entity(Map.of(
                                        "status", "error",
                                        "message", "User not found",
                                        "chatId", chatId))
                                .build();
                    }
                });
    }

    @PUT
    @Path("/users/{chatId}")
    public Uni<Response> updateUser(@PathParam("chatId") Long chatId, UserRequestDto request) {
        return userRepository.findByChatId(chatId)
                .onItem().ifNotNull().transformToUni(user -> {
                    // Update fields only if they are provided in the JSON
                    if (request.cardNumber != null) {
                        user.setCard(request.cardNumber);
                    }

                    // Update boolean flags
                    user.setSubscribed(request.subscribed);
                    user.setUserName(request.userName);
                    user.setSubscribed(request.subscribed);
                    user.setSubscribedToSpendMoneyReminder(request.reminderEnabled);

                    // We must return the user object after the persist operation completes
                    return userRepository.persistOrUpdate(user)
                            .replaceWith(Response.ok(user).build());
                })
                .onItem().ifNull().continueWith(() -> Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\": \"User with ID " + chatId + " not found\"}")
                        .build());
    }

    
    @POST
    @Path("/users/register")
    public Response register(@Valid UserRegistrationRequest request) {
        try {
            // Using the 3-argument method you defined for DataInitializer
            UserDb user = userService.registerOrUpdateUser(
                request.getChatId(),
                request.getUserName(),
                request.getCardNumber()
            );

            log.info("Successfully registered user: " + request.getChatId());
            
            return Response.status(Response.Status.CREATED)
                    .entity(user)
                    .build();

        } catch (Exception e) {
            log.error("Failed to register user: " + request.getChatId(), e);
            
            // Return a structured JSON error response
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                        "error", "Registration failed",
                        "message", e.getMessage(),
                        "chatId", request.getChatId()
                    ))
                    .build();
        }
    }
}