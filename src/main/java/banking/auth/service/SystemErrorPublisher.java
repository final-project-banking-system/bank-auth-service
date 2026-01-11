package banking.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemErrorPublisher {
    private static final String TOPIC_SYSTEM_ERRORS = "system.errors";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publish(String service, String operation, String message, @Nullable Throwable e) {
        var errorId = UUID.randomUUID();

        Map<String, Object> payload = Map.of(
                "eventType", "SYSTEM_ERROR",
                "errorId", errorId,
                "service", service,
                "operation", operation,
                "message", message,
                "exceptionClass", e == null ? null : e.getClass().getName(),
                "exceptionMessage", e == null ? null : e.getMessage(),
                "occurredAt", LocalDateTime.now().toString()
        );

        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize system error payload, errorId={}", errorId, ex);
            return;
        }
        kafkaTemplate.send(TOPIC_SYSTEM_ERRORS, errorId.toString(), json)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Failed to publish system error to Kafka: errorId={}", errorId, error);
                    } else {
                        log.warn("System error published to Kafka: errorId={}, operation={}, message={}",
                                errorId, operation, message);
                    }
                });
    }
}
