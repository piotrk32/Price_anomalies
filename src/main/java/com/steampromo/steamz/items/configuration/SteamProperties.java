package com.steampromo.steamz.items.configuration;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.antlr.v4.runtime.misc.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "steam")
@AllArgsConstructor
@Data
public class SteamProperties {
    @NotNull
    private String baseUrl;
    @NotNull
    private String country;
    @NotNull
    private String currency;
    @NotNull
    private Integer appId;
}

