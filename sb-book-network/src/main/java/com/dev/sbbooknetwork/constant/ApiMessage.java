package com.dev.sbbooknetwork.constant;

public class ApiMessage {
    public static final String ACCOUNT_ACTIVATED_SUCCESSFULLY = "Account activated successfully.";
    public static final String ACTIVATION_FAILED_INVALID_TOKEN = "Activation failed. Invalid token.";

    public static final String FORBIDDEN_MESSAGE = "You do not have permission to access this resource.";

    public static final String ACCESS_DENIED_MESSAGE = "Access is denied.";

    public static final String LOGIN_SUCCESSFULLY = "Login successfully.";
    public ApiMessage(int value, String forbidden, String forbiddenMessage) {
    }
}
