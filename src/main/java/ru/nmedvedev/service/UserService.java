package ru.nmedvedev.service;

import java.util.Optional;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import ru.nmedvedev.model.HistoryDb;
import ru.nmedvedev.model.UserDb;
import ru.nmedvedev.rest.SodexoClient;

@ApplicationScoped
public class UserService {

    @Inject
    UserService userService;
    @RestClient
    SodexoClient sodexoClient;
    
    public Optional<UserDb> findByChatId(Long chatId) {
        // Standard blocking find returns Optional<UserDb> directly
        return UserDb.find("chatId", chatId).firstResultOptional();
    }

    @Transactional
    public UserDb createOrUpdateUser(Long chatId, String userName, String cardNumber) {
        Optional<UserDb> userOpt = findByChatId(chatId);

        return userOpt.map(user -> {
            user.setUserName(userName);
            user.setCard(cardNumber);
            user.update(); // Blocking update returns void
            return user;
        }).orElseGet(() -> {
            UserDb newUser = UserDb.builder()
                    .chatId(chatId)
                    .userName(userName)
                    .card(cardNumber)
                    .subscribed(true)
                    .build();
            newUser.persist(); // Blocking persist returns void
            return newUser;
        });
    }

    @Transactional
    public void updateSubscription(Long chatId, boolean status) {
        findByChatId(chatId).ifPresent(user -> {
            user.setSubscribed(status);
            user.update();
        });
    }

    // Keep the Uni wrapper here only if your RestClient (SodexoClient) is reactive
    private Uni<UserDb> updateLatestOperation(UserDb userDb) {
        if (Boolean.TRUE.equals(userDb.getSubscribed())) {
            return sodexoClient.getByCard(userDb.getCard())
                    .map(response -> {
                        var latest = response.getData().getHistory().get(0);
                        HistoryDb historyDb = new HistoryDb(
                            latest.getAmount(), 
                            latest.getCurrency(), 
                            latest.getLocationName().get(0), 
                            latest.getTime()
                        );
                        userDb.setLatestOperation(historyDb);
                        userDb.update(); // Persist changes locally
                        return userDb;
                    });
        } else {
            return Uni.createFrom().item(userDb);
        }
    }

    public UserDb registerOrUpdateUser(Long chatId, String userName, String cardNumber) {
        return createOrUpdateUser(chatId, userName, cardNumber);
    }
}