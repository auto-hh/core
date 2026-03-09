package ru.hh.match.llmmock;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaMockProducer {

    private static final Logger log = LoggerFactory.getLogger(KafkaMockProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String responseTopic;

    public KafkaMockProducer(KafkaTemplate<String, Object> kafkaTemplate,
                             @Value("${app.response-topic}") String responseTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.responseTopic = responseTopic;
    }

    public void sendResponse(String correlationId, String sessionId,
                             Long resumeId, Long vacancyId, double score) {
        Map<String, Object> response = Map.of(
                "correlationId", correlationId,
                "sessionId", sessionId,
                "resumeId", resumeId,
                "vacancyId", vacancyId,
                "score", score,
                "status", "COMPLETED"
        );

        kafkaTemplate.send(responseTopic, sessionId, response);
        log.info("Sent response to topic {}, correlationId: {}", responseTopic, correlationId);
    }
}
