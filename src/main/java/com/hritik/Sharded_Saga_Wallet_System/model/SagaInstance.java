package com.hritik.Sharded_Saga_Wallet_System.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.apache.calcite.model.JsonType;

import com.fasterxml.jackson.annotation.JsonSubTypes.Type;

@Entity
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
@Table(name = "saga_instance")
public class SagaInstance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SagaStatus status = SagaStatus.STARTED;

    @Type(JsonType.class)
    @Column(name = "context", columnDefinition = "json")
    private String context;

    @Column(name = "current_step")
    private String currentStep;

    public void markAsRunning() {
        this.status = SagaStatus.RUNNING;
    }

    public void markAsFailed() {
        this.status = SagaStatus.FAILED;
    }

    public void markAsCompensated() {
        this.status = SagaStatus.COMPENSATED;
    }

    public void markAsCompleted() {
        this.status = SagaStatus.COMPLETED;
    }

    public void markAsCompensating() {
        this.status = SagaStatus.COMPENSATING;
    }
}
