package com.example.orders;

import org.flywaydb.core.Flyway;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class DatabaseManager {
    private final String url;
    private final String username;
    private final String password;

    public DatabaseManager(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public Connection getConnection() throws Exception {
        return DriverManager.getConnection(url, username, password);
    }

    public void migrate() {
        System.out.println("🔄 Запуск миграций Flyway...");

        try {
            Flyway flyway = Flyway.configure()
                    .dataSource(url, username, password)
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .cleanDisabled(false) // Разрешаем очистку для разработки
                    .load();

            // Для разработки: очищаем и пересоздаем базу
            flyway.clean();
            flyway.migrate();

            System.out.println("✅ Миграции Flyway выполнены успешно");

        } catch (Exception e) {
            System.err.println("❌ Ошибка Flyway: " + e.getMessage());
            System.out.println("🔄 Попытка ручной миграции...");
            manualMigration();
        }
    }

    private void manualMigration() {
        try (Connection connection = getConnection();
             var statement = connection.createStatement()) {

            System.out.println("🔄 Выполнение миграций вручную...");

            // Читаем и выполняем файлы миграций
            String schema = readResourceFile("db/migration/V1__Create_schema.sql");
            statement.execute(schema);

            String testData = readResourceFile("db/migration/V2__Insert_test_data.sql");
            statement.execute(testData);

            System.out.println("✅ Ручные миграции выполнены успешно");

        } catch (Exception e) {
            System.err.println("❌ Ошибка ручной миграции: " + e.getMessage());
            throw new RuntimeException("Не удалось выполнить миграции", e);
        }
    }

    private String readResourceFile(String resourcePath) {
        try {
            var inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
            if (inputStream == null) {
                throw new RuntimeException("Файл не найден: " + resourcePath);
            }
            return new String(inputStream.readAllBytes(), "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("Ошибка чтения файла: " + resourcePath, e);
        }
    }

    public static DatabaseManager fromProperties() {
        try {
            Properties properties = new Properties();
            var inputStream = DatabaseManager.class.getClassLoader()
                    .getResourceAsStream("application.properties");

            if (inputStream == null) {
                return new DatabaseManager(
                        "jdbc:postgresql://localhost:5432/order_management",
                        "postgres",
                        "password"
                );
            }

            properties.load(inputStream);

            String url = properties.getProperty("db.url");
            String username = properties.getProperty("db.username");
            String password = properties.getProperty("db.password");

            return new DatabaseManager(
                    url != null ? url : "jdbc:postgresql://localhost:5432/order_management",
                    username != null ? username : "postgres",
                    password != null ? password : "password"
            );

        } catch (Exception e) {
            System.err.println("⚠️  Используются настройки по умолчанию: " + e.getMessage());
            return new DatabaseManager(
                    "jdbc:postgresql://localhost:5432/order_management",
                    "postgres",
                    "password"
            );
        }
    }
}