package com.resumeanalyzer.backend.repository;

import com.resumeanalyzer.backend.model.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ResumeRepository extends JpaRepository<Resume, Long> {
    Optional<Resume> findByIdAndUserId(Long id, Long userId);
}

