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
public class CardDb  extends PanacheMongoEntity{// Optional: map 'cardNumber' field to 'card_number' in DB
    public String cardNumber;
    public Double balance;
    public String currency;
    public String status;
    public Long ownerChatId;    
}
