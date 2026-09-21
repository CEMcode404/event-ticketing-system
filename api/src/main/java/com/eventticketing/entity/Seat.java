package com.eventticketing.entity;

import com.eventticketing.enums.SeatStatus;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;

import java.util.UUID;

@DynamoDbBean
public class Seat {

    private String id;
    private String eventId;
    private String section;
    private String rowLabel;
    private Integer seatNumber;
    private Integer priceCents;
    private String status;

    private String heldUntil;

    private String holdToken;

    public Seat() {
        // required by the DynamoDB Enhanced Client
    }

    public static Seat newAvailable(UUID eventId, String section, String rowLabel, Integer seatNumber, Integer priceCents) {
        Seat seat = new Seat();
        seat.id = UUID.randomUUID().toString();
        seat.eventId = eventId.toString();
        seat.section = section;
        seat.rowLabel = rowLabel;
        seat.seatNumber = seatNumber;
        seat.priceCents = priceCents;
        seat.status = SeatStatus.AVAILABLE.name();
        return seat;
    }

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "EventStatusIndex")
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getRowLabel() {
        return rowLabel;
    }

    public void setRowLabel(String rowLabel) {
        this.rowLabel = rowLabel;
    }

    public Integer getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(Integer seatNumber) {
        this.seatNumber = seatNumber;
    }

    public Integer getPriceCents() {
        return priceCents;
    }

    public void setPriceCents(Integer priceCents) {
        this.priceCents = priceCents;
    }

    @DynamoDbSecondarySortKey(indexNames = "EventStatusIndex")
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getHeldUntil() {
        return heldUntil;
    }

    public void setHeldUntil(String heldUntil) {
        this.heldUntil = heldUntil;
    }

    public String getHoldToken() {
        return holdToken;
    }

    public void setHoldToken(String holdToken) {
        this.holdToken = holdToken;
    }
}