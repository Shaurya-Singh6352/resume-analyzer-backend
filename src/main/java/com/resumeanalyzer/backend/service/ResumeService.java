package com.resumeanalyzer.backend.service;

import com.resumeanalyzer.backend.model.Resume;
import com.resumeanalyzer.backend.repository.ResumeRepository;
import com.resumeanalyzer.backend.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final Tika tika = new Tika();

    public ResumeService(ResumeRepository resumeRepository, UserRepository userRepository) {
        this.resumeRepository = resumeRepository;
        this.userRepository = userRepository;
    }

    public Resume upload(MultipartFile file, Long userId) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The file is empty");
        }

        try {
            byte[] bytes = file.getBytes();

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
            // getReferenceById links the user by id without loading the whole row
            resume.setUser(userRepository.getReferenceById(userId));
            return resumeRepository.save(resume);

        } catch (IOException | TikaException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Could not read this file", e);
        }
    }

    /**
     * Finds a resume only if it belongs to this user. Someone else's resume gets
     * the same 404 as one that doesn't exist, so ids can't be probed.
     */
    public Resume getOwned(Long id, Long userId) {
        return resumeRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resume not found"));
    }
}