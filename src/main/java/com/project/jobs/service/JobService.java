package com.project.jobs.service;

import com.project.jobs.dto.CreateJobRequest;
import com.project.jobs.model.Job;
import com.project.jobs.model.JobStatus;
import com.project.jobs.model.JobType;
import com.project.jobs.model.result.JobResult;
import com.project.jobs.processor.JobProcessor;
import com.project.jobs.repository.InMemoryJobRepository;
import com.project.jobs.stats.JobStatistics;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;

public final class JobService {

    private final InMemoryJobRepository repository;
    private final Executor executor;
    private final Map<JobType, JobProcessor<? extends JobResult>> processors;
    private final JobStatistics statistics;

    public JobService(
            InMemoryJobRepository repository,
            Executor executor,
            Map<JobType, JobProcessor<? extends JobResult>> processors,
            JobStatistics statistics
    ) {
        this.repository = Objects.requireNonNull(
                repository,
                "repository must not be null");

        this.executor = Objects.requireNonNull(
                executor,
                "executor must not be null");

        Objects.requireNonNull(processors, "processors must not be null");

        this.processors = Map.copyOf(processors);
        this.statistics = Objects.requireNonNull(
                statistics,
                "statistics must not be null"
        );
    }

    public Job create(CreateJobRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        Job job = repository.save(
                Job.pending(request.type(), request.content()));

        statistics.recordSubmitted();
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
            failJob(id);
            return;
        }

        Optional<Job> possibleJob = repository.findById(id);

        if (possibleJob.isEmpty()) {
            return;
        }

        Job job = possibleJob.get();

        JobProcessor<? extends JobResult> processor =
                processors.get(job.type());

        if (processor == null) {
            System.err.printf(
                    "No processor registered for job type %s%n",
                    job.type());

            failJob(id);
            return;
        }

        try {
            JobResult result = processor.process(job.content());
            completeJob(id, result);
        } catch (RuntimeException exception) {
            System.err.printf(
                    "Failed to process job %s: %s%n",
                    id,
                    exception.getMessage());

            failJob(id);
        }
    }

    private Optional<Job> updateStatus(UUID id, JobStatus newStatus) {
        Optional<Job> updatedJob = repository.updateById(
                id,
                job -> job.withStatus(newStatus));

        Thread currentThread = Thread.currentThread();

        if (updatedJob.isEmpty()) {
            System.out.printf(
                    "[thread=%s, virtual=%s] Job %s não encontrado%n",
                    currentThread,
                    currentThread.isVirtual(),
                    id);
            return Optional.empty();
        }

        System.out.printf(
                "[thread=%s, virtual=%s] Job %s -> %s%n",
                currentThread,
                currentThread.isVirtual(),
                id,
                updatedJob.get().status());

        return updatedJob;
    }

    private void completeJob(UUID id, JobResult result) {
        Optional<Job> updatedJob = repository.updateById(
            id,
            job -> job.complete(result)
        );

        Thread currentThread = Thread.currentThread();

        if (updatedJob.isEmpty()) {
            System.out.printf(
                    "[thread=%s, virtual=%s] Job %s não encontrado%n",
                    currentThread.getName(),
                    currentThread.isVirtual(),
                    id);
            return;
        }

        statistics.recordCompleted();

        System.out.printf(
                "[thread=%s, virtual=%s] Job %s -> COMPLETED, result=%s%n",
                currentThread.getName(),
                currentThread.isVirtual(),
                id,
                result);

    }

    private void failJob(UUID id) {
        if (updateStatus(id, JobStatus.FAILED).isPresent()) {
            statistics.recordFailed();
        }
    }

}
