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
import java.awt.image.BufferedImage;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

import org.terasology.math.Rect2i;
import org.terasology.math.geom.BaseVector2f;
import org.terasology.polyworld.voronoi.Corner;
import org.terasology.polyworld.voronoi.Edge;
import org.terasology.polyworld.voronoi.Graph;
import org.terasology.polyworld.voronoi.GraphFacet;
import org.terasology.polyworld.voronoi.Region;
import org.terasology.polyworld.voronoi.Triangle;
import org.terasology.world.generation.WorldFacet;

import com.google.common.math.DoubleMath;

/**
 * Draws the generated graph on a AWT graphics instance
 * @author Martin Steiger
 */
public class GraphFacetTrait implements FacetLayer {

    @Override
    public Class<? extends WorldFacet> getFacetClass() {
        return GraphFacet.class;
    }

    @Override
    public void render(BufferedImage img, org.terasology.world.generation.Region region) {
        GraphFacet graphFacet = region.getFacet(GraphFacet.class);

        Graphics2D g = img.createGraphics();
        int dx = region.getRegion().minX();
        int dy = region.getRegion().minZ();
        g.translate(-dx, -dy);
        for (Graph graph : graphFacet.getAllGraphs()) {
            drawEdges(g, graph);
            drawBounds(g, graph);
        }

        g.dispose();
    }

    @Override
    public String getWorldText(org.terasology.world.generation.Region region, int wx, int wy) {
        return "";
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    public static void drawEdges(Graphics2D g, Graph graph) {
        g.setStroke(new BasicStroke(1));
        g.setColor(Color.CYAN);
        for (Edge e : graph.getEdges()) {
            BaseVector2f r0c = e.getCorner0().getLocation();
            BaseVector2f r1c = e.getCorner1().getLocation();
            g.drawLine((int) r0c.getX(), (int) r0c.getY(), (int) r1c.getX(), (int) r1c.getY());
        }
    }

    public static void drawPolys(Graphics2D g, Graph graph, Function<Region, Color> colorFunc) {
        List<Region> regions = graph.getRegions();

        for (final Region reg : regions) {

            Color col = colorFunc.apply(reg);
            Collection<Corner> pts = reg.getCorners();

            int[] xPoints = new int[pts.size()];
            int[] yPoints = new int[pts.size()];

            int i = 0;
            for (Corner corner : pts) {
                xPoints[i] = (int) corner.getLocation().getX();
                yPoints[i] = (int) corner.getLocation().getY();
                i++;
            }

            g.setColor(col);
            g.fillPolygon(xPoints, yPoints, pts.size());
        }
    }

    public static void drawTriangles(Graphics2D g, Graph graph) {
        List<Region> regions = graph.getRegions();

        Random r = new Random(12332434);

        for (final Region reg : regions) {
            for (Triangle t : reg.computeTriangles()) {
                g.setColor(new Color(r.nextInt(0xFFFFFF)));
                drawTriangle(g, t);
            }
        }
    }

    public static void drawTriangle(Graphics2D g, Triangle tri) {

        RoundingMode mode = RoundingMode.HALF_UP;
        int[] xPoints = new int[3];
        int[] yPoints = new int[3];

        BaseVector2f p0 = tri.getRegion().getCenter();
        BaseVector2f p1 = tri.getCorner1().getLocation();
        BaseVector2f p2 = tri.getCorner2().getLocation();

        xPoints[0] = DoubleMath.roundToInt(p0.getX(), mode);
        yPoints[0] = DoubleMath.roundToInt(p0.getY(), mode);

        xPoints[1] = DoubleMath.roundToInt(p1.getX(), mode);
        yPoints[1] = DoubleMath.roundToInt(p1.getY(), mode);

        xPoints[2] = DoubleMath.roundToInt(p2.getX(), mode);
        yPoints[2] = DoubleMath.roundToInt(p2.getY(), mode);

        g.fillPolygon(xPoints, yPoints, 3);
    }

    public static void drawSites(Graphics2D g, Graph graph) {
        List<Region> centers = graph.getRegions();

        g.setColor(Color.BLACK);
        for (Region s : centers) {
            g.fillOval((int) (s.getCenter().getX() - 2), (int) (s.getCenter().getY() - 2), 4, 4);
        }
    }

    public static void drawCorners(Graphics2D g, Graph graph) {
        g.setColor(Color.WHITE);
        for (Corner c : graph.getCorners()) {
            g.fillOval((int) (c.getLocation().getX() - 2), (int) (c.getLocation().getY() - 2), 4, 4);
        }
    }

    public static void drawBounds(Graphics2D g, Graph graph) {
        Rect2i bounds = graph.getBounds();
        g.setColor(Color.PINK);
        g.drawRect(bounds.minX(), bounds.minY(), bounds.width(), bounds.height());
    }

    public static void fillBounds(Graphics2D g, Graph graph) {
        Rect2i bounds = graph.getBounds();
        g.setColor(Color.MAGENTA);
        g.fillRect(bounds.minX() + 1, bounds.minY() + 1, bounds.width() - 1, bounds.height() - 1);
    }

}
