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

        proxies.add(new HttpHost("3.11.218.78", 80));
        proxies.add(new HttpHost("3.12.178.169", 80));
        proxies.add(new HttpHost("3.19.97.90", 80));
        proxies.add(new HttpHost("3.23.115.89", 3128));
        proxies.add(new HttpHost("3.24.58.156", 3128));
        proxies.add(new HttpHost("3.24.178.81", 80));
        proxies.add(new HttpHost("3.35.217.104", 4000));
        proxies.add(new HttpHost("3.37.125.76", 3128));
        proxies.add(new HttpHost("3.68.124.231", 80));
        proxies.add(new HttpHost("3.73.120.104", 3128));
        proxies.add(new HttpHost("3.78.78.151", 3127));
        proxies.add(new HttpHost("3.82.74.254", 80));
        proxies.add(new HttpHost("3.93.71.36", 80));
        proxies.add(new HttpHost("3.111.188.36", 80));
        proxies.add(new HttpHost("3.122.84.99", 3128));
        proxies.add(new HttpHost("3.127.121.101", 80));
        proxies.add(new HttpHost("3.128.32.232", 80));
        proxies.add(new HttpHost("3.128.142.113", 80));




    }

    public CloseableHttpResponse executeRequest(String url) throws IOException {
        int attempts = 0;
        while (attempts < proxies.size()) {
            HttpHost proxy = getNextProxy();
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpGet request = new HttpGet(url);
                RequestConfig config = RequestConfig.custom()
                        .setProxy(proxy)
                        .setConnectTimeout(5000) // Set timeout to 5 seconds
                        .setSocketTimeout(5000) // Set socket timeout to 5 seconds
                        .build();
                request.setConfig(config);

                CloseableHttpResponse response = httpClient.execute(request);
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode >= 200 && statusCode < 300) { // Check for HTTP OK status codes
                    return response;
                } else {
                    logger.error("HTTP Error: {} for proxy {}", statusCode, proxy);
                    response.close(); // Close the response to avoid resource leak
                }
            } catch (IOException e) {
                logger.error("Attempt {} failed using proxy {}: {}", attempts + 1, proxy, e.getMessage());
                // Optionally, you could check here if the error is a connection timeout and decide to retry immediately
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


