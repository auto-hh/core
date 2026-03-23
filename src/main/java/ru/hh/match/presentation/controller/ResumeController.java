package ru.hh.match.presentation.controller;

import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.hh.match.application.service.ResumeService;
import ru.hh.match.domain.model.Resume;
import ru.hh.match.presentation.dto.response.ApiResponse;
import ru.hh.match.presentation.dto.response.ResumeResponse;

@RestController
@RequestMapping("/api")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @GetMapping("/resumes")
    public ResponseEntity<ApiResponse<List<ResumeResponse>>> getAllResumes(
            @CookieValue("session_id") UUID sessionId) {
        List<Resume> resumes = resumeService.getAllResumes(sessionId);
        List<ResumeResponse> responses = resumes.stream()
                .map(ResumeResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    @GetMapping("/resumes/{id}")
    public ResponseEntity<ApiResponse<ResumeResponse>> getResume(
            @CookieValue("session_id") UUID sessionId,
            @PathVariable Long id) {
        Resume resume = resumeService.getResume(sessionId, id);
        return ResponseEntity.ok(ApiResponse.ok(ResumeResponse.from(resume)));
    }

    @PutMapping("/resumes/{id}/activate")
    public ResponseEntity<ApiResponse<Void>> activateResume(
            @CookieValue("session_id") UUID sessionId,
            @PathVariable Long id) {
        resumeService.activateResume(sessionId, id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/resumes/sync")
    public ResponseEntity<ApiResponse<List<ResumeResponse>>> syncResumes(
            @CookieValue("session_id") UUID sessionId) {
        List<Resume> resumes = resumeService.syncAllResumes(sessionId);
        List<ResumeResponse> responses = resumes.stream()
                .map(ResumeResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    // Keep old endpoint for backward compatibility
    @GetMapping("/resume")
    public ResponseEntity<ApiResponse<ResumeResponse>> getActiveResume(
            @CookieValue("session_id") UUID sessionId) {
        Resume resume = resumeService.getActiveResume(sessionId);
        return ResponseEntity.ok(ApiResponse.ok(ResumeResponse.from(resume)));
    }
}
