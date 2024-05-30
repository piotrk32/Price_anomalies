package com.steampromo.steamz.items.configuration;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.antlr.v4.runtime.misc.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "item")
@AllArgsConstructor
@Data
public class ItemProperties {
    @NotNull
    private int attempts;
    @NotNull
    private int maxAttempts;
    @NotNull
    private int  waitTimeInMillis;
}
