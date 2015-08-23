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

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Controls a camera based on keyboard interaction
 */
public class CameraKeyController extends KeyAdapter {

    private final Camera camera;

    public CameraKeyController(Camera camera) {
        this.camera = camera;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int moveInterval = 64;

        if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            camera.translate(-moveInterval, 0);
        }
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            camera.translate(moveInterval, 0);
        }
        if (e.getKeyCode() == KeyEvent.VK_UP) {
            camera.translate(0, -moveInterval);
        }
        if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            camera.translate(0, moveInterval);
        }
    }
}
