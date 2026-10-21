package com.alejandroplata.reservasapi.config;

import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.reservation")
public record ReservationProperties(int minMinutes, int maxMinutes, ZoneId timeZone) {
}
