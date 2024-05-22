//package com.steampromo.steamz.items.service.item;
//
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
//
//    @Scheduled(fixedRate = 360) // every hour
//    public void checkPriceAnomalies() {
//        itemService.checkPriceAnomaliesForCases();
//    }
//
//    @Scheduled(cron = "0 0 * * * ?") // Runs every hour
//    public void fetchAndSaveItems() {
//        String[] marketHashNames = {"Kilowatt Case"}; // Add more item names as needed
//        for (String name : marketHashNames) {
//            itemService.fetchAndSaveItem(name);
//        }
//    }
//}
