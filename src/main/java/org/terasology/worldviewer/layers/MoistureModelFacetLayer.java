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

package org.terasology.worldviewer.layers;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.math.geom.BaseVector2f;
import org.terasology.math.geom.Vector2f;
import org.terasology.polyworld.moisture.MoistureModel;
import org.terasology.polyworld.moisture.MoistureModelFacet;
import org.terasology.polyworld.voronoi.Corner;
import org.terasology.polyworld.voronoi.Graph;
import org.terasology.world.generation.Region;
import org.terasology.world.generation.WorldFacet;

import com.google.common.base.Stopwatch;

/**
 * TODO Type description
 * @author Martin Steiger
 */
public class MoistureModelFacetLayer extends AbstractFacetLayer {

    private static final Logger logger = LoggerFactory.getLogger(MoistureModelFacetLayer.class);

    public MoistureModelFacetLayer() {
     // use default settings
    }

    @Override
    public void render(BufferedImage img, Region region) {
        MoistureModelFacet facet = region.getFacet(MoistureModelFacet.class);

        Stopwatch sw = Stopwatch.createStarted();

        Graphics2D g = img.createGraphics();
        int dx = region.getRegion().minX();
        int dy = region.getRegion().minZ();
        g.translate(-dx, -dy);

        for (Graph graph : facet.getKeys()) {
            MoistureModel model = facet.get(graph);
            draw(g, model, graph);
        }

        g.dispose();

        if (logger.isTraceEnabled()) {
            logger.debug("Rendered regions in {}ms.", sw.elapsed(TimeUnit.MILLISECONDS));
        }
    }

    private void draw(Graphics2D g, MoistureModel model, Graph graph) {
        g.setColor(new Color(0x4040FF));
        for (Corner c : graph.getCorners()) {
            float moisture = model.getMoisture(c);
            float r = 4 * moisture;
            BaseVector2f loc = c.getLocation();
            g.fill(new Ellipse2D.Float(loc.getX() - r, loc.getY() - r, 2 * r, 2 * r));
        }
    }

    @Override
    public String getWorldText(Region region, int wx, int wy) {
        MoistureModelFacet moistureModelFacet = region.getFacet(MoistureModelFacet.class);
        double minDistSq = Double.MAX_VALUE;
        double radSq = 5 * 5;
        double moisture = Double.NaN;
        Corner closest = null;
        Vector2f cursor = new Vector2f(wx, wy);
        for (Graph graph : moistureModelFacet.getKeys()) {
            if (graph.getBounds().contains(wx, wy)) {
                MoistureModel model = moistureModelFacet.get(graph);
                for (Corner c : graph.getCorners()) {
                    float distSq = c.getLocation().distanceSquared(cursor);
                    if (distSq < radSq && distSq < minDistSq) {
                        minDistSq = distSq;
                        closest = c;
                        moisture = model.getMoisture(c);
                    }
                }
            }
        }

        if (closest != null) {
            return String.format("Moisture: %.2f", moisture);
        } else {
            return null;
        }
    }

    @Override
    public Class<? extends WorldFacet> getFacetClass() {
        return MoistureModelFacet.class;
    }
}
