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
import java.awt.image.BufferedImage;
import java.math.RoundingMode;
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
import org.terasology.world.chunks.ChunkConstants;
import org.terasology.world.generation.Region;
import org.terasology.world.generation.World;
import org.terasology.world.generation.facets.SurfaceHeightFacet;
import org.terasology.world.generation.facets.base.FieldFacet2D;

import com.google.common.base.Stopwatch;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.math.DoubleMath;
import com.google.common.math.IntMath;

/**
 * TODO Type description
 * @author Martin Steiger
 */
public final class WorldViewer extends JComponent {

    private static final Logger logger = LoggerFactory.getLogger(WorldViewer.class);

    private static final long serialVersionUID = 4178713176841691478L;

    private final LoadingCache<Vector2i, BufferedImage> imageCache = CacheBuilder.newBuilder().build(new CacheLoader<Vector2i, BufferedImage>() {

        @Override
        public BufferedImage load(Vector2i chunkPos) throws Exception {
            return createImage(chunkPos);
        }
    });

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

        int sizeX = ChunkConstants.SIZE_X;
        int sizeZ = ChunkConstants.SIZE_Z;
        int camChunkMinX = IntMath.divide(area.minX(), sizeX, RoundingMode.FLOOR);
        int camChunkMinZ = IntMath.divide(area.minY(), sizeZ, RoundingMode.FLOOR);

        int camChunkMaxX = IntMath.divide(area.maxX(), sizeX, RoundingMode.CEILING);
        int camChunkMaxZ = IntMath.divide(area.maxY(), sizeZ, RoundingMode.CEILING);

        int numChunkX = camChunkMaxX - camChunkMinX + 1;
        int numChunkZ = camChunkMaxZ - camChunkMinZ + 1;

        logger.debug("Drawing {}x{} chunks", numChunkX, numChunkZ);

        for (int z = camChunkMinZ; z < camChunkMaxZ; z++) {
            for (int x = camChunkMinX; x < camChunkMaxX; x++) {
                BufferedImage image = imageCache.getUnchecked(new Vector2i(x, z));
                g.drawImage(image, x * sizeX, z * sizeZ, null);
            }
        }
    }

    private BufferedImage createImage(Vector2i chunkPos) {

        Stopwatch sw = Stopwatch.createStarted();

        int sizeX = ChunkConstants.SIZE_X;
        int sizeZ = ChunkConstants.SIZE_Z;
        int minX = chunkPos.x * sizeX;
        int minZ = chunkPos.y * sizeZ;
        Region3i area3d = Region3i.createFromMinAndSize(new Vector3i(minX, 0, minZ), new Vector3i(sizeX, 1, sizeZ));
        Region region = world.getWorldData(area3d);

        FieldFacet2D facet = region.getFacet(SurfaceHeightFacet.class);

        BufferedImage img = new BufferedImage(sizeX, sizeZ, BufferedImage.TYPE_INT_RGB);

        for (int z = 0; z < sizeZ; z++) {
            for (int x = 0; x < sizeX; x++) {
                float val = facet.getWorld(minX + x, minZ + z);
                int c = mapFloat(val);
                img.setRGB(x, z, c);
            }
        }

        logger.debug("Rendered regions in {}ms.", sw.elapsed(TimeUnit.MILLISECONDS));

        return img;
    }

    private int mapFloat(float val) {
        int g = TeraMath.clamp((int)(val * 255), 0, 255);
        return g | (g << 8) | (g << 16);
    }


}
