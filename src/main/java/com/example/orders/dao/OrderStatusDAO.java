package com.example.orders.dao;

import com.example.orders.model.OrderStatus;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderStatusDAO {
    private final Connection connection;

    public OrderStatusDAO(Connection connection) {
        this.connection = connection;
    }

    public OrderStatus findById(Long id) throws SQLException {
        String sql = "SELECT * FROM order_status WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToOrderStatus(resultSet);
                }
            }
        }
        return null;
    }

    public OrderStatus findByName(String statusName) throws SQLException {
        String sql = "SELECT * FROM order_status WHERE status_name = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, statusName);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToOrderStatus(resultSet);
                }
            }
        }
        return null;
    }

    public List<OrderStatus> findAll() throws SQLException {
        String sql = "SELECT * FROM order_status ORDER BY id";
        List<OrderStatus> statuses = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                statuses.add(mapResultSetToOrderStatus(resultSet));
            }
        }
        return statuses;
    }

    private OrderStatus mapResultSetToOrderStatus(ResultSet resultSet) throws SQLException {
        OrderStatus status = new OrderStatus();
        status.setId(resultSet.getLong("id"));
        status.setStatusName(resultSet.getString("status_name"));
        return status;
    }
}