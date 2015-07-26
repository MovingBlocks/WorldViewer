/*
 * Copyright 2014 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terasology.worldviewer.camera;

import java.util.Collection;

import org.terasology.math.TeraMath;
import org.terasology.math.geom.ImmutableVector2f;
import org.terasology.math.geom.Rect2i;
import org.terasology.math.geom.Vector2f;

import com.google.common.collect.Lists;

/**
 * Defines a simple camera
 */
public class Camera {
    private final Vector2f pos = new Vector2f();
    private final Collection<CameraListener> listeners = Lists.newLinkedList();
    private float zoom = 1.0f;

    public float getZoom() {
        return zoom;
    }

    public void setZoom(float zoom) {
        this.zoom = zoom;
        for (CameraListener listener : listeners) {
            listener.onZoomChange();
        }
    }

    public ImmutableVector2f getPos() {
        return new ImmutableVector2f(pos.x, pos.y);
    }

    /**
     * @param dx the x translation
     * @param dy the y translation
     */
    public void translate(float dx, float dy) {
        this.pos.addX(dx / zoom);
        this.pos.addY(dy / zoom);
        for (CameraListener listener : listeners) {
            listener.onPosChange();
        }
    }

    public void addListener(CameraListener listener) {
        listeners.add(listener);
    }

    public void removeListener(CameraListener listener) {
        listeners.remove(listener);
    }

    /**
     * @param width the width of the window
     * @param height the height of the window
     * @return the window that is currently visible by the camera
     */
    public Rect2i getVisibleArea(int width, int height) {
        int cx = TeraMath.floorToInt(pos.getX());
        int cy = TeraMath.floorToInt(pos.getY());

        // Compensate rounding errors by adding 2px to the visible window size
        int w = (int) (width / getZoom()) + 2;
        int h = (int) (height / getZoom()) + 2;
        int minX = cx - w / 2;
        int minY = cy - h / 2;
        Rect2i visWorld = Rect2i.createFromMinAndSize(minX, minY, w, h);
        return visWorld;
    }
}
