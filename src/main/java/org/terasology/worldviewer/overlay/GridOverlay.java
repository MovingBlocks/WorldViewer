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

package org.terasology.worldviewer.overlay;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.math.RoundingMode;

import org.terasology.math.Rect2i;

import com.google.common.math.IntMath;

/**
 * Renders a grid that is aligned along tile borders
 * @author Martin Steiger
 */
public class GridOverlay implements Overlay {

    private Color majorGridColor = new Color(128, 128, 128, 192);
    private Color minorGridColor = new Color(128, 128, 128, 64);

    private int majorToMinor = 8;

    private int tileSizeX;
    private int tileSizeY;

    public GridOverlay(int tileSizeX, int tileSizeY) {
        this.tileSizeX = tileSizeX;
        this.tileSizeY = tileSizeY;
    }


    @Override
    public void render(Graphics2D g, Rect2i area) {
        int tileMinX = IntMath.divide(area.minX(), tileSizeX, RoundingMode.FLOOR);
        int tileMinZ = IntMath.divide(area.minY(), tileSizeY, RoundingMode.FLOOR);

        int tileMaxX = IntMath.divide(area.maxX(), tileSizeX, RoundingMode.CEILING);
        int tileMaxZ = IntMath.divide(area.maxY(), tileSizeY, RoundingMode.CEILING);

        g.setStroke(new BasicStroke(0));

        for (int z = tileMinZ; z < tileMaxZ; z++) {
            g.setColor((z % majorToMinor == 0) ? majorGridColor : minorGridColor);
            g.drawLine(tileMinX * tileSizeX, z * tileSizeY, tileMaxX * tileSizeX, z * tileSizeY);
        }

        for (int x = tileMinX; x < tileMaxX; x++) {
            g.setColor((x % majorToMinor == 0) ? majorGridColor : minorGridColor);
            g.drawLine(x * tileSizeX, tileMinZ * tileSizeY, x * tileSizeX, tileMaxZ * tileSizeY);
        }
    }
}
