package com.resumeanalyzer.backend.service;

import com.resumeanalyzer.backend.model.Resume;
import com.resumeanalyzer.backend.repository.ResumeRepository;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Set;

@Service
public class ResumeService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword"
    );

    private final ResumeRepository resumeRepository;
    private final Tika tika = new Tika();

    // Spring passes in the repository automatically (constructor injection)
    public ResumeService(ResumeRepository resumeRepository) {
        this.resumeRepository = resumeRepository;
    }

    public Resume upload(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The file is empty");
        }

        try {
            byte[] bytes = file.getBytes();

            // Tika checks the file's real content, not just its extension
            String type = tika.detect(bytes);
            if (!ALLOWED_TYPES.contains(type)) {
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "Only PDF and Word files are supported (detected: " + type + ")");
            }

            String text = tika.parseToString(new ByteArrayInputStream(bytes)).trim();
            if (text.isBlank()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "No text found. Scanned or image-only resumes are not supported yet");
            }

            Resume resume = new Resume();
            resume.setFileName(file.getOriginalFilename());
            resume.setExtractedText(text);
            return resumeRepository.save(resume);

        } catch (IOException | TikaException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Could not read this file", e);
        }
    }
    public Resume getById(Long id) {
        return resumeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resume not found"));
    }
}