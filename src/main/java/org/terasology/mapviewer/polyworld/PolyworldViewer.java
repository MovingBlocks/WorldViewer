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

package org.terasology.mapviewer.polyworld;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Collection;

import javax.swing.JComponent;

import org.terasology.math.geom.Rect2d;
import org.terasology.polyworld.IslandGenerator;
import org.terasology.polyworld.voronoi.Graph;

import com.google.common.collect.ImmutableList;

/**
 * TODO Type description
 * @author Martin Steiger
 */
public final class PolyworldViewer extends JComponent {

    private static final long serialVersionUID = 4178713176841691478L;

    private final int width = 512;
    private final int height = 512;
    private final long seed = 9782985378925l;//System.nanoTime();
    private Rect2d bounds1 = Rect2d.createFromMinAndSize(0, 0, width, height);
    private Rect2d bounds2 = Rect2d.createFromMinAndSize(width, 0, width, height);
    private IslandGenerator sm1 = new IslandGenerator(bounds1, 1234);
    private IslandGenerator sm2 = new IslandGenerator(bounds2, 5678);
    private final Collection<IslandGenerator> sectors = ImmutableList.of(sm1, sm2);
    private final GraphPainter graphPainter = new GraphPainter();
    private final RiverPainter riverPainter = new RiverPainter();
    private final LavaPainter lavaPainter = new LavaPainter();

    @Override
    public void paint(Graphics g1) {
        Graphics2D g = (Graphics2D) g1;

        for (IslandGenerator sm : sectors) {
            Graph graph = sm.getGraph();
            BiomeColors colorFunc = new BiomeColors(sm.getBiomeModel());

            graphPainter.fillBounds(g, graph);
            graphPainter.drawPolys(g, graph, colorFunc);

            riverPainter.drawRivers(g, sm.getRiverModel(), graph);
            lavaPainter.drawLava(g, sm.getLavaModel(), graph);

//            graphPainter.drawSites(g, graph);
//            graphPainter.drawEdges(g, graph);
//            graphPainter.drawCorners(g, graph);
//            graphPainter.drawBounds(g, graph);
        }
    }
}
