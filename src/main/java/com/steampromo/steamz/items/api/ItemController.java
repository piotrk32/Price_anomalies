package com.steampromo.steamz.items.api;

import com.steampromo.steamz.items.service.item.ItemService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;
    private static final Logger logger = LoggerFactory.getLogger(ItemController.class);


    @GetMapping("/check-anomalies")
    public String checkPriceAnomalies() {
        itemService.checkPriceAnomaliesForCases();
        return "Price anomalies check completed";
    }

    @PostMapping("/fetch-and-save")
    public String fetchAndSaveItem(@RequestParam String marketHashName) {
        logger.info("Received request to fetch and save item with marketHashName: {}", marketHashName);
        itemService.fetchAndSaveItem(marketHashName);
        return "Item fetch and save completed for: " + marketHashName;
    }
}
