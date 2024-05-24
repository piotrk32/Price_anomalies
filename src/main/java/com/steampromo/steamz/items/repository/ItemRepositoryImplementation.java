package com.steampromo.steamz.items.repository;

import com.steampromo.steamz.items.domain.Item;
import com.steampromo.steamz.items.domain.PriceOverviewResponse;
import com.steampromo.steamz.items.domain.enums.CategoryEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class ItemRepositoryImplementation implements ItemRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Item findItemByName(String itemName) {
        String sql = "SELECT * FROM items WHERE item_name = ?";
        List<Item> items = jdbcTemplate.query(sql, new Object[]{itemName}, this::mapRowToItem);
        if (items.isEmpty()) {
            return null;
        }
        return items.get(0);
    }

    @Override
    public void updateItem(Item item, PriceOverviewResponse response) {
        String sql = "UPDATE items SET lowest_price = ?, median_price = ? WHERE item_name = ?";
        jdbcTemplate.update(sql, parsePrice(response.getLowestPrice()), parsePrice(response.getMedianPrice()), item.getItemName());
    }

    @Override
    public void insertNewItem(Item item) {
        String sql = "INSERT INTO items (id, item_name, lowest_price, median_price, category) VALUES (?, ?, ?, ?, CAST(? AS category_enum))";
        jdbcTemplate.update(sql, item.getId(), item.getItemName(), item.getLowestPrice(), item.getMedianPrice(), item.getCategory().name());
    }

    @Override
    public List<Item> findByCategory(String category) {
        String sql = "SELECT * FROM items WHERE category = CAST(? AS category_enum)";
        return jdbcTemplate.query(sql, new Object[]{category}, this::mapRowToItem);
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
    private double parsePrice(String price) {
        return Double.parseDouble(price.replaceAll("[^\\d,\\.]", "").replace(",", "."));
    }


}
