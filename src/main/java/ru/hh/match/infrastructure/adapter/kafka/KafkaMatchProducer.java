package ru.hh.match.infrastructure.adapter.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.hh.match.application.port.out.MatchRequestPort;
import ru.hh.match.infrastructure.adapter.kafka.dto.MatchRequestMessage;
import ru.hh.match.infrastructure.config.AppProperties;

@Component
public class KafkaMatchProducer implements MatchRequestPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaMatchProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AppProperties appProperties;

    public KafkaMatchProducer(KafkaTemplate<String, Object> kafkaTemplate, AppProperties appProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.appProperties = appProperties;
    }

    @Override
    public void sendMatchRequest(MatchRequestMessage message) {
        String topic = appProperties.kafka().topics().matchRequest();
        String key = message.sessionId();

        log.info("Sending match request to topic {}, correlationId: {}", topic, message.correlationId());

        kafkaTemplate.send(topic, key, message);
    }
}
