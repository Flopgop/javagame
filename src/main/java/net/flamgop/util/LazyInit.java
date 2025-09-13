package net.flamgop.util;

import java.util.function.Supplier;

public class LazyInit<T> {
    private T value;

    private final Supplier<T> supplier;

    public LazyInit(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public T get() {
        if (this.value == null)
            this.value = supplier.get();
        return this.value;
    }
}
