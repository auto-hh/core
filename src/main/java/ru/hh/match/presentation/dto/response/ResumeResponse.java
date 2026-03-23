package ru.hh.match.presentation.dto.response;

import ru.hh.match.domain.model.Resume;

public record ResumeResponse(
    Long id,
    String hhResumeId,
    String grade,
    String jobTitle,
    String location,
    Integer salaryVal,
    String salaryCurr,
    String skillsRes,
    String aboutMe,
    Integer expCount,
    String expText,
    String eduUni,
    String eduYear,
    String status,
    boolean isActive,
    String hhUrl
) {
    public static ResumeResponse from(Resume r) {
        return new ResumeResponse(
            r.getId(),
            r.getHhResumeId(),
            r.getGrade(),
            r.getJobTitle(),
            r.getLocation(),
            r.getSalaryVal(),
            r.getSalaryCurr(),
            r.getSkillsRes(),
            r.getAboutMe(),
            r.getExpCount(),
            r.getExpText(),
            r.getEduUni(),
            r.getEduYear(),
            r.getStatus(),
            r.isActive(),
            r.getHhUrl()
        );
    }
}
