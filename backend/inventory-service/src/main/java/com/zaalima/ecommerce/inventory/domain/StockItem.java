package com.zaalima.ecommerce.inventory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "stock_items")
@Getter
@Setter
@NoArgsConstructor
public class StockItem {

    @Id
    private String productId;

    private int availableQuantity;

    @Version
    private Long version;

    public StockItem(String productId, int availableQuantity) {
        this.productId = productId;
        this.availableQuantity = availableQuantity;
    }
}
