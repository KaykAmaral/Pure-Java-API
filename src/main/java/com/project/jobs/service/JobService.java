package com.project.jobs.service;

import com.project.jobs.dto.CreateJobRequest;
import com.project.jobs.model.Job;
import com.project.jobs.model.JobStatus;
import com.project.jobs.repository.InMemoryJobRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;

public final class JobService {

    private final InMemoryJobRepository repository;
    private final Executor executor;

    public JobService(InMemoryJobRepository repository, Executor executor) {
        this.repository = Objects.requireNonNull(
                repository,
                "repository must not be null"
        );

        this.executor = Objects.requireNonNull(
            executor,
            "executor must not be null"
        );
    }

    public Job create(CreateJobRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        
        Job job = repository.save(
            Job.pending(request.type(), request.content())
        );

        startProcessing(job.id());

        return job;

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

    private void startProcessing(UUID id) {
        Runnable task = () -> process(id);

        executor.execute(task);
    }

    private void process(UUID id) {
        updateStatus(id, JobStatus.PROCESSING);

        try {
            Thread.sleep(3_000);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            updateStatus(id, JobStatus.FAILED);
            return;
        }

        updateStatus(id, JobStatus.COMPLETED);
    }

    private void updateStatus(UUID id, JobStatus newStatus) {
        Optional<Job> updatedJob = repository.updateById(
            id,
            job -> job.withStatus(newStatus)
        );

        Thread currentThread = Thread.currentThread();

        if (updatedJob.isEmpty()) {
            System.out.printf(
                "[thread=%s, virtual=%s] Job %s não encontrado%n",
                currentThread,
                currentThread.isVirtual(),
                id
            );
            return;
        }

        System.out.printf(
            "[thread=%s, virtual=%s] Job %s -> %s%n",
            currentThread,
            currentThread.isVirtual(),
            id,
            updatedJob.get().status()
        );
    }

}