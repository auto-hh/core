package ru.hh.match.presentation.controller;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.hh.match.application.port.in.SyncResumeUseCase;
import ru.hh.match.domain.model.Resume;
import ru.hh.match.presentation.dto.response.ApiResponse;
import ru.hh.match.presentation.dto.response.ResumeResponse;

@RestController
@RequestMapping("/api/resume")
public class ResumeController {

    private final SyncResumeUseCase syncResumeUseCase;

    public ResumeController(SyncResumeUseCase syncResumeUseCase) {
        this.syncResumeUseCase = syncResumeUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ResumeResponse>> getResume(
            @CookieValue("session_id") UUID sessionId) {
        Resume resume = syncResumeUseCase.syncResume(sessionId);
        ResumeResponse response = new ResumeResponse(
                resume.getId(),
                resume.getHhResumeId(),
                resume.getGrade(),
                resume.getJobTitle(),
                resume.getLocation(),
                resume.getSalaryVal(),
                resume.getSalaryCurr(),
                resume.getSkillsRes(),
                resume.getAboutMe(),
                resume.getExpCount(),
                resume.getExpText(),
                resume.getEduUni(),
                resume.getEduYear()
        );
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
