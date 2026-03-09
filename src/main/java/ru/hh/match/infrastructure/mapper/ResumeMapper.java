package ru.hh.match.infrastructure.mapper;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import ru.hh.match.domain.model.Resume;
import ru.hh.match.infrastructure.adapter.hh.dto.HhResumeDto;

@Component
public class ResumeMapper {

    public Resume toEntity(HhResumeDto dto) {
        Resume resume = new Resume();
        resume.setHhResumeId(dto.id());
        resume.setJobTitle(dto.title() != null ? dto.title() : "");
        resume.setLocation(dto.area() != null && dto.area().name() != null ? dto.area().name() : "");

        if (dto.salary() != null) {
            resume.setSalaryVal(dto.salary().amount() != null ? dto.salary().amount() : 0);
            resume.setSalaryCurr(dto.salary().currency() != null ? dto.salary().currency() : "RUB");
        }

        if (dto.skillSet() != null) {
            resume.setSkillsRes(String.join(", ", dto.skillSet()));
        }

        resume.setAboutMe(dto.skills() != null ? dto.skills() : "");

        if (dto.totalExperience() != null && dto.totalExperience().months() != null) {
            int years = Math.round(dto.totalExperience().months() / 12.0f);
            resume.setExpCount(years);
        }

        if (dto.experience() != null && !dto.experience().isEmpty()) {
            String expText = dto.experience().stream()
                    .map(e -> {
                        String position = e.position() != null ? e.position() : "";
                        String company = e.company() != null ? e.company() : "";
                        return position + " в " + company;
                    })
                    .collect(Collectors.joining("; "));
            resume.setExpText(expText);
        }

        resume.setGrade(determineGrade(resume.getExpCount()));

        if (dto.education() != null && dto.education().primary() != null && !dto.education().primary().isEmpty()) {
            var primary = dto.education().primary().getFirst();
            resume.setEduUni(primary.name() != null ? primary.name() : "");
            resume.setEduYear(primary.year() != null ? String.valueOf(primary.year()) : "");
        }

        return resume;
    }

    private String determineGrade(int yearsOfExperience) {
        if (yearsOfExperience < 1) return "Junior";
        if (yearsOfExperience < 3) return "Junior+";
        if (yearsOfExperience < 6) return "Middle";
        return "Senior";
    }
}
