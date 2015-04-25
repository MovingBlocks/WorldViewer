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

package org.terasology.worldviewer.config;

import org.terasology.math.geom.Vector2i;

/**
 * Stores view-related config params.
 * @author Martin Steiger
 */
public class ViewConfig {

    private Vector2i camPos = new Vector2i(512, 512);
    private float zoomFactor = 1f;

    public Vector2i getCamPos() {
        return camPos;
    }

    public void setCamPos(Vector2i camPos) {
        this.camPos = camPos;
    }

    public float getZoomFactor() {
        return zoomFactor;
    }

    public void setZoomFactor(float zoomFactor) {
        this.zoomFactor = zoomFactor;
    }
}
