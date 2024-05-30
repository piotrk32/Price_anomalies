package com.steampromo.steamz.alerts.configuration;

import com.steampromo.steamz.alerts.repository.AlertRepository;
import com.steampromo.steamz.alerts.repository.AlertRepositoryImplementation;
import com.steampromo.steamz.alerts.service.AlertService;
import com.steampromo.steamz.items.configuration.SteamProperties;
import com.steampromo.steamz.items.repository.ItemRepository;
import com.steampromo.steamz.items.repository.ItemRepositoryImplementation;
import com.steampromo.steamz.proxy.ProxyService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties({AlertProperties.class, SteamProperties.class})
public class AlertConfiguration {

    @Bean
    public AlertRepository alertRepository(JdbcTemplate jdbcTemplate) {
        return new AlertRepositoryImplementation(jdbcTemplate);
    }

    @Bean(name = "alertItemRepository")
    public ItemRepository itemRepository(JdbcTemplate jdbcTemplate) {
        return new ItemRepositoryImplementation(jdbcTemplate);
    }

    @Bean
    public AlertService alertService(
            @Qualifier("alertItemRepository") ItemRepository itemRepository,
            AlertRepository alertRepository,
            ProxyService proxyService,
            SteamProperties steamProperties,
            AlertProperties alertProperties) {
        return new AlertService(itemRepository, alertRepository, proxyService, alertProperties, steamProperties);
    }

}
