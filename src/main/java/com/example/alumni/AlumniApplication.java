package com.example.alumni;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AlumniApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlumniApplication.class, args);
    }
}
