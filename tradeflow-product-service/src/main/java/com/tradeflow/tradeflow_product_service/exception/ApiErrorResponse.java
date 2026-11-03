package com.tradeflow.tradeflow_product_service.exception;

import java.time.Instant;

public record ApiErrorResponse(Instant timestamp, int status, String errorCode, String message, String path) {
}