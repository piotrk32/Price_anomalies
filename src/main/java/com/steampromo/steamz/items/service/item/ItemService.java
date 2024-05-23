package com.steampromo.steamz.items.service.item;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.steampromo.steamz.items.domain.alert.Alert;
import com.steampromo.steamz.items.domain.item.Item;
import com.steampromo.steamz.items.domain.item.MarketHashCaseNameHolder;
import com.steampromo.steamz.items.domain.item.PriceOverviewResponse;
import com.steampromo.steamz.items.domain.item.enums.CategoryEnum;
import com.steampromo.steamz.items.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
        try {
            URI uri = constructURI(marketHashName);
            RestTemplate restTemplate = new RestTemplate();
            logger.info("Fetching data from URL: {}", uri);
            ResponseEntity<String> responseEntity = restTemplate.getForEntity(uri, String.class);
            processResponse(responseEntity, marketHashName);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("HTTP error occurred while fetching data for marketHashName: {}. Response: {}", marketHashName, e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            logger.error("An unexpected error occurred while fetching data for marketHashName: {}", marketHashName, e);
        }
    }

    public URI constructURI(String marketHashName) throws Exception {
        String decodedMarketHashName = URLDecoder.decode(marketHashName, StandardCharsets.UTF_8.toString());
        String encodedMarketHashName = URLEncoder.encode(decodedMarketHashName, StandardCharsets.UTF_8.toString());
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
        // Check if the item already exists
        Item existingItem = findItemByName(marketHashName);
        if (existingItem != null) {
            // Update existing item
            updateItem(existingItem, response);
        } else {
            // Insert new item
            Item newItem = new Item();
            newItem.setId(UUID.randomUUID());  // If you use a UUID as ID
            newItem.setItemName(marketHashName);
            newItem.setLowestPrice(parsePrice(response.getLowestPrice()));
            newItem.setMedianPrice(parsePrice(response.getMedianPrice()));
            newItem.setCategory(CategoryEnum.CASE);
            insertNewItem(newItem);
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



    private Item findItemByName(String itemName) {
        String sql = "SELECT * FROM items WHERE item_name = ?";
        List<Item> items = jdbcTemplate.query(sql, new Object[]{itemName}, this::mapRowToItem);
        if (items.isEmpty()) {
            return null;
        }
        return items.get(0);
    }

    private void updateItem(Item item, PriceOverviewResponse response) {
        String sql = "UPDATE items SET lowest_price = ?, median_price = ? WHERE item_name = ?";
        jdbcTemplate.update(sql, parsePrice(response.getLowestPrice()), parsePrice(response.getMedianPrice()), item.getItemName());
    }

    private void insertNewItem(Item item) {
        String sql = "INSERT INTO items (id, item_name, lowest_price, median_price, category) VALUES (?, ?, ?, ?, CAST(? AS category_enum))";
        jdbcTemplate.update(sql, item.getId(), item.getItemName(), item.getLowestPrice(), item.getMedianPrice(), item.getCategory().name());
    }

}

