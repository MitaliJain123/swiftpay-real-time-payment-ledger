CREATE TABLE IF NOT EXISTS payment_events
(
    transaction_id String,
    sender_id      Int64,
    receiver_id    Int64,
    amount         Decimal(19, 2),
    currency       LowCardinality(String),
    status         LowCardinality(String),
    completed_at   DateTime DEFAULT now()
)
ENGINE = ReplacingMergeTree(completed_at)
ORDER BY transaction_id;
