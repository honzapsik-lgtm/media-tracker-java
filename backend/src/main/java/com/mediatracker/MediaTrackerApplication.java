package com.mediatracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MediaTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MediaTrackerApplication.class, args);
    }
}
