package com.steampromo.steamz.alerts.repository;

import com.steampromo.steamz.alerts.domain.Alert;
import com.steampromo.steamz.items.domain.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AlertRepositoryImplementation implements AlertRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void save(Alert alert) {
        String sql = "INSERT INTO alerts (id, item_id, date, price_gap) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, alert.getId(), alert.getItem().getId(), alert.getDate(), alert.getPriceGap());
    }

}