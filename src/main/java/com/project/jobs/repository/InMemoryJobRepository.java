package com.project.jobs.repository;

import com.project.jobs.model.Job;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class InMemoryJobRepository {

    private final Map<UUID, Job> jobsById = new HashMap<>();

    public Job save(Job job) {
        Objects.requireNonNull(job, "job must not be null");

        jobsById.put(job.id(), job);
        return job;
    }

    public Optional<Job> findById(UUID id) {
        Objects.requireNonNull(id, "id must not be null");

        return Optional.ofNullable(jobsById.get(id));
    }

    public List<Job> findAll() {
        return List.copyOf(jobsById.values());
    }

    public boolean deleteById(UUID id) {
        Objects.requireNonNull(id, "id must not be null");

        return jobsById.remove(id) != null;
    }
}
