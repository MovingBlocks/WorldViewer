package org.terasology.worldviewer.lambda;


@FunctionalInterface
public interface CheckedConsumer<T> {

    /**
     * Performs this operation on the given argument.
     *
     * @param t the input argument
     * @throws Exception any checked exception
     */
    void accept(T t) throws Exception;
}
