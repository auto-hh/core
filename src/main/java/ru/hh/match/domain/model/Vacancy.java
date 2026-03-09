package ru.hh.match.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "vacancies", schema = "vacancy")
public class Vacancy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hh_vacancy_id", nullable = false, unique = true)
    private String hhVacancyId;

    @Column(name = "target_role")
    private String targetRole = "";

    @Column(name = "job_title")
    private String jobTitle = "";

    private String experience = "";

    private String grade = "";

    @Column(name = "skills_vac", columnDefinition = "TEXT")
    private String skillsVac = "";

    @Column(name = "vacancy_text", columnDefinition = "TEXT")
    private String vacancyText = "";

    private String salary = "";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Vacancy() {}

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getHhVacancyId() { return hhVacancyId; }
    public void setHhVacancyId(String hhVacancyId) { this.hhVacancyId = hhVacancyId; }

    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String targetRole) { this.targetRole = targetRole; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public String getSkillsVac() { return skillsVac; }
    public void setSkillsVac(String skillsVac) { this.skillsVac = skillsVac; }

    public String getVacancyText() { return vacancyText; }
    public void setVacancyText(String vacancyText) { this.vacancyText = vacancyText; }

    public String getSalary() { return salary; }
    public void setSalary(String salary) { this.salary = salary; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
