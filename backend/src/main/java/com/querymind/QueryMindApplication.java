package com.querymind;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class QueryMindApplication {
    public static void main(String[] args) {
        SpringApplication.run(QueryMindApplication.class, args);
    }
}
