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

        //Deutsche wurst
        proxies.add(new HttpHost("85.214.158.184", 18123));
        proxies.add(new HttpHost("91.200.102.194", 80));
        proxies.add(new HttpHost("116.203.49.36", 80));
        proxies.add(new HttpHost("46.101.115.59", 80));

    }

    public CloseableHttpResponse executeRequest(String url) throws IOException {
        int retryCount = 0;
        int maxRetries = proxies.size();

        while (retryCount < maxRetries) {
            HttpHost proxy = getNextProxy();
            CloseableHttpClient httpClient = HttpClients.createDefault();
            RequestConfig config = RequestConfig.custom().setProxy(proxy).build();
            HttpGet request = new HttpGet(url);
            request.setConfig(config);

            try {
                return httpClient.execute(request);
            } catch (IOException e) {
                httpClient.close();
                logger.error("Attempt {} failed using proxy {}: {}", retryCount + 1, proxy, e.getMessage());
                retryCount++;

                if (retryCount == maxRetries) {
                    logger.error("All proxies exhausted for request. Final attempt failed.");
                    throw new IOException("Failed to execute request using any configured proxy.", e);
                }
            }
        }
        throw new IOException("Proxy rotation logic failed unexpectedly.");
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


