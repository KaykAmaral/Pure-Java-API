package com.project.jobs.dto;

import com.project.jobs.model.JobStatus;
import com.project.jobs.model.JobType;

import java.util.Objects;
import java.util.UUID;

public record JobSummaryResponse(
        UUID id,
        JobType type,
        JobStatus status
) {

    public JobSummaryResponse {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(status, "status must not be null");
    }
}