package com.project.jobs.json;

import com.project.jobs.dto.JobSummaryResponse;
import com.project.jobs.model.result.CharacterCountResult;
import com.project.jobs.model.result.JobResult;
import com.project.jobs.model.result.HttpStatusResult;
import com.project.jobs.model.result.WordCountResult;

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

        if (response.result() == null) {
            return """
                    {"id":"%s","type":"%s","status":"%s"}
                    """.formatted(
                    response.id(),
                    response.type(),
                    response.status()
            ).strip();
        }

        int resultValue = extractResultValue(response.result());

        return """
                {"id":"%s","type":"%s","status":"%s","result":%d}
                """.formatted(
                response.id(),
                response.type(),
                response.status(),
                resultValue
        ).strip();
    }

    private static int extractResultValue(JobResult result) {
    return switch (result) {
            case WordCountResult wordCount ->
                    wordCount.value();

            case CharacterCountResult characterCount ->
                    characterCount.value();

            case HttpStatusResult httpStatus ->
                    httpStatus.value();
        };
    }

}
