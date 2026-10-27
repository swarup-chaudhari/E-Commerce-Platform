package com.zaalima.ecommerce.order.repository;

import com.zaalima.ecommerce.order.domain.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
}
