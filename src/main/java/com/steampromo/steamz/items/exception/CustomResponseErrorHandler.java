package com.steampromo.steamz.items.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class CustomResponseErrorHandler extends DefaultResponseErrorHandler {

    private static final Logger logger = LoggerFactory.getLogger(CustomResponseErrorHandler.class);

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        HttpStatus statusCode = HttpStatus.resolve(response.getStatusCode().value());
        if (statusCode != null) {
            if (statusCode.is5xxServerError()) {
                logger.error("Server error: " + statusCode + ", " + response.getStatusText());
            } else if (statusCode.is4xxClientError()) {
                if (statusCode == HttpStatus.NOT_FOUND) {
                    logger.error("Error 404: Not Found");
                } else {
                    logger.error("Client error: " + statusCode + ", " + response.getStatusText());
                }
            }
        } else {
            logger.error("Unknown status code: " + response.getStatusCode() + ", " + response.getStatusText());
        }
    }
}
