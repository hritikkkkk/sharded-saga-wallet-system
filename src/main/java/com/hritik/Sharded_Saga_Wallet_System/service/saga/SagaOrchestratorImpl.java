package com.hritik.Sharded_Saga_Wallet_System.service.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hritik.Sharded_Saga_Wallet_System.exceptions.ResourceNotFoundException;
import com.hritik.Sharded_Saga_Wallet_System.exceptions.SagaException;
import com.hritik.Sharded_Saga_Wallet_System.model.SagaInstance;
import com.hritik.Sharded_Saga_Wallet_System.model.SagaStatus;
import com.hritik.Sharded_Saga_Wallet_System.model.SagaStep;
import com.hritik.Sharded_Saga_Wallet_System.model.StepStatus;
import com.hritik.Sharded_Saga_Wallet_System.repository.SagaInstanceRepository;
import com.hritik.Sharded_Saga_Wallet_System.repository.SagaStepRepository;
import com.hritik.Sharded_Saga_Wallet_System.service.saga.SagaContext;
import com.hritik.Sharded_Saga_Wallet_System.service.saga.SagaOrchestrator;
import com.hritik.Sharded_Saga_Wallet_System.service.saga.SagaStepInterface;
import com.hritik.Sharded_Saga_Wallet_System.service.saga.steps.SagaStepFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.core.type.TypeReference; // ✅ correct
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SagaOrchestratorImpl implements SagaOrchestrator {

    private final ObjectMapper objectMapper;
    private final SagaInstanceRepository sagaInstanceRepository;
    private final SagaStepRepository sagaStepRepository;
    private final SagaStepFactory sagaStepFactory;

    @Override
    @Transactional
    public Long startSaga(SagaContext context) {
        if (context == null || context.getData() == null || context.getData().isEmpty()) {
            throw new SagaException("Saga context cannot be null or empty");
        }

        try {
            String contextJson = objectMapper.writeValueAsString(context.getData());

            SagaInstance sagaInstance = SagaInstance.builder()
                    .context(contextJson)
                    .status(SagaStatus.STARTED)
                    .build();

            sagaInstance = sagaInstanceRepository.save(sagaInstance);

            log.info("Saga started successfully with id {}", sagaInstance.getId());

            return sagaInstance.getId();

        } catch (JsonProcessingException e) {
            log.error("Error serializing saga context", e);
            throw new SagaException("Failed to serialize saga context", e);
        } catch (DataAccessException e) {
            log.error("Database error while starting saga", e);
            throw new SagaException("Failed to start saga due to database error", e);
        }
    }

    @Override
    @Transactional
    public boolean executeStep(Long sagaInstanceId, String stepName) {
        log.info("Executing step '{}' for saga instance {}", stepName, sagaInstanceId);

        if (sagaInstanceId == null) {
            throw new IllegalArgumentException("Saga instance ID cannot be null");
        }

        if (stepName == null || stepName.trim().isEmpty()) {
            throw new IllegalArgumentException("Step name cannot be null or empty");
        }

        try {
            SagaInstance sagaInstance = sagaInstanceRepository.findById(sagaInstanceId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Saga instance not found with id: " + sagaInstanceId));

            System.out.println("**********saga instance get context"+sagaInstance.getContext());

            // Check if saga is in valid state for execution
            if (sagaInstance.getStatus() == SagaStatus.FAILED ||
                    sagaInstance.getStatus() == SagaStatus.COMPENSATING ||
                    sagaInstance.getStatus() == SagaStatus.COMPENSATED) {
                log.warn("Cannot execute step for saga in {} state", sagaInstance.getStatus());
                return false;
            }

            SagaStepInterface step = sagaStepFactory.getStepName(stepName);
            if (step == null) {
                log.error("Saga step implementation not found for step name: {}", stepName);
                throw new SagaException("Saga step not found: " + stepName);
            }

            // Get or create saga step record
            SagaStep sagaStepDB = sagaStepRepository
                    .findBySagaInstanceIdAndStepNameAndStatus(sagaInstanceId, stepName, StepStatus.PENDING)
                    .orElseGet(() -> {
                        // Check if step was already completed
                        var completed = sagaStepRepository
                                .findBySagaInstanceIdAndStepNameAndStatus(
                                        sagaInstanceId, stepName, StepStatus.COMPLETED);
                        if (completed.isPresent()) {
                            log.info("Step '{}' already completed for saga {}", stepName, sagaInstanceId);
                            return null;
                        }

                        return SagaStep.builder()
                                .sagaInstanceId(sagaInstanceId)
                                .stepName(stepName)
                                .status(StepStatus.PENDING)
                                .build();
                    });

            // If step already completed, return success
            if (sagaStepDB == null) {
                return true;
            }

            // Save step if it's new
            if (sagaStepDB.getId() == null) {
                sagaStepDB = sagaStepRepository.save(sagaStepDB);
            }

            // Parse saga context
            SagaContext sagaContext = parseSagaContext(sagaInstance.getContext());
//            SagaContext sagaContext = objectMapper.readValue(sagaInstance.getContext(), SagaContext.class);
            System.out.println("parsed saga context"+sagaContext.toString());
//            Map<String, Object> contextMap = objectMapper.readValue(
//                    sagaInstance.getContext(),
//                    new TypeReference<Map<String, Object>>() {}
//            );
//
//            SagaContext sagaContext = new SagaContext(contextMap);
            // Mark step as running
            sagaStepDB.markAsRunning();
            sagaStepRepository.save(sagaStepDB);

            // Execute the step
            boolean success = step.execute(sagaContext);

            if (success) {
                sagaStepDB.markAsCompleted();
                sagaStepRepository.save(sagaStepDB);

                // Update saga instance
                sagaInstance.setCurrentStep(stepName);
                sagaInstance.markAsRunning();

                // Update context with any changes
                String updatedContext = objectMapper.writeValueAsString(sagaContext.getData());
                sagaInstance.setContext(updatedContext);
                sagaInstanceRepository.save(sagaInstance);

                log.info("Step '{}' executed successfully for saga {}", stepName, sagaInstanceId);
                return true;
            } else {
                sagaStepDB.markAsFailed();
                sagaStepDB.setErrorMessage("Step execution returned false");
                sagaStepRepository.save(sagaStepDB);

                log.error("Step '{}' failed for saga {}", stepName, sagaInstanceId);
                return false;
            }

        } catch (JsonProcessingException e) {
            log.error("Error processing saga context for step '{}'", stepName, e);
            updateStepAsFailed(sagaInstanceId, stepName, "Context serialization error: " + e.getMessage());
            throw new SagaException("Failed to process saga context", e);
        } catch (Exception e) {
            log.error("Unexpected error executing step '{}' for saga {}", stepName, sagaInstanceId, e);
            updateStepAsFailed(sagaInstanceId, stepName, "Unexpected error: " + e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean compensateStep(Long sagaInstanceId, String stepName) {
        log.info("Compensating step '{}' for saga instance {}", stepName, sagaInstanceId);

        if (sagaInstanceId == null) {
            throw new IllegalArgumentException("Saga instance ID cannot be null");
        }

        if (stepName == null || stepName.trim().isEmpty()) {
            throw new IllegalArgumentException("Step name cannot be null or empty");
        }

        try {
            SagaInstance sagaInstance = sagaInstanceRepository.findById(sagaInstanceId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Saga instance not found with id: " + sagaInstanceId));

            SagaStepInterface step = sagaStepFactory.getStepName(stepName);
            if (step == null) {
                log.error("Saga step implementation not found for step name: {}", stepName);
                throw new SagaException("Saga step not found: " + stepName);
            }

            // Find completed step to compensate
            SagaStep sagaStepDB = sagaStepRepository
                    .findBySagaInstanceIdAndStepNameAndStatus(sagaInstanceId, stepName, StepStatus.COMPLETED)
                    .orElse(null);

            if (sagaStepDB == null) {
                log.info("Step '{}' not found or not completed for saga {}, skipping compensation",
                        stepName, sagaInstanceId);
                return true;
            }

            // Parse saga context
            SagaContext sagaContext = parseSagaContext(sagaInstance.getContext());

            // Mark step as compensating
            sagaStepDB.markAsCompensating();
            sagaStepRepository.save(sagaStepDB);

            // Execute compensation
            boolean success = step.compensate(sagaContext);

            if (success) {
                sagaStepDB.markAsCompensated();
                sagaStepRepository.save(sagaStepDB);

                log.info("Step '{}' compensated successfully for saga {}", stepName, sagaInstanceId);
                return true;
            } else {
                sagaStepDB.markAsFailed();
                sagaStepDB.setErrorMessage("Compensation returned false");
                sagaStepRepository.save(sagaStepDB);

                log.error("Compensation failed for step '{}' in saga {}", stepName, sagaInstanceId);
                return false;
            }

        } catch (Exception e) {
            log.error("Unexpected error compensating step '{}' for saga {}",
                    stepName, sagaInstanceId, e);
            updateStepAsFailed(sagaInstanceId, stepName, "Compensation error: " + e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SagaInstance getSagaInstance(Long sagaInstanceId) {
        if (sagaInstanceId == null) {
            throw new IllegalArgumentException("Saga instance ID cannot be null");
        }

        return sagaInstanceRepository.findById(sagaInstanceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Saga instance not found with id: " + sagaInstanceId));
    }

    @Override
    @Transactional
    public void compensateSaga(Long sagaInstanceId) {
        log.info("Starting compensation for saga {}", sagaInstanceId);

        if (sagaInstanceId == null) {
            throw new IllegalArgumentException("Saga instance ID cannot be null");
        }

        try {
            SagaInstance sagaInstance = getSagaInstance(sagaInstanceId);

            // Mark saga as compensating
            sagaInstance.markAsCompensating();
            sagaInstanceRepository.save(sagaInstance);

            // Get all completed steps in reverse order
            List<SagaStep> completedSteps = sagaStepRepository
                    .findCompletedStepsBySagaInstanceId(sagaInstanceId);

            Collections.reverse(completedSteps);

            boolean allCompensated = true;
            for (SagaStep completedStep : completedSteps) {
                boolean compensated = compensateStep(sagaInstanceId, completedStep.getStepName());
                if (!compensated) {
                    allCompensated = false;
                    log.error("Failed to compensate step '{}' for saga {}",
                            completedStep.getStepName(), sagaInstanceId);
                }
            }

            if (allCompensated) {
                sagaInstance.markAsCompensated();
                sagaInstanceRepository.save(sagaInstance);
                log.info("Saga {} compensated successfully", sagaInstanceId);
            } else {
                log.error("Saga {} compensation partially failed", sagaInstanceId);
            }

        } catch (DataAccessException e) {
            log.error("Database error during saga compensation", e);
            throw new SagaException("Failed to compensate saga due to database error", e);
        }
    }

    @Override
    @Transactional
    public void failSaga(Long sagaInstanceId) {
        log.info("Failing saga {}", sagaInstanceId);

        if (sagaInstanceId == null) {
            throw new IllegalArgumentException("Saga instance ID cannot be null");
        }

        try {
            SagaInstance sagaInstance = getSagaInstance(sagaInstanceId);

            if (sagaInstance.getStatus() == SagaStatus.COMPLETED) {
                log.warn("Cannot fail already completed saga {}", sagaInstanceId);
                return;
            }

            sagaInstance.markAsFailed();
            sagaInstanceRepository.save(sagaInstance);

            // Trigger compensation
            compensateSaga(sagaInstanceId);

            log.info("Saga {} marked as failed and compensation initiated", sagaInstanceId);

        } catch (DataAccessException e) {
            log.error("Database error while failing saga", e);
            throw new SagaException("Failed to mark saga as failed due to database error", e);
        }
    }

    @Override
    @Transactional
    public void completeSaga(Long sagaInstanceId) {
        log.info("Completing saga {}", sagaInstanceId);

        if (sagaInstanceId == null) {
            throw new IllegalArgumentException("Saga instance ID cannot be null");
        }

        try {
            SagaInstance sagaInstance = getSagaInstance(sagaInstanceId);

            if (sagaInstance.getStatus() == SagaStatus.FAILED) {
                log.warn("Cannot complete failed saga {}", sagaInstanceId);
                return;
            }

            sagaInstance.markAsCompleted();
            sagaInstanceRepository.save(sagaInstance);

            log.info("Saga {} completed successfully", sagaInstanceId);

        } catch (DataAccessException e) {
            log.error("Database error while completing saga", e);
            throw new SagaException("Failed to complete saga due to database error", e);
        }
    }

    /**
     * FIXED: Parse JSON context string back to SagaContext with proper deserialization
     */
    private SagaContext parseSagaContext(String contextJson) {
        try {
            // Deserialize JSON directly to Map<String, Object>
            Map<String, Object> data = objectMapper.readValue(
                    contextJson,
                    new TypeReference<Map<String, Object>>() {}
            );

            log.debug("Deserialized saga context data: {}", data);

            return SagaContext.builder()
                    .data(data)
                    .build();

        } catch (JsonProcessingException e) {
            log.error("Error parsing saga context from JSON: {}", contextJson, e);
            throw new SagaException("Failed to parse saga context", e);
        }
    }


    private void updateStepAsFailed(Long sagaInstanceId, String stepName, String errorMessage) {
        try {
            sagaStepRepository.findBySagaInstanceIdAndStepNameAndStatus(
                            sagaInstanceId, stepName, StepStatus.RUNNING)
                    .ifPresent(step -> {
                        step.markAsFailed();
                        step.setErrorMessage(errorMessage);
                        sagaStepRepository.save(step);
                    });
        } catch (Exception e) {
            log.error("Failed to update step status to failed", e);
        }
    }
}
