package ru.hh.match.application.port.out;

import ru.hh.match.infrastructure.adapter.kafka.dto.MatchRequestMessage;

public interface MatchRequestPort {

    void sendMatchRequest(MatchRequestMessage message);
}
