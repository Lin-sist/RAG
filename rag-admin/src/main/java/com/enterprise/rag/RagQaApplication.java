package com.enterprise.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enterprise RAG QA System 主应用入口
 */
@SpringBootApplication
@EnableAsync
public class RagQaApplication {

    public static void main(String[] args) {
        SpringApplication.run(RagQaApplication.class, args);
    }
}
