package com.steampromo.steamz.items.service.item;

import com.steampromo.steamz.items.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private JavaMailSender mailSender;

    private boolean isAnomaly(double latestPrice, double medianPrice) {
        double priceGap = Math.abs(latestPrice - medianPrice);
        return (priceGap / medianPrice) > 0.1;
    }


}
