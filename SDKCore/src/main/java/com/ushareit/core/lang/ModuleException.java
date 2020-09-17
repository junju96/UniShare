package com.ushareit.core.lang;

/**
 * the application wide base exception that has a error code.
 * error codes values are usually defined in sub classes.
 */
public class ModuleException extends Exception {
    private static final long serialVersionUID = 1L;

    private int code;

    public ModuleException(int code, String message) {
        super(message);
        this.code = code;
    }

    public ModuleException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public ModuleException(int code, Throwable cause) {
        super(cause);
        this.code = code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    @Override
    public String toString() {
        String msg = getLocalizedMessage();
        String name = getClass().getName();

        return name + ": " + "[ code = " + code + ", msg = " + msg + "]";
    }
}
