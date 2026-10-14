package com.project.jobs.processor;

import java.util.Objects;

public final class WordCountJobProcessor implements JobProcessor {

    @Override 
    public int process(String content) {
        Objects.requireNonNull(content, "content must not be null");

        if (content.isBlank()) {
            return 0;
        }

        return content.trim().split("\\s+").length;

    }

}