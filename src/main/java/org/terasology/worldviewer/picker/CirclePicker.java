
package org.terasology.worldviewer.picker;

import java.util.Set;

import org.terasology.math.geom.BaseVector2f;

/**
 * Retrieves a set of circular objects in the proximity of a given anchor point.
 * @param <T> the object type
 * @author Martin Steiger
 */
public interface CirclePicker<T> {

    void offer(float locX, float locY, T object);

    default void offer(BaseVector2f location, T object) {
        offer(location.getX(), location.getY(), object);
    }

    Set<T> getAll();
}
