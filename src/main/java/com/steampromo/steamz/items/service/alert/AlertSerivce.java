package com.steampromo.steamz.items.service.alert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.steampromo.steamz.items.domain.alert.Alert;
import com.steampromo.steamz.items.domain.item.Item;
import com.steampromo.steamz.items.domain.item.PriceOverviewResponse;
import com.steampromo.steamz.items.domain.item.enums.CategoryEnum;
import com.steampromo.steamz.items.repository.AlertRepository;
import com.steampromo.steamz.items.repository.ItemRepository;
import com.steampromo.steamz.items.service.item.ItemService;
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
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AlertSerivce {

    @Value("${app.price-alert.threshold}")
    private double threshold;
    private final ItemRepository itemRepository;
    private final AlertRepository alertRepository;
    private final RestTemplate restTemplate;
    private static final Logger logger = LoggerFactory.getLogger(ItemService.class);

    public void checkPriceAnomaliesForCases() {
        List<Item> items = itemRepository.findByCategory(CategoryEnum.CASE.name());
        for (Item item : items) {
            int retries = 3;
            boolean success = false;
            while (retries > 0 && !success) {
                try {
                    URI uri = constructURI(item.getItemName());
                    String rawResponse = restTemplate.getForObject(uri, String.class);
                    if (rawResponse != null && !rawResponse.isEmpty()) {
                        logger.info("Raw API Response: {}", rawResponse);

                        PriceOverviewResponse response = new ObjectMapper().readValue(rawResponse, PriceOverviewResponse.class);
                        if (response.isSuccess()) {
                            double latestPrice = parsePrice(response.getLowestPrice());
                            logger.info("Comparing latest price: {} zł to database stored lowest price: {} zł for item: {}", latestPrice, item.getLowestPrice(), item.getItemName());

                            if (latestPrice < item.getLowestPrice() * (1 - threshold)) {
                                createAlert(item, item.getLowestPrice(), latestPrice);
                                logger.info("Alert created for item: {} with price anomaly detected.", item.getItemName());
                            } else {
                                logger.info("No significant price anomaly detected for item: {} (latest: {} zł, stored: {} zł)", item.getItemName(), latestPrice, item.getLowestPrice());
                            }
                        }
                        success = true;
                    } else {
                        logger.error("Empty or null response for item: {}", item.getItemName());
                    }
                } catch (HttpClientErrorException.TooManyRequests e) {
                    logger.error("Too Many Requests: Retrying after delay...");
                    try {
                        TimeUnit.SECONDS.sleep(10); // Sleep for 10 seconds before retrying
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    retries--;
                } catch (Exception e) {
                    logger.error("Error processing item: " + item.getItemName(), e);
                    break; // break the loop on other exceptions
                }
            }
            if (!success) {
                logger.error("Failed to process item after retries: {}", item.getItemName());
            }
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
        // Remove the currency symbol and replace comma with a decimal point for proper parsing
        priceStr = priceStr.replaceAll("zł", "").trim().replace(",", ".");
        return Double.parseDouble(priceStr);
    }

}
