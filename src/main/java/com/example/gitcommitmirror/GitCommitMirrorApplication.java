package com.example.gitcommitmirror;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GitCommitMirrorApplication {

    public static void main(String[] args) {
        SpringApplication.run(GitCommitMirrorApplication.class, args);
    }

}
