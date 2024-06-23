package com.dev.sbbooknetwork.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class ResponseMessage {
    private String message;
    private int statusCode;

    public ResponseMessage(String message) {
        this.message = message;
    }

    public ResponseMessage(String message, int statusCode) {
        this.message = message;
        this.statusCode = statusCode;
    }
}
