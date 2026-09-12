package com.bravetest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class BraveTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(BraveTestApplication.class, args);
    }
}
