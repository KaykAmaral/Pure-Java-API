package com.project.jobs.dto;

import com.project.jobs.model.JobStatus;
import com.project.jobs.model.JobType;

import java.util.Objects;
import java.util.Optional;

public record JobSearchCriteria(
        Optional<JobStatus> status,
        Optional<JobType> type
) {

    public JobSearchCriteria {
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(type, "type must not be null");
    }
}
