package com.steampromo.steamz.alerts.repository;

import com.steampromo.steamz.alerts.domain.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AlertRepository {

    void save(Alert alert);

}
