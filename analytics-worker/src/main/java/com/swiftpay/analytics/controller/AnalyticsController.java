package com.swiftpay.analytics.controller;

import com.swiftpay.analytics.dto.CurrencyVolume;
import com.swiftpay.analytics.dto.MinuteVolume;
import com.swiftpay.analytics.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/analytics")
@Tag(name = "Analytics", description = "Real-time payment volume monitoring (ClickHouse)")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(
            AnalyticsService analyticsService
    ) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/volume")
    @Operation(
            summary = "Volume by currency",
            description = """
                    Total completed-payment count and volume per currency within \
                    the last N minutes (data from ClickHouse).
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Volume summary",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "windowMinutes": 60,
                                      "currencies": [
                                        {
                                          "currency": "INR",
                                          "payments": 4,
                                          "totalVolume": 675.50
                                        }
                                      ]
                                    }
                                    """
                    )
            )
    )
    public ResponseEntity<Map<String, Object>> volume(
            @Parameter(description = "Lookback window in minutes", example = "60")
            @RequestParam(defaultValue = "60") int minutes
    ) {

        List<CurrencyVolume> currencies =
                analyticsService.volumeByCurrency(minutes);

        return ResponseEntity.ok(Map.of(
                "windowMinutes", minutes,
                "currencies", currencies
        ));
    }

    @GetMapping("/volume/timeseries")
    @Operation(
            summary = "Per-minute volume timeseries",
            description = """
                    Per-minute payment count and volume within the last N minutes. \
                    Timestamps are UTC.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Timeseries points",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "windowMinutes": 60,
                                      "points": [
                                        {
                                          "minute": "2026-09-12T21:36:00",
                                          "payments": 4,
                                          "volume": 675.50
                                        }
                                      ]
                                    }
                                    """
                    )
            )
    )
    public ResponseEntity<Map<String, Object>> timeseries(
            @Parameter(description = "Lookback window in minutes", example = "60")
            @RequestParam(defaultValue = "60") int minutes
    ) {

        List<MinuteVolume> points =
                analyticsService.volumePerMinute(minutes);

        return ResponseEntity.ok(Map.of(
                "windowMinutes", minutes,
                "points", points
        ));
    }
}
