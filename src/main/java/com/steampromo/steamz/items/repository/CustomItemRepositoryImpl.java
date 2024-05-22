package com.steampromo.steamz.items.repository;

import com.steampromo.steamz.items.domain.item.Item;
import com.steampromo.steamz.items.domain.item.enums.CategoryEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Repository
public class CustomItemRepositoryImpl implements CustomItemRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void saveItemWithJdbc(Item item) {
        String sql = "INSERT INTO items (id, item_name, lowest_price, median_price, category) VALUES (?, ?, ?, ?, ?::category_enum)";
        jdbcTemplate.update(sql, item.getId(), item.getItemName(), item.getLowestPrice(), item.getMedianPrice(), item.getCategory().name());
    }

    @Override
    public List<Item> findAllItemsWithJdbc() {
        String sql = "SELECT * FROM items";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Item item = new Item();
            item.setId(UUID.fromString(rs.getString("id")));
            item.setItemName(rs.getString("item_name"));
            item.setLowestPrice(rs.getDouble("lowest_price"));
            item.setMedianPrice(rs.getDouble("median_price"));
            item.setCategory(CategoryEnum.valueOf(rs.getString("category")));
            return item;
        });
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