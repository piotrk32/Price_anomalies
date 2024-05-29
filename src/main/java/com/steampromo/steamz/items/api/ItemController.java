package com.steampromo.steamz.items.api;

import com.steampromo.steamz.items.service.ItemFacade;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemFacade itemFacade;

    private static final Logger logger = LoggerFactory.getLogger(ItemController.class);

    @PostMapping("/fetch-and-save")
    public String saveSingleItemData(@RequestParam String marketHashName) {
        logger.info("Received request to fetch and save item with marketHashName: {}", marketHashName);
        itemFacade.saveSingleItemData(marketHashName);
        return "Item fetch and save completed for: " + marketHashName;
    }

    @PostMapping("/fetch-and-save-all")
    public String saveAllItemsData() {
        logger.info("Received request to fetch and save all items");
        itemFacade.saveAllItemsData();
        return "Item fetch and save completed for all items";
    }
}
