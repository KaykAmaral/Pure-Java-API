package com.project.jobs.model.result;

public record CharacterCountResult(int value) implements JobResult {

    public CharacterCountResult {
        if (value < 0) {
            throw new IllegalArgumentException(
                    "character count must not be negative"
            );
        }
    }
}