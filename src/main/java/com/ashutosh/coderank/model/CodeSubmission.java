package com.ashutosh.coderank.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Entity
@Data
public class CodeSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private  String language;
    private String code;
    private String status;
    private LocalDateTime submissionDate;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private Users user;
    private String error;
    private Long executionTime;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private Long retryCount = 0L;
    private String output;

}
