package org.terasology.worldviewer.lambda;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author Martin Steiger
 */
public final class Lambda
{
    private Lambda() {
        // no instances
    }

    public static <T> Supplier<T> toRuntime(CheckedSupplier<T> checkedSupp)
    {
        return () -> {
            try {
                return checkedSupp.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    public static <T> Consumer<T> toRuntime(CheckedConsumer<T> checkedConsumer)
    {
        return (t) -> {
            try {
                checkedConsumer.accept(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
}
