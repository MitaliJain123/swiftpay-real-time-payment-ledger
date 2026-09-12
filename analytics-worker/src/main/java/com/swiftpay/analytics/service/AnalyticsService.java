package com.swiftpay.analytics.service;

import com.swiftpay.analytics.dto.CurrencyVolume;
import com.swiftpay.analytics.dto.MinuteVolume;
import com.swiftpay.analytics.kafka.PaymentCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnalyticsService {

    private static final Logger log =
            LoggerFactory.getLogger(AnalyticsService.class);

    private final JdbcTemplate jdbcTemplate;

    public AnalyticsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Writes a completed payment to the ClickHouse analytics table.
     * The table is a ReplacingMergeTree keyed by transaction_id, so a
     * redelivered Kafka event never double-counts a payment.
     */
    public void recordPayment(PaymentCompletedEvent event) {

        jdbcTemplate.update(
                """
                INSERT INTO payment_events
                    (transaction_id, sender_id, receiver_id,
                     amount, currency, status)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                event.getTransactionId(),
                event.getSenderId(),
                event.getReceiverId(),
                event.getAmount(),
                event.getCurrency(),
                event.getStatus()
        );

        log.info(
                "Recorded payment {} ({} {}) in analytics store",
                event.getTransactionId(),
                event.getAmount(),
                event.getCurrency()
        );
    }

    /**
     * Total payment count and volume per currency within the last
     * {@code minutes} minutes.
     */
    public List<CurrencyVolume> volumeByCurrency(int minutes) {

        return jdbcTemplate.query(
                """
                SELECT currency,
                       count()     AS payments,
                       sum(amount) AS total_volume
                FROM payment_events FINAL
                WHERE completed_at >= now() - toIntervalMinute(?)
                GROUP BY currency
                ORDER BY total_volume DESC
                """,
                (rs, rowNum) -> new CurrencyVolume(
                        rs.getString("currency"),
                        rs.getLong("payments"),
                        rs.getBigDecimal("total_volume")
                ),
                minutes
        );
    }

    /**
     * Per-minute payment count and volume within the last
     * {@code minutes} minutes (real-time volume monitoring).
     */
    public List<MinuteVolume> volumePerMinute(int minutes) {

        return jdbcTemplate.query(
                """
                SELECT toStartOfMinute(completed_at) AS minute,
                       count()                       AS payments,
                       sum(amount)                   AS volume
                FROM payment_events FINAL
                WHERE completed_at >= now() - toIntervalMinute(?)
                GROUP BY minute
                ORDER BY minute
                """,
                (rs, rowNum) -> new MinuteVolume(
                        rs.getTimestamp("minute")
                                .toLocalDateTime(),
                        rs.getLong("payments"),
                        rs.getBigDecimal("volume")
                ),
                minutes
        );
    }
}
