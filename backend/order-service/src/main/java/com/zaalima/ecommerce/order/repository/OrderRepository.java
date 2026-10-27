package com.zaalima.ecommerce.order.repository;

import com.zaalima.ecommerce.order.domain.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<CustomerOrder, String> {
    List<CustomerOrder> findByCustomerIdOrderByCreatedAtDesc(String customerId);
}
