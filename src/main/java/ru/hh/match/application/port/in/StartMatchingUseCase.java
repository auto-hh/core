package ru.hh.match.application.port.in;

import java.util.UUID;

public interface StartMatchingUseCase {

    int startMatching(UUID sessionId);
}
