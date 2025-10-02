
-- Создание таблицы статусов заказов
CREATE TABLE IF NOT EXISTS order_status (
    id SERIAL PRIMARY KEY,
    status_name VARCHAR(50) NOT NULL UNIQUE
);

COMMENT ON TABLE order_status IS 'Справочник статусов заказов';
COMMENT ON COLUMN order_status.id IS 'Уникальный идентификатор статуса';
COMMENT ON COLUMN order_status.status_name IS 'Наименование статуса';

-- Создание таблицы товаров
CREATE TABLE IF NOT EXISTS products (
    id SERIAL PRIMARY KEY,
    description TEXT NOT NULL,
    price DECIMAL(10, 2) NOT NULL CHECK (price >= 0),
    quantity INT NOT NULL CHECK (quantity >= 0),
    category VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE products IS 'Таблица товаров';
COMMENT ON COLUMN products.id IS 'Уникальный идентификатор товара';
COMMENT ON COLUMN products.description IS 'Описание товара';
COMMENT ON COLUMN products.price IS 'Стоимость товара';
COMMENT ON COLUMN products.quantity IS 'Количество товара на складе';
COMMENT ON COLUMN products.category IS 'Категория товара';

-- Создание таблицы покупателей
CREATE TABLE IF NOT EXISTS customer (
    id SERIAL PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100) UNIQUE
);

COMMENT ON TABLE customer IS 'Таблица покупателей';
COMMENT ON COLUMN customer.id IS 'Уникальный идентификатор покупателя';
COMMENT ON COLUMN customer.first_name IS 'Имя покупателя';
COMMENT ON COLUMN customer.last_name IS 'Фамилия покупателя';
COMMENT ON COLUMN customer.phone IS 'Телефон покупателя';
COMMENT ON COLUMN customer.email IS 'Электронная почта покупателя';

-- Создание таблицы заказов
CREATE TABLE IF NOT EXISTS orders (
    id SERIAL PRIMARY KEY,
    product_id INT NOT NULL,
    customer_id INT NOT NULL,
    order_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    quantity INT NOT NULL CHECK (quantity > 0),
    status_id INT NOT NULL,
    FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE RESTRICT,
    FOREIGN KEY (customer_id) REFERENCES customer (id) ON DELETE RESTRICT,
    FOREIGN KEY (status_id) REFERENCES order_status (id) ON DELETE RESTRICT
);

COMMENT ON TABLE orders IS 'Таблица заказов';
COMMENT ON COLUMN orders.id IS 'Уникальный идентификатор заказа';
COMMENT ON COLUMN orders.product_id IS 'Идентификатор товара (внешний ключ)';
COMMENT ON COLUMN orders.customer_id IS 'Идентификатор покупателя (внешний ключ)';
COMMENT ON COLUMN orders.order_date IS 'Дата и время заказа';
COMMENT ON COLUMN orders.quantity IS 'Количество заказанного товара';
COMMENT ON COLUMN orders.status_id IS 'Идентификатор статуса заказа (внешний ключ)';

-- Создание индексов
CREATE INDEX IF NOT EXISTS idx_orders_product_id ON orders (product_id);
CREATE INDEX IF NOT EXISTS idx_orders_customer_id ON orders (customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_order_date ON orders (order_date);
CREATE INDEX IF NOT EXISTS idx_orders_status_id ON orders (status_id);
CREATE INDEX IF NOT EXISTS idx_products_category ON products (category);
