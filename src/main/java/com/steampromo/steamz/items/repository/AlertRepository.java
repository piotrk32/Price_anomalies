package com.steampromo.steamz.items.repository;

import com.steampromo.steamz.items.domain.alert.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AlertRepository extends JpaRepository<Alert, UUID> {
}
