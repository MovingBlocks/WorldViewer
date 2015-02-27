package org.terasology.worldviewer.core;

@FunctionalInterface
public interface Observer<T> {

    void update(T layer);
}
