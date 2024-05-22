package com.steampromo.steamz.items.repository;

import com.steampromo.steamz.items.domain.item.Item;

import java.util.List;

public interface CustomItemRepository {
    void saveItemWithJdbc(Item item);

    List<Item> findAllItemsWithJdbc();
}
