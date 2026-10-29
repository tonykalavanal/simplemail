package com.tonyjose.simplemail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point. Starts the embedded web server on port 8080. */
@SpringBootApplication
public class SimpleMailApplication {
    public static void main(String[] args) {
        SpringApplication.run(SimpleMailApplication.class, args);
    }
}
