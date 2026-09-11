package com.project.jobs.json;

import com.project.jobs.dto.JobSummaryResponse;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public final class JobSummaryResponseJsonEncoder {

    private JobSummaryResponseJsonEncoder() {
    }

    public static String encodeList(List<JobSummaryResponse> responses) {
        Objects.requireNonNull(responses, "responses must not be null");

        StringJoiner json = new StringJoiner(",", "[", "]");

        for (JobSummaryResponse response : responses) {
            json.add(encode(response));
        }

        return json.toString();
    }

    public static String encode(JobSummaryResponse response) {
        Objects.requireNonNull(response, "response must not be null");

        return """
                {"id":"%s","type":"%s","status":"%s"}
                """.formatted(
                response.id(),
                response.type(),
                response.status()
        ).strip();
    }
}