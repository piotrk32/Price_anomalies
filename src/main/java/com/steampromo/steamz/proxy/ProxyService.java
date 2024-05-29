package com.steampromo.steamz.proxy;

import lombok.RequiredArgsConstructor;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;


@Service
@RequiredArgsConstructor
public class ProxyService {

    private static final Logger logger = LoggerFactory.getLogger(ProxyService.class);

    private final List<HttpHost> proxies;

    private AtomicInteger currentProxyIndex = new AtomicInteger(0);
    private final Random random;

    public ProxyService() {
        this.proxies = new ArrayList<>();
        this.random = new Random();



        proxies.add(new HttpHost("129.226.193.16", 3128));
        proxies.add(new HttpHost("154.127.240.115", 64001));
        proxies.add(new HttpHost("220.233.27.127", 80));
        proxies.add(new HttpHost("206.1.65.12", 3128));
        proxies.add(new HttpHost("185.255.45.241", 8080));
        proxies.add(new HttpHost("165.16.59.225", 8080));
        proxies.add(new HttpHost("203.205.9.105", 8080));



    }

    public ProxyResponse executeRequest(String url) throws IOException {
        int attempts = 0;
        while (attempts < proxies.size()) {
            HttpHost proxy = getNextProxy();
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpGet request = new HttpGet(url);
                RequestConfig config = RequestConfig.custom()
                        .setProxy(proxy)
                        .setConnectTimeout(3000) // Set timeout to 3 seconds
                        .setSocketTimeout(3000) // Set socket timeout to 3 seconds
                        .build();
                request.setConfig(config);

                CloseableHttpResponse response = httpClient.execute(request);
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode >= 200 && statusCode < 300) { // Check for HTTP OK status codes
                    return new ProxyResponse(response, proxy);
                } else {
                    logger.error("HTTP Error: {} for proxy {}", statusCode, proxy);
                    response.close(); // Close the response to avoid resource leak
                }
            } catch (IOException e) {
                logger.error("Attempt {} failed using proxy {}: {}", attempts + 1, proxy, e.getMessage());
            }
            attempts++;
            if (attempts >= proxies.size()) {
                logger.error("All proxies exhausted for request. Final attempt failed.");
                throw new IOException("Failed to execute request using any configured proxy.");
            }
        }
        throw new IllegalStateException("Proxy rotation logic failed unexpectedly.");
    }

    private HttpHost getNextProxy() {
        int index = currentProxyIndex.getAndIncrement();
        if (index >= proxies.size()) {
            currentProxyIndex.set(0);
            index = 0;
        }
        return proxies.get(index);
    }
}


