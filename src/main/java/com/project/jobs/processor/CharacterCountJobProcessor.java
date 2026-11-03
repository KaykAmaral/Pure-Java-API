package com.project.jobs.processor;

import com.project.jobs.model.result.CharacterCountResult;

import java.util.Objects;

public final class CharacterCountJobProcessor
        implements JobProcessor<CharacterCountResult> {

    @Override
    public CharacterCountResult process(String content) {
        Objects.requireNonNull(content, "content must not be null");

        int count = content.codePointCount(0, content.length());

        return new CharacterCountResult(count);
    }
}
