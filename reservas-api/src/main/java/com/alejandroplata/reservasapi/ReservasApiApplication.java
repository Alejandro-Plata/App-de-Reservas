package com.alejandroplata.reservasapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ReservasApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReservasApiApplication.class, args);
    }
}
