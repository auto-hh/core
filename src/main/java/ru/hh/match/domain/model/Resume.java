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
import java.util.UUID;

@Entity
@Table(name = "resumes", schema = "resume")
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hh_resume_id", nullable = false, unique = true)
    private String hhResumeId;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    private String grade = "";

    @Column(name = "job_title")
    private String jobTitle = "";

    private String location = "";

    @Column(name = "salary_val")
    private Integer salaryVal = 0;

    @Column(name = "salary_curr")
    private String salaryCurr = "RUB";

    @Column(name = "skills_res", columnDefinition = "TEXT")
    private String skillsRes = "";

    @Column(name = "about_me", columnDefinition = "TEXT")
    private String aboutMe = "";

    @Column(name = "exp_count")
    private Integer expCount = 0;

    @Column(name = "exp_text", columnDefinition = "TEXT")
    private String expText = "";

    @Column(name = "edu_uni")
    private String eduUni = "";

    @Column(name = "edu_year")
    private String eduYear = "";

    @Column(length = 32)
    private String status = "published";

    @Column(name = "is_active")
    private Boolean isActive = false;

    @Column(name = "hh_url", length = 512)
    private String hhUrl = "";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Resume() {}

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

    public String getHhResumeId() { return hhResumeId; }
    public void setHhResumeId(String hhResumeId) { this.hhResumeId = hhResumeId; }

    public UUID getSessionId() { return sessionId; }
    public void setSessionId(UUID sessionId) { this.sessionId = sessionId; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Integer getSalaryVal() { return salaryVal; }
    public void setSalaryVal(Integer salaryVal) { this.salaryVal = salaryVal; }

    public String getSalaryCurr() { return salaryCurr; }
    public void setSalaryCurr(String salaryCurr) { this.salaryCurr = salaryCurr; }

    public String getSkillsRes() { return skillsRes; }
    public void setSkillsRes(String skillsRes) { this.skillsRes = skillsRes; }

    public String getAboutMe() { return aboutMe; }
    public void setAboutMe(String aboutMe) { this.aboutMe = aboutMe; }

    public Integer getExpCount() { return expCount; }
    public void setExpCount(Integer expCount) { this.expCount = expCount; }

    public String getExpText() { return expText; }
    public void setExpText(String expText) { this.expText = expText; }

    public String getEduUni() { return eduUni; }
    public void setEduUni(String eduUni) { this.eduUni = eduUni; }

    public String getEduYear() { return eduYear; }
    public void setEduYear(String eduYear) { this.eduYear = eduYear; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isActive() { return isActive != null && isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getHhUrl() { return hhUrl; }
    public void setHhUrl(String hhUrl) { this.hhUrl = hhUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
