package com.steampromo.steamz.items.repository;

import com.steampromo.steamz.items.domain.item.Item;
import com.steampromo.steamz.items.domain.item.enums.CategoryEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ItemRepository extends JpaRepository<Item, UUID> {
    List<Item> findByCategory(CategoryEnum category);
}
