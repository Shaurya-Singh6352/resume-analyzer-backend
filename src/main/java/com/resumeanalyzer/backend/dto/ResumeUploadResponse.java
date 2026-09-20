package com.resumeanalyzer.backend.dto;

public record ResumeUploadResponse(
        Long id,
        String fileName,
        int wordCount,
        String textPreview
) {}