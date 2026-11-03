package com.project.jobs.model.result;

public sealed interface JobResult
        permits WordCountResult, CharacterCountResult {
}