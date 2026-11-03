package com.project.jobs.model.result;

public record WordCountResult(int value) implements JobResult {

    public WordCountResult {
        if (value < 0) {
            throw new IllegalArgumentException(
                    "word count must not be negative"
            );
        }
    }
}