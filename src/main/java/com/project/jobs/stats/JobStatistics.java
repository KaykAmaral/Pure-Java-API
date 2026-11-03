package com.project.jobs.stats;

import java.util.concurrent.atomic.AtomicLong;

public final class JobStatistics {

    private final AtomicLong submitted = new AtomicLong();
    private final AtomicLong completed = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();

    public void recordSubmitted() {
        submitted.incrementAndGet();
    }

    public void recordCompleted() {
        completed.incrementAndGet();
    }

    public void recordFailed() {
        failed.incrementAndGet();
    }

    public JobStatisticsSnapshot snapshot() {
        long completedCount = completed.get();
        long failedCount = failed.get();
        long submittedCount = submitted.get();

        return new JobStatisticsSnapshot(
                submittedCount,
                completedCount,
                failedCount
        );
    }
}
