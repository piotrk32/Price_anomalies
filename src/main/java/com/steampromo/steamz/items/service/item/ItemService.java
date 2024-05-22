package com.steampromo.steamz.items.service.item;

import com.steampromo.steamz.items.domain.alert.Alert;
import com.steampromo.steamz.items.domain.item.Item;
import com.steampromo.steamz.items.domain.item.PriceOverviewResponse;
import com.steampromo.steamz.items.domain.item.enums.CategoryEnum;
import com.steampromo.steamz.items.repository.CustomItemRepository;
import com.steampromo.steamz.items.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final JavaMailSender mailSender;
    private final JdbcTemplate jdbcTemplate;
    private final CustomItemRepository customItemRepository;

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

    public void fetchAndSaveItem(String marketHashName) {
        try {
            String encodedMarketHashName = URLEncoder.encode(marketHashName, "UTF-8");
            String url = "https://steamcommunity.com/market/priceoverview/?country=PL&currency=6&appid=730&market_hash_name=" + encodedMarketHashName;
            RestTemplate restTemplate = new RestTemplate();
            PriceOverviewResponse response = restTemplate.getForObject(url, PriceOverviewResponse.class);

            if (response != null && response.isSuccess()) {
                Item item = new Item();
                item.setId(UUID.randomUUID());
                item.setItemName(marketHashName);
                item.setLowestPrice(parsePrice(response.getLowestPrice()));
                item.setMedianPrice(parsePrice(response.getMedianPrice()));
                item.setCategory(CategoryEnum.CASE);

                customItemRepository.saveItemWithJdbc(item);  // Use the custom repository for JDBC operations
            }
        } catch (UnsupportedEncodingException e) {
            // Handle the encoding exception
            e.printStackTrace();
        }
    }

    private double parsePrice(String price) {
        // Remove all non-numeric characters (except for the decimal point)
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
