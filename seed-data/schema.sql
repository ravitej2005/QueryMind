-- QueryMind demo dataset: E-commerce Analytics
-- This is a SEPARATE database from QueryMind's own app schema (users,
-- workspaces, connections, query_history, chat_messages). It represents
-- the kind of production database a real QueryMind user would connect to
-- and ask questions about. Load into its own schema/user, then create the
-- read-only credential against THIS database (see SETUP_TODO.md §5).

CREATE DATABASE IF NOT EXISTS querymind_demo;
USE querymind_demo;

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE product_categories (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    parent_category_id INT NULL,
    FOREIGN KEY (parent_category_id) REFERENCES product_categories(id)
);

CREATE TABLE suppliers (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    country VARCHAR(100) NOT NULL,
    contact_email VARCHAR(200),
    reliability_score DECIMAL(3,2), -- 0.00 - 5.00, used for supplier performance analysis
    created_at DATETIME NOT NULL
);

CREATE TABLE warehouses (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    region VARCHAR(100) NOT NULL,
    capacity_units INT NOT NULL
);

CREATE TABLE products (
    id INT PRIMARY KEY AUTO_INCREMENT,
    sku VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    category_id INT NOT NULL,
    supplier_id INT NOT NULL,
    unit_cost DECIMAL(10,2) NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (category_id) REFERENCES product_categories(id),
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
);

CREATE TABLE inventory (
    id INT PRIMARY KEY AUTO_INCREMENT,
    product_id INT NOT NULL,
    warehouse_id INT NOT NULL,
    quantity_on_hand INT NOT NULL,
    reorder_threshold INT NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
);

CREATE TABLE customers (
    id INT PRIMARY KEY AUTO_INCREMENT,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(200) NOT NULL UNIQUE,
    country VARCHAR(100) NOT NULL,
    signup_date DATE NOT NULL,
    is_vip TINYINT(1) NOT NULL DEFAULT 0
);

CREATE TABLE coupons (
    id INT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(30) NOT NULL UNIQUE,
    discount_percent DECIMAL(5,2) NOT NULL,
    valid_from DATE NOT NULL,
    valid_to DATE NOT NULL
);

CREATE TABLE orders (
    id INT PRIMARY KEY AUTO_INCREMENT,
    customer_id INT NOT NULL,
    warehouse_id INT NOT NULL,
    coupon_id INT NULL,
    order_date DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL, -- PENDING, PAID, SHIPPED, DELIVERED, CANCELLED
    total_amount DECIMAL(12,2) NOT NULL,
    FOREIGN KEY (customer_id) REFERENCES customers(id),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
    FOREIGN KEY (coupon_id) REFERENCES coupons(id)
);

CREATE TABLE order_items (
    id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL,
    unit_price_at_purchase DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE payments (
    id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    method VARCHAR(30) NOT NULL, -- CARD, PAYPAL, BANK_TRANSFER
    status VARCHAR(20) NOT NULL, -- SUCCESS, FAILED, PENDING
    paid_at DATETIME NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE TABLE shipments (
    id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT NOT NULL,
    carrier VARCHAR(50) NOT NULL,
    shipped_at DATETIME NULL,
    delivered_at DATETIME NULL,
    status VARCHAR(20) NOT NULL, -- PREPARING, IN_TRANSIT, DELIVERED, RETURNED
    FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE TABLE refunds (
    id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    reason VARCHAR(200),
    requested_at DATETIME NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE TABLE reviews (
    id INT PRIMARY KEY AUTO_INCREMENT,
    product_id INT NOT NULL,
    customer_id INT NOT NULL,
    rating TINYINT NOT NULL, -- 1-5
    comment VARCHAR(500),
    created_at DATETIME NOT NULL,
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (customer_id) REFERENCES customers(id)
);

CREATE TABLE support_tickets (
    id INT PRIMARY KEY AUTO_INCREMENT,
    customer_id INT NOT NULL,
    order_id INT NULL,
    subject VARCHAR(200) NOT NULL,
    status VARCHAR(20) NOT NULL, -- OPEN, IN_PROGRESS, RESOLVED, CLOSED
    created_at DATETIME NOT NULL,
    resolved_at DATETIME NULL,
    FOREIGN KEY (customer_id) REFERENCES customers(id),
    FOREIGN KEY (order_id) REFERENCES orders(id)
);

SET FOREIGN_KEY_CHECKS = 1;

-- Read-only demo user for QueryMind to connect with (see SETUP_TODO.md §5).
-- Uncomment and set a real password before running:
-- CREATE USER 'qm_reader'@'%' IDENTIFIED BY 'REPLACE_ME';
-- GRANT SELECT ON querymind_demo.* TO 'qm_reader'@'%';
-- FLUSH PRIVILEGES;
