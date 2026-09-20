package com.resumeanalyzer.backend.repository;

import com.resumeanalyzer.backend.model.Resume;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeRepository extends JpaRepository<Resume, Long> {
}