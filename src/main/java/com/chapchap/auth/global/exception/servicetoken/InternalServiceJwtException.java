package com.chapchap.auth.global.exception.servicetoken;

public final class InternalServiceJwtException extends RuntimeException {
    private final Reason reason;

    private InternalServiceJwtException(Reason reason) {
        super(reason.error());
        this.reason = reason;
    }

    public static InternalServiceJwtException invalidRequest() {
        return new InternalServiceJwtException(Reason.INVALID_REQUEST);
    }

    public static InternalServiceJwtException invalidClient() {
        return new InternalServiceJwtException(Reason.INVALID_CLIENT);
    }

    public static InternalServiceJwtException insufficientScope() {
        return new InternalServiceJwtException(Reason.INSUFFICIENT_SCOPE);
    }

    public Reason reason() {
        return reason;
    }

    public enum Reason {
        INVALID_REQUEST("invalid_request"),
        INVALID_CLIENT("invalid_client"),
        INSUFFICIENT_SCOPE("insufficient_scope");

        private final String error;

        Reason(String error) {
            this.error = error;
        }

        public String error() {
            return error;
        }
    }
}
