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

package org.terasology.world.viewer.camera;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.math.RoundingMode;

import javax.swing.SwingUtilities;

import org.terasology.math.TeraMath;

import com.google.common.math.DoubleMath;

/**
 * Controls a camera based on mouse interaction
 */
public class CameraMouseController extends MouseAdapter {

    private Point draggedPoint;
    private final Camera camera;

    private int zoomLevel;
    private int minZoomLevel = -8;
    private int maxZoomLevel = 16;

    private final float zoomDelta = 0.25f;

    public CameraMouseController(Camera camera) {
        this.camera = camera;
        this.zoomLevel = findZoomLevel(camera.getZoom());
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (draggedPoint != null) {
            int dx = draggedPoint.x - e.getX();
            int dy = draggedPoint.y - e.getY();
            draggedPoint.setLocation(e.getPoint());
            camera.translate(dx, dy);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isRightMouseButton(e)) {
            draggedPoint = e.getPoint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        draggedPoint = null;
    }

    private int findZoomLevel(float zoom) {
        double est = Math.log(zoom) / Math.log(2);
        return DoubleMath.roundToInt(est / zoomDelta, RoundingMode.HALF_UP);
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        zoomLevel += e.getWheelRotation();

        zoomLevel = TeraMath.clamp(zoomLevel, minZoomLevel, maxZoomLevel);

        // Zoom only in deterministic steps
        // Don't concatenate with previous zooms to avoid rounding errors
        float zoom = (float) Math.pow(2.0, zoomLevel * zoomDelta);

        // This cast is safe since MouseWheelEvent takes only Component sources
        Component source = (Component) e.getSource();

        int relX = e.getX() - source.getWidth() / 2;
        int relY = e.getY() - source.getHeight() / 2;

        // move the camera to the cursor position
        camera.translate(relX, relY);

        // zoom
        camera.setZoom(zoom);

        // revert the camera movement from above
        camera.translate(-relX, -relY);
    }
}
