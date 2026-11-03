package com.project.jobs.processor;

import com.project.jobs.model.result.JobResult;

public interface JobProcessor<R extends JobResult> {

    R process(String content);

}
