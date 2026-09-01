package com.project.jobs.model;

import java.util.Objects;
import java.util.UUID;

public record Job(
        UUID id,
        JobType type,
        String content,
        JobStatus status
) {

    public Job {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(content, "content must not be null");
        Objects.requireNonNull(status, "status must not be null");

        if (content.isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
    }

    public static Job pending(JobType type, String content) {
        return new Job(
                UUID.randomUUID(),
                type,
                content,
                JobStatus.PENDING
        );
    }
}
