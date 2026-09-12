package com.swiftpay.gateway.controller;

import com.swiftpay.gateway.dto.PaymentRequest;
import com.swiftpay.gateway.dto.PaymentResponse;
import com.swiftpay.gateway.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/payments")
@Tag(name = "Payments", description = "Initiate peer-to-peer payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(
            PaymentService paymentService
    ) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @Operation(
            summary = "Initiate a payment",
            description = """
                    Validates the request, reserves the transactionId in Redis \
                    (idempotency), saves a PENDING payment, and publishes a \
                    payment-initiated Kafka event. Settlement is asynchronous \
                    via the Ledger Service.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "202",
                    description = "Payment accepted (PENDING) or already processed",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PaymentResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Accepted",
                                            value = """
                                                    {
                                                      "transactionId": "txn-001",
                                                      "status": "PENDING",
                                                      "message": "Payment initiated successfully"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Duplicate",
                                            value = """
                                                    {
                                                      "transactionId": "txn-001",
                                                      "status": "COMPLETED",
                                                      "message": "Transaction already processed"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error, insufficient funds, or invalid payment",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "error": "INSUFFICIENT_FUNDS",
                                              "message": "Insufficient funds",
                                              "status": 400
                                            }
                                            """
                            )
                    )
            )
    })
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody PaymentRequest request
    ) {

        PaymentResponse response =
                paymentService.initiatePayment(request);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(response);
    }
}
