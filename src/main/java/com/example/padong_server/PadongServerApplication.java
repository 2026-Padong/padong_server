package com.example.padong_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PadongServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PadongServerApplication.class, args);
    }

}
