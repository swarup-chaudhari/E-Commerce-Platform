package com.zaalima.ecommerce.inventory.repository.jpa;

import com.zaalima.ecommerce.inventory.domain.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
}
