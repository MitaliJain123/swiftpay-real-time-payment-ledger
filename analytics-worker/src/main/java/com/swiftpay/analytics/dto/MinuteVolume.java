package com.swiftpay.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MinuteVolume(
        LocalDateTime minute,
        long payments,
        BigDecimal volume
) {
}
