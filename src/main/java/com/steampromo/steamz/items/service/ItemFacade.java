package com.steampromo.steamz.items.service;

import com.steampromo.steamz.items.domain.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ItemFacade {

    private final ItemService itemService;
    public void saveAllItemsData() {
        itemService.saveAllItemsData();
    }

    public Item saveSingleItemData(String marketHashName) {
        return itemService.saveSingleItemData(marketHashName);
    }

}
