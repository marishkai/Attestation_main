package com.example.orders.model;

import java.math.BigDecimal;

public class Product {
    private Long id;
    private String description;
    private BigDecimal price;
    private Integer quantity;
    private String category;

    public Product() {}

    public Product(String description, BigDecimal price, Integer quantity, String category) {
        this.description = description;
        this.price = price;
        this.quantity = quantity;
        this.category = category;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    @Override
    public String toString() {
        return String.format("Product{id=%d, description='%s', price=%.2f, quantity=%d, category='%s'}",
                id, description, price, quantity, category);
    }
}