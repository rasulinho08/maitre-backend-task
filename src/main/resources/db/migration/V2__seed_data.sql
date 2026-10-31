INSERT INTO restaurants (id, name) VALUES
    (1, 'Maitre Downtown'),
    (2, 'Maitre Airport'),
    (3, 'Maitre Seaside');

INSERT INTO products (restaurant_id, name, category, price_cents, available, created_at, updated_at) VALUES
    (1, 'Classic Burger', 'Burgers', 890, 1, 1735689600000, 1735689600000),
    (1, 'Cheese Burger',  'Burgers', 990, 1, 1735689600000, 1735689600000),
    (1, 'French Fries',   'Sides',   350, 1, 1735689600000, 1735689600000),
    (1, 'Cola',           'Drinks',  250, 1, 1735689600000, 1735689600000),
    (2, 'Chicken Wrap',   'Wraps',   790, 1, 1735689600000, 1735689600000),
    (2, 'Orange Juice',   'Drinks',  300, 1, 1735689600000, 1735689600000);
