package com.ashutosh.coderank.service;


import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.ashutosh.coderank.constant.ErrorCode;
import com.ashutosh.coderank.constant.SubmissionConstant;
import com.ashutosh.coderank.constant.UtilConstant;
import com.ashutosh.coderank.exceptions.CodeSubmissionException;
import com.ashutosh.coderank.model.CodeSubmission;
import com.ashutosh.coderank.repository.CodeSubmissionRepository;

@Service
public class KafkaConsumer {

    Environment environment;

    @Autowired
    CodeSubmissionRepository codeSubmissionRepository;

    @Autowired
    DockerExecutorService dockerExecutorService;

    public KafkaConsumer(Environment environment){
           this.environment = environment;
    }


    
    @KafkaListener(topics = UtilConstant.TOPIC_STRING, groupId = "coderank-worker")
    public void consumer(String message){
        System.out.println("Message received from Kafka topic: " + message);
        UUID submissionId;
        try {
            submissionId = UUID.fromString(message);
        } catch (IllegalArgumentException e) {
            throw new CodeSubmissionException(ErrorCode.INVALID_JOB);
        }
        
        // Find the Code from the repository
        Optional<CodeSubmission> codeSubmission = codeSubmissionRepository.findById(submissionId);
        if(codeSubmission.isPresent()){
            CodeSubmission code = codeSubmission.get();
            if(!code.getStatus().equals(SubmissionConstant.STATUS_PENDING)){
              throw new CodeSubmissionException(ErrorCode.INVALID_JOB);
            }  
            // Here you can add the logic to execute the code and update the status and result in the database
            // For example, you can set the status to "Processing" and then execute the code
            code.setStatus(SubmissionConstant.STATUS_PROCESSING);
            codeSubmissionRepository.save(code);


            // Need to Invoke the Docker using ProcessBuilder to execute the code
            // and capture output and error and update in the database 
            code.setStatus(SubmissionConstant.STATUS_PROCESSING);
            codeSubmissionRepository.save(code);
            dockerExecutorService.executeCode(code);
          
        } else {
            throw new CodeSubmissionException(ErrorCode.SUBMISSION_NOT_FOUND);
        }

    }
    
}
