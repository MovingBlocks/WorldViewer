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
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Function;

import org.terasology.math.geom.BaseVector2f;
import org.terasology.math.geom.ImmutableVector2f;
import org.terasology.math.geom.ImmutableVector2i;
import org.terasology.math.geom.Rect2i;
import org.terasology.math.geom.Vector2f;
import org.terasology.pathfinding.GeneralPathFinder;
import org.terasology.pathfinding.GeneralPathFinder.Path;
import org.terasology.polyworld.graph.Corner;
import org.terasology.polyworld.graph.Edge;
import org.terasology.polyworld.graph.Graph;
import org.terasology.polyworld.graph.GraphFacet;
import org.terasology.polyworld.graph.Triangle;
import org.terasology.world.generation.Region;
import org.terasology.world.viewer.picker.CirclePickerClosest;

/**
 * Shows the triangle under the mouse cursor.
 */
public class PathfinderOverlay extends AbstractOverlay implements WorldOverlay {

    private final Function<ImmutableVector2i, Region> regions;

    private Color fillColor = new Color(192, 32, 32, 128);
    private Color frameColor = new Color(192, 32, 32, 255);
    private BasicStroke stroke = new BasicStroke(0.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

    public enum Mode {
        CORNER,
        TRIANGLE,
        REGION,
        PATH
    }

    private Mode mode = Mode.PATH;

    public PathfinderOverlay(Function<ImmutableVector2i, Region> regionFunc) {
        regions = regionFunc;
    }

    @Override
    public void render(Graphics2D g, Rect2i area, ImmutableVector2i cursor) {
        if (cursor == null) {
            return;
        }

        Region r = regions.apply(cursor);
        GraphFacet graphs = r.getFacet(GraphFacet.class);
        g.setStroke(stroke);
        Vector2f cursorf = new Vector2f(cursor.getX(), cursor.getY());

        if (graphs != null) {
            Triangle t = graphs.getWorldTriangle(cursor.getX(), cursor.getY());
            Shape path = null;

            switch (mode) {
                case PATH: {
                    Graph gr = graphs.getWorld(cursor.getX(), cursor.getY());
                    Corner other = gr.getCorners().get(gr.getCorners().size() / 2);
                    CirclePickerClosest<Corner> picker = new CirclePickerClosest<>(cursorf, c -> 13);

                    picker.offer(t.getCorner1().getLocation(), t.getCorner1());
                    picker.offer(t.getCorner2().getLocation(), t.getCorner2());

                    if (picker.getClosest() != null) {
                        Corner c = picker.getClosest();
Collection<GeneralPathFinder.Edge<Corner>> edges = new ArrayList<>();
                        for (Edge e : gr.getEdges()) {
                            edges.add(new GeneralPathFinder.DefaultEdge<Corner>(e.getCorner0(), e.getCorner1(),
e.getCorner0().getLocation().distance(e.getCorner1().getLocation())
                                    ));
                        }
                        GeneralPathFinder<Corner> dijkstra = new GeneralPathFinder<>(edges, false);
                        Optional<Path<Corner>> opt = dijkstra.computePath(c, other, 400, v ->
(double)v.getLocation().distance(other.getLocation())
                        );
                        if (opt.isPresent()) {
                        Path<Corner> road = opt.get();

                        ImmutableVector2f p0 = road.getStart().getLocation();
                        Path2D path2 = new Path2D.Float();
                        path2.moveTo(p0.getX(), p0.getY());
                        for (int i = 1; i < road.getSequence().size(); i++) {
                            ImmutableVector2f p1 = road.getSequence().get(i).getLocation();
                            path2.lineTo(p1.getX(), p1.getY());
                        }
                        g.setColor(Color.ORANGE);
                        g.setStroke(new BasicStroke(0f));

                        for (Corner c3 : gr.getCorners()) {
                            try {
                            road.getDistance(c3);   // just test for the IllArgEx
                            ImmutableVector2f ol = c3.getLocation();
                            g.fill(new Ellipse2D.Float(ol.getX()-2, ol.getY()-2, 4, 4));
                            } catch (IllegalArgumentException e) {
                                // ignore silently
                            }
                        }
                        path = path2;
                        }
                        g.setColor(Color.GREEN);
                        g.drawOval((int)c.getLocation().getX()-400, (int)c.getLocation().getY()-400, 800, 800);
                        g.setStroke(new BasicStroke(3f));
                    }
                    }
//                    ImmutableVector2f edgep1 = t.getCorner1().getLocation();
//                    ImmutableVector2f edgep2 = t.getCorner2().getLocation();
//                    LineSegment seg = new LineSegment(edgep1, edgep2);
//                    if (seg.distanceToPoint(cursorf) < 5) {
//                        g.setColor(Color.GREEN);
//                        g.setStroke(new BasicStroke(3f));
//                        path = new Line2D.Float(edgep1.getX(), edgep1.getY(), edgep2.getX(), edgep2.getY());
//                    }

                    break;

                case CORNER:
                    CirclePickerClosest<Corner> picker = new CirclePickerClosest<>(cursorf, c -> 3);

                    picker.offer(t.getCorner1().getLocation(), t.getCorner1());
                    picker.offer(t.getCorner2().getLocation(), t.getCorner2());

                    if (picker.getClosest() != null) {
                        Corner c = picker.getClosest();
                        path = createCornerPath(c);
                    } else {
                        path = new Path2D.Float();
                    }
                    break;

                case TRIANGLE:
                    path = createTrianglePath(t);
                    break;

                case REGION:
                    path = createRegionPath(t.getRegion());
                    break;
            }

            if (path != null) {
//            g.setColor(fillColor);
//            g.fill(path);
                g.setColor(frameColor);
                g.draw(path);
            }
        }
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    private Shape createCornerPath(Corner c) {
        Path2D path = new Path2D.Float();

        float radCenter = 3;
        float radAdj = 2;

        float cx = c.getLocation().getX();
        float cy = c.getLocation().getY();

        for (Corner adj : c.getAdjacent()) {
            float adx = adj.getLocation().getX();
            float ady = adj.getLocation().getY();
            path.moveTo(cx, cy);
            path.lineTo(adx, ady);
        }

        Area result = new Area(new BasicStroke(1f).createStrokedShape(path));
        result.add(new Area(new Ellipse2D.Float(cx - radCenter, cy - radCenter, radCenter * 2, radCenter * 2)));

        for (Corner adj : c.getAdjacent()) {
            float adx = adj.getLocation().getX();
            float ady = adj.getLocation().getY();
            result.add(new Area(new Ellipse2D.Float(adx - radAdj, ady - radAdj, radAdj * 2, radAdj * 2)));
        }

        return result;
    }

    private Path2D createTrianglePath(Triangle t) {
        BaseVector2f p0 = t.getRegion().getCenter();
        BaseVector2f p1 = t.getCorner1().getLocation();
        BaseVector2f p2 = t.getCorner2().getLocation();

        Path2D path = new Path2D.Float();

        path.moveTo(p0.getX(), p0.getY());
        path.lineTo(p1.getX(), p1.getY());
        path.lineTo(p2.getX(), p2.getY());
        path.closePath();

        return path;
    }

    private Path2D createRegionPath(org.terasology.polyworld.graph.Region region) {

        Path2D path = new Path2D.Float();

        Iterator<Corner> it = region.getCorners().iterator();

        if (!it.hasNext()) {
            return path;
        }

        ImmutableVector2f p = it.next().getLocation();
        path.moveTo(p.getX(), p.getY());

        while (it.hasNext()) {
            p = it.next().getLocation();
            path.lineTo(p.getX(), p.getY());
        }

        path.closePath();
        return path;
    }

}

