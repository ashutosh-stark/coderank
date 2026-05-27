package com.ashutosh.coderank.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.ashutosh.coderank.constant.ErrorCode;
import com.ashutosh.coderank.constant.SubmissionConstant;
import com.ashutosh.coderank.model.CodeSubmission;
import com.ashutosh.coderank.repository.CodeSubmissionRepository;

@Service
public class DockerExecutorService {

    private static final Logger log = LoggerFactory.getLogger(DockerExecutorService.class);

    private static final int MAX_OUTPUT_CHARS = 65_000;
    private static final long EXECUTION_TIMEOUT_SECONDS = 30L;

    @Autowired
    CodeSubmissionRepository codeSubmissionRepository;

    private static final Map<String, String> images = Map.of(
            "java", "openjdk:latest",
            "python", "python:latest",
            "cpp", "gcc:latest");

    private static final Map<String, String> fileNames = Map.of(
            "java", "Main.java",
            "python", "main.py",
            "cpp", "main.cpp");

    private static final Map<String, String> runCommands = Map.of(
            "python", "python /code/main.py",
            "java", "cd /code && javac Main.java && java Main",
            "cpp", "cd /code && g++ main.cpp -o main && ./main");

    private static String truncate(String value) {
        if (value == null || value.length() <= MAX_OUTPUT_CHARS) {
            return value;
        }
        return value.substring(0, MAX_OUTPUT_CHARS) + "\n...[truncated]";
    }

    @Async("dockerExecutorPool")
    public void executeCode(CodeSubmission code) {

        long startTime = System.currentTimeMillis();
        String language = code.getLanguage() == null ? "" : code.getLanguage().toLowerCase();
        File tempDir = null;

        try {
            if (!images.containsKey(language)) {
                code.setStatus(SubmissionConstant.STATUS_FAILED);
                code.setError(language + " is not supported");
                code.setExecutionTime(System.currentTimeMillis() - startTime);
                code.setCompletedAt(LocalDateTime.now());
                codeSubmissionRepository.save(code);
                return;
            }

            tempDir = Files.createTempDirectory("coderank-").toFile();
            File codeFile = new File(tempDir, fileNames.get(language));
            Files.write(codeFile.toPath(), code.getCode().getBytes());

            List<String> command = List.of(
                    "docker", "run",
                    "--rm",
                    "--network", "none",
                    "--memory", "128m",
                    "--cpus", "0.5",
                    "-v", tempDir.getAbsolutePath() + ":/code",
                    images.get(language),
                    "sh", "-c", runCommands.get(language));

            log.info("Running docker command for submission {}: {}", code.getId(), String.join(" ", command));

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(false);

            Process process = processBuilder.start();
            boolean finished = process.waitFor(EXECUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                code.setStatus(SubmissionConstant.STATUS_FAILED);
                code.setError(ErrorCode.TIME_LIMIT_EXCEED);
                code.setExecutionTime(System.currentTimeMillis() - startTime);
                code.setCompletedAt(LocalDateTime.now());
                codeSubmissionRepository.save(code);
                return;
            }

            String output = new String(process.getInputStream().readAllBytes()).trim();
            String error = new String(process.getErrorStream().readAllBytes()).trim();
            long execTime = System.currentTimeMillis() - startTime;

            if (process.exitValue() == 0) {
                code.setStatus(SubmissionConstant.STATUS_SUCCESS);
                code.setOutput(truncate(output));
                code.setError(error.isEmpty() ? null : truncate(error));
            } else {
                code.setStatus(SubmissionConstant.STATUS_FAILED);
                code.setOutput(output.isEmpty() ? null : truncate(output));
                code.setError(truncate(error));
            }
            code.setExecutionTime(execTime);
            code.setCompletedAt(LocalDateTime.now());
            codeSubmissionRepository.save(code);

        } catch (Exception e) {
            log.error("Submission {} failed during docker execution", code.getId(), e);
            code.setStatus(SubmissionConstant.STATUS_FAILED);
            code.setError(truncate(ErrorCode.CODE_EXECUTION_ERROR + " : " + e.getMessage()));
            code.setExecutionTime(System.currentTimeMillis() - startTime);
            code.setCompletedAt(LocalDateTime.now());
            codeSubmissionRepository.save(code);
        } finally {
            cleanup(tempDir);
        }
    }

    private static void cleanup(File tempDir) {
        if (tempDir == null || !tempDir.exists()) {
            return;
        }
        try {
            Files.walk(tempDir.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(java.nio.file.Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            log.warn("Failed to clean up temp directory {}: {}", tempDir, e.getMessage());
        }
    }
}
