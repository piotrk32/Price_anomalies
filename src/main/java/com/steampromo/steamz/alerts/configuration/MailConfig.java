package com.steampromo.steamz.alerts.configuration;

import com.sendgrid.SendGrid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class MailConfig {

    private final AlertProperties alertProperties;

    @Autowired
    public MailConfig(AlertProperties alertProperties) {
        this.alertProperties = alertProperties;
    }

    @Bean
    public SendGrid sendGrid() {
        return new SendGrid(alertProperties.getApiKey());
    }
}
