package net.flamgop.asset;

import java.util.concurrent.atomic.AtomicInteger;

public class Asset<T> {
    private final T data;
    private final AtomicInteger referenceCount = new AtomicInteger(1);

    public Asset(T data) {
        this.data = data;
    }

    public T get() {
        return this.data;
    }

    public void retain() {
        this.referenceCount.incrementAndGet();
    }

    public boolean release() {
        return this.referenceCount.decrementAndGet() <= 0;
    }
}
