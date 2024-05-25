package com.steampromo.steamz.items.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.steampromo.steamz.items.domain.Item;
import com.steampromo.steamz.items.domain.MarketHashCaseNameHolder;
import com.steampromo.steamz.items.domain.PriceOverviewResponse;
import com.steampromo.steamz.items.domain.enums.CategoryEnum;
import com.steampromo.steamz.items.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final Logger logger = LoggerFactory.getLogger(ItemService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Value("${steam.api.base-url}")
    private String baseUrl;

    @Value("${steam.api.country}")
    private String country;

    @Value("${steam.api.currency}")
    private int currency;

    @Value("${steam.api.appid}")
    private int appid;
    public void fetchAndSaveAllItems() {
        List<String> marketHashNames = MarketHashCaseNameHolder.getMarketHashNames();
        int delay = 0;
        for (String marketHashName : marketHashNames) {
            scheduler.schedule(() -> {
                try {
                    fetchAndSaveSingleItem(marketHashName);
                } catch (Exception e) {
                    logger.error("Error during fetching or saving for marketHashName: {}", marketHashName, e);
                }
            }, delay, TimeUnit.SECONDS);
            delay += 3;
        }
    }

    public Item fetchAndSaveSingleItem(String marketHashName) {
        int attempts = 0;
        int maxAttempts = 5;
        long waitTimeInMillis = 3000;

        while (attempts < maxAttempts) {
            try {
                URI uri = constructURI(marketHashName);
                logger.info("Fetching data from URL: {}", uri);
                ResponseEntity<String> responseEntity = restTemplate.getForEntity(uri, String.class);
                if (processResponse(responseEntity, marketHashName)) {
                    return itemRepository.findItemByName(marketHashName);
                }
                break;
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                    logger.error("Too many requests, retrying after {} ms", waitTimeInMillis);
                    waitAndRetry(waitTimeInMillis);
                    waitTimeInMillis = Math.min(waitTimeInMillis * 2, 15000);
                    attempts++;
                } else {
                    throw e;
                }
            } catch (Exception e) {
                logger.error("An unexpected error occurred while fetching data for marketHashName: {}", marketHashName, e);
                return null;
            }
        }
        return null;
    }

    private void waitAndRetry(long waitTimeInMillis) {
        try {
            Thread.sleep(waitTimeInMillis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            logger.error("Thread interrupted during wait", ie);
        }
    }

    public URI constructURI(String marketHashName) throws Exception {
        String decodedMarketHashName = URLDecoder.decode(marketHashName, StandardCharsets.UTF_8);
        String encodedMarketHashName = URLEncoder.encode(decodedMarketHashName, StandardCharsets.UTF_8);
        String queryString = String.format("?country=%s&currency=%d&appid=%d&market_hash_name=%s",
                country, currency, appid, encodedMarketHashName);
        return new URI(baseUrl + queryString);
    }

    private boolean processResponse(ResponseEntity<String> responseEntity, String marketHashName) {
        String responseBody = responseEntity.getBody();
        HttpHeaders headers = responseEntity.getHeaders();

        if (responseBody != null && responseEntity.getStatusCode().is2xxSuccessful()) {
            if (headers.getContentType() != null && headers.getContentType().toString().contains("application/json")) {
                try {
                    PriceOverviewResponse response = objectMapper.readValue(responseBody, PriceOverviewResponse.class);
                    if (response.isSuccess()) {
                        saveItem(response, marketHashName);
                        return true;
                    } else {
                        logger.error("API response indicates failure for marketHashName: {}", marketHashName);
                    }
                } catch (JsonProcessingException e) {
                    logger.error("Error processing JSON response for marketHashName: {}", marketHashName, e);
                }
            } else {
                logger.error("Unexpected content type for marketHashName: {}. Content type: {}", marketHashName, headers.getContentType());
            }
        } else {
            logger.error("Failed to fetch data for marketHashName: {}. HTTP Status: {}, Response Body: {}", marketHashName, responseEntity.getStatusCode(), responseBody);
        }
        return false;
    }

    private void saveItem(PriceOverviewResponse response, String marketHashName) {
        Item existingItem = itemRepository.findItemByName(marketHashName);
        if (existingItem != null) {
            itemRepository.updateItem(existingItem, response);
        } else {
            Item newItem = new Item();
            newItem.setId(UUID.randomUUID());
            newItem.setItemName(marketHashName);
            newItem.setLowestPrice(parsePrice(response.getLowestPrice()));
            newItem.setMedianPrice(parsePrice(response.getMedianPrice()));
            newItem.setCategory(CategoryEnum.CASE);
            itemRepository.insertNewItem(newItem);
        }
    }

    private double parsePrice(String price) {
        return Double.parseDouble(price.replaceAll("[^\\d,.]", "").replace(",", "."));
    }



}

