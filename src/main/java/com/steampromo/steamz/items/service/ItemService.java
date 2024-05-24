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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final JavaMailSender mailSender;
    private final JdbcTemplate jdbcTemplate;
    private static final Logger logger = LoggerFactory.getLogger(ItemService.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void fetchAndSaveAllItems() {
        List<String> marketHashNames = MarketHashCaseNameHolder.getMarketHashNames();

        for (String marketHashName : marketHashNames) {
            try {
                fetchAndSaveSingleItem(marketHashName);
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Interrupted while waiting between API calls", e);
                break;
            } catch (Exception e) {
                logger.error("Error during fetching or saving for marketHashName: {}", marketHashName, e);
            }
        }
    }

    public void fetchAndSaveSingleItem(String marketHashName) {
        int attempts = 0;
        int maxAttempts = 5;
        long waitTimeInMillis = 1000;

        while (attempts < maxAttempts) {
            try {
                URI uri = constructURI(marketHashName);
                RestTemplate restTemplate = new RestTemplate();
                logger.info("Fetching data from URL: {}", uri);
                ResponseEntity<String> responseEntity = restTemplate.getForEntity(uri, String.class);
                processResponse(responseEntity, marketHashName);
                break;
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                    logger.error("Too many requests, retrying after {} ms", waitTimeInMillis);
                    try {
                        Thread.sleep(waitTimeInMillis);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        logger.error("Thread interrupted during wait", ie);
                    }
                    waitTimeInMillis *= 2;
                    attempts++;
                } else {
                    throw e;
                }
            } catch (Exception e) {
                logger.error("An unexpected error occurred while fetching data for marketHashName: {}", marketHashName, e);
                break;
            }
        }
    }

    public URI constructURI(String marketHashName) throws Exception {
        String decodedMarketHashName = URLDecoder.decode(marketHashName, StandardCharsets.UTF_8);
        String encodedMarketHashName = URLEncoder.encode(decodedMarketHashName, StandardCharsets.UTF_8);
        String baseUrl = "https://steamcommunity.com/market/priceoverview/";
        String queryString = String.format("?country=PL&currency=6&appid=730&market_hash_name=%s", encodedMarketHashName);
        return new URI(baseUrl + queryString);
    }

    private void processResponse(ResponseEntity<String> responseEntity, String marketHashName) {
        String responseBody = responseEntity.getBody();
        HttpHeaders headers = responseEntity.getHeaders();

        logger.debug("Response body: {}", responseBody);
        logger.debug("Response headers: {}", headers);

        if (responseBody != null && responseEntity.getStatusCode().is2xxSuccessful()) {
            if (headers.getContentType() != null && headers.getContentType().toString().contains("application/json")) {
                try {
                    PriceOverviewResponse response = objectMapper.readValue(responseBody, PriceOverviewResponse.class);

                    if (response.isSuccess()) {
                        saveItem(response, marketHashName);
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
        return Double.parseDouble(price.replaceAll("[^\\d,\\.]", "").replace(",", "."));
    }


    private void sendAlertEmail(Item item, double latestPrice, double medianPrice) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo("piotrkepisty@example.com");
        message.setSubject("Price Anomaly Detected");
        message.setText("An anomaly was detected for item " + item.getItemName() + ". Latest price: " + latestPrice + ", Median price: " + medianPrice);
        mailSender.send(message);
    }

}

