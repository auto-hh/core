package ru.hh.match.application.port.out;

import ru.hh.match.infrastructure.adapter.hh.dto.HhResumeDto;

public interface HhResumePort {

    HhResumeDto fetchFirstResume(String accessToken);
}
