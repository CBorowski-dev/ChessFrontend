package com.example.chessfrontend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ChessFrontendApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChessFrontendApplication.class, args);
    }
} 