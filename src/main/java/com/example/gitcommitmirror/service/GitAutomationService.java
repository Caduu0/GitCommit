package com.example.gitcommitmirror.service;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

@Service
public class GitAutomationService {

    @Value("${github.repo.url}")
    private String repoUrl;

    @Value("${github.pat}")
    private String pat;

    @Value("${git.author.name}")
    private String authorName;

    @Value("${git.author.email}")
    private String authorEmail;

    @Value("${git.local.dir}")
    private String localDirPath;

    public void createCommit(String message) {
        File localPath = new File(localDirPath);
        CredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(pat, "");

        try {
            Git git;
            if (!localPath.exists() || !new File(localPath, ".git").exists()) {
                System.out.println("Cloning repository...");
                git = Git.cloneRepository()
                        .setURI(repoUrl)
                        .setDirectory(localPath)
                        .setCredentialsProvider(credentialsProvider)
                        .call();
            } else {
                System.out.println("Repository exists. Pulling latest changes...");
                git = Git.open(localPath);
                git.pull().setCredentialsProvider(credentialsProvider).call();
            }

            // Update log file
            File logFile = new File(localPath, "commit-mirror-log.txt");
            String logEntry = LocalDateTime.now() + " - " + message + "\n";
            Files.write(logFile.toPath(), logEntry.getBytes(),
                    logFile.exists() ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);

            System.out.println("Adding changes...");
            git.add().addFilepattern(".").call();

            System.out.println("Committing changes...");
            git.commit()
                    .setMessage(message)
                    .setAuthor(authorName, authorEmail)
                    .call();

            System.out.println("Pushing changes...");
            git.push().setCredentialsProvider(credentialsProvider).call();
            
            git.close();
            System.out.println("Commit pushed successfully!");

        } catch (GitAPIException | IOException e) {
            System.err.println("Error executing Git operations: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to execute git operations", e);
        }
    }
}
