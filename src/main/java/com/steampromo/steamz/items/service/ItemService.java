package com.steampromo.steamz.items.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.steampromo.steamz.items.configuration.ItemProperties;
import com.steampromo.steamz.items.configuration.SteamProperties;
import com.steampromo.steamz.items.domain.Item;
import com.steampromo.steamz.items.domain.MarketHashCaseNameHolder;
import com.steampromo.steamz.items.domain.PriceOverviewResponse;
import com.steampromo.steamz.items.domain.enums.CategoryEnum;
import com.steampromo.steamz.items.repository.ItemRepository;
import com.steampromo.steamz.proxy.ProxyResponse;
import com.steampromo.steamz.proxy.ProxyService;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ItemService {

    public static final String COUNTRY = "country";
    public static final String CURRENCY = "currency";

    private final ItemRepository itemRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private static final Logger logger = LoggerFactory.getLogger(ItemService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final SteamProperties steamProperties;
    private final ItemProperties itemProperties;
    private final ProxyService proxyService;

    public void saveAllItemsData() {
        List<String> marketHashNames = MarketHashCaseNameHolder.getMarketHashNames();
        for (String marketHashName : marketHashNames) {
            executorService.submit(() -> {
                try {
                    saveSingleItemData(marketHashName);
                } catch (Exception e) {
                    logger.error("Error during fetching or saving for marketHashName: {}", marketHashName, e);
                }
            });
        }
    }

    public Item saveSingleItemData(String marketHashName) {
        int attempts = itemProperties.getAttempts();
        int maxAttempts = itemProperties.getMaxAttempts();
        int waitTimeInMillis = itemProperties.getWaitTimeInMillis();

        while (attempts < maxAttempts) {
            try {
                URI uri = constructURI(marketHashName);
                logger.info("Fetching data from URL: {}", uri);

                ProxyResponse proxyResponse = proxyService.executeRequest(uri.toString());
                HttpResponse response = proxyResponse.getResponse();
                HttpHost proxy = proxyResponse.getProxy();
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity());

                logger.info("Response Status Code: {} using proxy: {}", statusCode, proxy);
                logger.info("Raw API Response: {}", responseBody);

                if (statusCode == 200) {
                    PriceOverviewResponse parsedResponse = objectMapper.readValue(responseBody, PriceOverviewResponse.class);
                    if (parsedResponse.isSuccess()) {
                        saveItem(parsedResponse, marketHashName);
                        return itemRepository.findItemByName(marketHashName);
                    } else {
                        logger.error("API response indicates failure for marketHashName: {}", marketHashName);
                    }
                } else {
                    logger.error("Received HTTP {} error from server for marketHashName: {} using proxy: {}", statusCode, marketHashName, proxy);
                    attempts++;
                }
            } catch (IOException e) {
                logger.error("Attempt {} failed for marketHashName: {}, error: {}", attempts, marketHashName, e.getMessage());
                attempts++;
            } catch (Exception e) {
                logger.error("An unexpected error occurred while fetching data for marketHashName: {}", marketHashName, e);
                return null;
            }

            try {
                Thread.sleep(waitTimeInMillis);
                waitTimeInMillis = Math.min(waitTimeInMillis * 2, 15000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                logger.error("Thread interrupted during wait", ie);
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
        String queryString = String.format("?%s=%s&%s=%s&appid=%d&market_hash_name=%s",
                COUNTRY, steamProperties.getCountry(), CURRENCY, steamProperties.getCurrency(), steamProperties.getAppId(), encodedMarketHashName);
        return new URI(steamProperties.getBaseUrl() + queryString);
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

        disableItem(marketHashName);
        return false;
    }

    private void disableItem(String marketHashName) {
        itemRepository.disableItem(marketHashName);
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

