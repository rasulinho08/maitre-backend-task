package com.maitre.kiosk;

import java.time.Clock;
import java.time.Duration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class KioskApplication {

    public static void main(String[] args) {
        SpringApplication.run(KioskApplication.class, args);
    }

    @Bean
    Clock clock() {
        // Millisecond precision matches what is stored in the database.
        return Clock.tick(Clock.systemUTC(), Duration.ofMillis(1));
    }
}
