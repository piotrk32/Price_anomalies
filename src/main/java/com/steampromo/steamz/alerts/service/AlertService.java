package com.steampromo.steamz.alerts.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.steampromo.steamz.alerts.domain.Alert;
import com.steampromo.steamz.items.domain.Item;
import com.steampromo.steamz.items.domain.PriceOverviewResponse;
import com.steampromo.steamz.items.domain.enums.CategoryEnum;
import com.steampromo.steamz.alerts.repository.AlertRepository;
import com.steampromo.steamz.items.repository.ItemRepository;
import com.steampromo.steamz.items.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AlertService {

    @Value("${app.price-alert.threshold}")
    private double threshold;
    private final ItemRepository itemRepository;
    private final AlertRepository alertRepository;
    private final RestTemplate restTemplate;
    private static final Logger logger = LoggerFactory.getLogger(ItemService.class);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);


    public void checkPriceAnomaliesForCases() {
        List<Item> items = itemRepository.findByCategory(CategoryEnum.CASE.name());
        int delay = 0;
        for (Item item : items) {
            scheduler.schedule(() -> processItemPriceCheck(item), delay, TimeUnit.SECONDS);
            delay += 3;
        }
    }

    private void processItemPriceCheck(Item item) {
        try {
            URI uri = constructURI(item.getItemName());
            String rawResponse = restTemplate.getForObject(uri, String.class);
            if (rawResponse != null && !rawResponse.isEmpty()) {
                logger.info("Raw API Response: {}", rawResponse);

                PriceOverviewResponse response = new ObjectMapper().readValue(rawResponse, PriceOverviewResponse.class);
                if (response.isSuccess()) {
                    double latestPrice = parsePrice(response.getLowestPrice());
                    logger.info("Comparing latest price: {} to database stored lowest price: {} for item: {}", latestPrice, item.getLowestPrice(), item.getItemName());

                    if (latestPrice < item.getLowestPrice() * (1 - threshold)) {
                        createAlert(item, item.getLowestPrice(), latestPrice);
                        logger.info("Alert created for item: {} with price anomaly detected.", item.getItemName());
                    } else {
                        logger.info("No significant price anomaly detected for item: {} (latest: {}, stored: {})", item.getItemName(), latestPrice, item.getLowestPrice());
                    }
                }
            } else {
                logger.error("Empty or null response for item: {}", item.getItemName());
            }
        } catch (HttpClientErrorException.TooManyRequests e) {
            logger.error("Too Many Requests: Retrying after delay...");
        } catch (Exception e) {
            logger.error("Error processing item: {}", item.getItemName(), e);
        }
    }

    public URI constructURI(String marketHashName) throws Exception {
        String decodedMarketHashName = URLDecoder.decode(marketHashName, StandardCharsets.UTF_8);
        String encodedMarketHashName = URLEncoder.encode(decodedMarketHashName, StandardCharsets.UTF_8);
        String baseUrl = "https://steamcommunity.com/market/priceoverview/";
        String queryString = String.format("?country=NL&currency=6&appid=730&market_hash_name=%s", encodedMarketHashName);
        return new URI(baseUrl + queryString);
    }

    private void createAlert(Item item, double dbLowestPrice, double latestPrice) {
        Alert alert = new Alert();
        alert.setItem(item);
        alert.setDate(LocalDateTime.now());
        int priceGap = (int) ((dbLowestPrice - latestPrice) / dbLowestPrice * 100);
        alert.setPriceGap(priceGap);
        alertRepository.save(alert);
        logger.info("Alert saved for item: {} with price gap of {}%", item.getItemName(), priceGap);
    }

    private double parsePrice(String priceStr) {
        priceStr = priceStr.replaceAll("zł", "").trim().replace(",", ".");
        return Double.parseDouble(priceStr);
    }

}
