package com.steampromo.steamz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@ConfigurationPropertiesScan

public class SteamzApplication {

    public static void main(String[] args) {
        SpringApplication.run(SteamzApplication.class, args);
    }

}
