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
import java.awt.geom.Line2D;
import java.util.function.Function;

import org.terasology.math.geom.ImmutableVector2f;
import org.terasology.math.geom.ImmutableVector2i;
import org.terasology.math.geom.LineSegment;
import org.terasology.math.geom.Rect2i;
import org.terasology.math.geom.Vector2f;
import org.terasology.polyworld.graph.GraphFacet;
import org.terasology.polyworld.graph.Triangle;
import org.terasology.world.generation.Region;

public class RiverModelOverlay extends AbstractOverlay implements WorldOverlay {

    private Function<ImmutableVector2i, Region> regions;

    public RiverModelOverlay(Function<ImmutableVector2i, Region> regionFunc) {
        regions = regionFunc;
    }

    @Override
    public void render(Graphics2D g, Rect2i area, ImmutableVector2i cursor) {
        if (cursor == null) {
            return;
        }

        Region r = regions.apply(cursor);
        GraphFacet graphs = r.getFacet(GraphFacet.class);
        if (graphs != null) {
            Triangle t = graphs.getWorldTriangle(cursor.getX(), cursor.getY());
            ImmutableVector2f p0 = t.getCorner1().getLocation();
            ImmutableVector2f p1 = t.getCorner2().getLocation();
            if (LineSegment.distanceToPoint(p0, p1, new Vector2f(cursor.getX(), cursor.getY())) < 5) {
                g.setStroke(new BasicStroke(3f));
                g.setColor(Color.RED);
                g.draw(new Line2D.Float(p0.getX(), p0.getY(), p1.getX(), p1.getY()));
            }
        }
    }
}
