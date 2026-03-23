package ru.hh.match.presentation.controller;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import ru.hh.match.application.port.in.ListResumesUseCase;
import ru.hh.match.application.port.in.SyncResumeUseCase;
import ru.hh.match.domain.model.Resume;
import ru.hh.match.presentation.dto.response.ApiResponse;
import ru.hh.match.presentation.dto.response.ResumeResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeControllerTest {

    @Mock private SyncResumeUseCase syncResumeUseCase;
    @Mock private ListResumesUseCase listResumesUseCase;

    @InjectMocks private ResumeController resumeController;

    @Test
    void getResume_shouldReturnResumeResponse() {
        UUID sessionId = UUID.randomUUID();
        Resume resume = new Resume();
        resume.setId(1L);
        resume.setHhResumeId("r1");
        resume.setJobTitle("Java Dev");
        resume.setGrade("Middle");
        resume.setLocation("Moscow");

        when(syncResumeUseCase.syncResume(sessionId)).thenReturn(resume);

        var response = resumeController.getResume(sessionId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().data().jobTitle()).isEqualTo("Java Dev");
    }
}
