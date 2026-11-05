package com.project.jobs.controller;

import com.project.jobs.dto.CreateJobRequest;
import com.project.jobs.dto.CreateJobResponse;
import com.project.jobs.dto.JobSummaryResponse;
import com.project.jobs.dto.JobSearchCriteria;
import com.project.jobs.exception.InvalidRequestException;
import com.project.jobs.http.HttpResponse;
import com.project.jobs.http.JobSearchCriteriaDecoder;
import com.project.jobs.json.CreateJobRequestJsonDecoder;
import com.project.jobs.json.CreateJobResponseJsonEncoder;
import com.project.jobs.json.JobSummaryResponseJsonEncoder;
import com.project.jobs.model.Job;
import com.project.jobs.service.JobService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class JobController {

    private final JobService service;

    public JobController(JobService service) {
        this.service = Objects.requireNonNull(
                service,
                "service must not be null"
        );
    }

    public void create(HttpExchange exchange) throws IOException {
        try (InputStream requestBody = exchange.getRequestBody()) {
            byte[] requestBytes = requestBody.readAllBytes();
            String requestJson = new String(
                    requestBytes,
                    StandardCharsets.UTF_8
            );

            CreateJobRequest request =
                    CreateJobRequestJsonDecoder.decode(requestJson);

            Job job = service.create(request);

            CreateJobResponse response =
                    new CreateJobResponse(job.id(), job.status());

            String responseJson =
                    CreateJobResponseJsonEncoder.encode(response);

            HttpResponse.sendJson(exchange, 201, responseJson);
        }
    }

    public void findAll(HttpExchange exchange) throws IOException {
        JobSearchCriteria criteria = JobSearchCriteriaDecoder.decode(
                exchange.getRequestURI().getRawQuery()
        );

        List<JobSummaryResponse> responses = service
                .findAll(criteria)
                .stream()
                .map(JobController::toSummaryResponse)
                .toList();

        String responseJson = JobSummaryResponseJsonEncoder.encodeList(responses);

        HttpResponse.sendJson(exchange, 200, responseJson);

    }

    private static JobSummaryResponse toSummaryResponse(Job job) {
        return new JobSummaryResponse(
                job.id(),
                job.type(),
                job.status(),
                job.result()
        );
    }

    public void findById(HttpExchange exchange) throws IOException {
        UUID id = extractJobId(exchange);

        Optional<Job> possibleJob = service.findById(id);

        if (possibleJob.isEmpty()) {
                HttpResponse.sendJson(
                        exchange,
                        404,
                        "{\"error\":\"Job Not Found\"}"
                );
                return;
        }

        Job job = possibleJob.get();

        JobSummaryResponse response = new JobSummaryResponse(
                job.id(),
                job.type(),
                job.status(),
                job.result()
        );

        String responseJson = JobSummaryResponseJsonEncoder.encode(response);

        HttpResponse.sendJson(exchange, 200, responseJson);

    }

    private UUID extractJobId(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        int lastSlashPosition = path.lastIndexOf('/');
        String idText = path.substring(lastSlashPosition + 1);

        try {
                return UUID.fromString(idText);
        } catch (IllegalArgumentException exception) {
                throw new InvalidRequestException(
                        "invalid job id: " + idText, exception
                );
        }
    }

    public void deleteById(HttpExchange exchange) throws IOException {
        UUID id = extractJobId(exchange);

        boolean deleted = service.deleteById(id);

        if (!deleted) {
                HttpResponse.sendJson(
                        exchange,
                        404,
                        "{\"error\":\"Job Not Found\"}"
                );
                return;
        }

        HttpResponse.sendNoContent(exchange);
    }

}
