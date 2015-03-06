package org.terasology.worldviewer.lambda;


@FunctionalInterface
public interface CheckedSupplier<T>
{
    /**
     * Gets a result.
     *
     * @return a result
     * @throws Exception any checked exception
     */
    T get() throws Exception;
}
