package com.steampromo.steamz.alerts.repository;

import com.steampromo.steamz.alerts.domain.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AlertRepository extends JpaRepository<Alert, UUID> {
}
