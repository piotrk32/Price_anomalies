package com.steampromo.steamz.items.configuration;

import com.steampromo.steamz.items.repository.ItemRepository;
import com.steampromo.steamz.items.repository.ItemRepositoryImplementation;
import com.steampromo.steamz.items.service.ItemFacade;
import com.steampromo.steamz.items.service.ItemService;
import com.steampromo.steamz.proxy.ProxyService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class ItemConfiguration {

    @Bean(name = "itemRepository")
    public ItemRepository itemRepository(JdbcTemplate jdbcTemplate) {
        return new ItemRepositoryImplementation(jdbcTemplate);
    }

    @Bean
    public ItemService itemService(
            @Qualifier("itemRepository") ItemRepository itemRepository,
            SteamProperties steamProperties,
            ItemProperties itemProperties,
            ProxyService proxyService) {
        return new ItemService(itemRepository, steamProperties, itemProperties, proxyService);
    }

    @Bean
    public ItemFacade itemFacade(ItemService itemService) {
        return new ItemFacade(itemService);
    }
}

