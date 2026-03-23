package ru.hh.match.application.port.out;

import java.util.Optional;
import java.util.UUID;

public interface SessionPort {

    Optional<String> getAccessToken(UUID sessionId);

    void deleteAccessToken(UUID sessionId);
}
