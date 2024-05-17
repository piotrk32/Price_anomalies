CREATE TYPE category_enum AS ENUM ('CASE');

CREATE TABLE items (
    id UUID PRIMARY KEY,
    item_name VARCHAR(255) NOT NULL,
    lowest_price DOUBLE PRECISION NOT NULL,
    median_price DOUBLE PRECISION NOT NULL,
    category category_enum NOT NULL
);

CREATE TABLE alerts (
    id UUID PRIMARY KEY,
    item_id UUID NOT NULL,
    date TIMESTAMP NOT NULL,
    price_gap INT NOT NULL,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
);
