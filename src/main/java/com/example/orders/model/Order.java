package com.example.orders.model;

import java.time.LocalDateTime;

public class Order {
    private Long id;
    private Long productId;
    private Long customerId;
    private LocalDateTime orderDate;
    private Integer quantity;
    private Long statusId;

    // Для JOIN запросов
    private String customerName;
    private String productDescription;
    private String statusName;
    private Double totalAmount;

    public Order() {}

    public Order(Long productId, Long customerId, Integer quantity, Long statusId) {
        this.productId = productId;
        this.customerId = customerId;
        this.quantity = quantity;
        this.statusId = statusId;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Long getStatusId() { return statusId; }
    public void setStatusId(Long statusId) { this.statusId = statusId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getProductDescription() { return productDescription; }
    public void setProductDescription(String productDescription) { this.productDescription = productDescription; }
    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }
    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    @Override
    public String toString() {
        return String.format("Order{id=%d, customer='%s', product='%s', quantity=%d, status='%s', total=%.2f}",
                id, customerName, productDescription, quantity, statusName, totalAmount);
    }
}