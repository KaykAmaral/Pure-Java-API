package com.project.jobs.json;

import com.project.jobs.stats.JobStatisticsSnapshot;

import java.util.Objects;

public final class JobStatisticsSnapshotJsonEncoder {

    private JobStatisticsSnapshotJsonEncoder() {
    }

    public static String encode(JobStatisticsSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");

        return """
                {"submitted":%d,"completed":%d,"failed":%d}
                """.formatted(
                snapshot.submitted(),
                snapshot.completed(),
                snapshot.failed()
        ).strip();
    }
}
