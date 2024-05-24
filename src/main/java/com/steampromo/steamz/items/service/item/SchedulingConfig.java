//package com.steampromo.steamz.items.service.item;
//
//import com.steampromo.steamz.items.domain.item.MarketHashCaseNameHolder;
//import com.steampromo.steamz.items.service.alert.AlertSerivce;
//import lombok.RequiredArgsConstructor;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.scheduling.annotation.EnableScheduling;
//import org.springframework.scheduling.annotation.Scheduled;
//
//@Configuration
//@EnableScheduling
//@RequiredArgsConstructor
//public class SchedulingConfig {
//
//
//    private final  ItemService itemService;
//    private final AlertSerivce alertSerivce;
//
//    @Scheduled(fixedRate = 360) // every hour
//    public void checkPriceAnomalies() {
//        alertSerivce.checkPriceAnomaliesForCases();
//    }
//}
