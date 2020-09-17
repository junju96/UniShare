package com.ushareit.core.lang;

/**
 * a reference to reference.
 * @param <T>
 */
public class HardReference<T> {
    private T t;

    public HardReference(T t) {
        this.t = t;
    }

    public T get() {
        return this.t;
    }

    public void clear() {
        this.t = null;
    }
}
