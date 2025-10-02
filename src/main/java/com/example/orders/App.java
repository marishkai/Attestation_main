package com.example.orders;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class App {
    public static void main(String[] args) {
        printHeader("🚀 ВЫПОЛНЕНИЕ ТЕСТОВЫХ ЗАПРОСОВ С Flyway МИГРАЦИЯМИ");

        try {
            DatabaseManager dbManager = DatabaseManager.fromProperties();
            printSuccess("Подключение к PostgreSQL установлено");

            // Выполняем миграции Flyway
            printInfo("Выполнение миграций базы данных...");
            dbManager.migrate();

            // Выполняем тестовые запросы
            printInfo("Выполнение тестовых запросов из test-queries.sql...");
            executeTestQueries(dbManager);

        } catch (Exception e) {
            printError("Ошибка приложения: " + e.getMessage());
            e.printStackTrace();
        }

        printHeader("ВЫПОЛНЕНИЕ ЗАПРОСОВ ЗАВЕРШЕНО");
    }

    private static void executeTestQueries(DatabaseManager dbManager) {
        try (Connection connection = dbManager.getConnection()) {

            // Загружаем запросы из файла
            List<String> queries = loadQueriesFromFile();
            printSuccess("Загружено запросов: " + queries.size());

            int executedQueries = 0;
            for (int i = 0; i < queries.size(); i++) {
                String query = queries.get(i).trim();
                if (query.isEmpty()) continue;

                printQueryHeader(i + 1, getQueryDescription(i + 1));
                System.out.println("SQL: " + query);
                printSeparator();
                executeSingleQuery(connection, query, i + 1);
                executedQueries++;

                Thread.sleep(200);
            }

            printSuccess("Выполнено запросов: " + executedQueries + " из " + queries.size());

        } catch (Exception e) {
            printError("Ошибка выполнения тестовых запросов: " + e.getMessage());
        }
    }

    private static String getQueryDescription(int queryNumber) {
        switch (queryNumber) {
            case 1: return "Список всех заказов за последние 7 дней";
            case 2: return "Топ-3 самых популярных товаров";
            case 3: return "Покупатели с общей суммой заказов";
            case 4: return "Товары, которых осталось меньше 10 на складе";
            case 5: return "Ежемесячная статистика заказов";
            case 6: return "Обновление количества товара на складе";
            case 7: return "Обновление статуса заказа";
            case 8: return "Обновление цены товара";
            case 9: return "Удаление клиентов без заказов";
            case 10: return "Удаление старых отмененных заказов";
            default: return "Запрос #" + queryNumber;
        }
    }

    private static List<String> loadQueriesFromFile() {
        try {
            var inputStream = App.class.getClassLoader().getResourceAsStream("test-queries.sql");
            if (inputStream == null) {
                throw new RuntimeException("Файл test-queries.sql не найден");
            }
            return parseQueries(new String(inputStream.readAllBytes(), "UTF-8"));
        } catch (Exception e) {
            throw new RuntimeException("Ошибка загрузки test-queries.sql: " + e.getMessage());
        }
    }

    private static List<String> parseQueries(String content) {
        List<String> queries = new ArrayList<>();
        StringBuilder currentQuery = new StringBuilder();

        for (String line : content.split("\\r?\\n")) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("--")) continue;

            currentQuery.append(trimmedLine).append(" ");
            if (trimmedLine.endsWith(";")) {
                String query = currentQuery.toString().trim();
                if (!query.isEmpty()) queries.add(query);
                currentQuery = new StringBuilder();
            }
        }

        String lastQuery = currentQuery.toString().trim();
        if (!lastQuery.isEmpty()) {
            if (!lastQuery.endsWith(";")) lastQuery += ";";
            queries.add(lastQuery);
        }

        return queries;
    }

    private static void executeSingleQuery(Connection connection, String query, int queryNumber) {
        try (Statement statement = connection.createStatement()) {
            if (query.endsWith(";")) query = query.substring(0, query.length() - 1);

            if (query.toUpperCase().startsWith("SELECT")) {
                executeSelectQuery(statement, query, queryNumber);
            } else {
                executeUpdateQuery(statement, query, queryNumber);
            }
        } catch (Exception e) {
            printError("Ошибка запроса #" + queryNumber + ": " + e.getMessage());
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
                        // Обрезаем миллисекунды
                        String dateString = value.toString();
                        if (dateString.length() > 19) { // Обрезаем после секунд
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
}