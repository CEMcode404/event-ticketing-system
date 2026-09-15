package com.eventticketing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "sale_opens_at", nullable = false)
    private Instant saleOpensAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Event() {}

    public Event(String name, Instant saleOpensAt) {
        this.name = name;
        this.saleOpensAt = saleOpensAt;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Instant getSaleOpensAt() {
        return saleOpensAt;
    }

    public void setSaleOpensAt(Instant saleOpensAt) {
        this.saleOpensAt = saleOpensAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}