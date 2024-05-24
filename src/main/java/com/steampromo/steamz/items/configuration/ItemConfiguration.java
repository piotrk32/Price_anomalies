package com.steampromo.steamz.items.configuration;

import com.steampromo.steamz.items.repository.ItemRepository;
import com.steampromo.steamz.items.repository.ItemRepositoryImplementation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class ItemConfiguration {

    @Bean
    ItemRepository itemRepository(JdbcTemplate jdbcTemplate) {
        return new ItemRepositoryImplementation(jdbcTemplate);
    }
}

