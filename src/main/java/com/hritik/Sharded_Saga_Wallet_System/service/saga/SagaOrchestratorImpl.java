package com.hritik.Sharded_Saga_Wallet_System.service.saga;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hritik.Sharded_Saga_Wallet_System.exceptions.SagaException;
import com.hritik.Sharded_Saga_Wallet_System.model.SagaInstance;
import com.hritik.Sharded_Saga_Wallet_System.model.SagaStatus;
import com.hritik.Sharded_Saga_Wallet_System.repository.SagaInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SagaOrchestratorImpl implements SagaOrchestrator {
    private final SagaInstanceRepository sagaInstanceRepository;

    private final ObjectMapper objectMapper;

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
    public boolean executeStep(Long sagaInstanceId, String stepName) {
        return false;
    }

    @Override
    public boolean compensateStep(Long sagaInstanceId, String stepName) {
        return false;
    }

    @Override
    public SagaInstance getSagaInstance(Long sagaInstanceId) {
        return null;
    }

    @Override
    public void compensateSaga(Long sagaInstanceId) {

    }

    @Override
    public void failSaga(Long sagaInstanceId) {

    }

    @Override
    public void completeSaga(Long sagaInstanceId) {

    }
}
