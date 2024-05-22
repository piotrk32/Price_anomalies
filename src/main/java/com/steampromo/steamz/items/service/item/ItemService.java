package com.steampromo.steamz.items.service.item;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.steampromo.steamz.items.domain.alert.Alert;
import com.steampromo.steamz.items.domain.item.Item;
import com.steampromo.steamz.items.domain.item.MarketHashCaseNameHolder;
import com.steampromo.steamz.items.domain.item.PriceOverviewResponse;
import com.steampromo.steamz.items.domain.item.enums.CategoryEnum;
import com.steampromo.steamz.items.repository.CustomItemRepository;
import com.steampromo.steamz.items.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final JavaMailSender mailSender;
    private final JdbcTemplate jdbcTemplate;
    private final CustomItemRepository customItemRepository;
    private static final Logger logger = LoggerFactory.getLogger(ItemService.class);
    private  RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();


    private static final int MAX_RETRIES = 3;



    public void checkPriceAnomaliesForCases() {
        List<Item> items = itemRepository.findByCategory(CategoryEnum.CASE.name());

        for (Item item : items) {
            String url = "https://steamcommunity.com/market/priceoverview/?country=NL&currency=3&appid=" + item.getId() + "&market_hash_name=" + item.getItemName();
            RestTemplate restTemplate = new RestTemplate();
            PriceOverviewResponse response = restTemplate.getForObject(url, PriceOverviewResponse.class);

            if (response != null && response.isSuccess()) {
                double latestPrice = parsePrice(response.getLowestPrice());
                double medianPrice = parsePrice(response.getMedianPrice());

                if (isAnomaly(latestPrice, medianPrice)) {
                    createAlert(item, latestPrice, medianPrice);
                    sendAlertEmail(item, latestPrice, medianPrice);
                }

                item.setLowestPrice(latestPrice);
                item.setMedianPrice(medianPrice);
                itemRepository.save(item);
            }
        }
    }

    public void fetchAndSaveSingleItem(String marketHashName) {
        try {
            logger.info("Raw marketHashName: {}", marketHashName);
            String encodedMarketHashName = URLEncoder.encode(marketHashName, "UTF-8");
            logger.info("Encoded marketHashName: {}", encodedMarketHashName);

            String urlString = "https://steamcommunity.com/market/priceoverview/?country=PL&currency=6&appid=730&market_hash_name=" + encodedMarketHashName;
            URI uri = new URI(urlString);
            RestTemplate restTemplate = new RestTemplate();

            logger.info("Fetching data from URL: {}", uri);

            ResponseEntity<String> responseEntity = restTemplate.getForEntity(uri, String.class);
            String responseBody = responseEntity.getBody();
            HttpHeaders headers = responseEntity.getHeaders();

            logger.debug("Response body: {}", responseBody);
            logger.debug("Response headers: {}", headers);

            if (responseBody != null && responseEntity.getStatusCode().is2xxSuccessful()) {
                if (headers.getContentType() != null && headers.getContentType().toString().contains("application/json")) {
                    try {
                        PriceOverviewResponse response = objectMapper.readValue(responseBody, PriceOverviewResponse.class);

                        if (response.isSuccess()) {
                            Item item = new Item();
                            item.setId(UUID.randomUUID());
                            item.setItemName(marketHashName);
                            item.setLowestPrice(parsePrice(response.getLowestPrice()));
                            item.setMedianPrice(parsePrice(response.getMedianPrice()));
                            item.setCategory(CategoryEnum.CASE);

                            customItemRepository.saveItemWithJdbc(item);  // Use the custom repository for JDBC operations
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
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            logger.error("Error constructing URL for marketHashName: {}", marketHashName, e);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("HTTP error occurred while fetching data for marketHashName: {}. Response: {}", marketHashName, e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            logger.error("An unexpected error occurred while fetching data for marketHashName: {}", marketHashName, e);
        }
    }

    private double parsePrice(String price) {
        return Double.parseDouble(price.replaceAll("[^\\d,\\.]", "").replace(",", "."));
    }



    private boolean isAnomaly(double latestPrice, double medianPrice) {
        double priceGap = Math.abs(latestPrice - medianPrice);
        return (priceGap / medianPrice) > 0.01;
    }

    private void createAlert(Item item, double latestPrice, double medianPrice) {
        Alert alert = new Alert();
        alert.setItem(item);
        alert.setDate(LocalDateTime.now());
        alert.setPriceGap((int) ((latestPrice - medianPrice) * 100));
        item.getAlerts().add(alert);
    }

    private void sendAlertEmail(Item item, double latestPrice, double medianPrice) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo("piotrkepisty@example.com");
        message.setSubject("Price Anomaly Detected");
        message.setText("An anomaly was detected for item " + item.getItemName() + ". Latest price: " + latestPrice + ", Median price: " + medianPrice);
        mailSender.send(message);
    }

    public void save(Item item) {
        String sql = "INSERT INTO items (id, item_name, lowest_price, median_price, category) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, item.getId(), item.getItemName(), item.getLowestPrice(), item.getMedianPrice(), item.getCategory().name());
    }

    public List<Item> findAll() {
        String sql = "SELECT * FROM items";
        return jdbcTemplate.query(sql, this::mapRowToItem);
    }

    private Item mapRowToItem(ResultSet rs, int rowNum) throws SQLException {
        Item item = new Item();
        item.setId(UUID.fromString(rs.getString("id")));
        item.setItemName(rs.getString("item_name"));
        item.setLowestPrice(rs.getDouble("lowest_price"));
        item.setMedianPrice(rs.getDouble("median_price"));
        item.setCategory(CategoryEnum.valueOf(rs.getString("category")));
        return item;
    }
}
