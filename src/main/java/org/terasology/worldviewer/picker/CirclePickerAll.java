package org.terasology.worldviewer.picker;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import org.terasology.math.geom.BaseVector2f;
import org.terasology.math.geom.Vector2f;

public class CirclePickerAll<T> implements CirclePicker<T> {
    private final Set<T> hits = new HashSet<T>();

    private final BaseVector2f cursor;
    private final Function<? super T, ? extends Number> radiusFunc;

    public CirclePickerAll(Vector2f cursor, Function<? super T, ? extends Number> radiusFunc) {
        this.cursor = cursor;
        this.radiusFunc = radiusFunc;
    }

    @Override
    public void offer(float locX, float locY, T object) {
        float dx = cursor.getX() - locX;
        float dy = cursor.getY() - locY;
        float distSq = dx * dx + dy * dy;

        float rad = radiusFunc.apply(object).floatValue();
        if (distSq <= rad * rad) {
            hits.add(object);
        }
    }

    @Override
    public Set<T> getAll() {
        return Collections.unmodifiableSet(hits);
    }
}
