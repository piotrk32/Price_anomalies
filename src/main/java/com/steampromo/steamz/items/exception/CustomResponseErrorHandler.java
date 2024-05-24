package com.steampromo.steamz.items.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.DefaultResponseErrorHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class CustomResponseErrorHandler extends DefaultResponseErrorHandler {

    private static final Logger logger = LoggerFactory.getLogger(CustomResponseErrorHandler.class);

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        String body = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
        HttpStatus statusCode = HttpStatus.resolve(response.getStatusCode().value());

        if (statusCode != null) {
            if (statusCode.is4xxClientError()) {
                if (statusCode == HttpStatus.NOT_FOUND) {
                    logger.error("Error 404: Not Found - {}", body);
                } else {
                    logger.error("Client error: {} {}, Body: {}", statusCode, response.getStatusText(), body);
                }
            } else if (statusCode.is5xxServerError()) {
                logger.error("Server error: {} {}, Body: {}", statusCode, response.getStatusText(), body);
            }
        } else {
            logger.error("Unknown status code: {}, Body: {}", response.getStatusCode(), body);
        }
    }

}
