package com.hritik.Sharded_Saga_Wallet_System.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "saga_step")
public class SagaStep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "saga_instance_id", nullable = false)
    private Long sagaInstanceId;

    @Column(name = "step_name", nullable = false)
    private String stepName;

    @Column(name = "status", nullable = false)
    private StepStatus status;

    @Column(name = "error_message", nullable = true)
    private String errorMessage;

    // json step data
    @Column(name = "step_data", columnDefinition = "json")
    private String stepData;

    public void markAsRunning() {
        this.status = StepStatus.RUNNING;
    }

    public void markAsCompleted() {
        this.status = StepStatus.COMPLETED;
    }

    public void markAsFailed() {
        this.status = StepStatus.FAILED;
    }

    public void markAsCompensating() {
        this.status = StepStatus.COMPENSATING;
    }

    public void markAsCompensated() {
        this.status = StepStatus.COMPENSATED;
    }
}