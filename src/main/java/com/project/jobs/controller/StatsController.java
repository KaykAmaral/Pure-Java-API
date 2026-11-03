package com.project.jobs.controller;

import com.project.jobs.http.HttpResponse;
import com.project.jobs.json.JobStatisticsSnapshotJsonEncoder;
import com.project.jobs.stats.JobStatistics;
import com.project.jobs.stats.JobStatisticsSnapshot;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.Objects;

public final class StatsController {

    private final JobStatistics statistics;

    public StatsController(JobStatistics statistics) {
        this.statistics = Objects.requireNonNull(
                statistics,
                "statistics must not be null"
        );
    }

    public void get(HttpExchange exchange) throws IOException {
        JobStatisticsSnapshot snapshot = statistics.snapshot();
        String responseJson =
                JobStatisticsSnapshotJsonEncoder.encode(snapshot);

        HttpResponse.sendJson(exchange, 200, responseJson);
    }
}
