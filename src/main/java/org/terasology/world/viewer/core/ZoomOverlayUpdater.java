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

package org.terasology.world.viewer.core;

import java.awt.Component;

import javax.swing.Timer;

import org.terasology.world.viewer.overlay.TextOverlay;
import org.terasology.world.viewer.camera.CameraListener;


/**
 */
public class ZoomOverlayUpdater implements CameraListener {

    private final TextOverlay overlay;
    private final Timer timer;
    private final Component comp;

    /**
     * Uses 1000 milliSec visible time
     * @param comp the parent component (needed to send repaint events)
     * @param zoomOverlay the overlay
     */
    public ZoomOverlayUpdater(Component comp, TextOverlay zoomOverlay) {
        this(comp, zoomOverlay, 1000);
    }

    /**
     * @param comp the parent component (needed to send repaint events)
     * @param zoomOverlay the overlay
     * @param visTime the time in millisecs the overlay is visible for
     */
    public ZoomOverlayUpdater(Component comp, TextOverlay zoomOverlay, int visTime) {
        this.comp = comp;
        this.overlay = zoomOverlay;
        timer = new Timer(visTime, e -> {
            overlay.setVisible(false);
            comp.repaint();
        });
        timer.setRepeats(false);
    }

    @Override
    public void onPosChange() {
        // ignore
    }

    @Override
    public void onZoomChange() {
        overlay.setVisible(true);
        comp.repaint();
        timer.restart();
    }

}
