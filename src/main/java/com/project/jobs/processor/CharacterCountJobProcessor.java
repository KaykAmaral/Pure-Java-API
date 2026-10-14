package com.project.jobs.processor;

import java.util.Objects;

public final class CharacterCountJobProcessor implements JobProcessor{

    @Override 
    public int process(String content) {
        Objects.requireNonNull(content, "content must not be null");

        return content.codePointCount(0, content.length());
    }
    
}
