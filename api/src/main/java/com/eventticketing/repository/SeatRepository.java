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
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteResult;
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedGlobalSecondaryIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactUpdateItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;
import software.amazon.awssdk.services.dynamodb.model.Update;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.time.Duration;
import java.time.Instant;
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
    private static final String SECTION_GSI_NAME = "EventSectionIndex";

    private static final int BATCH_SIZE = 25;
    private static final int MAX_BATCH_RETRIES = 5;

    private final DynamoDbEnhancedClient enhancedClient;
    private final DynamoDbTable<Seat> table;
    private final DynamoDbClient dynamoDbClient;

    public SeatRepository(DynamoDbEnhancedClient enhancedClient, DynamoDbClient dynamoDbClient) {
        this.enhancedClient = enhancedClient;
        this.table = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(Seat.class));
        this.dynamoDbClient = dynamoDbClient;
    }

    @PostConstruct
    void ensureTableExists() {
        try {
            table.createTable(CreateTableEnhancedRequest.builder()
                    .globalSecondaryIndices(gsi(GSI_NAME), gsi(SECTION_GSI_NAME))
                    .build());
        } catch (ResourceInUseException alreadyExists) {
            // table already exists
        } catch (Exception e) {
            log.error("Could not create/verify the Seats DynamoDB table at startup", e);
        }
    }

    private static EnhancedGlobalSecondaryIndex gsi(String indexName) {
        return EnhancedGlobalSecondaryIndex.builder()
                .indexName(indexName)
                .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                .provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits(5L)
                        .writeCapacityUnits(5L)
                        .build())
                .build();
    }

    public void save(Seat seat) {
        table.putItem(seat);
    }

    public List<Seat> saveAll(List<Seat> seats) {
        for (int i = 0; i < seats.size(); i += BATCH_SIZE) {
            List<Seat> pending = seats.subList(i, Math.min(i + BATCH_SIZE, seats.size()));
            int attempt = 0;

            while (!pending.isEmpty()) {
                if (attempt > MAX_BATCH_RETRIES) {
                    throw new IllegalStateException(pending.size() + " seats could not be written after retries");
                }
                if (attempt > 0) {
                    sleepQuietly(50L << attempt);
                }

                WriteBatch.Builder<Seat> builder = WriteBatch.builder(Seat.class).mappedTableResource(table);
                pending.forEach(builder::addPutItem);
                WriteBatch batch = builder.build();

                BatchWriteResult result = enhancedClient.batchWriteItem(r -> r.addWriteBatch(batch));
                pending = result.unprocessedPutItemsForTable(table);
                attempt++;
            }
        }
        return seats;
    }

    public Optional<Seat> findById(String id) {
        Seat seat = table.getItem(Key.builder().partitionValue(id).build());
        return Optional.ofNullable(seat);
    }

    public List<Seat> findAllByEvent(UUID eventId) {
        return table.index(GSI_NAME)
                .query(QueryConditional.keyEqualTo(Key.builder()
                        .partitionValue(eventId.toString())
                        .build()))
                .stream()
                .flatMap(page -> page.items().stream())
                .collect(Collectors.toList());
    }

    public List<Seat> findAvailableInSection(UUID eventId, String section, int limit) {
        Expression notHeld = Expression.builder()
                .expression("attribute_not_exists(heldUntil) OR heldUntil < :now")
                .expressionValues(Map.of(":now", AttributeValue.fromS(Instant.now().toString())))
                .build();

        return table.index(SECTION_GSI_NAME)
                .query(QueryEnhancedRequest.builder()
                        .queryConditional(QueryConditional.keyEqualTo(Key.builder()
                                .partitionValue(Seat.eventSectionKey(eventId, section))
                                .sortValue(SeatStatus.AVAILABLE.name())
                                .build()))
                        .filterExpression(notHeld)
                        .build())
                .stream()
                .flatMap(page -> page.items().stream())
                .limit(limit)
                .toList();
    }

    public boolean tryHoldBlock(List<String> seatIds, String holdToken, Duration ttl) {
        Instant now = Instant.now();
        String heldUntil = now.plus(ttl).toString();
        Expression condition = holdableCondition(now);

        try {
            enhancedClient.transactWriteItems(r -> {
                for (String seatId : seatIds) {
                    Seat seat = new Seat();
                    seat.setId(seatId);
                    seat.setHeldUntil(heldUntil);
                    seat.setHoldToken(holdToken);

                    r.addUpdateItem(table, TransactUpdateItemEnhancedRequest.builder(Seat.class)
                            .item(seat)
                            .ignoreNulls(true)
                            .conditionExpression(condition)
                            .build());
                }
            });
            return true;
        } catch (TransactionCanceledException conflict) {
            return false;
        }
    }

    private static Expression holdableCondition(Instant now) {
        return Expression.builder()
                .expression("#status = :available AND (attribute_not_exists(heldUntil) OR heldUntil < :now)")
                .expressionNames(Map.of("#status", "status"))
                .expressionValues(Map.of(
                        ":now", AttributeValue.fromS(now.toString()),
                        ":available", AttributeValue.fromS(SeatStatus.AVAILABLE.name())))
                .build();
    }

    public void releaseHold(String seatId, String holdToken) {
        try {
            dynamoDbClient.updateItem(UpdateItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .key(Map.of("id", AttributeValue.fromS(seatId)))
                    .updateExpression("REMOVE heldUntil, holdToken")
                    .conditionExpression("holdToken = :token")
                    .expressionAttributeValues(Map.of(":token", AttributeValue.fromS(holdToken)))
                    .build());
        } catch (ConditionalCheckFailedException notOurHold) {
            // already released, or now held by someone else: nothing to do
        }
    }

    public boolean trySellBlock(List<String> seatIds, String holdToken) {
        List<TransactWriteItem> sales = seatIds.stream()
                .map(seatId -> TransactWriteItem.builder()
                        .update(Update.builder()
                                .tableName(TABLE_NAME)
                                .key(Map.of("id", AttributeValue.fromS(seatId)))
                                .updateExpression("SET #status = :sold REMOVE heldUntil, holdToken")
                                .conditionExpression("#status = :available AND holdToken = :token")
                                .expressionAttributeNames(Map.of("#status", "status"))
                                .expressionAttributeValues(Map.of(
                                        ":sold", AttributeValue.fromS(SeatStatus.SOLD.name()),
                                        ":available", AttributeValue.fromS(SeatStatus.AVAILABLE.name()),
                                        ":token", AttributeValue.fromS(holdToken)))
                                .build())
                        .build())
                .toList();

        try {
            dynamoDbClient.transactWriteItems(TransactWriteItemsRequest.builder()
                    .transactItems(sales)
                    .build());
            return true;
        } catch (TransactionCanceledException holdLost) {
            return false;
        }
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while retrying batch write", e);
        }
    }
}