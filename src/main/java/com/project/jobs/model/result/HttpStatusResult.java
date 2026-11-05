package com.project.jobs.model.result;

public record HttpStatusResult(int value) implements JobResult {

    public HttpStatusResult {
        if (value < 100 || value > 599) {
            throw new IllegalArgumentException(
                    "HTTP status code must be between 100 and 599"
            );
        }
    }
}
