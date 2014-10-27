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

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.SwingUtilities;

/**
 * Controls a camera based on mouse interaction
 * @author Martin Steiger
 */
public class CameraMouseController extends MouseAdapter {

    private Point draggedPoint;
    private final Camera camera;

    public CameraMouseController(Camera camera) {
        this.camera = camera;
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
}
