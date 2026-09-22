package com.eventticketing.repository;

import com.eventticketing.entity.Seat;
import com.eventticketing.enums.SeatStatus;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedGlobalSecondaryIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class SeatRepository {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SeatRepository.class);

    private static final String TABLE_NAME = "Seats";
    private static final String GSI_NAME = "EventStatusIndex";

    private final DynamoDbTable<Seat> table;

    public SeatRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(Seat.class));
    }

    @PostConstruct
    void ensureTableExists() {
        try {
            table.createTable(CreateTableEnhancedRequest.builder()
                    .globalSecondaryIndices(EnhancedGlobalSecondaryIndex.builder()
                            .indexName(GSI_NAME)
                            .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                            .provisionedThroughput(ProvisionedThroughput.builder()
                                    .readCapacityUnits(5L)
                                    .writeCapacityUnits(5L)
                                    .build())
                            .build())
                    .build());
        } catch (ResourceInUseException alreadyExists) {
        } catch (Exception e) {
            log.error("Could not create/verify the Seats DynamoDB table at startup", e);
        }
    }

    public void save(Seat seat) {
        table.putItem(seat);
    }

    public List<Seat> saveAll(List<Seat> seats) {
        int batchSize = 25;
        for (int i = 0; i < seats.size(); i += batchSize) {
            List<Seat> batch = seats.subList(i, Math.min(i + batchSize, seats.size()));
            batch.forEach(table::putItem);
        }
        return seats;
    }

    public Optional<Seat> findById(String id) {
        Seat seat = table.getItem(Key.builder().partitionValue(id).build());
        return Optional.ofNullable(seat);
    }

    public List<Seat> findAvailableByEvent(UUID eventId, int limit) {
        return table.index(GSI_NAME)
                .query(QueryConditional.keyEqualTo(Key.builder()
                        .partitionValue(eventId.toString())
                        .sortValue(SeatStatus.AVAILABLE.name())
                        .build()))
                .stream()
                .flatMap(page -> page.items().stream())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public boolean tryAcquireHold(String seatId, String holdToken, Duration ttl) {
        Instant now = Instant.now();
        Seat seat = new Seat();
        seat.setId(seatId);
        seat.setHeldUntil(now.plus(ttl).toString());
        seat.setHoldToken(holdToken);

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":now", AttributeValue.builder().s(now.toString()).build());
        expressionValues.put(":available", AttributeValue.builder().s(SeatStatus.AVAILABLE.name()).build());

        Expression condition = Expression.builder()
                .expression("#status = :available AND (attribute_not_exists(heldUntil) OR heldUntil < :now)")
                .expressionNames(Map.of("#status", "status"))
                .expressionValues(expressionValues)
                .build();

        try {
            table.updateItem(UpdateItemEnhancedRequest.builder(Seat.class)
                    .item(seat)
                    .ignoreNulls(true)
                    .conditionExpression(condition)
                    .build());
            return true;
        } catch (ConditionalCheckFailedException seatUnavailable) {
            return false;
        }
    }

    public void releaseHold(String seatId) {
        Seat seat = new Seat();
        seat.setId(seatId);
        seat.setHeldUntil(null);
        seat.setHoldToken(null);
        table.updateItem(UpdateItemEnhancedRequest.builder(Seat.class)
                .item(seat)
                .ignoreNulls(false)
                .build());
    }
}