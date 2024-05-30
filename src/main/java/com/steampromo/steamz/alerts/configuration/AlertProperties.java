package com.steampromo.steamz.alerts.configuration;

import lombok.Data;
import org.antlr.v4.runtime.misc.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "alert")
public class AlertProperties {
    @NotNull
    private double threshold;
    @NotNull
    private int retries;
    @NotNull
    private int maxRetries;
    @NotNull
    private long retryDelay;
    @NotNull
    private String apiKey;
    @NotNull
    private String emailSource;
    @NotNull
    private String emailDestination;
}
