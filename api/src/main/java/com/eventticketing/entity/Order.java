package com.eventticketing.entity;

import com.eventticketing.enums.OrderStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID eventId;

    @Column(nullable = false)
    private String section;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_seats", joinColumns = @JoinColumn(name = "order_id"))
    @Column(name = "seat_id", nullable = false)
    private List<String> seatIds = new ArrayList<>();

    @Column(nullable = false)
    private String holdToken;

    @Column(nullable = false)
    private String admissionToken;

    @Column(nullable = false)
    private int totalCents;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(unique = true)
    private String stripeSessionId;

    private String customerEmail;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant completedAt;

    protected Order() {
    }

    public static Order pending(UUID eventId, String section, List<String> seatIds,
                                String holdToken, String admissionToken, int totalCents) {
        Order order = new Order();
        order.id = UUID.randomUUID();
        order.eventId = eventId;
        order.section = section;
        order.seatIds = new ArrayList<>(seatIds);
        order.holdToken = holdToken;
        order.admissionToken = admissionToken;
        order.totalCents = totalCents;
        order.status = OrderStatus.PENDING;
        order.createdAt = Instant.now();
        return order;
    }

    public void attachStripeSession(String stripeSessionId) {
        this.stripeSessionId = stripeSessionId;
    }

    public void markPaid(String customerEmail) {
        this.status = OrderStatus.PAID;
        this.customerEmail = customerEmail;
        this.completedAt = Instant.now();
    }

    public void markRefunded(String customerEmail) {
        this.status = OrderStatus.REFUNDED;
        this.customerEmail = customerEmail;
        this.completedAt = Instant.now();
    }

    public void markExpired() {
        this.status = OrderStatus.EXPIRED;
        this.completedAt = Instant.now();
    }

    public boolean isPending() {
        return status == OrderStatus.PENDING;
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public String getSection() { return section; }
    public List<String> getSeatIds() { return List.copyOf(seatIds); }
    public String getHoldToken() { return holdToken; }
    public String getAdmissionToken() { return admissionToken; }
    public int getTotalCents() { return totalCents; }
    public OrderStatus getStatus() { return status; }
    public String getStripeSessionId() { return stripeSessionId; }
    public String getCustomerEmail() { return customerEmail; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
}