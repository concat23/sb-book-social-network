package com.dev.sbbooknetwork.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@AllArgsConstructor
@SuperBuilder
public class ResponseMessage {
    private String message;
    private int statusCode;
}
