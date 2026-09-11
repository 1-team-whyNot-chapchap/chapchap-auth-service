package com.chapchap.auth.global.messaging.kafka.consumer;

/** 필수 Envelope/Payload가 깨진 메시지로, retry 후 DLT 이동 대상이다. */
public class KafkaPayloadException extends RuntimeException {
    public KafkaPayloadException(String message) {
        super(message);
    }

    public KafkaPayloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
