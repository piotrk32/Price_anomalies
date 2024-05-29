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
import com.steampromo.steamz.proxy.ProxyResponse;
import com.steampromo.steamz.proxy.ProxyService;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
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
import java.util.concurrent.ExecutorService;
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
    private final ProxyService proxyService;

    @Value("${sendgrid.api-key}")
    private String sendgridApiKey;

    @Value("${mail.emailSource}")
    private String emailSource;
    @Value("${mail.emailDestination}")
    private String emailDestination;

    @Value("${steam.api.country}")
    private String country;

    @Value("${steam.api.currency}")
    private int currency;

    @Value("${steam.api.appid}")
    private int appid;
    private static final Logger logger = LoggerFactory.getLogger(ItemService.class);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);


    public void checkPriceAnomaliesForCases() {
        List<Item> items = itemRepository.findByCategory(CategoryEnum.CASE.name());
        for (Item item : items) {
            executorService.submit(() -> processItemPriceCheck(item));
        }
    }

    private void processItemPriceCheck(Item item) {
        int retries = 0;
        int maxRetries = 3;  // Reduced max retries
        long retryDelay = 500;  // Reduced initial delay

        while (retries <= maxRetries) {
            try {
                URI uri = constructURI(item.getItemName());
                logger.info("Sending request for URI: {}", uri);

                ProxyResponse proxyResponse = proxyService.executeRequest(uri.toString());
                HttpResponse response = proxyResponse.getResponse();
                HttpHost proxy = proxyResponse.getProxy();
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity());

                logger.info("Response Status Code: {} using proxy: {}", statusCode, proxy);
                logger.info("Raw API Response: {}", responseBody);

                if (statusCode == 200) {
                    handleSuccessfulResponse(item, responseBody);
                    break;
                } else {
                    logger.error("Received HTTP {} error from server for item: {} using proxy: {}", statusCode, item.getItemName(), proxy);
                    retries++;
                }
            } catch (IOException e) {
                logger.error("Attempt {} failed for item: {}, error: {}", retries, item.getItemName(), e.getMessage());
                retries++;
            } catch (Exception e) {
                logger.error("Unexpected error during the price check for item: {}, Message: {}", item.getItemName(), e.getMessage(), e);
                break;
            }
            try {
                Thread.sleep(retryDelay);  // Apply delay before retrying
                retryDelay *= 2;  // Exponential backoff
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void handleSuccessfulResponse(Item item, String responseBody) throws IOException {
        PriceOverviewResponse parsedResponse = new ObjectMapper().readValue(responseBody, PriceOverviewResponse.class);
        if (parsedResponse.isSuccess()) {
            double latestPrice = parsePrice(parsedResponse.getLowestPrice());
            double storedPrice = item.getLowestPrice();
            double priceDifference = Math.abs(storedPrice - latestPrice);

            logger.info("Comparing latest price: {} zł to database stored lowest price: {} zł for item: {}. Price difference needed: {} zł", latestPrice, storedPrice, item.getItemName(), storedPrice * threshold);

            if (latestPrice < storedPrice && priceDifference >= storedPrice * threshold) {
                Alert alert = createAlert(item, storedPrice, latestPrice);
                sendAlertEmail(alert);
                logger.info("Alert created and email sent for item: {} with price anomaly detected. Price difference: {} zł", item.getItemName(), priceDifference);
            } else {
                logger.info("No significant price anomaly detected for item: {} (latest: {} zł, stored: {} zł, difference: {} zł)", item.getItemName(), latestPrice, storedPrice, priceDifference);
            }
        } else {
            logger.error("API response was not successful for item: {}", item.getItemName());
        }
    }

    private void sendAlertEmail(Alert alert) {
        String decodedItemName = decodeItemName(alert.getItem().getItemName());
        Email from = new Email(emailSource);
        Email to = new Email(emailDestination);
        String subject = "Price Alert for " + decodedItemName;

        String itemUrl = String.format("https://steamcommunity.com/market/listings/%d/%s", appid, URLEncoder.encode(decodedItemName, StandardCharsets.UTF_8).replace("+", "%20"));

        String emailContent = String.format("A price drop has been detected for %s. Price gap: %d%%. See more details: %s",
                decodedItemName, alert.getPriceGap(), itemUrl);

        Content content = new Content("text/plain", emailContent);
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
            logger.error("Error sending email for alert: {}", decodedItemName, ex);
        }
    }

    private String decodeItemName(String encodedName) {
        return URLDecoder.decode(encodedName, StandardCharsets.UTF_8);
    }

    public URI constructURI(String marketHashName) throws Exception {
        String decodedMarketHashName = URLDecoder.decode(marketHashName, StandardCharsets.UTF_8);
        String encodedMarketHashName = URLEncoder.encode(decodedMarketHashName, StandardCharsets.UTF_8);
        String baseUrl = "https://steamcommunity.com/market/priceoverview/";
        String queryString = String.format("?country=%s&currency=%d&appid=%d&market_hash_name=%s",
                country, currency, appid, encodedMarketHashName);
        return new URI(baseUrl + queryString);
    }

    private Alert createAlert(Item item, double dbLowestPrice, double latestPrice) {
        Alert alert = new Alert();
        alert.setItem(item);
        alert.setDate(LocalDateTime.now());

        int priceGap = Math.abs((int) ((dbLowestPrice - latestPrice) / dbLowestPrice * 100));
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
