package com.project.jobs.json;

import com.project.jobs.dto.CreateJobResponse;

import java.util.Objects;

public final class CreateJobResponseJsonEncoder {

    private CreateJobResponseJsonEncoder() {
    }

    public static String encode(CreateJobResponse response) {
        Objects.requireNonNull(response, "response must not be null");

        return """
                {"id":"%s","status":"%s"}
                """.formatted(
                response.id(),
                response.status()
        ).strip();
    }
}