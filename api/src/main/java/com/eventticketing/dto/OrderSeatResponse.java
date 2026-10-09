package com.eventticketing.dto;

public record OrderSeatResponse(String rowLabel, int seatNumber, int priceCents) {}