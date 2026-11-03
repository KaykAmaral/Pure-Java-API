package com.project.jobs.stats;

public record JobStatisticsSnapshot(
        long submitted,
        long completed,
        long failed
) {

    public JobStatisticsSnapshot {
        if (submitted < 0 || completed < 0 || failed < 0) {
            throw new IllegalArgumentException(
                    "job statistics must not be negative"
            );
        }
    }
}
