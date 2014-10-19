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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.concurrent.TimeUnit;

import javax.swing.JComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.mapviewer.camera.Camera;
import org.terasology.mapviewer.camera.CameraListener;
import org.terasology.math.Rect2i;
import org.terasology.math.Region3i;
import org.terasology.math.TeraMath;
import org.terasology.math.Vector2i;
import org.terasology.math.Vector3i;
import org.terasology.world.generation.Region;
import org.terasology.world.generation.World;
import org.terasology.world.generation.facets.SurfaceHeightFacet;
import org.terasology.world.generation.facets.base.FieldFacet2D;

import com.google.common.base.Stopwatch;

/**
 * TODO Type description
 * @author Martin Steiger
 */
public final class WorldViewer extends JComponent {

    private static final Logger logger = LoggerFactory.getLogger(WorldViewer.class);

    private static final long serialVersionUID = 4178713176841691478L;

    private final Camera camera;
    private final World world;

    /**
     * @param world
     * @param camera
     */
    public WorldViewer(World world, Camera camera) {
        this.world = world;
        this.camera = camera;
        this.camera.addListener(new CameraListener() {

            @Override
            public void onZoomChange() {
                repaint();
            }

            @Override
            public void onPosChange() {
                repaint();

            }
        });
    }

    @Override
    public void paint(Graphics g1) {
        Graphics2D g = (Graphics2D) g1;

        int cx = camera.getPos().getX();
        int cy = camera.getPos().getY();
        int w = getWidth();
        int h = getHeight();
        int minX = cx - w / 2;
        int minY = cy - h / 2;
        Rect2i area = Rect2i.createFromMinAndSize(minX, minY, w, h);

        g.translate(-minX, -minY);

        Region3i area3d = Region3i.createFromMinAndSize(new Vector3i(area.minX(), 0, area.minY()), new Vector3i(area.sizeX(), 1, area.sizeY()));
        Region region = world.getWorldData(area3d);

        FieldFacet2D facet = region.getFacet(SurfaceHeightFacet.class);

        Stopwatch sw = Stopwatch.createStarted();
        for (Vector2i p : area) {
            float val = facet.getWorld(p.x, p.y);
            Color c = mapFloat(val);
            g.setColor(c);
            g.drawLine(p.x, p.y, p.x, p.y);
        }

        logger.debug("Rendered regions in {}ms.", sw.elapsed(TimeUnit.MILLISECONDS));
    }

    private Color mapFloat(float val) {
        int g = TeraMath.clamp((int)(val * 255), 0, 255);
        return new Color(g, g, g);
    }


}
