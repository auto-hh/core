package ru.hh.match.infrastructure.mapper;

import java.util.List;
import org.junit.jupiter.api.Test;
import ru.hh.match.domain.model.Resume;
import ru.hh.match.infrastructure.adapter.hh.dto.HhResumeDto;

import static org.assertj.core.api.Assertions.assertThat;

class ResumeMapperTest {

    private final ResumeMapper mapper = new ResumeMapper();

    @Test
    void toEntity_shouldMapAllFields() {
        HhResumeDto dto = new HhResumeDto(
                "resume-123",
                "Java Developer",
                new HhResumeDto.Area("Moscow"),
                new HhResumeDto.Salary(200000, "RUB"),
                List.of("Java", "Spring", "Kafka"),
                "About me text",
                new HhResumeDto.TotalExperience(48),
                List.of(
                        new HhResumeDto.Experience("Developer", "Company A"),
                        new HhResumeDto.Experience("Senior Dev", "Company B")
                ),
                new HhResumeDto.Education(List.of(
                        new HhResumeDto.Education.Primary("MSU", 2019)
                ))
        );

        Resume result = mapper.toEntity(dto);

        assertThat(result.getHhResumeId()).isEqualTo("resume-123");
        assertThat(result.getJobTitle()).isEqualTo("Java Developer");
        assertThat(result.getLocation()).isEqualTo("Moscow");
        assertThat(result.getSalaryVal()).isEqualTo(200000);
        assertThat(result.getSalaryCurr()).isEqualTo("RUB");
        assertThat(result.getSkillsRes()).isEqualTo("Java, Spring, Kafka");
        assertThat(result.getAboutMe()).isEqualTo("About me text");
        assertThat(result.getExpCount()).isEqualTo(4);
        assertThat(result.getExpText()).contains("Developer в Company A");
        assertThat(result.getGrade()).isEqualTo("Middle");
        assertThat(result.getEduUni()).isEqualTo("MSU");
        assertThat(result.getEduYear()).isEqualTo("2019");
    }

    @Test
    void toEntity_nullFields_shouldUseDefaults() {
        HhResumeDto dto = new HhResumeDto("r1", null, null, null, null, null, null, null, null);

        Resume result = mapper.toEntity(dto);

        assertThat(result.getHhResumeId()).isEqualTo("r1");
        assertThat(result.getJobTitle()).isEmpty();
        assertThat(result.getLocation()).isEmpty();
        assertThat(result.getSalaryVal()).isEqualTo(0);
        assertThat(result.getSalaryCurr()).isEqualTo("RUB");
        assertThat(result.getSkillsRes()).isEmpty();
        assertThat(result.getGrade()).isEqualTo("Junior");
    }

    @Test
    void toEntity_gradeMapping() {
        // 0 months -> Junior
        HhResumeDto junior = new HhResumeDto("r1", null, null, null, null, null,
                new HhResumeDto.TotalExperience(5), null, null);
        assertThat(mapper.toEntity(junior).getGrade()).isEqualTo("Junior");

        // 18 months -> Junior+
        HhResumeDto juniorPlus = new HhResumeDto("r2", null, null, null, null, null,
                new HhResumeDto.TotalExperience(18), null, null);
        assertThat(mapper.toEntity(juniorPlus).getGrade()).isEqualTo("Junior+");

        // 48 months -> Middle
        HhResumeDto middle = new HhResumeDto("r3", null, null, null, null, null,
                new HhResumeDto.TotalExperience(48), null, null);
        assertThat(mapper.toEntity(middle).getGrade()).isEqualTo("Middle");

        // 96 months -> Senior
        HhResumeDto senior = new HhResumeDto("r4", null, null, null, null, null,
                new HhResumeDto.TotalExperience(96), null, null);
        assertThat(mapper.toEntity(senior).getGrade()).isEqualTo("Senior");
    }
}
