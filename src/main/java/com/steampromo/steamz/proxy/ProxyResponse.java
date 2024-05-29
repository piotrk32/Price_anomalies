package com.steampromo.steamz.proxy;


import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;

public class ProxyResponse {
    private final CloseableHttpResponse response;
    private final HttpHost proxy;

    public ProxyResponse(CloseableHttpResponse response, HttpHost proxy) {
        this.response = response;
        this.proxy = proxy;
    }

    public CloseableHttpResponse getResponse() {
        return response;
    }

    public HttpHost getProxy() {
        return proxy;
    }
}
