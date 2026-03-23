package ru.hh.match.application.port.out;

import java.util.List;
import ru.hh.match.infrastructure.adapter.hh.dto.HhMeDto;
import ru.hh.match.infrastructure.adapter.hh.dto.HhResumeDto;

public interface HhResumePort {

    List<HhResumeDto> fetchAllResumes(String accessToken);

    HhResumeDto fetchResumeById(String resumeId, String accessToken);

    HhMeDto fetchMe(String accessToken);
}
