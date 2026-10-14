package com.project.jobs.model;

import java.util.Objects;
import java.util.UUID;

public record Job(
        UUID id,
        JobType type,
        String content,
        JobStatus status,
        Integer result
) {

    public Job {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(content, "content must not be null");
        Objects.requireNonNull(status, "status must not be null");

        if (content.isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }

        if (status == JobStatus.COMPLETED && result == null) {
            throw new IllegalArgumentException(
                "completed job must have a result"
            );
        }
    }

    public static Job pending(JobType type, String content) {
        return new Job(
                UUID.randomUUID(),
                type,
                content,
                JobStatus.PENDING,
                null
        );
    }

    public Job withStatus(JobStatus newStatus) {
        return new Job(id, type, content, newStatus, result);
    }

    public Job complete(int newResult) {
        return new Job(id, type, content, JobStatus.COMPLETED, newResult);
    }
}
