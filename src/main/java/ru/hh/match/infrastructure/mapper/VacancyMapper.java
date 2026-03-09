package ru.hh.match.infrastructure.mapper;

import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import ru.hh.match.domain.model.Vacancy;
import ru.hh.match.infrastructure.adapter.hh.dto.HhVacancyDto;

@Component
public class VacancyMapper {

    public Vacancy toEntity(HhVacancyDto dto) {
        Vacancy vacancy = new Vacancy();
        vacancy.setHhVacancyId(dto.id());
        vacancy.setJobTitle(dto.name() != null ? dto.name() : "");

        if (dto.professionalRoles() != null && !dto.professionalRoles().isEmpty()) {
            vacancy.setTargetRole(dto.professionalRoles().getFirst().name());
        }

        if (dto.experience() != null) {
            vacancy.setExperience(dto.experience().name() != null ? dto.experience().name() : "");
            vacancy.setGrade(mapExperienceToGrade(dto.experience().id()));
        }

        if (dto.keySkills() != null) {
            String skills = dto.keySkills().stream()
                    .map(HhVacancyDto.KeySkill::name)
                    .collect(Collectors.joining(", "));
            vacancy.setSkillsVac(skills);
        }

        vacancy.setVacancyText(stripHtml(dto.description() != null ? dto.description() : ""));

        if (dto.salary() != null) {
            vacancy.setSalary(formatSalary(dto.salary()));
        }

        return vacancy;
    }

    private String mapExperienceToGrade(String experienceId) {
        if (experienceId == null) return "";
        return switch (experienceId) {
            case "noExperience" -> "Junior";
            case "between1And3" -> "Middle";
            case "between3And6" -> "Senior";
            case "moreThan6" -> "Lead";
            default -> "";
        };
    }

    private String formatSalary(HhVacancyDto.Salary salary) {
        StringBuilder sb = new StringBuilder();
        if (salary.from() != null) {
            sb.append("от ").append(salary.from());
        }
        if (salary.to() != null) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append("до ").append(salary.to());
        }
        if (salary.currency() != null) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(salary.currency());
        }
        return sb.toString();
    }

    private String stripHtml(String html) {
        return html.replaceAll("<[^>]*>", "").replaceAll("&nbsp;", " ").trim();
    }
}
