package com.ashutosh.coderank.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.ashutosh.coderank.Dto.OutputCodeDto;
import com.ashutosh.coderank.Dto.SubmitedCodeDto;
import com.ashutosh.coderank.constant.ErrorCode;
import com.ashutosh.coderank.constant.SubmissionConstant;
import com.ashutosh.coderank.exceptions.CodeSubmissionException;
import com.ashutosh.coderank.model.CodeSubmission;
import com.ashutosh.coderank.model.Users;
import com.ashutosh.coderank.repository.CodeSubmissionRepository;
import com.ashutosh.coderank.repository.UserRepository;

@Service
public class CodeSubmissionService {


    @Autowired
    CodeSubmissionRepository codeSubmissionRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    KafkaProducer kafkaProducer;


    public String createSubmission(SubmitedCodeDto submitedCodeDto){
        if(submitedCodeDto.getCode() == null || submitedCodeDto.getLanguage() == null){
            throw new CodeSubmissionException(ErrorCode.CODE_FORMATE_ERROR);
        }

        CodeSubmission codeSubmission = new CodeSubmission();
        codeSubmission.setCode(submitedCodeDto.getCode());
        codeSubmission.setLanguage(submitedCodeDto.getLanguage());
        codeSubmission.setCreatedAt(LocalDateTime.now());
        codeSubmission.setSubmissionDate(LocalDateTime.now());
        codeSubmission.setStatus(SubmissionConstant.STATUS_PENDING);
        String userName = SecurityContextHolder.getContext().getAuthentication().getName();
        if(userName.isBlank()){
            throw new CodeSubmissionException(ErrorCode.USER_NOT_FOUND);
        }
        Optional<Users> user = userRepository.findByUserName(userName);
        if (!user.isPresent()) {
            throw new CodeSubmissionException(ErrorCode.USER_NOT_FOUND);
        }
        codeSubmission.setUser(user.get());

        codeSubmissionRepository.save(codeSubmission);
        kafkaProducer.producer(codeSubmission.getId());
        return codeSubmission.getId().toString();   
    }


    public OutputCodeDto getSubmissionResult(String id) {
        UUID submissionId;
        try {
            submissionId = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new CodeSubmissionException(ErrorCode.CODE_FORMATE_ERROR);
        }

        Optional<CodeSubmission> codeSubmission = codeSubmissionRepository.findById(submissionId);
        if(!codeSubmission.isPresent()){
            throw new CodeSubmissionException(ErrorCode.CODE_EXECUTION_ERROR);
        }

        // OtherWise fetch the submission output and return to the user
        CodeSubmission getCodeSubmission = codeSubmission.get();
        OutputCodeDto outputCodeDto = new OutputCodeDto();
        outputCodeDto.setStdout(getCodeSubmission.getOutput());
        outputCodeDto.setStderr(getCodeSubmission.getError());
        return outputCodeDto;
    }

}