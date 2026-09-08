package com.project.jobs.service;

import com.project.jobs.dto.CreateJobRequest;
import com.project.jobs.model.Job;
import com.project.jobs.repository.InMemoryJobRepository;

import java.util.Objects;

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
}