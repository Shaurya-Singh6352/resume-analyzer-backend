package com.resumeanalyzer.backend.controller;

import com.resumeanalyzer.backend.dto.AnalysisResponse;
import com.resumeanalyzer.backend.dto.ResumeUploadResponse;
import com.resumeanalyzer.backend.model.Resume;
import com.resumeanalyzer.backend.service.ResumeService;
import com.resumeanalyzer.backend.service.ScoringService;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final ResumeService resumeService;
    private final ScoringService scoringService;

    public ResumeController(ResumeService resumeService, ScoringService scoringService) {
        this.resumeService = resumeService;
        this.scoringService = scoringService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResumeUploadResponse upload(@RequestParam("file") MultipartFile file,
                                       @AuthenticationPrincipal Jwt jwt) {
        Resume saved = resumeService.upload(file, userId(jwt));

        String text = saved.getExtractedText();
        String preview = text.length() > 300 ? text.substring(0, 300) + "..." : text;
        int wordCount = text.split("\\s+").length;

        return new ResumeUploadResponse(saved.getId(), saved.getFileName(), wordCount, preview);
    }

    @GetMapping("/{id}/analysis")
    public AnalysisResponse analysis(@PathVariable Long id,
                                     @AuthenticationPrincipal Jwt jwt) {
        return scoringService.analyze(resumeService.getOwned(id, userId(jwt)));
    }

    // The token's subject is the user id we put there in JwtService
    private Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}