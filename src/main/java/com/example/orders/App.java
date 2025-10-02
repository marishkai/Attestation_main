package com.example.orders;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

public class App {
    private static int newProductId;
    private static int newCustomerId;
    private static int newOrderId;

    public static void main(String[] args) {
        printHeader("🚀 ЗАПУСК JAVA-ПРИЛОЖЕНИЯ ДЛЯ УПРАВЛЕНИЯ ЗАКАЗАМИ");

        try {
            DatabaseManager dbManager = DatabaseManager.fromProperties();
            printSuccess("Подключение к PostgreSQL установлено");

            // Выполняем миграции Flyway
            printInfo("Выполнение миграций базы данных...");
            dbManager.migrate();
            printSuccess("Миграции выполнены успешно");

            // ДЕМОНСТРАЦИЯ CRUD ОПЕРАЦИЙ
            demonstrateCRUDOperations(dbManager);

            // ВЫПОЛНЕНИЕ ТЕСТОВЫХ SQL-ЗАПРОСОВ
            executeTestSQLQueries(dbManager);

        } catch (Exception e) {
            printError("Ошибка приложения: " + e.getMessage());
            e.printStackTrace();
        }

        printHeader("ВЫПОЛНЕНИЕ ПРИЛОЖЕНИЯ ЗАВЕРШЕНО");
    }

