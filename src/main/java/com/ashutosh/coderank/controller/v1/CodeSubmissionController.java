package com.ashutosh.coderank.controller.v1;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ashutosh.coderank.Dto.OutputCodeDto;
import com.ashutosh.coderank.Dto.SubmitedCodeDto;
import com.ashutosh.coderank.constant.UtilConstant;
import com.ashutosh.coderank.service.CodeSubmissionService;

@RestController
@RequestMapping("/submit/v1")
public class CodeSubmissionController {

    @Autowired
    CodeSubmissionService  codeSubmissionService;

    @PostMapping("/submit")
    public ResponseEntity<String> submitCode(@jakarta.validation.Valid @RequestBody SubmitedCodeDto submitedCodeDto) {
        // Logic to submit code for execution   
        String submissionId =  codeSubmissionService.createSubmission(submitedCodeDto);
        return new ResponseEntity<>(submissionId,HttpStatus.CREATED);
    }

    @GetMapping("/result/{id}")
    public ResponseEntity<OutputCodeDto> getSubmissionResult(@PathVariable("id") String id){
        OutputCodeDto outputCodeDto = codeSubmissionService.getSubmissionResult(id);
        return new ResponseEntity<>(outputCodeDto,HttpStatus.OK);
    }


}
