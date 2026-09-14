package com.project.jobs.service;

import com.project.jobs.dto.CreateJobRequest;
import com.project.jobs.model.Job;
import com.project.jobs.model.JobStatus;
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

        Thread worker = new Thread(
            task,
            "job-worker-" + id
        );

        worker.start();
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
        Optional<Job> possibleJob = repository.findById(id);

        if (possibleJob.isEmpty()) {
            System.out.printf(
                    "[thread=%s] Job %s não encontrado%n",
                    Thread.currentThread().getName(),
                    id
            );
            return;
        }

        Job currentJob = possibleJob.get();
        Job updatedJob = currentJob.withStatus(newStatus);

        repository.save(updatedJob);

        System.out.printf(
                "[thread=%s] Job %s -> %s%n",
                Thread.currentThread().getName(),
                id,
                newStatus
        );
}

}