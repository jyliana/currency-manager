-- DROP TABLE IF EXISTS exchange_rates;

CREATE TABLE exchange_rates
(
    id            UUID PRIMARY KEY,
    date          TIMESTAMPTZ    NOT NULL,
    currency      VARCHAR(3)     NOT NULL,
    sale_rate     NUMERIC(18, 7) NOT NULL,
    purchase_rate NUMERIC(18, 7) NOT NULL,
    UNIQUE (date, currency)
);