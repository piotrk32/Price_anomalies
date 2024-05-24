package com.steampromo.steamz.alerts.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AlertFacade {

    private final AlertService alertService;
    private static final Logger logger = LoggerFactory.getLogger(AlertFacade.class);

    public void checkForPriceAnomalies() {
        try {
            logger.info("Initiating check for price anomalies for cases.");
            alertService.checkPriceAnomaliesForCases();
        } catch (Exception e) {
            logger.error("Error during price anomaly check: {}", e.getMessage(), e);
        }
    }
}
