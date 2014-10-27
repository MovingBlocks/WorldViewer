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

package org.terasology.worldviewer.core;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Renders a grid
 * @author Martin Steiger
 */
public class GridRenderer {

    private Color majorGridColor = new Color(128, 128, 128, 192);
    private Color minorGridColor = new Color(128, 128, 128, 64);

    private int majorToMinor = 8;

    private int tileSizeX;
    private int tileSizeY;


    /**
     * @param tileSizeX
     * @param tileSizeY
     */
    public GridRenderer(int tileSizeX, int tileSizeY) {
        this.tileSizeX = tileSizeX;
        this.tileSizeY = tileSizeY;
    }


    public void draw(Graphics2D g, int tileMinX, int tileMinZ, int tileMaxX, int tileMaxZ) {
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
