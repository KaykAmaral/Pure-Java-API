package com.project.jobs.dto;

import com.project.jobs.model.JobStatus;

import java.util.Objects;
import java.util.UUID;

public record CreateJobResponse(
        UUID id,
        JobStatus status
) {

    public CreateJobResponse {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(status, "status must not be null");
    }
}