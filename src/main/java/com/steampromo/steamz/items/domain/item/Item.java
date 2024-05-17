package com.steampromo.steamz.items.domain.item;

import com.steampromo.steamz.items.domain.alert.Alert;
import com.steampromo.steamz.items.domain.item.enums.CategoryEnum;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "items")
@Setter
@Getter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Item {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Column(name = "lowest_price", nullable = false)
    private double lowestPrice;

    @Column(name = "median_price", nullable = false)
    private double medianPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private CategoryEnum category;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Alert> alerts;
}