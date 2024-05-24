package com.steampromo.steamz.config;

import com.steampromo.steamz.exceptions.CustomResponseErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;


@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new CustomResponseErrorHandler());
        return restTemplate;
    }
}
