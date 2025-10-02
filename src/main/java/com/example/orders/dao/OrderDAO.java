package com.example.orders.dao;

import com.example.orders.model.Order;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {
    private final Connection connection;

    public OrderDAO(Connection connection) {
        this.connection = connection;
    }

    public void create(Order order) throws SQLException {
        String sql = "INSERT INTO orders (product_id, customer_id, quantity, status_id) VALUES (?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, order.getProductId());
            statement.setLong(2, order.getCustomerId());
            statement.setInt(3, order.getQuantity());
            statement.setLong(4, order.getStatusId());

            int affectedRows = statement.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        order.setId(generatedKeys.getLong(1));
                    }
                }
            }
        }
    }

    public List<Order> findLastOrders(int limit) throws SQLException {
        String sql = """
            SELECT o.*, 
                   c.first_name || ' ' || c.last_name as customer_name,
                   p.description as product_description,
                   os.status_name,
                   (p.price * o.quantity) as total_amount
            FROM orders o
            JOIN customer c ON o.customer_id = c.id
            JOIN product p ON o.product_id = p.id
            JOIN order_status os ON o.status_id = os.id
            ORDER BY o.order_date DESC
            LIMIT ?
            """;

        List<Order> orders = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    orders.add(mapResultSetToOrder(resultSet));
                }
            }
        }
        return orders;
    }

    public void updateStatus(Long orderId, Long statusId) throws SQLException {
        String sql = "UPDATE orders SET status_id = ? WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, statusId);
            statement.setLong(2, orderId);
            statement.executeUpdate();
        }
    }

    public void delete(Long orderId) throws SQLException {
        String sql = "DELETE FROM orders WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);
            statement.executeUpdate();
        }
    }

    public void updateProductQuantityAfterOrder(Long productId, int quantity) throws SQLException {
        String sql = "UPDATE product SET quantity = quantity - ? WHERE id = ? AND quantity >= ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, quantity);
            statement.setLong(2, productId);
            statement.setInt(3, quantity);
            statement.executeUpdate();
        }
    }

    private Order mapResultSetToOrder(ResultSet resultSet) throws SQLException {
        Order order = new Order();
        order.setId(resultSet.getLong("id"));
        order.setProductId(resultSet.getLong("product_id"));
        order.setCustomerId(resultSet.getLong("customer_id"));
        order.setOrderDate(resultSet.getTimestamp("order_date").toLocalDateTime());
        order.setQuantity(resultSet.getInt("quantity"));
        order.setStatusId(resultSet.getLong("status_id"));
        order.setCustomerName(resultSet.getString("customer_name"));
        order.setProductDescription(resultSet.getString("product_description"));
        order.setStatusName(resultSet.getString("status_name"));
        order.setTotalAmount(resultSet.getDouble("total_amount"));
        return order;
    }
}