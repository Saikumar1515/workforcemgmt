package com.railse.hiring.workforcemgmt.common.response;

public class SimpleResponseStatus extends ResponseStatus {
    public SimpleResponseStatus(int code, String message) {
        super(code, message);
    }

    @Override
    public int value() {
        return getCode();
    }
}
