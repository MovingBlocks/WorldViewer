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

package org.terasology.world.viewer.overlay;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

import org.terasology.math.geom.ImmutableVector2i;
import org.terasology.math.geom.Rect2i;

/**
 * Renders a grid over ever world block (pixel).
 * Makes sense only for large zoom factors.
 */
public class PixelOverlay extends AbstractOverlay implements WorldOverlay {

    private Color gridColor = new Color(96, 96, 96, 96);
    private float minScaleFactor;

    /**
     * @param minScaleFactor the minimum scale factor that is required for the pixel grid to become visible
     */
    public PixelOverlay(float minScaleFactor) {
        this.minScaleFactor = minScaleFactor;
    }

    @Override
    public void render(Graphics2D g, Rect2i area, ImmutableVector2i cursor) {
        AffineTransform at = g.getTransform();
        if (at.getScaleX() < minScaleFactor || at.getScaleX() < minScaleFactor) {
            return;
        }

        g.setColor(gridColor);
        g.setStroke(new BasicStroke(0));

        for (int z = area.minY(); z < area.maxY(); z++) {
            g.drawLine(area.minX(), z, area.maxX(), z);
        }

        for (int x = area.minX(); x < area.maxX(); x++) {
            g.drawLine(x, area.minY(), x, area.maxY());
        }
    }
}
