package ru.hh.match.application.port.in;

import java.util.UUID;
import ru.hh.match.domain.model.Resume;

public interface SyncResumeUseCase {

    Resume syncResume(UUID sessionId);
}
