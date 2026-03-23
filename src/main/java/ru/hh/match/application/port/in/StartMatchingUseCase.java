package ru.hh.match.application.port.in;

import java.util.UUID;

public interface StartMatchingUseCase {

    int startMatching(UUID sessionId);

    int startMatching(UUID sessionId, String query);

    int startMatching(UUID sessionId, String hhResumeId, String query);
}
