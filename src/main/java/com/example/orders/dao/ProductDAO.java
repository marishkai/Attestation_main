package com.example.orders.dao;

import com.example.orders.model.Product;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {
    private final Connection connection;

    public ProductDAO(Connection connection) {
        this.connection = connection;
    }

    public void create(Product product) throws SQLException {
        String sql = "INSERT INTO product (description, price, quantity, category) VALUES (?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, product.getDescription());
            statement.setBigDecimal(2, product.getPrice());
            statement.setInt(3, product.getQuantity());
            statement.setString(4, product.getCategory());

            int affectedRows = statement.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        product.setId(generatedKeys.getLong(1));
                    }
                }
            }
        }
    }

    public Product findById(Long id) throws SQLException {
        String sql = "SELECT * FROM product WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToProduct(resultSet);
                }
            }
        }
        return null;
    }

    public List<Product> findAll() throws SQLException {
        String sql = "SELECT * FROM product ORDER BY id";
        List<Product> products = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                products.add(mapResultSetToProduct(resultSet));
            }
        }
        return products;
    }

    public void update(Product product) throws SQLException {
        String sql = "UPDATE product SET description = ?, price = ?, quantity = ?, category = ? WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, product.getDescription());
            statement.setBigDecimal(2, product.getPrice());
            statement.setInt(3, product.getQuantity());
            statement.setString(4, product.getCategory());
            statement.setLong(5, product.getId());

            statement.executeUpdate();
        }
    }

    public void delete(Long id) throws SQLException {
        String sql = "DELETE FROM product WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }

    public void updatePrice(Long productId, BigDecimal newPrice) throws SQLException {
        String sql = "UPDATE product SET price = ? WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, newPrice);
            statement.setLong(2, productId);
            statement.executeUpdate();
        }
    }

    public void updateQuantity(Long productId, Integer newQuantity) throws SQLException {
        String sql = "UPDATE product SET quantity = ? WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, newQuantity);
            statement.setLong(2, productId);
            statement.executeUpdate();
        }
    }

    public List<Product> findTopProductsByQuantity(int limit) throws SQLException {
        String sql = "SELECT * FROM product ORDER BY quantity DESC LIMIT ?";
        List<Product> products = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    products.add(mapResultSetToProduct(resultSet));
                }
            }
        }
        return products;
    }

    private Product mapResultSetToProduct(ResultSet resultSet) throws SQLException {
        Product product = new Product();
        product.setId(resultSet.getLong("id"));
        product.setDescription(resultSet.getString("description"));
        product.setPrice(resultSet.getBigDecimal("price"));
        product.setQuantity(resultSet.getInt("quantity"));
        product.setCategory(resultSet.getString("category"));
        return product;
    }
}