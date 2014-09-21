/*
 * Copyright 2013 MovingBlocks
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

package org.terasology.mapviewer.cities;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JComponent;
import javax.vecmath.Point2i;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.commonworld.Sector;
import org.terasology.commonworld.Sectors;
import org.terasology.mapviewer.camera.Camera;
import org.terasology.mapviewer.camera.CameraListener;
import org.terasology.math.geom.ImmutableVector2i;
import org.terasology.world.chunks.ChunkConstants;

/**
 * A JComponent that displays the rasterized images using a virtual camera
 * @author Martin Steiger
 */
public class CitiesViewer extends JComponent {
    private static final long serialVersionUID = 6918469720616969973L;

    private static final Logger logger = LoggerFactory.getLogger(CitiesViewer.class);

    private SwingRasterizer rasterizer;

    private Camera camera;

    public CitiesViewer(String seed, Camera camera)
    {
        this.camera = camera;

        camera.addListener(new CameraListener()
        {
            @Override
            public void onPosChange() {
                CitiesViewer.this.repaint();
            }

            @Override
            public void onZoomChange() {
                CitiesViewer.this.repaint();
            }
        });

        rasterizer = new SwingRasterizer(seed);
    }

    @Override
    protected void paintComponent(Graphics g1) {

        super.paintComponent(g1);

        Graphics2D g = (Graphics2D) g1;

        int scale = 1;

        g.setColor(Color.BLACK);

        g.scale(scale, scale);
        ImmutableVector2i cameraPos = camera.getPos();
        g.translate(cameraPos.x(), cameraPos.y());

        int camChunkOffX = (int) Math.floor(cameraPos.x() / (double) ChunkConstants.SIZE_X);
        int camChunkOffZ = (int) Math.floor(cameraPos.y() / (double) ChunkConstants.SIZE_Z);

        int numChunkX = getWidth() / (ChunkConstants.SIZE_X * scale) + 1;
        int numChunkZ = getHeight() / (ChunkConstants.SIZE_Z * scale) + 1;

        logger.debug("Drawing {}x{} chunks", numChunkX + 1, numChunkZ + 1);

        for (int z = -1; z < numChunkZ; z++) {
            for (int x = -1; x < numChunkX; x++) {
                Point2i coord = new Point2i(x - camChunkOffX, z - camChunkOffZ);
                rasterizer.rasterizeChunk(g, coord);
            }
        }

        int camOffX = (int) Math.floor(cameraPos.x() / (double) Sector.SIZE);
        int camOffZ = (int) Math.floor(cameraPos.y() / (double) Sector.SIZE);

        int numSecX = getWidth() / (Sector.SIZE * scale) + 1;
        int numSecZ = getHeight() / (Sector.SIZE * scale) + 1;

        logger.debug("Drawing {}x{} sectors", numSecX + 1, numSecZ + 1);

        for (int z = -1; z < numSecZ; z++) {
            for (int x = -1; x < numSecX; x++) {
                Point2i coord = new Point2i(x - camOffX, z - camOffZ);
                Sector sector = Sectors.getSector(coord);
                rasterizer.rasterizeSector(g, sector);
            }
        }

        rasterizer.drawDebug(g);

    }
}
