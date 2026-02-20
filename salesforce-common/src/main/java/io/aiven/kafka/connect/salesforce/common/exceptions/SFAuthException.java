package io.aiven.kafka.connect.salesforce.common.exceptions;

public class SFAuthException extends Exception {
    public SFAuthException(String message) {
        super(message);
    }
}
