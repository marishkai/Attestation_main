-- 1. Список всех заказов за последние 7 дней с именем покупателя и описанием товара
SELECT o.id AS "Номер заказа", o.order_date AS "Дата заказа",
c.first_name AS "Имя", c.last_name AS "Фамилия",
p.description AS "Товар", o.quantity AS "Количество", os.status_name AS "Статус"
FROM orders o
JOIN customer c ON o.customer_id = c.id
JOIN products p ON o.product_id = p.id
JOIN order_status os ON o.status_id = os.id
WHERE o.order_date >= CURRENT_DATE - INTERVAL '7 days'
ORDER BY o.order_date DESC;

-- 2. Топ-3 самых популярных товаров (по количеству заказов)
SELECT p.id, p.description, COUNT(o.id) as order_count
FROM products p
JOIN orders o ON p.id = o.product_id
GROUP BY p.id, p.description
ORDER BY order_count DESC
LIMIT 3;

-- 3. Покупатели с общей суммой заказов
SELECT c.id, c.first_name, c.last_name, SUM(p.price * o.quantity) as total_spent
FROM customer c
JOIN orders o ON c.id = o.customer_id
JOIN products p ON o.product_id = p.id
GROUP BY c.id, c.first_name, c.last_name
ORDER BY total_spent DESC;

-- 4. Товары, которых осталось меньше 10 на складе
SELECT p.description, p.quantity, p.category
FROM products p
WHERE p.quantity < 10
ORDER BY p.quantity ASC;

-- 5. Ежемесячная статистика заказов
SELECT
    EXTRACT(YEAR FROM order_date) as year,
    EXTRACT(MONTH FROM order_date) as month,
    COUNT(*) as order_count,
    SUM(p.price * o.quantity) as total_amount
FROM orders o
JOIN products p ON o.product_id = p.id
GROUP BY year, month
ORDER BY year, month;

-- 6. Обновление количества товара на складе после заказа
UPDATE products
SET quantity = quantity - 1
WHERE id = 1;

-- 7. Обновление статуса заказа
UPDATE orders
SET status_id = (SELECT id FROM order_status WHERE status_name = 'Завершен')
WHERE id = 1;

-- 8. Обновление цены товара
UPDATE products
SET price = price * 1.1
WHERE category = 'Электроника';

-- 9. Удаление конкретных заказов с ID 16 и 17
DELETE FROM orders WHERE id IN (16, 17);

-- 10. Удаление старых завершенных заказов (до 20 сентября 2025)
DELETE FROM orders
WHERE status_id = (SELECT id FROM order_status WHERE status_name = 'Отменен')
AND order_date < '2025-09-20';