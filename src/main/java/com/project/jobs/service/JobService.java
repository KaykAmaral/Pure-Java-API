package com.project.jobs.service;

import com.project.jobs.dto.CreateJobRequest;
import com.project.jobs.model.Job;
import com.project.jobs.repository.InMemoryJobRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class JobService {

    private final InMemoryJobRepository repository;

    public JobService(InMemoryJobRepository repository) {
        this.repository = Objects.requireNonNull(
                repository,
                "repository must not be null"
        );
    }

    public Job create(CreateJobRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        return repository.save(Job.pending(request.type(), request.content()));
    }

    public List<Job> findAll() {
        return repository.findAll();
    }

    public Optional<Job> findById(UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        return repository.findById(id);
    }

    public boolean deleteById(UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        return repository.deleteById(id);
    }

}