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
        try {
            itemService.fetchAndSaveAllItems();
        } catch (Exception e) {
            logger.error("Error in fetching and saving all items", e);
        }
    }

    public Item fetchAndSaveItem(String marketHashName) {
        try {
            return itemService.fetchAndSaveSingleItem(marketHashName);
        } catch (Exception e) {
            logger.error("Error in fetching and saving item for marketHashName: {}", marketHashName, e);
            return null;
        }
    }

}
