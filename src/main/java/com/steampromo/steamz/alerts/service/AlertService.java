package com.steampromo.steamz.alerts.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.steampromo.steamz.alerts.configuration.AlertProperties;
import com.steampromo.steamz.alerts.domain.Alert;
import com.steampromo.steamz.items.configuration.SteamProperties;
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
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final ItemRepository itemRepository;
    private final AlertRepository alertRepository;
    private final ProxyService proxyService;
    private final AlertProperties alertProperties;
    private final SteamProperties steamProperties;

    private static final Logger logger = LoggerFactory.getLogger(ItemService.class);
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    public void checkPriceAnomaliesForCases() {
        try {
            logger.info("Initiating check for price anomalies for cases.");
            List<Item> items = itemRepository.findByCategory(CategoryEnum.CASE.name());
            for (Item item : items) {
                executorService.submit(() -> processItemPriceCheck(item));
            }
        } catch (Exception e) {
            logger.error("Error during price anomaly check: {}", e.getMessage(), e);
        }
    }

    private void processItemPriceCheck(Item item) {
        int retries = alertProperties.getRetries();
        int maxRetries = alertProperties.getMaxRetries();
        long retryDelay = alertProperties.getRetryDelay();

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
                Thread.sleep(retryDelay);
                retryDelay *= 2;
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

            logger.info("Comparing latest price: {} zł to database stored lowest price: {} zł for item: {}. Price difference needed: {} zł", latestPrice, storedPrice, item.getItemName(), storedPrice * alertProperties.getThreshold());

            if (latestPrice < storedPrice && priceDifference >= storedPrice * alertProperties.getThreshold()) {
                Alert alert = createAlert(item, storedPrice, latestPrice);
//                sendAlertEmail(alert);
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
        Email from = new Email(alertProperties.getEmailSource());
        Email to = new Email(alertProperties.getEmailDestination());
        String subject = "Price Alert for " + decodedItemName;

        String itemUrl = String.format("https://steamcommunity.com/market/listings/%d/%s", steamProperties.getAppId(), URLEncoder.encode(decodedItemName, StandardCharsets.UTF_8).replace("+", "%20"));

        String emailContent = String.format("A price drop has been detected for %s. Price gap: %d%%. See more details: %s",
                decodedItemName, alert.getPriceGap(), itemUrl);

        Content content = new Content("text/plain", emailContent);
        Mail mail = new Mail(from, subject, to, content);

        SendGrid sg = new SendGrid(alertProperties.getApiKey());
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
        String baseUrl = steamProperties.getBaseUrl();
        String queryString = String.format("?country=%s&currency=%s&appid=%d&market_hash_name=%s",
                steamProperties.getCountry(), steamProperties.getCurrency(), steamProperties.getAppId(), encodedMarketHashName);
        return new URI(baseUrl + queryString);
    }

    private Alert createAlert(Item item, double dbLowestPrice, double latestPrice) {
        Alert alert = new Alert();
        alert.setId(UUID.randomUUID()); // Ensure the UUID is generated and set here
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
