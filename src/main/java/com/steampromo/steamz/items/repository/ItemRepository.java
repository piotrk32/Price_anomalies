package com.steampromo.steamz.items.repository;

import com.steampromo.steamz.items.domain.item.Item;
import com.steampromo.steamz.items.domain.item.enums.CategoryEnum;
import org.flywaydb.core.internal.jdbc.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Repository
public interface ItemRepository extends JpaRepository<Item, UUID> {

    @Query(value = "SELECT * FROM items WHERE category = CAST(:category AS category_enum)", nativeQuery = true)
    List<Item> findByCategory(@Param("category") String category);


}
