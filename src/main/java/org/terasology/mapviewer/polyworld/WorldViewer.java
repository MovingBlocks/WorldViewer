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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.math.RoundingMode;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

import com.google.common.base.Objects;
import com.google.common.base.Stopwatch;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.math.IntMath;

/**
 * TODO Type description
 * @author Martin Steiger
 */
public final class WorldViewer extends JComponent implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(WorldViewer.class);

    private static final int TILE_SIZE_X = ChunkConstants.SIZE_X * 4;
    private static final int TILE_SIZE_Y = ChunkConstants.SIZE_Z * 4;

    private static final long serialVersionUID = 4178713176841691478L;

    private BufferedImage dummyImg = new BufferedImage(TILE_SIZE_X, TILE_SIZE_Y, BufferedImage.TYPE_INT_RGB);

    private ExecutorService threadPool = Executors.newCachedThreadPool();

    private final LoadingCache<Vector2i, BufferedImage> imageCache = CacheBuilder.newBuilder().build(new CacheLoader<Vector2i, BufferedImage>() {

        @Override
        public BufferedImage load(final Vector2i chunkPos) throws Exception {
            // msteiger: there should be a more elegent way based on CacheLoader.refresh()
            // that loads missing cache values asynchronously
            threadPool.execute(new Runnable() {

                @Override
                public void run() {
                    imageCache.put(chunkPos, createImage(chunkPos));
                    repaint();
                }
            });
            return dummyImg;
        }
    });

    private final Camera camera;
    private final World world;

    private Class<? extends FieldFacet2D> facetClass;

    /**
     * @param world
     * @param camera
     */
    public WorldViewer(World world, Camera camera) {
        this.world = world;
        this.camera = camera;

        Graphics2D g = dummyImg.createGraphics();
        g.setColor(Color.GRAY);
        g.fillRect(0, 0, dummyImg.getWidth(), dummyImg.getHeight());
        g.setColor(Color.LIGHT_GRAY);
        g.drawRect(0, 0, dummyImg.getWidth() - 1, dummyImg.getHeight() - 1);
        g.dispose();

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

        int camChunkMinX = IntMath.divide(area.minX(), TILE_SIZE_X, RoundingMode.FLOOR);
        int camChunkMinZ = IntMath.divide(area.minY(), TILE_SIZE_Y, RoundingMode.FLOOR);

        int camChunkMaxX = IntMath.divide(area.maxX(), TILE_SIZE_X, RoundingMode.CEILING);
        int camChunkMaxZ = IntMath.divide(area.maxY(), TILE_SIZE_Y, RoundingMode.CEILING);

        g.setColor(new Color(128, 128, 128, 64));
        g.setStroke(new BasicStroke(0));

        for (int z = camChunkMinZ; z < camChunkMaxZ; z++) {
            for (int x = camChunkMinX; x < camChunkMaxX; x++) {
                Vector2i pos = new Vector2i(x, z);
                BufferedImage image = imageCache.getUnchecked(pos);
                g.drawImage(image, x * TILE_SIZE_X, z * TILE_SIZE_Y, null);
                g.drawRect(x * TILE_SIZE_X, z * TILE_SIZE_Y, TILE_SIZE_X, TILE_SIZE_X);
            }
        }
    }

    private BufferedImage createImage(Vector2i chunkPos) {

        if (facetClass == null)
            return dummyImg;

        Stopwatch sw = Stopwatch.createStarted();

        int minX = chunkPos.x * TILE_SIZE_X;
        int minZ = chunkPos.y * TILE_SIZE_Y;
        Region3i area3d = Region3i.createFromMinAndSize(new Vector3i(minX, 0, minZ), new Vector3i(TILE_SIZE_X, 1, TILE_SIZE_Y));
        Region region = world.getWorldData(area3d);

        FieldFacet2D facet = region.getFacet(facetClass);

        if (facet == null)
            return dummyImg;

        BufferedImage img = new BufferedImage(TILE_SIZE_X, TILE_SIZE_Y, BufferedImage.TYPE_INT_RGB);

        for (int z = 0; z < TILE_SIZE_Y; z++) {
            for (int x = 0; x < TILE_SIZE_X; x++) {
                float val = facet.getWorld(minX + x, minZ + z);
                int c = mapFloat(val);
                img.setRGB(x, z, c);
            }
        }

        if (logger.isTraceEnabled()) {
            logger.trace("Rendered regions in {}ms.", sw.elapsed(TimeUnit.MILLISECONDS));
        }

        return img;
    }

    private int mapFloat(float val) {
        int g = TeraMath.clamp((int)(val * 4), 0, 255);
        return g | (g << 8) | (g << 16);
    }

    public void setFacet(Class<? extends FieldFacet2D> clazz)
    {
        if (Objects.equal(facetClass, clazz)) {
            return;
        }

        this.facetClass = clazz;
        imageCache.invalidateAll();
        repaint();
    }

    @Override
    public void close() {
        threadPool.shutdownNow();
    }


}
