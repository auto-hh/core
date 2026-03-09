package ru.hh.match.infrastructure.mapper;

import java.util.List;
import org.junit.jupiter.api.Test;
import ru.hh.match.domain.model.Vacancy;
import ru.hh.match.infrastructure.adapter.hh.dto.HhVacancyDto;

import static org.assertj.core.api.Assertions.assertThat;

class VacancyMapperTest {

    private final VacancyMapper mapper = new VacancyMapper();

    @Test
    void toEntity_shouldMapAllFields() {
        HhVacancyDto dto = new HhVacancyDto(
                "v123",
                "Senior Java Developer",
                "<p>Job description <b>here</b></p>",
                new HhVacancyDto.VacancyExperience("between3And6", "3-6 years"),
                new HhVacancyDto.Salary(200000, 350000, "RUR"),
                List.of(
                        new HhVacancyDto.KeySkill("Java"),
                        new HhVacancyDto.KeySkill("Spring Boot")
                ),
                List.of(new HhVacancyDto.ProfessionalRole("Backend Developer"))
        );

        Vacancy result = mapper.toEntity(dto);

        assertThat(result.getHhVacancyId()).isEqualTo("v123");
        assertThat(result.getJobTitle()).isEqualTo("Senior Java Developer");
        assertThat(result.getTargetRole()).isEqualTo("Backend Developer");
        assertThat(result.getExperience()).isEqualTo("3-6 years");
        assertThat(result.getGrade()).isEqualTo("Senior");
        assertThat(result.getSkillsVac()).isEqualTo("Java, Spring Boot");
        assertThat(result.getVacancyText()).isEqualTo("Job description here");
        assertThat(result.getSalary()).isEqualTo("от 200000 до 350000 RUR");
    }

    @Test
    void toEntity_nullFields_shouldUseDefaults() {
        HhVacancyDto dto = new HhVacancyDto("v1", null, null, null, null, null, null);

        Vacancy result = mapper.toEntity(dto);

        assertThat(result.getHhVacancyId()).isEqualTo("v1");
        assertThat(result.getJobTitle()).isEmpty();
        assertThat(result.getVacancyText()).isEmpty();
        assertThat(result.getSalary()).isEmpty();
    }

    @Test
    void toEntity_gradeMapping() {
        var noExp = new HhVacancyDto("v1", "", "", new HhVacancyDto.VacancyExperience("noExperience", ""), null, null, null);
        assertThat(mapper.toEntity(noExp).getGrade()).isEqualTo("Junior");

        var mid = new HhVacancyDto("v2", "", "", new HhVacancyDto.VacancyExperience("between1And3", ""), null, null, null);
        assertThat(mapper.toEntity(mid).getGrade()).isEqualTo("Middle");

        var senior = new HhVacancyDto("v3", "", "", new HhVacancyDto.VacancyExperience("between3And6", ""), null, null, null);
        assertThat(mapper.toEntity(senior).getGrade()).isEqualTo("Senior");

        var lead = new HhVacancyDto("v4", "", "", new HhVacancyDto.VacancyExperience("moreThan6", ""), null, null, null);
        assertThat(mapper.toEntity(lead).getGrade()).isEqualTo("Lead");
    }
}
