package com.zaalima.ecommerce.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "processed_events")
@Getter
@NoArgsConstructor
public class ProcessedEvent {
    @Id
    private String eventId;

    @Column(nullable = false)
    private Instant processedAt;

    public ProcessedEvent(String eventId) {
        this.eventId = eventId;
        this.processedAt = Instant.now();
    }
}
