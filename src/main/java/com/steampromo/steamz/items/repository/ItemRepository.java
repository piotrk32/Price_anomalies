package com.steampromo.steamz.items.repository;

import com.steampromo.steamz.items.domain.Item;
import com.steampromo.steamz.items.domain.PriceOverviewResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ItemRepository  {

    List<Item> findByCategory(@Param("category") String category);

    Item findItemByName(String itemName);
    void updateItem(Item item, PriceOverviewResponse response);
    void insertNewItem(Item item);

    void disableItem(String itemName);






}
