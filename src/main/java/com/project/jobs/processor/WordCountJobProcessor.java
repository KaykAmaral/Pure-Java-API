package com.project.jobs.processor;

import com.project.jobs.model.result.WordCountResult;

import java.util.Objects;

public final class WordCountJobProcessor implements JobProcessor<WordCountResult> {

    @Override
    public WordCountResult process(String content) {
        Objects.requireNonNull(content, "content must not be null");

        if (content.isBlank()) {
            return new WordCountResult(0);
        }

        int count = content.trim().split("\\s+").length;

        return new WordCountResult(count);
    }
}