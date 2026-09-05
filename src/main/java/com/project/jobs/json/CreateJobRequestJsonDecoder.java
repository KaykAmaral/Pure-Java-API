package com.project.jobs.json;

import com.project.jobs.dto.CreateJobRequest;
import com.project.jobs.exception.InvalidRequestException;
import com.project.jobs.model.JobType;

import java.util.Map;
import java.util.Set;

public final class CreateJobRequestJsonDecoder {

    private static final Set<String> ALLOWED_FIELDS = Set.of("type", "content");

    private CreateJobRequestJsonDecoder() {
    }

    public static CreateJobRequest decode(String json) {
        Map<String, String> fields = parseFields(json);

        rejectUnknownFields(fields);

        String typeText = requireField(fields, "type");
        String content = requireField(fields, "content");
        JobType type = parseJobType(typeText);

        try {
            return new CreateJobRequest(type, content);
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException(exception.getMessage(), exception);
        }
    }

    private static Map<String, String> parseFields(String json) {
        if (json == null) {
            throw new InvalidRequestException("request body must not be null");
        }

        try {
            return FlatJsonObjectParser.parse(json);
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException("malformed JSON", exception);
        }
    }

    private static void rejectUnknownFields(Map<String, String> fields) {
        for (String field : fields.keySet()) {
            if (!ALLOWED_FIELDS.contains(field)) {
                throw new InvalidRequestException("unknown field: " + field);
            }
        }
    }

    private static String requireField(Map<String, String> fields, String fieldName) {
        if (!fields.containsKey(fieldName)) {
            throw new InvalidRequestException("missing required field: " + fieldName);
        }

        return fields.get(fieldName);
    }

    private static JobType parseJobType(String typeText) {
        try {
            return JobType.valueOf(typeText);
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException("unknown job type: " + typeText, exception);
        }
    }
}
