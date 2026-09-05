package com.project.jobs.dto;

import com.project.jobs.model.JobType;

import java.util.Objects;

public record CreateJobRequest(
        JobType type,
        String content
) {

    public CreateJobRequest {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(content, "content must not be null");

        if (content.isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
    }
}
