package com.ashutosh.coderank.service;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.ashutosh.coderank.constant.ErrorCode;
import com.ashutosh.coderank.constant.SubmissionConstant;
import com.ashutosh.coderank.exceptions.CodeSubmissionException;
import com.ashutosh.coderank.model.CodeSubmission;
import com.ashutosh.coderank.repository.CodeSubmissionRepository;

@Service
public class DockerExecutorService {

    @Autowired
    CodeSubmissionRepository codeSubmissionRepository;

          private static final Map<String,String> images = Map.of(
            "java", "openjdk:latest",
            "python", "python:latest",
            "cpp", "gcc:latest"
        );

        private static final Map<String,String> fileNames = Map.of(
            "java", "Main.java",
            "python", "main.py",
            "cpp", "main.cpp"
        );

        private static final Map<String,String> runCommands = Map.of(
           "python", "python /code/solution/main.py",
           "java",   "sh -c 'cd /code && javac Solution.java && java Solution'",
           "cpp",    "sh -c 'cd /code && g++ solution.cpp -o solution && ./solution'"
        );
        
        
    @Async("dockerExecutorPool")    
    public void executeCode(CodeSubmission code) {
        
        // start time of the processing of the code
        long startTime = System.currentTimeMillis();
        String language = code.getLanguage();

        try{
            if(!images.containsKey(language.toLowerCase())){
                throw new CodeSubmissionException(language + " is not supported");
            }

            // Firstly we taking the code from the db and write locally in order to execute it
            File tempDir = Files.createTempDirectory("coderank-").toFile();
            File codeFile = new File(tempDir, fileNames.get(language));
            Files.write(codeFile.toPath(), code.getCode().getBytes());

            // Building Docker run command
            List<String> command = List.of(
                "docker", "run",
                "--rm",
                "--network", "none",
                "--memory", "128m",
                "--cpus", "0.5",
                "-v", tempDir.getAbsolutePath() + ":/code",
                images.get(language.toLowerCase()),
                "sh", "-c", runCommands.get(language.toLowerCase())
            );

            System.out.println("Running Docker command "+ String.join(" ", command));


            // Execute the Docker using the processBuilder
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(false);
            boolean finished = false;
            Process process = null;

              //    try{
                 process = processBuilder.start();
                 // Wait for max 10 seconds to procees to complete
                  finished = process.waitFor(5, TimeUnit.SECONDS);
            // }catch (Exception e){
            //     if(process != null){
            //         process.destroyForcibly();
            //     }
            //     throw new CodeSubmissionException(ErrorCode.CODE_EXECUTION_ERROR + " : " + e.getMessage());
            // }
           


            if(!finished){

                // if the process is not finished destory the process and update 
                // the status in the database as failed and return 
                process.destroyForcibly();
                code.setStatus(SubmissionConstant.STATUS_FAILED);
                code.setError(ErrorCode.TIME_LIMIT_EXCEED);
                code.setExecutionTime(System.currentTimeMillis() - startTime);
                codeSubmissionRepository.save(code);
                throw new CodeSubmissionException(code +" "+ ErrorCode.TIME_LIMIT_EXCEED);

            }

            // OtherWise if the process if finished hence the code is successfully executed 
            String output = new String (process.getInputStream().readAllBytes()).trim();
            String error = new String (process.getErrorStream().readAllBytes()).trim();
            long execTime = System.currentTimeMillis() - startTime;

            if(process.exitValue() == 0){
                code.setStatus(SubmissionConstant.STATUS_SUCCESS);
                code.setOutput(output);
                code.setError(error.isEmpty()? null : error);
            } else {
                code.setStatus(SubmissionConstant.STATUS_FAILED);
                code.setOutput(output.isEmpty() ? null : output);
                code.setError(error);
            }
            code.setExecutionTime(execTime);
            code.setCompletedAt(LocalDateTime.now());
            codeSubmissionRepository.save(code);

            // cleanup the temp directory after execution
            tempDir.deleteOnExit();
        }catch (Exception e){
            code.setStatus(SubmissionConstant.STATUS_FAILED);
            code.setError(e.getMessage());
            code.setExecutionTime(System.currentTimeMillis() - startTime);
            codeSubmissionRepository.save(code);
            throw new CodeSubmissionException(ErrorCode.CODE_EXECUTION_ERROR + " : " + e.getMessage());
        }
  
    }


    
}
