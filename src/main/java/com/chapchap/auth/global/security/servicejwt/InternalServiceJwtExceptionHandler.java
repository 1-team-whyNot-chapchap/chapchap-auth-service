package com.chapchap.auth.global.security.servicejwt;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = ServiceTokenController.class)
public class InternalServiceJwtExceptionHandler {
    @ExceptionHandler(InternalServiceJwtException.class)
    public ResponseEntity<ErrorResponse> handle(InternalServiceJwtException exception) {
        HttpStatus status = switch (exception.reason()) {
            case INVALID_REQUEST -> HttpStatus.BAD_REQUEST;
            case INVALID_CLIENT -> HttpStatus.UNAUTHORIZED;
            case INSUFFICIENT_SCOPE -> HttpStatus.FORBIDDEN;
        };
        return ResponseEntity.status(status).body(new ErrorResponse(exception.reason().error()));
    }

    public record ErrorResponse(String error) {
    }
}
