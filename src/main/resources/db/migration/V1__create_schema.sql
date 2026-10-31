CREATE TABLE restaurants (
    id   INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT    NOT NULL
);

CREATE TABLE products (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    restaurant_id INTEGER NOT NULL REFERENCES restaurants (id),
    name          TEXT    NOT NULL,
    category      TEXT    NOT NULL,
    price_cents   INTEGER NOT NULL CHECK (price_cents > 0),
    available     INTEGER NOT NULL DEFAULT 1,
    created_at    INTEGER NOT NULL,
    updated_at    INTEGER NOT NULL
);

CREATE UNIQUE INDEX ux_products_restaurant_name ON products (restaurant_id, name COLLATE NOCASE);

CREATE TABLE orders (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    restaurant_id INTEGER NOT NULL REFERENCES restaurants (id),
    status        TEXT    NOT NULL CHECK (status IN ('PENDING', 'PREPARING', 'READY', 'COMPLETED', 'CANCELLED')),
    total_cents   INTEGER NOT NULL CHECK (total_cents >= 0),
    created_at    INTEGER NOT NULL,
    updated_at    INTEGER NOT NULL,
    version       INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX ix_orders_restaurant_created ON orders (restaurant_id, created_at DESC, id DESC);
CREATE INDEX ix_orders_restaurant_status_created ON orders (restaurant_id, status, created_at DESC, id DESC);

CREATE TABLE order_lines (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id         INTEGER NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_id       INTEGER NOT NULL REFERENCES products (id),
    product_name     TEXT    NOT NULL,
    quantity         INTEGER NOT NULL CHECK (quantity > 0),
    unit_price_cents INTEGER NOT NULL CHECK (unit_price_cents > 0),
    line_total_cents INTEGER NOT NULL CHECK (line_total_cents > 0)
);

CREATE INDEX ix_order_lines_order ON order_lines (order_id);
CREATE INDEX ix_order_lines_product ON order_lines (product_id);
