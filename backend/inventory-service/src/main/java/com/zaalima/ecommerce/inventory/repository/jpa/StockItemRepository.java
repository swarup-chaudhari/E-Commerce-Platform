package com.zaalima.ecommerce.inventory.repository.jpa;

import com.zaalima.ecommerce.inventory.domain.StockItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Collection;
import java.util.List;

public interface StockItemRepository extends JpaRepository<StockItem, String> {

    /** Row locks taken in productId order, so concurrent multi-item orders cannot deadlock. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<StockItem> findByProductIdInOrderByProductId(Collection<String> productIds);
}
