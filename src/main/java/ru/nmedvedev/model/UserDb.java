package ru.nmedvedev.model;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@MongoEntity(collection = "userData")
@Builder
public class UserDb extends PanacheMongoEntity {

    private Long chatId;
    private String card;
    private String userName; // Added field
    @Builder.Default // This ensures 'false' is used if not set in builder
    private Boolean subscribed = false;

    @Builder.Default // This ensures 'false' is used if not set in builder
    private boolean subscribedToSpendMoneyReminder = false;

    private HistoryDb latestOperation;
    @Builder.Default
    private Double lastBalance = 0.0;

    // ADD THIS TO FIX COMPILER ERRORS
    public org.bson.types.ObjectId getId() {
        return this.id;
    }
}