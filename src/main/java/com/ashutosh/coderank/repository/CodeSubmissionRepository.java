package com.ashutosh.coderank.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ashutosh.coderank.model.CodeSubmission;

public interface CodeSubmissionRepository extends JpaRepository<CodeSubmission, UUID> {
    
}
