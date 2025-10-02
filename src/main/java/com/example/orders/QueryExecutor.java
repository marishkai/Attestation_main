package com.example.orders;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QueryExecutor {
    private final Connection connection;

    public QueryExecutor(Connection connection) {
        this.connection = connection;
    }

    public void executeTestQueries() {
        printHeader("📊 ВЫПОЛНЕНИЕ TEST-QUERIES.SQL");

        try {
            String[] queries = loadQueriesFromFile();

            for (int i = 0; i < queries.length; i++) {
                if (queries[i].trim().isEmpty()) continue;

                printSection("Запрос #" + (i + 1));
                System.out.println(queries[i]);

                if (queries[i].trim().toUpperCase().startsWith("SELECT")) {
                    executeSelectQuery(queries[i], i + 1);
                } else if (queries[i].trim().toUpperCase().startsWith("UPDATE")) {
                    executeUpdateQuery(queries[i], i + 1);
                } else if (queries[i].trim().toUpperCase().startsWith("DELETE")) {
                    executeDeleteQuery(queries[i], i + 1);
                } else if (queries[i].trim().toUpperCase().startsWith("INSERT")) {
                    executeUpdateQuery(queries[i], i + 1);
                }

                System.out.println();
            }

        } catch (Exception e) {
            System.err.println("❌ Ошибка выполнения тестовых запросов: " + e.getMessage());
        }
    }

    private String[] loadQueriesFromFile() {
        try {
            var inputStream = getClass().getClassLoader().getResourceAsStream("test-queries.sql");
            if (inputStream == null) {
                throw new RuntimeException("Файл test-queries.sql не найден");
            }

            String content = new String(inputStream.readAllBytes(), "UTF-8");
            // Разделяем запросы по точке с запятой и переносам строк
            return content.split(";\\s*\\n");

        } catch (Exception e) {
            throw new RuntimeException("Ошибка загрузки test-queries.sql: " + e.getMessage());
        }
    }

    private void executeSelectQuery(String query, int queryNumber) {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            // Вывод заголовков
            List<String> headers = new ArrayList<>();
            List<Integer> columnWidths = new ArrayList<>();

            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                headers.add(columnName);
                columnWidths.add(Math.max(columnName.length(), 15));
            }

            // Вывод таблицы
            printTable(headers, columnWidths);

            // Вывод данных
            List<List<String>> rows = new ArrayList<>();
            int rowCount = 0;

            while (resultSet.next() && rowCount < 100) { // Ограничиваем вывод 100 строками
                List<String> row = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    Object value = resultSet.getObject(i);
                    row.add(value != null ? value.toString() : "NULL");
                }
                rows.add(row);
                rowCount++;
            }

            for (List<String> row : rows) {
                printTableRow(row, columnWidths);
            }

            printSeparator(columnWidths);
            System.out.println("✅ Найдено строк: " + rowCount);

        } catch (SQLException e) {
            System.err.println("❌ Ошибка выполнения SELECT запроса #" + queryNumber + ": " + e.getMessage());
        }
    }

    private void executeUpdateQuery(String query, int queryNumber) {
        try (Statement statement = connection.createStatement()) {
            int affectedRows = statement.executeUpdate(query);
            System.out.println("✅ Запрос выполнен. Затронуто строк: " + affectedRows);

        } catch (SQLException e) {
            System.err.println("❌ Ошибка выполнения UPDATE запроса #" + queryNumber + ": " + e.getMessage());
        }
    }

    private void executeDeleteQuery(String query, int queryNumber) {
        try (Statement statement = connection.createStatement()) {
            int affectedRows = statement.executeUpdate(query);
            System.out.println("✅ Запрос выполнен. Удалено строк: " + affectedRows);

        } catch (SQLException e) {
            System.err.println("❌ Ошибка выполнения DELETE запроса #" + queryNumber + ": " + e.getMessage());
        }
    }

    private void printTable(List<String> headers, List<Integer> widths) {
        StringBuilder headerLine = new StringBuilder();
        StringBuilder separator = new StringBuilder();

        for (int i = 0; i < headers.size(); i++) {
            headerLine.append(String.format("%-" + widths.get(i) + "s", headers.get(i)));
            separator.append("─".repeat(widths.get(i)));
            if (i < headers.size() - 1) {
                headerLine.append(" │ ");
                separator.append("─┼─");
            }
        }

        System.out.println(headerLine);
        System.out.println(separator);
    }

    private void printTableRow(List<String> row, List<Integer> widths) {
        StringBuilder rowLine = new StringBuilder();
        for (int i = 0; i < row.size(); i++) {
            String value = row.get(i);
            if (value.length() > widths.get(i) - 3) {
                value = value.substring(0, widths.get(i) - 3) + "...";
            }
            rowLine.append(String.format("%-" + widths.get(i) + "s", value));
            if (i < row.size() - 1) {
                rowLine.append(" │ ");
            }
        }
        System.out.println(rowLine);
    }

    private void printSeparator(List<Integer> widths) {
        StringBuilder separator = new StringBuilder();
        for (int i = 0; i < widths.size(); i++) {
            separator.append("─".repeat(widths.get(i)));
            if (i < widths.size() - 1) {
                separator.append("─┼─");
            }
        }
        System.out.println(separator);
    }

    private void printHeader(String text) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("✨ " + text);
        System.out.println("=".repeat(80));
    }

    private void printSection(String text) {
        System.out.println("\n" + "─".repeat(60));
        System.out.println("📌 " + text);
        System.out.println("─".repeat(60));
    }
}