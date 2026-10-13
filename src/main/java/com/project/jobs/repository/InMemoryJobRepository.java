package com.project.jobs.repository;

import com.project.jobs.model.Job;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.UnaryOperator;

public final class InMemoryJobRepository {

    private final ConcurrentMap<UUID, Job> jobsById = new ConcurrentHashMap<>();

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

    public Optional<Job> updateById(
        UUID id,
        UnaryOperator<Job> updateFunction
    ) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(updateFunction, "updateFunction must not be null");

        Job updatedJob = jobsById.computeIfPresent(
            id,
            (ignoredId, currentJob) -> Objects.requireNonNull(
                updateFunction.apply(currentJob), "updated job must not be null"
            )
        );

        return Optional.ofNullable(updatedJob);

    }
}