    private static void demonstrateCRUDOperations(DatabaseManager dbManager) {
        Connection connection = null;
        try {
            connection = dbManager.getConnection();
            connection.setAutoCommit(false);

            printHeader("🎯 ДЕМОНСТРАЦИЯ CRUD ОПЕРАЦИЙ ЧЕРЕЗ JAVA");

            // Показываем исходное состояние
            printHeader("📊 ИСХОДНОЕ СОСТОЯНИЕ БАЗЫ ДАННЫХ");
            showInitialState(connection);

            printHeader("1. CREATE - ВСТАВКА НОВОГО ТОВАРА И ПОКУПАТЕЛЯ");

            // Вставка нового товара
            printInfo("Добавление нового товара...");
            newProductId = insertNewProduct(connection);
            printSuccess("✅ Добавлен товар ID: " + newProductId + " - Игровая консоль PlayStation 5");

            // Вставка нового покупателя
            printInfo("Добавление нового покупателя...");
            newCustomerId = insertNewCustomer(connection);
            printSuccess("✅ Добавлен покупатель ID: " + newCustomerId + " - Александр Новиков");

            printHeader("2. CREATE - СОЗДАНИЕ ЗАКАЗА ДЛЯ ПОКУПАТЕЛЯ");
            printInfo("Создание заказа для нового покупателя...");
            newOrderId = createNewOrder(connection, newCustomerId, newProductId);
            printSuccess("✅ Создан заказ ID: " + newOrderId + " - PlayStation 5 для Александра Новикова");

            // Показываем состояние после создания
            printHeader("📊 СОСТОЯНИЕ ПОСЛЕ СОЗДАНИЯ ДАННЫХ");
            showDataAfterCreation(connection);

            printHeader("3. READ - ЧТЕНИЕ ПОСЛЕДНИХ 5 ЗАКАЗОВ");
            readLast5Orders(connection);

            printHeader("4. UPDATE - ОБНОВЛЕНИЕ ДАННЫХ");

            // Обновление количества товара на складе
            printInfo("Обновление количества товара на складе...");
            showProductState(connection, 1, "ДО обновления");
            updateProductQuantity(connection);
            showProductState(connection, 1, "ПОСЛЕ обновления");

            // Обновление статуса заказа
            printInfo("Обновление статуса заказа...");
            showOrderState(connection, 1, "ДО обновления");
            updateOrderStatus(connection);
            showOrderState(connection, 1, "ПОСЛЕ обновления");

            // Обновление цены товара
            printInfo("Обновление цены товара...");
            showElectronicsPrices(connection, "ДО обновления");
            updateProductPrices(connection);
            showElectronicsPrices(connection, "ПОСЛЕ обновления");

            printHeader("5. DELETE - УДАЛЕНИЕ ТЕСТОВЫХ ЗАПИСЕЙ");

            // Создаем тестовые данные для удаления
            printInfo("Создание тестовых данных для демонстрации DELETE...");
            int testCustomerId = createTestCustomerWithoutOrders(connection);
            int testCancelledOrderId = createTestCancelledOrder(connection);

            // Показываем состояние до удаления
            printInfo("Состояние ДО удаления:");
            showDataBeforeDeletion(connection, testCustomerId, testCancelledOrderId);

            // Удаление тестовых данных
            deleteTestData(connection, newOrderId, newCustomerId, newProductId, testCustomerId, testCancelledOrderId);

            // Показываем состояние после удаления
            printInfo("Состояние ПОСЛЕ удаления:");
            showDataAfterDeletion(connection, testCustomerId, testCancelledOrderId);

            connection.commit();
            printSuccess("✅ Все CRUD операции выполнены успешно!");

        } catch (Exception e) {
            if (connection != null) {
                try {
                    connection.rollback();
                    printError("❌ Транзакция откатана из-за ошибки: " + e.getMessage());
                } catch (SQLException ex) {
                    printError("❌ Ошибка при откате транзакции: " + ex.getMessage());
                }
            }
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    printError("❌ Ошибка при закрытии соединения: " + e.getMessage());
                }
            }
        }
    }

    private static void showInitialState(Connection connection) throws SQLException {
        String countSql = "SELECT " +
                "(SELECT COUNT(*) FROM products) as products_count, " +
                "(SELECT COUNT(*) FROM customer) as customers_count, " +
                "(SELECT COUNT(*) FROM orders) as orders_count";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(countSql)) {
            if (rs.next()) {
                System.out.println("📦 Товаров: " + rs.getInt("products_count"));
                System.out.println("👥 Покупателей: " + rs.getInt("customers_count"));
                System.out.println("📋 Заказов: " + rs.getInt("orders_count"));
            }
        }
    }

    private static void showDataAfterCreation(Connection connection) throws SQLException {
        String sql = "SELECT " +
                "(SELECT description FROM products WHERE id = ?) as product_name, " +
                "(SELECT first_name || ' ' || last_name FROM customer WHERE id = ?) as customer_name, " +
                "(SELECT COUNT(*) FROM orders WHERE id = ?) as order_exists";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, newProductId);
            stmt.setInt(2, newCustomerId);
            stmt.setInt(3, newOrderId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println("🆕 Созданный товар: " + rs.getString("product_name"));
                    System.out.println("🆕 Созданный покупатель: " + rs.getString("customer_name"));
                    System.out.println("🆕 Заказ создан: " + (rs.getInt("order_exists") > 0 ? "Да" : "Нет"));
                }
            }
        }
    }

    private static void showDataBeforeDeletion(Connection connection, int testCustomerId, int testCancelledOrderId) throws SQLException {
        String sql = "SELECT " +
                "(SELECT COUNT(*) FROM products WHERE id = ?) as product_exists, " +
                "(SELECT COUNT(*) FROM customer WHERE id = ?) as customer_exists, " +
                "(SELECT COUNT(*) FROM orders WHERE id = ?) as order_exists, " +
                "(SELECT COUNT(*) FROM customer WHERE id = ?) as test_customer_exists, " +
                "(SELECT COUNT(*) FROM orders WHERE id = ?) as test_order_exists";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, newProductId);
            stmt.setInt(2, newCustomerId);
            stmt.setInt(3, newOrderId);
            stmt.setInt(4, testCustomerId);
            stmt.setInt(5, testCancelledOrderId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println("🗑️ Товар для удаления существует: " + (rs.getInt("product_exists") > 0 ? "Да" : "Нет"));
                    System.out.println("🗑️ Покупатель для удаления существует: " + (rs.getInt("customer_exists") > 0 ? "Да" : "Нет"));
                    System.out.println("🗑️ Заказ для удаления существует: " + (rs.getInt("order_exists") > 0 ? "Да" : "Нет"));
                    System.out.println("🗑️ Тестовый покупатель без заказов существует: " + (rs.getInt("test_customer_exists") > 0 ? "Да" : "Нет"));
                    System.out.println("🗑️ Тестовый отмененный заказ существует: " + (rs.getInt("test_order_exists") > 0 ? "Да" : "Нет"));
                }
            }
        }
    }

    private static void showDataAfterDeletion(Connection connection, int testCustomerId, int testCancelledOrderId) throws SQLException {
        String sql = "SELECT " +
                "(SELECT COUNT(*) FROM products WHERE id = ?) as product_exists, " +
                "(SELECT COUNT(*) FROM customer WHERE id = ?) as customer_exists, " +
                "(SELECT COUNT(*) FROM orders WHERE id = ?) as order_exists, " +
                "(SELECT COUNT(*) FROM customer WHERE id = ?) as test_customer_exists, " +
                "(SELECT COUNT(*) FROM orders WHERE id = ?) as test_order_exists";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, newProductId);
            stmt.setInt(2, newCustomerId);
            stmt.setInt(3, newOrderId);
            stmt.setInt(4, testCustomerId);
            stmt.setInt(5, testCancelledOrderId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println("🗑️ Товар после удаления: " + (rs.getInt("product_exists") > 0 ? "Остался" : "Удален"));
                    System.out.println("🗑️ Покупатель после удаления: " + (rs.getInt("customer_exists") > 0 ? "Остался" : "Удален"));
                    System.out.println("🗑️ Заказ после удаления: " + (rs.getInt("order_exists") > 0 ? "Остался" : "Удален"));
                    System.out.println("🗑️ Тестовый покупатель после удаления: " + (rs.getInt("test_customer_exists") > 0 ? "Остался" : "Удален"));
                    System.out.println("🗑️ Тестовый заказ после удаления: " + (rs.getInt("test_order_exists") > 0 ? "Остался" : "Удален"));
                }
            }
        }
    }

    // CRUD операции
    private static int insertNewProduct(Connection connection) throws SQLException {
        String getMaxIdSql = "SELECT COALESCE(MAX(id), 0) + 1 as next_id FROM products";
        int nextId;

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(getMaxIdSql)) {
            if (rs.next()) {
                nextId = rs.getInt("next_id");
            } else {
                throw new SQLException("Не удалось получить следующий ID для товара");
            }
        }

        String sql = "INSERT INTO products (id, description, price, quantity, category) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, nextId);
            stmt.setString(2, "Игровая консоль PlayStation 5");
            stmt.setDouble(3, 49999.99);
            stmt.setInt(4, 5);
            stmt.setString(5, "Электроника");

            stmt.executeUpdate();
            return nextId;
        }
    }

    private static int insertNewCustomer(Connection connection) throws SQLException {
        String getMaxIdSql = "SELECT COALESCE(MAX(id), 0) + 1 as next_id FROM customer";
        int nextId;

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(getMaxIdSql)) {
            if (rs.next()) {
                nextId = rs.getInt("next_id");
            } else {
                throw new SQLException("Не удалось получить следующий ID для покупателя");
            }
        }

        String sql = "INSERT INTO customer (id, first_name, last_name, phone, email) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, nextId);
            stmt.setString(2, "Александр");
            stmt.setString(3, "Новиков");
            stmt.setString(4, "+7-999-123-45-67");
            stmt.setString(5, "alex.novikov@mail.ru");

            stmt.executeUpdate();
            return nextId;
        }
    }

    private static int createTestCustomerWithoutOrders(Connection connection) throws SQLException {
        String getMaxIdSql = "SELECT COALESCE(MAX(id), 0) + 1 as next_id FROM customer";
        int nextId;

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(getMaxIdSql)) {
            if (rs.next()) {
                nextId = rs.getInt("next_id");
            } else {
                throw new SQLException("Не удалось получить следующий ID для покупателя");
            }
        }

        String sql = "INSERT INTO customer (id, first_name, last_name, phone, email) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, nextId);
            stmt.setString(2, "Тестовый");
            stmt.setString(3, "Клиент");
            stmt.setString(4, "+7-000-000-00-00");
            stmt.setString(5, "test.client@mail.ru");

            stmt.executeUpdate();
            return nextId;
        }
    }

    private static int createTestCancelledOrder(Connection connection) throws SQLException {
        String getMaxIdSql = "SELECT COALESCE(MAX(id), 0) + 1 as next_id FROM orders";
        int nextId;

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(getMaxIdSql)) {
            if (rs.next()) {
                nextId = rs.getInt("next_id");
            } else {
                throw new SQLException("Не удалось получить следующий ID для заказа");
            }
        }

        String sql = "INSERT INTO orders (id, product_id, customer_id, order_date, quantity, status_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, nextId);
            stmt.setInt(2, 1); // Произвольный товар
            stmt.setInt(3, 1); // Произвольный покупатель
            stmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now().minusMonths(2))); // Старая дата
            stmt.setInt(5, 1);
            stmt.setInt(6, 6); // Статус "Отменен"

            stmt.executeUpdate();
            return nextId;
        }
    }

    private static int createNewOrder(Connection connection, int customerId, int productId) throws SQLException {
        String getMaxIdSql = "SELECT COALESCE(MAX(id), 0) + 1 as next_id FROM orders";
        int nextId;

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(getMaxIdSql)) {
            if (rs.next()) {
                nextId = rs.getInt("next_id");
            } else {
                throw new SQLException("Не удалось получить следующий ID для заказа");
            }
        }

        String sql = "INSERT INTO orders (id, product_id, customer_id, order_date, quantity, status_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, nextId);
            stmt.setInt(2, productId);
            stmt.setInt(3, customerId);
            stmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(5, 1);
            stmt.setInt(6, 1); // Статус "Новый"

            stmt.executeUpdate();
            return nextId;
        }
    }

    private static void readLast5Orders(Connection connection) throws SQLException {
        String sql = "SELECT o.id, o.order_date, c.first_name, c.last_name, p.description, p.price, o.quantity, os.status_name " +
                "FROM orders o " +
                "JOIN customer c ON o.customer_id = c.id " +
                "JOIN products p ON o.product_id = p.id " +
                "JOIN order_status os ON o.status_id = os.id " +
                "ORDER BY o.order_date DESC " +
                "LIMIT 5";

        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            System.out.println("┌─────┬─────────────────────┬────────────┬─────────────┬──────────────────────────────┬────────────┬──────────┬──────────────┐");
            System.out.println("│ ID  │ Дата заказа        │ Имя        │ Фамилия     │ Товар                        │ Цена       │ Кол-во   │ Статус       │");
            System.out.println("├─────┼─────────────────────┼────────────┼─────────────┼──────────────────────────────┼────────────┼──────────┼──────────────┤");

            while (rs.next()) {
                System.out.printf("│ %-3d │ %-19s │ %-10s │ %-11s │ %-28s │ %-10.2f │ %-8d │ %-12s │%n",
                        rs.getInt("id"),
                        rs.getTimestamp("order_date").toString().substring(0, 19),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("description").length() > 28 ?
                                rs.getString("description").substring(0, 25) + "..." : rs.getString("description"),
                        rs.getDouble("price"),
                        rs.getInt("quantity"),
                        rs.getString("status_name")
                );
            }
            System.out.println("└─────┴─────────────────────┴────────────┴─────────────┴──────────────────────────────┴────────────┴──────────┴──────────────┘");
        }
    }

    private static void updateProductQuantity(Connection connection) throws SQLException {
        String sql = "UPDATE products SET quantity = quantity - 1 WHERE id = 1";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int updatedRows = stmt.executeUpdate();
            printSuccess("Обновлено количество товара ID=1 (уменьшено на 1)");
        }
    }

    private static void updateOrderStatus(Connection connection) throws SQLException {
        String sql = "UPDATE orders SET status_id = (SELECT id FROM order_status WHERE status_name = 'Завершен') WHERE id = 1";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int updatedRows = stmt.executeUpdate();
            printSuccess("Обновлен статус заказа ID=1 на 'Завершен'");
        }
    }

    private static void updateProductPrices(Connection connection) throws SQLException {
        String sql = "UPDATE products SET price = price * 1.1 WHERE category = 'Электроника'";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int updatedRows = stmt.executeUpdate();
            printSuccess("Обновлено цен для " + updatedRows + " товаров категории 'Электроника' (+10%)");
        }
    }

    private static void deleteTestData(Connection connection, int orderId, int customerId, int productId, int testCustomerId, int testCancelledOrderId) throws SQLException {
        // Удаление тестового заказа
        String deleteOrderSql = "DELETE FROM orders WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(deleteOrderSql)) {
            stmt.setInt(1, orderId);
            int deletedRows = stmt.executeUpdate();
            if (deletedRows > 0) {
                printSuccess("✅ Удален тестовый заказ ID: " + orderId);
            }
        }

        // Удаление тестового покупателя
        String deleteCustomerSql = "DELETE FROM customer WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(deleteCustomerSql)) {
            stmt.setInt(1, customerId);
            int deletedRows = stmt.executeUpdate();
            if (deletedRows > 0) {
                printSuccess("✅ Удален тестовый покупатель ID: " + customerId);
            }
        }

        // Удаление тестового товара
        String deleteProductSql = "DELETE FROM products WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(deleteProductSql)) {
            stmt.setInt(1, productId);
            int deletedRows = stmt.executeUpdate();
            if (deletedRows > 0) {
                printSuccess("✅ Удален тестовый товар ID: " + productId);
            }
        }

        // Удаление тестового покупателя без заказов
        try (PreparedStatement stmt = connection.prepareStatement(deleteCustomerSql)) {
            stmt.setInt(1, testCustomerId);
            int deletedRows = stmt.executeUpdate();
            if (deletedRows > 0) {
                printSuccess("✅ Удален тестовый покупатель без заказов ID: " + testCustomerId);
            }
        }

        // Удаление тестового отмененного заказа
        try (PreparedStatement stmt = connection.prepareStatement(deleteOrderSql)) {
            stmt.setInt(1, testCancelledOrderId);
            int deletedRows = stmt.executeUpdate();
            if (deletedRows > 0) {
                printSuccess("✅ Удален тестовый отмененный заказ ID: " + testCancelledOrderId);
            }
        }
    }

    private static void showProductState(Connection connection, int productId, String state) throws SQLException {
        String sql = "SELECT description, price, quantity FROM products WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println("📦 Товар ID " + productId + " (" + state + "): " +
                            rs.getString("description") + " | " +
                            "Цена: " + rs.getDouble("price") + " | " +
                            "Количество: " + rs.getInt("quantity"));
                }
            }
        }
    }

    private static void showOrderState(Connection connection, int orderId, String state) throws SQLException {
        String sql = "SELECT o.id, os.status_name, o.order_date, c.first_name, c.last_name, p.description " +
                "FROM orders o " +
                "JOIN order_status os ON o.status_id = os.id " +
                "JOIN customer c ON o.customer_id = c.id " +
                "JOIN products p ON o.product_id = p.id " +
                "WHERE o.id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println("📋 Заказ ID " + orderId + " (" + state + "): " +
                            rs.getString("first_name") + " " + rs.getString("last_name") + " | " +
                            rs.getString("description") + " | " +
                            "Статус: " + rs.getString("status_name") + " | " +
                            "Дата: " + rs.getTimestamp("order_date").toString().substring(0, 19));
                }
            }
        }
    }

    private static void showElectronicsPrices(Connection connection, String state) throws SQLException {
        String sql = "SELECT description, price FROM products WHERE category = 'Электроника' ORDER BY id";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            System.out.println("💰 Цены товаров категории 'Электроника' (" + state + "):");
            while (rs.next()) {
                System.out.println("   " + rs.getString("description") + " | Цена: " + rs.getDouble("price"));
            }
        }
    }

    // SQL запросы
    private static void executeTestSQLQueries(DatabaseManager dbManager) {
        printHeader("📊 ВЫПОЛНЕНИЕ ТЕСТОВЫХ SQL-ЗАПРОСОВ");

        try (Connection connection = dbManager.getConnection()) {

            List<TestQuery> queries = createTestQueries();
            printSuccess("Загружено запросов: " + queries.size());

            int executedQueries = 0;
            for (TestQuery query : queries) {
                printQueryHeader(query.number, query.description);
                System.out.println("SQL: " + query.sql);
                printSeparator();

                // Для UPDATE и DELETE запросов показываем состояние до и после
                if (query.sql.toUpperCase().startsWith("UPDATE") || query.sql.toUpperCase().startsWith("DELETE")) {
                    showStateBeforeQuery(connection, query);
                }

                executeSingleQuery(connection, query);

                // Для UPDATE и DELETE запросов показываем состояние после
                if (query.sql.toUpperCase().startsWith("UPDATE") || query.sql.toUpperCase().startsWith("DELETE")) {
                    showStateAfterQuery(connection, query);
                }

                executedQueries++;

                try { Thread.sleep(300); } catch (InterruptedException e) {}
            }

            printSuccess("Выполнено запросов: " + executedQueries + " из " + queries.size());

        } catch (Exception e) {
            printError("Ошибка выполнения тестовых запросов: " + e.getMessage());
        }
    }

    private static List<TestQuery> createTestQueries() {
        List<TestQuery> queries = new ArrayList<>();

        // 5 ЗАПРОСОВ НА ЧТЕНИЕ
        queries.add(new TestQuery(1, "Список всех заказов за последние 7 дней",
                "SELECT o.id AS \"Номер заказа\", o.order_date AS \"Дата заказа\", " +
                        "c.first_name AS \"Имя\", c.last_name AS \"Фамилия\", " +
                        "p.description AS \"Товар\", o.quantity AS \"Количество\", " +
                        "os.status_name AS \"Статус\" " +
                        "FROM orders o " +
                        "JOIN customer c ON o.customer_id = c.id " +
                        "JOIN products p ON o.product_id = p.id " +
                        "JOIN order_status os ON o.status_id = os.id " +
                        "WHERE o.order_date >= CURRENT_DATE - INTERVAL '7 days' " +
                        "ORDER BY o.order_date DESC"));

        queries.add(new TestQuery(2, "Топ-3 самых популярных товаров",
                "SELECT p.id, p.description, COUNT(o.id) as order_count " +
                        "FROM products p JOIN orders o ON p.id = o.product_id " +
                        "GROUP BY p.id, p.description ORDER BY order_count DESC LIMIT 3"));

        queries.add(new TestQuery(3, "Покупатели с общей суммой заказов",
                "SELECT c.id, c.first_name, c.last_name, " +
                        "SUM(p.price * o.quantity) as total_spent " +
                        "FROM customer c JOIN orders o ON c.id = o.customer_id " +
                        "JOIN products p ON o.product_id = p.id " +
                        "GROUP BY c.id, c.first_name, c.last_name " +
                        "ORDER BY total_spent DESC"));

        queries.add(new TestQuery(4, "Товары, которых осталось меньше 10 на складе",
                "SELECT p.description, p.quantity, p.category " +
                        "FROM products p WHERE p.quantity < 10 " +
                        "ORDER BY p.quantity ASC"));

        queries.add(new TestQuery(5, "Ежемесячная статистика заказов",
                "SELECT EXTRACT(YEAR FROM order_date) as year, " +
                        "EXTRACT(MONTH FROM order_date) as month, " +
                        "COUNT(*) as order_count, " +
                        "SUM(p.price * o.quantity) as total_amount " +
                        "FROM orders o JOIN products p ON o.product_id = p.id " +
                        "GROUP BY year, month ORDER BY year, month"));

        // 3 ЗАПРОСА НА ИЗМЕНЕНИЕ
        queries.add(new TestQuery(6, "Обновление количества товара на складе",
                "UPDATE products SET quantity = quantity - 1 WHERE id = 1"));

        queries.add(new TestQuery(7, "Обновление статуса заказа",
                "UPDATE orders SET status_id = (SELECT id FROM order_status WHERE status_name = 'Завершен') " +
                        "WHERE id = 1"));

        queries.add(new TestQuery(8, "Обновление цены товара",
                "UPDATE products SET price = price * 1.1 WHERE category = 'Электроника'"));

        // 2 ЗАПРОСА НА УДАЛЕНИЕ
        queries.add(new TestQuery(9, "Удаление конкретных заказов с ID 16 и 17",
                "DELETE FROM orders WHERE id IN (16, 17)"));

        queries.add(new TestQuery(10, "Удаление старых отмененных заказов",
                "DELETE FROM orders WHERE status_id = (SELECT id FROM order_status WHERE status_name = 'Отменен') " +
                        "AND order_date < '2025-09-20'"));

        return queries;
    }

    private static void showStateBeforeQuery(Connection connection, TestQuery query) throws SQLException {
        if (query.number == 6) {
            System.out.println("📊 СОСТОЯНИЕ ДО ОБНОВЛЕНИЯ:");
            showProductState(connection, 1, "ДО обновления");
        }
        else if (query.number == 7) {
            System.out.println("📊 СОСТОЯНИЕ ДО ОБНОВЛЕНИЯ:");
            showOrderState(connection, 1, "ДО обновления");
        }
        else if (query.number == 8) {
            System.out.println("📊 СОСТОЯНИЕ ДО ОБНОВЛЕНИЯ:");
            showElectronicsPrices(connection, "ДО обновления");
        }
        else if (query.number == 9) {
            System.out.println("📊 СОСТОЯНИЕ ДО УДАЛЕНИЯ:");
            showSpecificOrders(connection, "ДО удаления", Arrays.asList(16, 17));
        }
        else if (query.number == 10) {
            System.out.println("📊 СОСТОЯНИЕ ДО УДАЛЕНИЯ:");
            showOldCancelledOrders(connection, "ДО удаления");
        }
    }

    private static void showStateAfterQuery(Connection connection, TestQuery query) throws SQLException {
        if (query.number == 6) {
            System.out.println("📊 СОСТОЯНИЕ ПОСЛЕ ОБНОВЛЕНИЯ:");
            showProductState(connection, 1, "ПОСЛЕ обновления");
        }
        else if (query.number == 7) {
            System.out.println("📊 СОСТОЯНИЕ ПОСЛЕ ОБНОВЛЕНИЯ:");
            showOrderState(connection, 1, "ПОСЛЕ обновления");
        }
        else if (query.number == 8) {
            System.out.println("📊 СОСТОЯНИЕ ПОСЛЕ ОБНОВЛЕНИЯ:");
            showElectronicsPrices(connection, "ПОСЛЕ обновления");
        }
        else if (query.number == 9) {
            System.out.println("📊 СОСТОЯНИЕ ПОСЛЕ УДАЛЕНИЯ:");
            showSpecificOrders(connection, "ПОСЛЕ удаления", Arrays.asList(16, 17));
        }
        else if (query.number == 10) {
            System.out.println("📊 СОСТОЯНИЕ ПОСЛЕ УДАЛЕНИЯ:");
            showOldCancelledOrders(connection, "ПОСЛЕ удаления");
        }
    }

    private static void showSpecificOrders(Connection connection, String state, List<Integer> orderIds) throws SQLException {
        String ids = orderIds.stream().map(String::valueOf).collect(Collectors.joining(","));

        String sql = "SELECT COUNT(*) as count FROM orders WHERE id IN (" + ids + ")";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                System.out.println("📋 Заказов с ID в (" + ids + ") (" + state + "): " + rs.getInt("count"));
            }
        }

        String detailsSql = "SELECT o.id, o.quantity, c.first_name, c.last_name, p.description, os.status_name " +
                "FROM orders o " +
                "JOIN customer c ON o.customer_id = c.id " +
                "JOIN products p ON o.product_id = p.id " +
                "JOIN order_status os ON o.status_id = os.id " +
                "WHERE o.id IN (" + ids + ") " +
                "ORDER BY o.id";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(detailsSql)) {
            while (rs.next()) {
                System.out.println("   Заказ ID " + rs.getInt("id") + ": " +
                        rs.getString("first_name") + " " + rs.getString("last_name") + " | " +
                        rs.getString("description") + " | Кол-во: " + rs.getInt("quantity") +
                        " | Статус: " + rs.getString("status_name"));
            }
        }
    }

    private static void showOldCancelledOrders(Connection connection, String state) throws SQLException {
        String sql = "SELECT COUNT(*) as count " +
                "FROM orders " +
                "WHERE status_id = (SELECT id FROM order_status WHERE status_name = 'Отменен') " +
                "AND order_date < '2025-09-20'";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                System.out.println("🗑️ Старых ОТМЕНЕННЫХ заказов (до 20 сентября 2025) (" + state + "): " + rs.getInt("count"));
            }
        }

        String detailsSql = "SELECT o.id, o.order_date, c.first_name, c.last_name, p.description, os.status_name " +
                "FROM orders o " +
                "JOIN customer c ON o.customer_id = c.id " +
                "JOIN products p ON o.product_id = p.id " +
                "JOIN order_status os ON o.status_id = os.id " +
                "WHERE os.status_name = 'Отменен' " +
                "AND o.order_date < '2025-09-20'" +
                "ORDER BY o.order_date";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(detailsSql)) {
            while (rs.next()) {
                System.out.println("   Заказ ID " + rs.getInt("id") + ": " +
                        rs.getString("first_name") + " " + rs.getString("last_name") + " | " +
                        rs.getString("description") + " | Дата: " + rs.getTimestamp("order_date").toString().substring(0, 10) +
                        " | Статус: " + rs.getString("status_name"));
            }
        }
    }

    private static void executeSingleQuery(Connection connection, TestQuery query) {
        try (Statement statement = connection.createStatement()) {

            if (query.sql.toUpperCase().startsWith("SELECT")) {
                executeSelectQuery(statement, query.sql, query.number);
            } else {
                executeUpdateQuery(statement, query.sql, query.number);
            }

        } catch (Exception e) {
            printError("Ошибка запроса #" + query.number + ": " + e.getMessage());
        }
    }

    private static void executeSelectQuery(Statement statement, String query, int queryNumber) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery(query)) {
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            List<String> headers = new ArrayList<>();
            List<Integer> columnWidths = new ArrayList<>();
            List<List<String>> allRows = new ArrayList<>();
            int rowCount = 0;

            while (resultSet.next()) {
                List<String> row = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    Object value = resultSet.getObject(i);
                    String stringValue;

                    if (value instanceof java.sql.Timestamp) {
                        String dateString = value.toString();
                        if (dateString.length() > 19) {
                            dateString = dateString.substring(0, 19);
                        }
                        stringValue = dateString;
                    } else {
                        stringValue = value != null ? value.toString() : "NULL";
                    }

                    row.add(stringValue);
                }
                allRows.add(row);
                rowCount++;
            }

            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                int maxWidth = columnName.length();
                for (List<String> row : allRows) {
                    maxWidth = Math.max(maxWidth, row.get(i-1).length());
                }
                headers.add(columnName);
                columnWidths.add(Math.max(maxWidth, 8) + 2);
            }

            printTable(headers, allRows, columnWidths);
            printSuccess("Найдено строк: " + rowCount);
        }
    }

    private static void executeUpdateQuery(Statement statement, String query, int queryNumber) throws SQLException {
        int affectedRows = statement.executeUpdate(query);
        if (query.toUpperCase().startsWith("UPDATE")) {
            printSuccess("Обновлено строк: " + affectedRows);
        } else if (query.toUpperCase().startsWith("DELETE")) {
            printSuccess("Удалено строк: " + affectedRows);
        } else {
            printSuccess("Выполнено. Затронуто строк: " + affectedRows);
        }
    }

    // Вспомогательные методы для вывода
    private static void printTable(List<String> headers, List<List<String>> rows, List<Integer> widths) {
        printTableBorder("┌", "┬", "┐", widths);

        StringBuilder headerLine = new StringBuilder("│");
        for (int i = 0; i < headers.size(); i++) {
            headerLine.append(" ").append(padCenter(headers.get(i), widths.get(i))).append(" │");
        }
        System.out.println(headerLine);

        printTableBorder("├", "┼", "┤", widths);

        for (List<String> row : rows) {
            StringBuilder dataLine = new StringBuilder("│");
            for (int i = 0; i < row.size(); i++) {
                dataLine.append(" ").append(padRight(row.get(i), widths.get(i))).append(" │");
            }
            System.out.println(dataLine);
        }

        printTableBorder("└", "┴", "┘", widths);
    }

    private static void printTableBorder(String left, String middle, String right, List<Integer> widths) {
        StringBuilder border = new StringBuilder(left);
        for (int i = 0; i < widths.size(); i++) {
            border.append("─".repeat(widths.get(i) + 2));
            border.append(i < widths.size() - 1 ? middle : right);
        }
        System.out.println(border);
    }

    private static String padRight(String s, int length) {
        return s.length() > length ? s.substring(0, length - 3) + "..." :
                String.format("%-" + length + "s", s);
    }

    private static String padCenter(String s, int length) {
        if (s.length() >= length) return s.substring(0, length);
        int padding = length - s.length();
        return " ".repeat(padding / 2) + s + " ".repeat(padding - padding / 2);
    }

    private static void printHeader(String text) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("✨ " + text);
        System.out.println("=".repeat(80));
    }

    private static void printQueryHeader(int number, String description) {
        System.out.println("\n" + "─".repeat(80));
        System.out.println("📌 ЗАПРОС #" + number + ": " + description);
        System.out.println("─".repeat(80));
    }

    private static void printSeparator() {
        System.out.println("─".repeat(80));
    }

    private static void printSuccess(String text) {
        System.out.println("✅ " + text);
    }

    private static void printInfo(String text) {
        System.out.println("ℹ️ " + text);
    }

    private static void printError(String text) {
        System.out.println("❌ " + text);
    }

    private static class TestQuery {
        int number;
        String description;
        String sql;

        TestQuery(int number, String description, String sql) {
            this.number = number;
            this.description = description;
            this.sql = sql;
        }
    }
}