package com.alejandroplata.reservasapi.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    // El reloj se inyecta en vez de llamar a Instant.now() para poder fijarlo en los tests
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
