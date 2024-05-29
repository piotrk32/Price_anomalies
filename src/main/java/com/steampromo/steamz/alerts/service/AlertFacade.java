package com.steampromo.steamz.alerts.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AlertFacade {

    private final AlertService alertService;

    public void checkForPriceAnomalies() {
        alertService.checkPriceAnomaliesForCases();
    }
}
