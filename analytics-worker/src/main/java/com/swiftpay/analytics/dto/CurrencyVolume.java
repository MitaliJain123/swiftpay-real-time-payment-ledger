package com.swiftpay.analytics.dto;

import java.math.BigDecimal;

public record CurrencyVolume(
        String currency,
        long payments,
        BigDecimal totalVolume
) {
}
