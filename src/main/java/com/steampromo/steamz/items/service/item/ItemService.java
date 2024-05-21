package com.steampromo.steamz.items.service.item;

import com.steampromo.steamz.items.domain.alert.Alert;
import com.steampromo.steamz.items.domain.item.Item;
import com.steampromo.steamz.items.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private JavaMailSender mailSender;

    private boolean isAnomaly(double latestPrice, double medianPrice) {
        double priceGap = Math.abs(latestPrice - medianPrice);
        return (priceGap / medianPrice) > 0.1;
    }

    private void createAlert(Item item, double latestPrice, double medianPrice) {
        Alert alert = new Alert();
        alert.setItem(item);
        alert.setDate(LocalDateTime.now());
        alert.setPriceGap((int) ((latestPrice - medianPrice) * 100));
        item.getAlerts().add(alert);
    }


}
