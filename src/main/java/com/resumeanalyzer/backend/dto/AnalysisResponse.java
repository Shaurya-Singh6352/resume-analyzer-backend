package com.resumeanalyzer.backend.dto;

import java.util.List;

public record AnalysisResponse(
        Long resumeId,
        String fileName,
        int overallScore,
        List<CategoryScore> categories,
        List<String> strengths,
        List<String> suggestions,
        List<String> missingKeywords
) {}