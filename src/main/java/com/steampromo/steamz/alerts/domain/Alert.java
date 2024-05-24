package com.steampromo.steamz.alerts.domain;


import com.steampromo.steamz.items.domain.Item;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "alerts")
@Setter
@Getter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Alert {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "date", nullable = false)
    private LocalDateTime date;

    @Column(name = "price_gap", nullable = false)
    private int priceGap;
}