package com.steampromo.steamz.items.api;

import com.steampromo.steamz.items.service.alert.AlertSerivce;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertSerivce alertSerivce;

    @PostMapping("/check-prices")
    public String checkItemPrices() {
        alertSerivce.checkPriceAnomaliesForCases();
        return "Price check initiated and alerts creation in progress.";
    }
}
