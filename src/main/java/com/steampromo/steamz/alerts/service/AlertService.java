package com.steampromo.steamz.alerts.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.steampromo.steamz.alerts.domain.Alert;
import com.steampromo.steamz.items.domain.Item;
import com.steampromo.steamz.items.domain.PriceOverviewResponse;
import com.steampromo.steamz.items.domain.enums.CategoryEnum;
import com.steampromo.steamz.alerts.repository.AlertRepository;
import com.steampromo.steamz.items.repository.ItemRepository;
import com.steampromo.steamz.items.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AlertService {

    @Value("${app.price-alert.threshold}")
    private double threshold;
    private final ItemRepository itemRepository;
    private final AlertRepository alertRepository;
    private final RestTemplate restTemplate;

    @Value("${sendgrid.api-key}")
    private String sendgridApiKey;

    @Value("${mail.emailSource}")
    private String emailSource;
    @Value("${mail.emailDestination}")
    private String emailDestination;
    private static final Logger logger = LoggerFactory.getLogger(ItemService.class);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);


    public void checkPriceAnomaliesForCases() {
        List<Item> items = itemRepository.findByCategory(CategoryEnum.CASE.name());
        int delay = 0;
        for (Item item : items) {
            scheduler.schedule(() -> processItemPriceCheck(item), delay, TimeUnit.SECONDS);
            delay += 3;
        }
    }

    private void processItemPriceCheck(Item item) {
        try {
            URI uri = constructURI(item.getItemName());
            logger.info("Sending request for URI: {}", uri);
            String rawResponse = restTemplate.getForObject(uri, String.class);
            if (rawResponse != null && !rawResponse.isEmpty()) {
                logger.info("Raw API Response: {}", rawResponse);
                PriceOverviewResponse response = new ObjectMapper().readValue(rawResponse, PriceOverviewResponse.class);
                if (response.isSuccess()) {
                    double latestPrice = parsePrice(response.getLowestPrice());
                    double storedPrice = item.getLowestPrice();
                    double priceDifference = Math.abs(storedPrice - latestPrice);
                    double thresholdAmount = storedPrice * threshold;

                    logger.info("Comparing latest price: {} zł to database stored lowest price: {} zł for item: {}. Threshold for alert: {}", latestPrice, storedPrice, item.getItemName(), thresholdAmount);

                    if (priceDifference >= thresholdAmount) {
                        Alert alert = createAlert(item, storedPrice, latestPrice);
                        sendAlertEmail(alert);
                        logger.info("Alert created and email sent for item: {} with price anomaly detected. Price gap: {}", item.getItemName(), priceDifference);
                    } else {
                        logger.info("No significant price anomaly detected for item: {} (latest: {} zł, stored: {} zł, difference: {} zł, threshold: {} zł)", item.getItemName(), latestPrice, storedPrice, priceDifference, thresholdAmount);
                    }
                }
            } else {
                logger.error("Empty or null response for item: {}", item.getItemName());
            }
        } catch (Exception e) {
            logger.error("Error processing price check for item: {}", item.getItemName(), e);
        }
    }



    private void sendAlertEmail(Alert alert) {
        Email from = new Email(emailSource);
        Email to = new Email(emailDestination);
        String subject = "Price Alert for " + alert.getItem().getItemName();
        Content content = new Content("text/plain", "A price drop has been detected for " + alert.getItem().getItemName() + ". Price gap: " + alert.getPriceGap() + "%.");
        Mail mail = new Mail(from, subject, to, content);

        SendGrid sg = new SendGrid(sendgridApiKey);
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            logger.info("Email sent status code: {}", response.getStatusCode());
        } catch (IOException ex) {
            logger.error("Error sending email for alert: {}", alert.getItem().getItemName(), ex);
        }
    }

    public URI constructURI(String marketHashName) throws Exception {
        String decodedMarketHashName = URLDecoder.decode(marketHashName, StandardCharsets.UTF_8);
        String encodedMarketHashName = URLEncoder.encode(decodedMarketHashName, StandardCharsets.UTF_8);
        String baseUrl = "https://steamcommunity.com/market/priceoverview/";
        String queryString = String.format("?country=NL&currency=6&appid=730&market_hash_name=%s", encodedMarketHashName);
        return new URI(baseUrl + queryString);
    }

    private Alert createAlert(Item item, double dbLowestPrice, double latestPrice) {
        Alert alert = new Alert();
        alert.setItem(item);
        alert.setDate(LocalDateTime.now());
        int priceGap = (int) ((dbLowestPrice - latestPrice) / dbLowestPrice * 100);
        alert.setPriceGap(priceGap);
        alertRepository.save(alert);
        logger.info("Alert saved for item: {} with price gap of {}%", item.getItemName(), priceGap);
        return alert;
    }

    private double parsePrice(String priceStr) {
        priceStr = priceStr.replaceAll("zł", "").trim().replace(",", ".");
        return Double.parseDouble(priceStr);
    }

}
