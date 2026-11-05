package com.project.jobs.http;

import com.project.jobs.dto.JobSearchCriteria;
import com.project.jobs.exception.InvalidRequestException;
import com.project.jobs.model.JobStatus;
import com.project.jobs.model.JobType;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class JobSearchCriteriaDecoder {

    private static final Set<String> ALLOWED_PARAMETERS =
            Set.of("status", "type");

    private JobSearchCriteriaDecoder() {
    }

    public static JobSearchCriteria decode(String rawQuery) {
        Map<String, String> parameters = parseParameters(rawQuery);

        rejectUnknownParameters(parameters);

        return new JobSearchCriteria(
                parseStatus(parameters.get("status")),
                parseType(parameters.get("type"))
        );
    }

    private static Map<String, String> parseParameters(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return Map.of();
        }

        Map<String, String> parameters = new HashMap<>();

        for (String pair : rawQuery.split("&", -1)) {
            int separatorPosition = pair.indexOf('=');

            if (separatorPosition <= 0) {
                throw new InvalidRequestException(
                        "invalid query parameter: " + pair
                );
            }

            String name = decodeComponent(
                    pair.substring(0, separatorPosition)
            );
            String value = decodeComponent(
                    pair.substring(separatorPosition + 1)
            );

            if (value.isBlank()) {
                throw new InvalidRequestException(
                        "query parameter must not be blank: " + name
                );
            }

            if (parameters.putIfAbsent(name, value) != null) {
                throw new InvalidRequestException(
                        "duplicate query parameter: " + name
                );
            }
        }

        return Map.copyOf(parameters);
    }

    private static String decodeComponent(String component) {
        try {
            return URLDecoder.decode(component, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException(
                    "invalid URL encoding in query string",
                    exception
            );
        }
    }

    private static void rejectUnknownParameters(
            Map<String, String> parameters
    ) {
        for (String parameter : parameters.keySet()) {
            if (!ALLOWED_PARAMETERS.contains(parameter)) {
                throw new InvalidRequestException(
                        "unknown query parameter: " + parameter
                );
            }
        }
    }

    private static Optional<JobStatus> parseStatus(String statusText) {
        if (statusText == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(JobStatus.valueOf(statusText));
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException(
                    "unknown job status: " + statusText,
                    exception
            );
        }
    }

    private static Optional<JobType> parseType(String typeText) {
        if (typeText == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(JobType.valueOf(typeText));
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException(
                    "unknown job type: " + typeText,
                    exception
            );
        }
    }
}
