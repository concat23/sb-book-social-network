package com.dev.sbbooknetwork.auth;

import com.dev.sbbooknetwork.common.ResponseMessage;

public class ActivationAccountResponse extends ResponseMessage {


    public ActivationAccountResponse(String message) {
        super(message);
    }

    public ActivationAccountResponse(String message, int statusCode) {
        super(message, statusCode);
    }
}
