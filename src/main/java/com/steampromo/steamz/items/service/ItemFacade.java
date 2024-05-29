package com.steampromo.steamz.items.service;

import com.steampromo.steamz.items.domain.Item;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ItemFacade {

    private final ItemService itemService;
    private static final Logger logger = LoggerFactory.getLogger(ItemFacade.class);

    public void fetchAndSaveAllItems() {
        itemService.fetchAndSaveAllItems();
    }

    public Item fetchAndSaveItem(String marketHashName) {
        return itemService.fetchAndSaveSingleItem(marketHashName);
    }

}
