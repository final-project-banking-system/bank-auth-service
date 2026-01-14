package banking.auth.service.publisher;

import banking.auth.model.entity.OutboxEvent;
import banking.auth.repository.OutboxEventRepository;
import banking.auth.service.publisher.util.OutboxJsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthOutboxPublisher {
    private final OutboxEventRepository outboxEventRepository;
    private final OutboxJsonUtil outboxJsonUtil;

    public void save(String aggregateType, UUID aggregateId, String topic, Object event, String context) {
        JsonNode payload = outboxJsonUtil.toJsonNode(event, context);

        outboxEventRepository.save(OutboxEvent.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .topic(topic)
                .payload(payload)
                .build());

        log.info("Outbox saved: type={}, id={}, topic={}, context={}", aggregateType, aggregateId, topic, context);
    }
}
