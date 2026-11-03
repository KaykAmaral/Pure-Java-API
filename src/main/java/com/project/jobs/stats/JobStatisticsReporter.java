package com.project.jobs.stats;

import java.util.Objects;

public final class JobStatisticsReporter implements Runnable {

    private final JobStatistics statistics;

    public JobStatisticsReporter(JobStatistics statistics) {
        this.statistics = Objects.requireNonNull(
                statistics,
                "statistics must not be null"
        );
    }

    @Override
    public void run() {
        try {
            JobStatisticsSnapshot snapshot = statistics.snapshot();
            Thread currentThread = Thread.currentThread();

            System.out.printf(
                    "[thread=%s, virtual=%s] "
                            + "Stats: submitted=%d, completed=%d, failed=%d%n",
                    currentThread.getName(),
                    currentThread.isVirtual(),
                    snapshot.submitted(),
                    snapshot.completed(),
                    snapshot.failed()
            );
        } catch (RuntimeException exception) {
            System.err.printf(
                    "Failed to report job statistics: %s%n",
                    exception.getMessage()
            );
        }
    }
}
