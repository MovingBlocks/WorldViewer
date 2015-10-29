/*
 * Copyright 2015 MovingBlocks
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
import java.awt.Polygon;
import java.util.function.Function;

import org.terasology.cities.lakes.Lake;
import org.terasology.cities.lakes.LakeFacet;
import org.terasology.math.geom.ImmutableVector2i;
import org.terasology.math.geom.Rect2i;
import org.terasology.world.generation.Region;

public class LakeOverlay extends AbstractOverlay implements WorldOverlay {

    private Function<ImmutableVector2i, Region> regions;

    public LakeOverlay(Function<ImmutableVector2i, Region> regionFunc) {
        regions = regionFunc;
    }

    @Override
    public void render(Graphics2D g, Rect2i area, ImmutableVector2i cursor) {
        if (cursor == null) {
            return;
        }

        Color fillColor = new Color(64, 64, 255, 128);
        Color frameColor = new Color(64, 64, 255, 224);

        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND));
        Region r = regions.apply(cursor);
        LakeFacet lakeFacet = r.getFacet(LakeFacet.class);
        for (Lake lake : lakeFacet.getLakes()) {
            if (lake.getContour().getPolygon().contains(cursor.getX(), cursor.getY())) {
                Polygon poly = lake.getContour().getPolygon();
                g.translate(0.5, 0.5);
                g.setColor(fillColor);
                g.fill(poly);
                g.setColor(frameColor);
                g.draw(poly);
                g.translate(-0.5, -0.5);
            }
        }
    }
}
