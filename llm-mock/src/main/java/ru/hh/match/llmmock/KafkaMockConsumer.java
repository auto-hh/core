package ru.hh.match.llmmock;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaMockConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaMockConsumer.class);

    private final KafkaMockProducer producer;

    public KafkaMockConsumer(KafkaMockProducer producer) {
        this.producer = producer;
    }

    @KafkaListener(topics = "${app.request-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleMatchRequest(Map<String, Object> message) {
        String correlationId = String.valueOf(message.get("correlationId"));
        String sessionId = String.valueOf(message.get("sessionId"));
        Object resumeIdObj = message.get("resumeId");
        Object vacancyIdObj = message.get("vacancyId");

        log.info("Received match request, correlationId: {}", correlationId);

        try {
            // Simulate LLM processing delay (500-2000ms)
            long delay = ThreadLocalRandom.current().nextLong(500, 2001);
            Thread.sleep(delay);

            double score = Math.round(ThreadLocalRandom.current().nextDouble() * 100.0) / 100.0;

            Long resumeId = resumeIdObj instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(resumeIdObj));
            Long vacancyId = vacancyIdObj instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(vacancyIdObj));

            producer.sendResponse(correlationId, sessionId, resumeId, vacancyId, score);

            log.info("Sent match response, correlationId: {}, score: {}", correlationId, score);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Match processing interrupted for correlationId: {}", correlationId);
        } catch (Exception e) {
            log.error("Error processing match request, correlationId: {}", correlationId, e);
        }
    }
}
