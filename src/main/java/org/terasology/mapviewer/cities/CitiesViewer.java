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
import java.math.RoundingMode;

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

import com.google.common.math.DoubleMath;

/**
 * A JComponent that displays the rasterized images using a virtual camera
 * @author Martin Steiger
 */
public class CitiesViewer extends JComponent {
    private static final long serialVersionUID = 6918469720616969973L;

    private static final Logger logger = LoggerFactory.getLogger(CitiesViewer.class);

    private SwingRasterizer rasterizer;

    private Camera camera;

    public CitiesViewer(String seed, Camera camera) {
        this.camera = camera;

        camera.addListener(new CameraListener() {
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

        g.setColor(Color.BLACK);

        double zoom = camera.getZoom();
        g.scale(zoom, zoom);
        ImmutableVector2i cameraPos = camera.getPos();
        g.translate(cameraPos.x(), cameraPos.y());

        double sizeX = ChunkConstants.SIZE_X;
        double sizeZ = ChunkConstants.SIZE_Z;
        int camChunkMinX = DoubleMath.roundToInt(cameraPos.x() / sizeX, RoundingMode.FLOOR);
        int camChunkMinZ = DoubleMath.roundToInt(cameraPos.y() / sizeZ, RoundingMode.FLOOR);

        int camChunkMaxX = DoubleMath.roundToInt((cameraPos.x() + getWidth()) / (sizeX * zoom), RoundingMode.CEILING);
        int camChunkMaxZ = DoubleMath.roundToInt((cameraPos.y() + getHeight()) / (sizeZ * zoom), RoundingMode.CEILING);

        int numChunkX = camChunkMaxX - camChunkMinX;
        int numChunkZ = camChunkMaxZ - camChunkMinZ;

        logger.debug("Drawing {}x{} chunks", numChunkX + 1, numChunkZ + 1);

        for (int z = -1; z < numChunkZ; z++) {
            for (int x = -1; x < numChunkX; x++) {
                Point2i coord = new Point2i(x - camChunkMinX, z - camChunkMinZ);
                rasterizer.rasterizeChunk(g, coord);
            }
        }

        int camOffX = DoubleMath.roundToInt(cameraPos.x() / (double) Sector.SIZE, RoundingMode.FLOOR);
        int camOffZ = DoubleMath.roundToInt(cameraPos.y() / (double) Sector.SIZE, RoundingMode.FLOOR);

        int numSecX = (int) (getWidth() / (Sector.SIZE * zoom) + 1);
        int numSecZ = (int) (getHeight() / (Sector.SIZE * zoom) + 1);

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
