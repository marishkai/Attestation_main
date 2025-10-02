package com.example.orders.model;

public class OrderStatus {
    private Long id;
    private String statusName;

    public OrderStatus() {}

    public OrderStatus(Long id, String statusName) {
        this.id = id;
        this.statusName = statusName;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }

    @Override
    public String toString() {
        return String.format("OrderStatus{id=%d, statusName='%s'}", id, statusName);
    }
}