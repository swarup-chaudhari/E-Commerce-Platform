package com.zaalima.ecommerce.order.repository;

import com.zaalima.ecommerce.order.domain.OrderEventRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderEventRepository extends JpaRepository<OrderEventRecord, Long> {
    List<OrderEventRecord> findByOrderIdOrderByIdAsc(String orderId);
}
