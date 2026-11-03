package com.tradeflow.tradeflow_product_service.dto;

import java.math.BigDecimal;

public class ProductResponse{
    Long id;
    String name;
    BigDecimal price;
    Integer stockQuantity;
    String category;

public Long getId() {return id;}

public void setId(Long id) {
        this.id = id;
    }

public BigDecimal getPrice() {
    return price;
}

public void setPrice(BigDecimal price) {
    this.price = price;
}

public String getName() {
    return name;
}

public void setName(String name) {
    this.name = name;
}

public Integer getStockQuantity() {
    return stockQuantity;
}

public void setStockQuantity(Integer stockQuantity) {
    this.stockQuantity = stockQuantity;
}

public String getCategory() {
    return category;
}

public void setCategory(String category) {
    this.category = category;
}
}